# =====================================================
# TechMind AI Service
# Dockerfile
# =====================================================

FROM python:3.11-slim

# Evita geração de arquivos .pyc
ENV PYTHONDONTWRITEBYTECODE=1

# Logs em tempo real
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# Dependências do sistema
RUN apt-get update && apt-get install -y \
    build-essential \
    gcc \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copia apenas o requirements primeiro (cache)
COPY requirements.txt .

RUN pip install --no-cache-dir --upgrade pip

RUN pip install --no-cache-dir -r requirements.txt

# Copia o restante da aplicação
COPY . .

# Cria diretórios persistentes
RUN mkdir -p /app/models /app/data

EXPOSE 8000

CMD [
    "uvicorn",
    "app.main:app",
    "--host",
    "0.0.0.0",
    "--port",
    "8000"
]