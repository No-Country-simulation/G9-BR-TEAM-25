import json
from pathlib import Path
from typing import Any, Dict
from keybert import KeyBERT
import joblib
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

from app.config import (
    ARTIGOS_BASE_PATH,
    CONFIG_MODEL_PATH,
    EMBEDDINGS_MATRIX_PATH,
    MODEL_PIPELINE_PATH,
)
from app.core.utils import limpar_texto

# Definição do caminho do novo modelo unificado
BASE_DIR = Path(__file__).resolve().parent.parent.parent
NOVO_MODEL_PATH = BASE_DIR / "models" / "classifier.pkl"


class AIEngine:

    def __init__(self):
        self.model_pipeline = None
        self.vectorizer = None
        self.config_model = None
        self.embeddings_matrix = None
        self.artigos_base = None
        self.encoder_model = None
        self.kw_model = None

    def carregar_recursos(self):
        """Carrega todos os modelos, artefatos e embeddings pesados na inicialização."""
        print("⏳ Carregando modelos e artefatos de IA na memória...")
        self.carregar_modelo_classificador()

        with open(CONFIG_MODEL_PATH, "r", encoding="utf-8") as f:
            self.config_model = json.load(f)

        self.embeddings_matrix = np.load(EMBEDDINGS_MATRIX_PATH)

        with open(ARTIGOS_BASE_PATH, "r", encoding="utf-8") as f:
            self.artigos_base = json.load(f)

        self.encoder_model = SentenceTransformer("all-MiniLM-L6-v2")
        self.kw_model = KeyBERT(model=self.encoder_model)
        print("✅ Recursos de IA carregados com sucesso!")

    def carregar_modelo_classificador(self):
        """Carrega ou atualiza o pipeline unificado de classificação."""
        try:
            caminho_modelo = NOVO_MODEL_PATH if NOVO_MODEL_PATH.exists() else MODEL_PIPELINE_PATH

            # Carrega diretamente o Pipeline completo (Vetorizador + Classificador)
            self.model_pipeline = joblib.load(caminho_modelo)

            print("🔄 Pipeline do classificador atualizada na memória!")
        except Exception as e:
            print(f"⚠️ Erro ao carregar a pipeline do modelo: {str(e)}")

    def processar_artigo(
        self, titulo: str, resumo: str, pipeline_override=None
    ) -> Dict[str, Any]:
        """Processa o artigo e gera a classificação, palavras-chave e recomendações.

        Permite receber um `pipeline_override` (vindo do app.state) atualizado.
        """
        titulo_limpo = limpar_texto(titulo)
        resumo_limpo = limpar_texto(resumo)
        texto_completo = f"{titulo_limpo} {resumo_limpo}".lower()

        # Seleciona o pipeline atualizado (ou o padrão em memória)
        active_pipeline = pipeline_override or self.model_pipeline

        # Garantia de que active_pipeline não seja uma tupla caso venha do pipeline_override
        if isinstance(active_pipeline, tuple):
            active_pipeline = active_pipeline[0]

        if active_pipeline is None:
            raise ValueError(
                "O modelo de classificação não está carregado na memória."
            )

        # 1. Classificação
        probas = active_pipeline.predict_proba([texto_completo])[0]
        classes = active_pipeline.classes_
        idx_max = np.argmax(probas)

        categoria_predita = str(classes[idx_max])
        score_confianca = float(probas[idx_max])

        threshold = 0.60
        if self.config_model:
            threshold = self.config_model.get(
                "limiar_confianca", self.config_model.get("threshold", 0.60)
            )

        status = (
            "APROVADO"
            if score_confianca >= threshold
            else "PENDENTE_MODERACAO"
        )

        # 2. Extração de Palavras-Chave (KeyBERT)
        keywords = self.kw_model.extract_keywords(
            texto_completo,
            keyphrase_ngram_range=(1, 2),
            stop_words="english",
            top_n=3,
        )
        palavrasChave = [kw[0] for kw in keywords]

        # 3. Recomendações por Busca Semântica
        embedding_query = self.encoder_model.encode(
            [texto_completo], convert_to_numpy=True
        )
        similaridades = cosine_similarity(
            embedding_query, self.embeddings_matrix
        )[0]
        top_3_idx = np.argsort(similaridades)[::-1][:3]

        artigos_recomendados = []
        for idx in top_3_idx:
            ref = self.artigos_base[idx]
            artigos_recomendados.append(
                {
                    "id": str(ref.get("id", idx)),
                    "titulo": str(ref.get("titulo", "")),
                    "categoria": str(
                        ref.get(
                            "categoria_projeto", ref.get("categoria", "")
                        )
                    ),
                    "scoreSimilaridade": round(float(similaridades[idx]), 4),
                }
            )

        return {
            "categoria": categoria_predita,
            "probabilidade": round(score_confianca, 4),
            "palavrasChave": palavrasChave,
            "status": status,
            "artigosRelacionados": artigos_recomendados,
        }


ai_engine = AIEngine()