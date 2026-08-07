from pydantic import BaseModel, Field
from typing import List, Optional

class ArtigoInput(BaseModel):
    """
    Payload recebido pelo endpoint
    /processar-completo.
    """
    titulo: str = Field(..., example="Introdução ao Spring Boot")
    resumo: str = Field(..., example="Conceitos básicos para criação de APIs REST com Java e Spring Boot.")

class ArtigoRecomendado(BaseModel):
    id: str = Field(..., example="4867")
    titulo: str = Field(..., example="Introdução ao Spring Boot")
    categoria: str = Field(..., example="Backend")
    scoreSimilaridade: float = Field(..., example=0.9234)

class ArtigoOutput(BaseModel):
    """
    Resultado produzido pela IA.
    """
    categoria: str = Field(..., example="Backend")
    probabilidade: float = Field(..., example=0.91)
    palavrasChave: List[str] = Field(
        ...,
        example=[
            "spring boot",
            "java",
            "rest api"
        ]
    )
    status: Optional[str] = Field(
        None,
        example="APROVADO"
    )
    artigosRelacionados: Optional[List[ArtigoRecomendado]] = None

class ArtigoTreinamento(BaseModel):
    titulo: str
    resumo: str
    categoria: str

class RetreinarInput(BaseModel):
    novos_dados: List[ArtigoTreinamento]