"""
knowledge_manager.py

Gerenciador de conhecimento da IA do TechMind.

Responsabilidades:
- Gerar embeddings semânticos
- Extrair tags com KeyBERT
- Gerar artigos_com_embeddings.json
- Salvar embeddings_techmind.npy
- Validar consistência dos artefatos

Baseado na lógica do notebook 03_embeddings_busca_semantica.
"""

from pathlib import Path
import json
import logging

import numpy as np
import pandas as pd
from keybert import KeyBERT
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parents[2]
MODELS_DIR = BASE_DIR / "models"

EMBEDDINGS_PATH = MODELS_DIR / "embeddings_techmind.npy"
ARTIGOS_PATH = MODELS_DIR / "artigos_com_embeddings.json"


class KnowledgeManager:

    def __init__(self):
        logger.info("Carregando modelo semântico...")
        self.embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
        self.kw_model = KeyBERT(model=self.embedding_model)

    def gerar_embeddings(self, df: pd.DataFrame):
        textos = df["texto_completo"].fillna("").tolist()
        return self.embedding_model.encode(
            textos,
            show_progress_bar=False,
            convert_to_numpy=True,
        )

    def extrair_tags(self, texto: str, top_n: int = 5):
        kws = self.kw_model.extract_keywords(
            texto,
            keyphrase_ngram_range=(1, 2),
            stop_words="english",
            top_n=top_n,
        )
        return [k[0] for k in kws]

    def gerar_base_semantica(self, df: pd.DataFrame, embeddings):
        export = df.copy()

        if "tags" not in export.columns:
            export["tags"] = export["texto_completo"].apply(
                self.extrair_tags
            )

        export["embedding"] = embeddings.tolist()
        return export

    def salvar_embeddings(self, embeddings):
        np.save(EMBEDDINGS_PATH, embeddings)

    def salvar_artigos(self, df_export):
        df_export.to_json(
            ARTIGOS_PATH,
            orient="records",
            indent=2,
            force_ascii=False,
        )

    def validar_consistencia(self, embeddings, df_export):
        if len(embeddings) != len(df_export):
            raise RuntimeError(
                "Quantidade de embeddings diferente da quantidade de artigos."
            )

    def atualizar_base(self, dataset_total: pd.DataFrame):
        logger.info("Atualizando base semântica...")

        if "texto_completo" not in dataset_total.columns:
            dataset_total["texto_completo"] = (
                dataset_total["titulo"].fillna("")
                + ". "
                + dataset_total["resumo"].fillna("")
            )

        embeddings = self.gerar_embeddings(dataset_total)

        artigos = self.gerar_base_semantica(
            dataset_total,
            embeddings,
        )

        self.validar_consistencia(
            embeddings,
            artigos,
        )

        self.salvar_embeddings(
            embeddings
        )

        self.salvar_artigos(
            artigos
        )

        logger.info(
            "Conhecimento atualizado com sucesso."
        )

        return {
            "artigos": len(artigos),
            "embeddings": len(embeddings),
            "dimensoes": int(embeddings.shape[1]),
        }


knowledge_manager = KnowledgeManager()
