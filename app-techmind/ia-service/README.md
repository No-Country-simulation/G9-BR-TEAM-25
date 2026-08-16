# TechMind AI Service

Microserviço de Inteligência Artificial desenvolvido para o **Hackathon TechMind**, responsável pela classificação automática de artigos científicos, extração de palavras-chave e recomendação de artigos relacionados.

---

# Funcionalidades

* Classificação automática de artigos científicos
* Extração automática de palavras-chave (KeyBERT)
* Recomendação de artigos semanticamente semelhantes
* Endpoint REST utilizando FastAPI
* Health Check para monitoramento
* Swagger/OpenAPI integrado
* Pronto para Docker e Oracle Cloud Infrastructure (OCI)
* Estrutura preparada para aprendizado contínuo (Retraining)

---

# Arquitetura

```text
Frontend
      │
      ▼
Backend Java (Spring Boot)
      │
      ▼
TechMind AI Service (FastAPI)
      │
      ├── Classificação
      ├── Extração de Palavras-chave
      └── Recomendação Semântica
```

---

# Tecnologias

* Python 3.11
* FastAPI
* Scikit-Learn
* SentenceTransformers
* KeyBERT
* NumPy
* Joblib
* Docker
* Docker Compose

---

# Estrutura do Projeto

```text
techmind-ai-service/

├── app/
│   ├── api/
│   ├── core/
│   ├── models/
│   ├── schemas/
│   ├── services/
│   ├── main.py
│   └── config.py
│
├── models/
│   ├── classifier.pkl
│   ├── embeddings_techmind.npy
│   ├── artigos_com_embeddings.json
│   └── config_classificador.json
│
├── data/
│
├── Dockerfile
├── docker-compose.yml
├── requirements.txt
├── .env.example
└── README.md
```

---

# Instalação

## 1. Clonar o projeto

```bash
git clone <repositorio>
cd techmind-ai-service
```

---

## 2. Criar ambiente virtual

Windows

```bash
python -m venv venv
```

Linux

```bash
python3 -m venv venv
```

---

## 3. Ativar ambiente

Windows

```bash
venv\Scripts\activate
```

Linux

```bash
source venv/bin/activate
```

---

## 4. Instalar dependências

```bash
pip install -r requirements.txt
```

---

## 5. Executar a API

```bash
uvicorn app.main:app --reload
```

---

# Docker

Construir a imagem

```bash
docker compose up --build
```

---

# Endpoints

## Health Check

```http
GET /health
```

Resposta

```json
{
    "status":"healthy",
    "service":"techmind-ai-service",
    "model_loaded":true
}
```

---

## Processamento de Artigos

```http
POST /api/v1/artigos/processar-completo
```

### Request

```json
{
    "titulo":"Article title",
    "resumo":"Article abstract"
}
```

### Response

```json
{
    "categoria":"Artificial Intelligence",
    "probabilidade":0.87,
    "palavrasChave":[
        "...",
        "...",
        "..."
    ],
    "status":"APROVADO",
    "artigosRelacionados":[]
}
```

---

# Swagger

Após iniciar a aplicação:

```
http://localhost:8000/docs
```

---

# Modelos Utilizados

A API utiliza os seguintes artefatos:

* classifier.pkl
* embeddings_techmind.npy
* artigos_com_embeddings.json
* config_classificador.json

Todos devem estar presentes na pasta **models/**.

---

# Configuração

As configurações são realizadas por variáveis de ambiente.

Arquivo de exemplo:

```
.env.example
```

Variáveis:

```text
HOST=0.0.0.0
PORT=8000
BACKEND_URL=http://backend-java:8080
PYTHONUNBUFFERED=1
```

---

# Tempo de Resposta

Nos testes realizados localmente:

* Artigos curtos: aproximadamente 2 segundos
* Artigos técnicos: entre 5 e 7 segundos

O tempo depende principalmente da complexidade linguística do texto.

---

# Deploy

A aplicação está preparada para execução utilizando:

* Docker
* Docker Compose
* Oracle Cloud Infrastructure (OCI)

---

# Roadmap

* Aprendizado contínuo (Retraining)
* Atualização incremental do dataset
* Versionamento automático do modelo
* Rollback automático de modelos
* Métrica composta para validação do retreinamento

---

# Equipe

Projeto desenvolvido para o Hackathon TechMind.

Microserviço responsável pela camada de Inteligência Artificial da plataforma.
