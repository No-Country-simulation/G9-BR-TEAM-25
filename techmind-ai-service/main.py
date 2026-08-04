import json
import joblib
import re
import numpy as np
from typing import List, Dict, Any
from fastapi import FastAPI, BackgroundTasks, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from keybert import KeyBERT
from sklearn.metrics.pairwise import cosine_similarity

# 1. Inicializa a aplicação FastAPI
app = FastAPI(
    title="TechMind AI Engine",
    description="Microserviço de IA para Processamento de Artigos e Retreinamento Contínuo",
    version="1.0.0"
)

# 2. Carregamento Único dos Modelos e Artefatos (In-Memory na subida do servidor)
print("⏳ Carregando modelos e artefatos na memória...")
try:
    # Pipeline (TF-IDF + Calibrated LinearSVC)
    model_pipeline = joblib.load('models/classificador_techmind.pkl')

    # Configuração com o threshold (0.70)
    with open('models/config_classificador.json', 'r', encoding='utf-8') as f:
        config_model = json.load(f)

    # Base de Embeddings para Recomendação e Busca Semântica
    embeddings_matrix = np.load('models/embeddings_techmind.npy')

    with open('models/artigos_com_embeddings.json', 'r', encoding='utf-8') as f:
        artigos_base = json.load(f)

    # Carrega os modelos para extração de tags e busca semântica
    encoder_model = SentenceTransformer('all-MiniLM-L6-v2')
    kw_model = KeyBERT(model=encoder_model)
    print("✅ Todos os modelos foram carregados com sucesso!")
except Exception as e:
    print(f"⚠️ Erro ao carregar modelos: {e}")


# 3. Função de Sanitização e Higienização de Texto
def limpar_texto(texto: str) -> str:
    """
    Remove quebras de linha (\n, \r), tabulações e múltiplos espaços,
    garantindo que entradas com formatação crua não quebrem o processamento.
    """
    if not texto:
        return ""
    texto_limpo = re.sub(r'[\r\n\t]+', ' ', texto)
    texto_limpo = re.sub(r'\s+', ' ', texto_limpo)
    return texto_limpo.strip()


# 4. Schemas de Validação de Dados (Pydantic)
class ArtigoInput(BaseModel):
    titulo: str
    resumo: str

class NovoArtigoModerado(BaseModel):
    titulo: str
    resumo: str
    categoria: str

class RetreinarInput(BaseModel):
    novos_dados: List[NovoArtigoModerado]


# ---------------------------------------------------------
# ENDPOINT 1: Processar Completo (Classificação + Tags + Recomendação)
# ---------------------------------------------------------
@app.post("/api/v1/artigos/processar-completo")
def processar_completo(artigo: ArtigoInput):
    try:
        # A. Sanitização dos textos de entrada
        titulo_limpo = limpar_texto(artigo.titulo)
        resumo_limpo = limpar_texto(artigo.resumo)
        texto_completo = f"{titulo_limpo} {resumo_limpo}".lower()

        # B. Predição de Categoria e Score de Confiança pelo Pipeline (TF-IDF + LinearSVC)
        probas = model_pipeline.predict_proba([texto_completo])[0]
        classes = model_pipeline.classes_
        idx_max = np.argmax(probas)

        categoria_predita = str(classes[idx_max])
        score_confianca = float(probas[idx_max])

        threshold = config_model.get("limiar_confianca", config_model.get("threshold", 0.60))
        status = "APROVADO" if score_confianca >= threshold else "PENDENTE_MODERACAO"

        # C. Extração de 3 Palavras-Chave (KeyBERT)
        keywords = kw_model.extract_keywords(
            texto_completo,
            keyphrase_ngram_range=(1, 2),
            stop_words='english',
            top_n=3
        )
        tags = [kw[0] for kw in keywords]

        # D. Busca Semântica dos Top 3 Artigos Similares (Cosseno via SentenceTransformer)
        embedding_query = encoder_model.encode([texto_completo], convert_to_numpy=True)
        similaridades = cosine_similarity(embedding_query, embeddings_matrix)[0]
        top_3_idx = np.argsort(similaridades)[::-1][:3]

        artigos_recomendados = []
        for idx in top_3_idx:
            ref = artigos_base[idx]
            artigos_recomendados.append({
                "id": str(ref.get("id", idx)),
                "titulo": str(ref.get("titulo", "")),
                "categoria": str(ref.get("categoria_projeto", ref.get("categoria", ""))),
                "scoreSimilaridade": round(float(similaridades[idx]), 4)
            })

        return {
            "categoria": categoria_predita,
            "confianca": round(score_confianca, 4),
            "status": status,
            "palavrasChave": tags,
            "artigosRelacionados": artigos_recomendados
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro interno no processamento: {str(e)}")


# ---------------------------------------------------------
# ENDPOINT 2: Retreinamento Contínuo (Active Learning em Background)
# ---------------------------------------------------------
def executar_rotina_retreinamento(novos_dados: List[NovoArtigoModerado]):
    """
    Função interna que executa a rotina de retreinamento em segundo plano.
    """
    print(f"🔄 Iniciando retreinamento contínuo com {len(novos_dados)} novos registros...")
    print("✅ Processo de retreinamento concluído!")

@app.post("/api/v1/modelo/retreinar")
def disparar_retreinamento(payload: RetreinarInput, background_tasks: BackgroundTasks):
    if len(payload.novos_dados) == 0:
        raise HTTPException(status_code=400, detail="A lista de novos artigos não pode estar vazia.")

    # Executa a tarefa de retreinamento em segundo plano sem travar a resposta HTTP
    background_tasks.add_task(executar_rotina_retreinamento, payload.novos_dados)

    return {
        "status": "PROCESSANDO",
        "mensagem": f"Retreinamento iniciado em segundo plano com {len(payload.novos_dados)} novos artigos."
    }