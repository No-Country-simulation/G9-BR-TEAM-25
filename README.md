
### 🧠 TechMind

Plataforma de organização inteligente de conteúdos técnicos utilizando Inteligência Artificial para classificação, extração de palavras-chave e recomendação de conteúdos relacionados.

Projeto desenvolvido durante o **Hackathon TechMind**, do programa **Oracle Next Education (ONE)**, em parceria com Oracle e Alura.

---

### 📌 Sobre o projeto

O TechMind recebe conteúdos técnicos, processa as informações por meio de um serviço de Inteligência Artificial e disponibiliza os resultados em um acervo pesquisável.

O fluxo principal é:

```text
Frontend
   ↓ REST/JSON
Backend Java
   ↓ REST/JSON
Data Science / IA
   ↓
Classificação + Palavras-chave + Recomendações
   ↓
Backend
   ↓
Oracle Autonomous Database
````

### Funcionalidades

* Classificação automática de conteúdos;
* Identificação da categoria e confiança;
* Extração de palavras-chave;
* Recomendação de conteúdos semanticamente semelhantes;
* Cadastro e consulta de conteúdos;
* Filtros e paginação;
* Dashboard com estatísticas;
* API REST;
* Swagger/OpenAPI;
* Health Check;
* Persistência em Oracle Autonomous Database.

> **Importante:** a versão atual não possui retreinamento real da IA nem Human in the Loop ativo. O retreinamento é apenas um placeholder e a moderação humana não faz parte da entrega atual.

---

# 🏗️ Arquitetura

```text
                    ┌─────────────────┐
                    │     Usuário     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    Frontend     │
                    │  React + Vite   │
                    └────────┬────────┘
                             │ REST/JSON
                             ▼
                    ┌─────────────────┐
                    │     Backend     │
                    │ Java + Spring   │
                    └────────┬────────┘
                             │ REST/JSON
                             ▼
                    ┌─────────────────┐
                    │  AI Service     │
                    │ Python/FastAPI  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        Classificação   Palavras-chave   Recomendação
                             │
                             ▼
                    ┌─────────────────┐
                    │ Oracle Database │
                    │      OCI        │
                    └─────────────────┘
```

Em produção, o Frontend e o Backend são executados no **Google Cloud Run**, enquanto o **Oracle Autonomous Database** permanece na OCI.

---

# 📂 Estrutura do projeto

```text
TechMind/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── techmind-ai-service/
│   ├── app/
│   ├── models/
│   ├── data/
│   ├── requirements.txt
│   ├── Dockerfile
│   └── ...
│
└── README.md
```

---

# ⚙️ Dependências e versões

## Backend

| Tecnologia        | Versão |
| ----------------- | ------ |
| Java              | 21     |
| Spring Boot       | 3.5.3  |
| Maven             | —      |
| Spring Data JPA   | —      |
| Hibernate         | —      |
| Flyway            | —      |
| springdoc/OpenAPI | —      |
| JUnit             | 5      |
| Mockito           | —      |

## Frontend

| Tecnologia     | Versão |
| -------------- | ------ |
| React          | 19     |
| Vite           | 6      |
| Node.js        | 18+    |
| JavaScript/JSX | —      |
| Nginx          | 1.27   |

## Data Science

| Tecnologia           | Versão |
| -------------------- | ------ |
| Python               | 3.11   |
| FastAPI              | —      |
| Scikit-Learn         | —      |
| SentenceTransformers | —      |
| KeyBERT              | —      |
| NumPy                | —      |
| Joblib               | —      |

As versões não especificadas acima são definidas pelos arquivos de dependências de cada componente.

## Banco e infraestrutura

* Oracle Autonomous Database;
* H2 para desenvolvimento/testes;
* Docker;
* Docker Compose;
* Google Cloud Run;
* Google Cloud Build;
* Artifact Registry;
* Google Secret Manager;
* Oracle Cloud Infrastructure.

---

# 🚀 Como executar o projeto

## Pré-requisitos

Instale:

* Java 21;
* Maven;
* Node.js 18+;
* npm;
* Python 3.11, caso execute o serviço de IA localmente;
* Docker, caso utilize containers.

---

# 1. Executar o Backend

Entre na pasta:

```bash
cd backend
```

Execute:

```bash
mvn spring-boot:run
```

Por padrão, o ambiente local utiliza **H2 em memória**, permitindo executar o Backend sem depender do Oracle.

O Backend utiliza a porta `8080` por padrão.

---

# 2. Executar o Frontend

Entre na pasta:

```bash
cd frontend
```

Instale as dependências:

```bash
npm install
```

Execute:

```bash
npm run dev
```

O Vite disponibiliza o Frontend normalmente em:

```text
http://localhost:5173
```

Durante o desenvolvimento, as chamadas `/api` são encaminhadas pelo proxy do Vite para o Backend local.

---

# 3. Executar o serviço de Data Science

Entre na pasta:

```bash
cd techmind-ai-service
```

Crie o ambiente virtual:

### Windows

```bash
python -m venv venv
venv\Scripts\activate
```

### Linux

```bash
python3 -m venv venv
source venv/bin/activate
```

Instale as dependências:

```bash
pip install -r requirements.txt
```

Execute:

```bash
uvicorn app.main:app --reload
```

O serviço utiliza normalmente a porta `8000`.

Swagger:

```text
http://localhost:8000/docs
```

Os artefatos necessários devem estar na pasta `models/`:

```text
classifier.pkl
embeddings_techmind.npy
artigos_com_embeddings.json
config_classificador.json
```

---

# 🐳 Executar com Docker

No serviço de Data Science:

```bash
docker compose up --build
```

O projeto também utiliza Docker para empacotamento do Frontend e Backend no ambiente de produção.

---

# 🔧 Variáveis de ambiente

## Backend

Utilize o arquivo:

```text
backend/.env.example
```

Principais variáveis:

```text
SERVER_PORT=
DB_URL=
DB_USERNAME=
DB_PASSWORD=
CORS_ALLOWED_ORIGINS=
ML_SERVICE_URL=
ML_SERVICE_MOCK_ENABLED=
ML_SERVICE_CONNECT_TIMEOUT_MS=
ML_SERVICE_READ_TIMEOUT_MS=
OCI_DATABASE_ENABLED=
OCI_DATABASE_URL=
OCI_DATABASE_USERNAME=
OCI_DATABASE_PASSWORD=
OCI_WALLET_PATH=
```

Para utilizar o serviço real de Data Science:

```text
ML_SERVICE_MOCK_ENABLED=false
ML_SERVICE_URL=http://localhost:8000
```

## Frontend

Utilize:

```text
frontend/.env.example
```

Exemplo:

```text
VITE_API_URL=http://localhost:8080
VITE_SWAGGER_URL=http://localhost:8080/swagger-ui.html
```

Durante o desenvolvimento com proxy do Vite, `VITE_API_URL` pode permanecer vazio.

## Data Science

Arquivo:

```text
.env.example
```

Principais configurações:

```text
HOST=0.0.0.0
PORT=8000
BACKEND_URL=http://backend-java:8080
PYTHONUNBUFFERED=1
```

> Nunca versione senhas, credenciais, wallets ou outras informações sensíveis.

---

# 🔌 Como utilizar a API

A API Backend utiliza o padrão **REST/JSON**.

Base local:

```text
http://localhost:8080
```

## Endpoints principais

| Método | Endpoint                    | Descrição                      |
| ------ | --------------------------- | ------------------------------ |
| POST   | `/api/artigos/classificar`  | Classifica e persiste conteúdo |
| GET    | `/api/artigos`              | Lista conteúdos                |
| GET    | `/api/artigos/{id}`         | Consulta um conteúdo           |
| PUT    | `/api/artigos/{id}`         | Atualiza conteúdo              |
| DELETE | `/api/artigos/{id}`         | Exclui conteúdo                |
| GET    | `/api/artigos/estatisticas` | Retorna estatísticas           |
| GET    | `/api/artigos/health`       | Health Check                   |

A documentação interativa está disponível em:

```text
http://localhost:8080/swagger-ui.html
```

---

# 📝 Exemplo de requisição — Backend

### POST `/api/artigos/classificar`

```json
{
  "titulo": "Construindo APIs REST com Spring Boot",
  "texto": "Este artigo apresenta conceitos de APIs REST utilizando Java e Spring Boot.",
  "autores": "Equipe TechMind",
  "ano": 2026,
  "link": "https://exemplo.com/artigo"
}
```

O Backend valida os dados, envia título e conteúdo ao serviço de Data Science e persiste o resultado.

---

# 🤖 API de Inteligência Artificial

Base local:

```text
http://localhost:8000
```

## Health Check

### GET `/health`

Exemplo:

```json
{
  "status": "healthy",
  "service": "techmind-ai-service",
  "model_loaded": true
}
```

---

## Processamento de conteúdo

### POST `/api/v1/artigos/processar-completo`

### Requisição

```json
{
  "titulo": "Article title",
  "resumo": "Article abstract"
}
```

### Resposta

```json
{
  "categoria": "Artificial Intelligence",
  "probabilidade": 0.87,
  "palavrasChave": [
    "machine learning",
    "artificial intelligence",
    "classification"
  ],
  "status": "APROVADO",
  "artigosRelacionados": []
}
```

A resposta pode conter conteúdos relacionados com seus respectivos scores de similaridade.

---

# 🧠 Modelo de IA

A classificação utiliza um pipeline baseado em:

```text
TF-IDF
   ↓
LinearSVC calibrado
   ↓
Categoria + confiança
```

A extração de palavras-chave utiliza:

```text
KeyBERT
```

A recomendação utiliza:

```text
SentenceTransformer
all-MiniLM-L6-v2
        ↓
Embeddings
        ↓
Similaridade de cosseno
```

---

# 🔄 Retreinamento

Existe um endpoint preparado para futura evolução:

```text
POST /api/v1/modelo/retreinar
```

Porém, **o retreinamento real não está implementado**.

Atualmente:

* o dataset não é atualizado automaticamente;
* não existe treinamento automático;
* o modelo não é substituído automaticamente;
* o Backend não chama esse endpoint.

O endpoint é apenas um placeholder para evolução futura.

---

# 👤 Human in the Loop

A versão atual do projeto **não possui moderação humana ativa**.

Não estão implementados na entrega atual:

* aprovação manual;
* rejeição manual;
* correção manual de categoria;
* fluxo de feedback humano;
* retreinamento baseado em feedback.

Essas funcionalidades permanecem como possibilidades de evolução.

---

# 🗄️ Banco de dados

### Produção

```text
Oracle Autonomous Database
```

A conexão utiliza:

* Oracle JDBC;
* Oracle Wallet;
* mTLS.

### Desenvolvimento e testes

```text
H2 em memória
```

O schema é versionado utilizando Flyway.

---

# 🧪 Testes

O Backend utiliza:

* JUnit 5;
* Mockito;
* Spring Boot Test;
* MockMvc;
* MockRestServiceServer;
* H2.

Execute:

```bash
cd backend
mvn clean test
```

Última regressão registrada:

```text
BUILD SUCCESS
68 testes
0 falhas
0 erros
```

Para validar o Frontend:

```bash
cd frontend
npm run build
```

Também pode ser executado:

```bash
npm run lint
```

---

# ☁️ Deploy

A arquitetura de produção utiliza:

```text
Frontend
   ↓
Google Cloud Run

Backend
   ↓
Google Cloud Run

Database
   ↓
Oracle Autonomous Database
```

Pipeline:

```text
Código
  ↓
Docker
  ↓
Google Cloud Build
  ↓
Artifact Registry
  ↓
Cloud Run
```

O Oracle Autonomous Database permanece hospedado na OCI.

---

# 🌐 Links

### Aplicação

https://techmind-frontend-447002759938.southamerica-east1.run.app

### Backend

https://techmind-backend-447002759938.southamerica-east1.run.app

### Swagger

https://techmind-backend-447002759938.southamerica-east1.run.app/swagger-ui.html

### Repositório

https://github.com/No-Country-simulation/G9-BR-TEAM-25

---

# 📊 Status do projeto

| Funcionalidade             | Status                             |
| -------------------------- | ---------------------------------- |
| Frontend React             | ✅ Implementado                     |
| Backend Spring Boot        | ✅ Implementado                     |
| Classificação por IA       | ✅ Implementado                     |
| Extração de palavras-chave | ✅ Implementado                     |
| Recomendação semântica     | ✅ Implementado                     |
| Persistência Oracle        | ✅ Implementado                     |
| Acervo e filtros           | ✅ Implementado                     |
| Dashboard                  | ✅ Implementado                     |
| API REST                   | ✅ Implementado                     |
| Swagger                    | ✅ Implementado                     |
| Docker                     | ✅ Implementado                     |
| Deploy Cloud Run           | ✅ Implementado                     |
| Retreinamento real         | ❌ Não implementado                 |
| Human in the Loop          | ❌ Não implementado na versão atual |
| CI/CD automático por push  | ❌ Não implementado                 |

---

# 👥 Equipe

**Diego Reis Pitoco**
**Laís Helena Guimarães**

**Equipe G9-BR-TEAM-25**

---

# 📄 Licença

Este projeto não possui um arquivo `LICENSE` no momento.

```
```
