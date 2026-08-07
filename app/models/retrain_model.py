import os
import joblib
import pandas as pd
from typing import List, Tuple
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from app.schemas.artigo import ArtigoTreinamento
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[2]
DATASET_PATH = BASE_DIR / "data" / "dataset_treinamento.json"
MODEL_PATH = BASE_DIR / "models" / "classifier.pkl"
VECTORIZER_PATH = BASE_DIR / "models" / "vectorizer.pkl"


def carregar_ou_criar_dataset(novos_dados: List[ArtigoTreinamento]) -> pd.DataFrame:
    """Carrega o dataset existente, concatena com os novos dados e o persiste."""
    novos_df = pd.DataFrame([d.model_dump() for d in novos_dados])
    
    # Prepara o texto combinado para o TF-IDF
    novos_df["texto_completo"] = novos_df["titulo"].fillna("") + " " + novos_df["resumo"].fillna("")

    if os.path.exists(DATASET_PATH):
        df_existente = pd.read_import os
import joblib
import pandas as pd
from typing import List, Tuple
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from app.schemas.artigo import ArtigoTreinamento
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[2]
DATASET_PATH = BASE_DIR / "data" / "dataset_treinamento.json"
MODEL_PATH = BASE_DIR / "models" / "classifier.pkl"
VECTORIZER_PATH = BASE_DIR / "models" / "vectorizer.pkl"


def carregar_ou_criar_dataset(novos_dados: List[ArtigoTreinamento]) -> pd.DataFrame:
    """Carrega o dataset existente, concatena com os novos dados e o persiste."""
    novos_df = pd.DataFrame([d.model_dump() for d in novos_dados])
    
    # Prepara o texto combinado para o TF-IDF
    novos_df["texto_completo"] = novos_df["titulo"].fillna("") + " " + novos_df["resumo"].fillna("")

    if os.path.exists(DATASET_PATH):
        df_existente = pd.read_json(DATASET_PATH)
        df_final = pd.concat([df_existente, novos_df], ignore_index=True)
        # Remove duplicatas exatas caso o mesmo artigo seja enviado mais de uma vez
        df_final.drop_duplicates(subset=["titulo", "resumo"], keep="last", inplace=True)
    else:
        df_final = novos_df

    # Garante que a pasta existe antes de salvar
    os.makedirs(os.path.dirname(DATASET_PATH), exist_ok=True)
    df_final.to_csv(DATASET_PATH, index=False)
    
    return df_final


def executar_pipeline_retreinamento(novos_dados: List[ArtigoTreinamento]) -> Tuple[LogisticRegression, TfidfVectorizer]:
    """Orquestra a atualização da base de dados, retreina os modelos e salva os artefatos."""
    # 1. Atualiza e persiste o dataset
    df = carregar_ou_criar_dataset(novos_dados)

    X = df["texto_completo"]
    y = df["categoria"]

    # 2. Re-fit do Vetorizador e Classificador
    vectorizer = TfidfVectorizer(max_features=5000)
    X_vec = vectorizer.fit_transform(X)

    classifier = LogisticRegression(max_iter=1000)
    classifier.fit(X_vec, y)

    # 3. Persiste os artefatos atualizados no disco
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    os.makedirs(os.path.dirname(VECTORIZER_PATH), exist_ok=True)
    
    joblib.dump(classifier, MODEL_PATH)
    joblib.dump(vectorizer, VECTORIZER_PATH)

    return classifier, vectorizer(DATASET_PATH)
        df_final = pd.concat([df_existente, novos_df], ignore_index=True)
        # Remove duplicatas exatas caso o mesmo artigo seja enviado mais de uma vez
        df_final.drop_duplicates(subset=["titulo", "resumo"], keep="last", inplace=True)
    else:
        df_final = novos_df

    # Garante que a pasta existe antes de salvar
    os.makedirs(os.path.dirname(DATASET_PATH), exist_ok=True)
    df_final.to_csv(DATASET_PATH, index=False)
    
    return df_final


def executar_pipeline_retreinamento(novos_dados: List[ArtigoTreinamento]) -> Tuple[LogisticRegression, TfidfVectorizer]:
    """Orquestra a atualização da base de dados, retreina os modelos e salva os artefatos."""
    # 1. Atualiza e persiste o dataset
    df = carregar_ou_criar_dataset(novos_dados)

    X = df["texto_completo"]
    y = df["categoria"]

    # 2. Re-fit do Vetorizador e Classificador
    vectorizer = TfidfVectorizer(max_features=5000)
    X_vec = vectorizer.fit_transform(X)

    classifier = LogisticRegression(max_iter=1000)
    classifier.fit(X_vec, y)

    # 3. Persiste os artefatos atualizados no disco
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    os.makedirs(os.path.dirname(VECTORIZER_PATH), exist_ok=True)
    
    joblib.dump(classifier, MODEL_PATH)
    joblib.dump(vectorizer, VECTORIZER_PATH)

    return classifier, vectorizer