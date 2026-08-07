from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# Importações com o prefixo app.
from app.config import HOST, PORT
from app.api.routes import router
from app.models.ai_loader import ai_engine

@asynccontextmanager
async def lifespan(app: FastAPI):

    try:
        # Carrega todos os modelos e recursos na subida da API
        ai_engine.carregar_recursos()
        # Armazena a referência do pipeline no estado global da aplicação
        app.state.model_pipeline = ai_engine.model_pipeline
        print("✅ IA carregada.")
        yield

    except Exception as e:
        print(f"❌ Erro ao iniciar IA: {e}")
        raise

app = FastAPI(
    title="TechMind AI Service",
    description="Microserviço para classificação e extração de metadados de artigos.",
    version="1.0.0",
    lifespan=lifespan
)

# 1. Adição do Middleware CORS (Liberado para integração local e na OCI)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2. Endpoint de Healthcheck (Usado pelo Docker, Spring Boot e OCI Load Balancer)
@app.get("/health", tags=["Health"])
async def health_check():
    is_ready = hasattr(app.state, "model_pipeline") and app.state.model_pipeline is not None
    return {
        "status": "healthy" if is_ready else "unhealthy",
        "service": "techmind-ai-service",
        "model_loaded": is_ready
    }

app.include_router(router)

# Permite rodar a API direto com "python main.py" usando o Host e Porta do config
if __name__ == "__main__":
    uvicorn.run("app.main:app", host=HOST, port=PORT, reload=True)