import json
import joblib
from datetime import datetime
from pathlib import Path
from fastapi import APIRouter, BackgroundTasks, HTTPException, Request, status
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import make_pipeline

from app.schemas.artigo import ArtigoInput, ArtigoOutput, RetreinarInput
from app.models.ai_loader import ai_engine

router = APIRouter()

BASE_DIR = Path(__file__).resolve().parent.parent.parent
DATASET_PATH = BASE_DIR / "app" / "data" / "dataset_historico.json"
MODEL_PATH = BASE_DIR / "models" / "classifier.pkl"
BACKUP_DIR = BASE_DIR / "models" / "backups"


def extrair_texto_e_categoria(item):
    """
    Normaliza os dados para garantir compatibilidade com o modelo e a base histórica.
    Busca pelas chaves reais do dataset_historico.json:
    - Texto: 'resumo_limpo' -> 'resumo_original' -> 'resumo' -> 'texto'
    - Categoria: 'categoria_projeto' -> 'categoria' -> 'classe'
    """
    # Prioridade para o texto limpo, depois resumo original ou campos alternativos
    texto = (
        item.get("resumo_limpo")
        or item.get("resumo_original")
        or item.get("resumo")
        or item.get("texto", "")
    )

    # Se houver título no payload novo, inclui no texto para dar mais contexto ao treino
    titulo = item.get("titulo", "")
    if titulo and not texto.startswith(titulo):
        texto = f"{titulo} {texto}".strip()

    # Prioridade para categoria_projeto (chave da base histórica)
    categoria = (
        item.get("categoria_projeto")
        or item.get("categoria")
        or item.get("classe", "Geral")
    )

    return {"texto": texto, "categoria": categoria}


def criar_backup_modelo_atual():
    """Cria uma cópia com timestamp do modelo ativo antes do retreinamento."""
    if MODEL_PATH.exists():
        BACKUP_DIR.mkdir(parents=True, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_path = BACKUP_DIR / f"classifier_{timestamp}.pkl"
        
        # Copia o arquivo atual para a pasta de backup
        joblib.dump(joblib.load(MODEL_PATH), backup_path)
        print(f"📦 [BACKUP] Versão anterior salva em: {backup_path.name}")
        
def executar_retreinamento_bg(novos_dados, app_state):
    """
    Função executada em segundo plano para realizar o Re-sampling,
    com versionamento automático do modelo anterior.
    """
    print(f"🔄 [BACKGROUND TASK] Retreinamento iniciado para {len(novos_dados)} novos itens...")

    try:
        DATASET_PATH.parent.mkdir(parents=True, exist_ok=True)
        MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)

        # 1. Carregar a base histórica
        if DATASET_PATH.exists():
            with open(DATASET_PATH, "r", encoding="utf-8") as f:
                base_historica = json.load(f)
        else:
            base_historica = []

        # 2. Adicionar os novos dados à base
        novos_itens_formatados = []
        prox_id = len(base_historica) + 1

        for item in novos_dados:
            dado_normalizado = extrair_texto_e_categoria(item)
            novos_itens_formatados.append({
                "id": item.get("id", prox_id),
                "titulo": item.get("titulo", ""),
                "resumo_original": item.get("resumo_original", item.get("resumo", "")),
                "autores": item.get("autores", ""),
                "ano": item.get("ano", 2026),
                "link": item.get("link", ""),
                "categoria_projeto": dado_normalizado["categoria"],
                "resumo_limpo": dado_normalizado["texto"]
            })
            prox_id += 1

        base_historica.extend(novos_itens_formatados)

        # 3. Salvar dataset atualizado
        with open(DATASET_PATH, "w", encoding="utf-8") as f:
            json.dump(base_historica, f, ensure_ascii=False, indent=2)

        # 4. Extrair X e y
        X_treino = [
            item.get("resumo_limpo") or item.get("resumo_original") or item.get("texto", "")
            for item in base_historica
        ]
        y_treino = [
            item.get("categoria_projeto") or item.get("categoria", "")
            for item in base_historica
        ]

        # 5. Treinar o novo pipeline
        novo_pipeline = make_pipeline(
            TfidfVectorizer(),
            LogisticRegression()
        )
        novo_pipeline.fit(X_treino, y_treino)

        # 🚀 VERSIONAMENTO: Gera backup do modelo atual ANTES de sobrescrever
        criar_backup_modelo_atual()

        # 6. Sobrescrever o arquivo principal usado em produção
        joblib.dump(novo_pipeline, MODEL_PATH)

        # 7. Hot-Reload em memória
        ai_engine.carregar_modelo_classificador()
        app_state.model_pipeline = ai_engine.model_pipeline

        print(f"✅ Modelo retreinado com sucesso! Total de {len(base_historica)} registros.")

    except Exception as e:
        print(f"❌ [ERRO RETREINAMENTO] Falha ao executar retreinamento: {str(e)}")


@router.post(
    "/api/v1/artigos/processar-completo",
    response_model=ArtigoOutput,
    status_code=status.HTTP_200_OK,
    summary="Processa um artigo científico",
    tags=["Artigos"]
)
def processar_conteudo(
    payload: ArtigoInput,
    request: Request
):
    """
    Classifica o artigo, extrai palavras-chave e
    recomenda artigos semelhantes.
    """

    try:

        pipeline_atual = getattr(
            request.app.state,
            "model_pipeline",
            None
        )

        return ai_engine.processar_artigo(
            titulo=payload.titulo,
            resumo=payload.resumo,
            pipeline_override=pipeline_atual
        )

    except Exception as e:

        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Erro interno no processamento de IA: {str(e)}"
        )

@router.post("/modelo/retreinar", status_code=status.HTTP_200_OK)
def disparar_retreinamento(
    payload: RetreinarInput,
    background_tasks: BackgroundTasks,
    request: Request
):
    if not payload.novos_dados:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A lista de novos dados não pode estar vazia."
        )

    novos_dados_dict = [
        item.dict() if hasattr(item, "dict") else item
        for item in payload.novos_dados
    ]

    background_tasks.add_task(
        executar_retreinamento_bg,
        novos_dados_dict,
        request.app.state
    )

    return {
        "status": "PROCESSANDO",
        "mensagem": f"Retreinamento agendado em segundo plano para {len(payload.novos_dados)} registros."
    }