import os
import pickle
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="TechMind ML Sidecar")
MODEL_PATH = os.getenv("MODEL_PATH", "/app/classificador_techmind.pkl")
model: Any = None


class PredictionRequest(BaseModel):
    titulo: str
    texto: str


class OfficialPredictionRequest(BaseModel):
    titulo: str
    resumo: str


@app.on_event("startup")
def load_model() -> None:
    global model
    try:
        with open(MODEL_PATH, "rb") as file:
            model = pickle.load(file)
    except FileNotFoundError:
        model = None


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP" if model is not None else "DOWN"}


def _predict(titulo: str, texto: str) -> dict[str, Any]:
    if model is None:
        raise HTTPException(status_code=503, detail="Modelo não carregado")
    # O modelo recebe exclusivamente titulo + texto; metadados ficam fora do pipeline.
    combined_text = f"{titulo.strip()}\n{texto.strip()}"
    probabilities = model.predict_proba([combined_text])[0]
    index = int(probabilities.argmax())
    category = str(model.classes_[index])
    keywords = [word for word in ("Java", "Spring Boot", "Python", "AWS", "Docker", "React")
                if word.lower() in combined_text.lower()]
    confidence = float(probabilities[index])
    return {
        "categoria": category,
        "confianca": confidence,
        "palavrasChave": keywords,
        "artigosRelacionados": []
    }


@app.post("/predict")
def predict(request: PredictionRequest) -> dict[str, Any]:
    return _predict(request.titulo, request.texto)


@app.post("/api/v1/artigos/processar-completo")
def processar_completo(request: OfficialPredictionRequest) -> dict[str, Any]:
    """Contrato compatível com o serviço oficial de Ciência de Dados."""
    return _predict(request.titulo, request.resumo)
