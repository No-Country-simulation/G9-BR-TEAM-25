import os

# Caminho até a pasta app (onde está este config.py)
APP_DIR = os.path.dirname(os.path.abspath(__file__))
# Sobe um nível para pegar a raiz do projeto (techmind-service-ai)
BASE_DIR = os.path.dirname(APP_DIR)
# Pasta models na raiz
MODELS_DIR = os.path.join(BASE_DIR, "models")

MODEL_PIPELINE_PATH = os.path.join(MODELS_DIR, "classifier.pkl")
CONFIG_MODEL_PATH = os.path.join(MODELS_DIR, "config_classificador.json")
EMBEDDINGS_MATRIX_PATH = os.path.join(MODELS_DIR, "embeddings_techmind.npy")
ARTIGOS_BASE_PATH = os.path.join(MODELS_DIR, "artigos_com_embeddings.json")

# Configurações de ambiente (OCI / Docker / Local)
HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", 8000))
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")