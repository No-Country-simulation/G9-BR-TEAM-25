# Checklist de demonstração — TechMind

Roteiro manual para validar a integração completa: **React → Spring Boot → API externa de Ciência de Dados → Oracle OCI → React**.

Arquitetura do cenário local de integração:

| Serviço | URL |
|---|---|
| Serviço de Ciência de Dados | `http://localhost:8000` |
| Backend (Spring Boot) | `http://localhost:8080` |
| Frontend (React/Vite) | `http://localhost:5173` |

## Variáveis de ambiente (backend, perfil `integration`)

```powershell
$env:SPRING_PROFILES_ACTIVE="integration"
$env:ML_SERVICE_MOCK_ENABLED="false"
$env:ML_SERVICE_URL="http://localhost:8000"
$env:ML_SERVICE_CONNECT_TIMEOUT_MS="3000"
$env:ML_SERVICE_READ_TIMEOUT_MS="10000"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173"

mvn spring-boot:run
```

## Variáveis de ambiente (frontend)

Copie `frontend/.env.example` para `frontend/.env` (arquivo local, nunca versionado):

```
VITE_API_URL=http://localhost:8080
VITE_SWAGGER_URL=http://localhost:8080/swagger-ui.html
```

> Durante `npm run dev`, `VITE_API_URL` pode ficar vazio para usar o proxy do Vite em vez de depender do CORS.

## Checklist

1. **Iniciar o serviço de Ciência de Dados** na porta `8000` (serviço oficial da outra equipe, ou o sidecar local em `backend/ml-service` para simulação).
2. **Confirmar health ou Swagger do serviço** — ex.: `GET http://localhost:8000/health` (sidecar local) ou o endpoint de saúde equivalente do serviço oficial.
3. **Iniciar o backend sem mock** com as variáveis acima (`mvn spring-boot:run`).
4. **Confirmar no log de inicialização a linha `modo=SERVIÇO EXTERNO`** (emitida por `PredicaoClient`), garantindo que o mock está realmente desligado.
5. **Iniciar o frontend**: `cd frontend && npm run dev`, abrir `http://localhost:5173`.
6. **Verificar "API online"** no topo da página (consulta `GET /api/artigos/health` — indica apenas a disponibilidade do backend Java, não do serviço de ML nem do Oracle).
7. **Classificar um artigo** pelo formulário (título + texto obrigatórios).
8. **Verificar a categoria** exibida no painel de resultado.
9. **Verificar a confiança** (percentual e barra de progresso).
10. **Verificar as palavras-chave** exibidas como chips.
11. **Verificar os artigos relacionados** no mesmo painel de resultado (título, categoria, `scoreSimilaridade` em %) — só aparecem nessa resposta imediata, pois ainda não são persistidos.
12. **Verificar o histórico paginado** na seção "Conteúdos classificados", com os controles Anterior/Próxima e "Página X de Y".
13. **Testar o filtro por categoria** (campo "Categoria" + botão "Filtrar").
14. **Testar o filtro por status** (`APROVADO` ou `PENDENTE`).
15. **Abrir os detalhes** de um item do histórico (clique no item) e conferir título, texto, autores, link, ano, categoria, confiança, status, palavras-chave e data de criação.
16. **Confirmar a persistência no Oracle OCI** (quando o perfil `oci` estiver em uso): consultar `SELECT * FROM ARTIGOS_CLASSIFICADOS ORDER BY CRIADO_EM DESC` no schema configurado, ou repetir o passo 15 após reiniciar o backend — o registro deve continuar disponível.
17. **Abrir o Swagger** (`http://localhost:8080/swagger-ui.html` ou via `VITE_SWAGGER_URL`) e testar os endpoints diretamente.

## Exemplos de teste (classificar um artigo)

Os três exemplos abaixo usam `POST /api/artigos/classificar`. `titulo` e `texto` são obrigatórios; `autores`, `link` e `ano` são opcionais.

**Importante:** `categoria`, `confianca`, `palavrasChave` e `artigosRelacionados` são gerados pelo modelo de Ciência de Dados — os valores abaixo são **aproximados** (correspondem ao comportamento do mock local de desenvolvimento) e podem variar com o serviço/modelo real.

### Exemplo 1 — Backend (Java / Spring Boot / API REST)

Requisição:

```json
{
  "titulo": "Construindo APIs REST com Spring Boot",
  "texto": "Artigo sobre como criar endpoints REST em Java usando Spring Boot, Spring Data JPA e boas práticas de arquitetura em camadas.",
  "autores": "Equipe TechMind",
  "link": "https://exemplo.com/spring-boot-api",
  "ano": 2026
}
```

Resposta aproximada:

```json
{
  "categoria": "Backend",
  "confianca": 0.89,
  "status": "APROVADO",
  "palavrasChave": ["Java", "Spring Boot", "API REST"],
  "artigosRelacionados": [
    { "id": "104", "titulo": "Construindo Microsservicos com Java e Spring Cloud", "categoria": "Backend", "scoreSimilaridade": 0.91 }
  ]
}
```

### Exemplo 2 — Frontend (React / Vite / componentes)

Requisição:

```json
{
  "titulo": "Componentes reutilizáveis com React e Vite",
  "texto": "Artigo sobre como estruturar componentes React reutilizáveis, hooks personalizados e configuração de build com Vite.",
  "autores": "Equipe TechMind",
  "link": "https://exemplo.com/react-vite-componentes",
  "ano": 2026
}
```

Resposta aproximada:

```json
{
  "categoria": "Frontend",
  "confianca": 0.82,
  "status": "APROVADO",
  "palavrasChave": ["React", "JavaScript"],
  "artigosRelacionados": [
    { "id": "201", "titulo": "Componentes reutilizaveis com React", "categoria": "Frontend", "scoreSimilaridade": 0.88 }
  ]
}
```

### Exemplo 3 — Cloud/DevOps (Docker / OCI / deploy)

Requisição:

```json
{
  "titulo": "Deploy de APIs Java na Oracle Cloud Infrastructure com Docker",
  "texto": "Artigo sobre containerização com Docker e implantação de aplicações Java na Oracle Cloud Infrastructure, incluindo armazenamento em nuvem e bancos de dados gerenciados.",
  "autores": "Equipe TechMind",
  "link": "https://exemplo.com/oci-docker-deploy",
  "ano": 2026
}
```

Resposta aproximada:

```json
{
  "categoria": "Cloud Computing",
  "confianca": 0.76,
  "status": "APROVADO",
  "palavrasChave": ["AWS"],
  "artigosRelacionados": [
    { "id": "407", "titulo": "Fundamentos de computacao em nuvem", "categoria": "Cloud Computing", "scoreSimilaridade": 0.81 }
  ]
}
```
