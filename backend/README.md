# TechMind Classificador

API Java 21/Spring Boot para classificar artigos através do sidecar Python.

## Swagger

Com a aplicação em execução, acesse `http://localhost:8080/swagger-ui.html` para testar os endpoints pelo navegador. O contrato OpenAPI também está disponível em `http://localhost:8080/v3/api-docs`.

No Swagger, clique em `Try it out` e use o exemplo preenchido automaticamente. Se o campo estiver com um exemplo antigo, clique em `Reset` ou substitua todo o conteúdo por uma única linha JSON.

## Execução

Os comandos desta documentação devem ser executados a partir do diretório `backend`:

```powershell
cd backend
```

```bash
mvn spring-boot:run
```

Por padrão, a aplicação usa H2 em memória e um mock local do classificador, permitindo testar sem OCI, MySQL ou o arquivo `.pkl`. O console H2 fica disponível em `http://localhost:8080/h2-console` com URL `jdbc:h2:mem:techmind` e usuário `sa`.

Para usar o MySQL local:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql -Dspring-boot.run.arguments="--MYSQL_PASSWORD=sua_senha"
```

Para testar o serviço real de Ciência de Dados, execute com `ML_SERVICE_MOCK_ENABLED=false` e configure `ML_SERVICE_URL` para a URL do `techmind-ai-service` da equipe. O backend consome o endpoint oficial `POST /api/v1/artigos/processar-completo`, enviando `titulo` e `resumo`.

No perfil `oci`, o mock vem **desabilitado por padrão** (`ML_SERVICE_MOCK_ENABLED=false`); é preciso ativá-lo explicitamente via variável de ambiente caso seja necessário para um teste pontual. Não há fallback automático para o mock quando o serviço externo está indisponível — nesse caso a chamada falha com `MlIntegrationException` (HTTP 502).

A chamada ao serviço de ML usa timeouts explícitos, configuráveis via `ML_SERVICE_CONNECT_TIMEOUT_MS` (padrão 3000 ms) e `ML_SERVICE_READ_TIMEOUT_MS` (padrão 5000 ms).

Cada item de `artigosRelacionados` retornado pelo serviço de ML é repassado ao front-end com `id` (String), `titulo`, `categoria` e `scoreSimilaridade` (Double), preservando o contrato oficial. Esses relacionados ainda não são persistidos — apenas retornados na resposta da classificação.

Se `ML_SERVICE_MOCK_ENABLED=false` e `ML_SERVICE_URL` estiver vazia ou não configurada, a aplicação falha na inicialização com uma mensagem explicando o problema — não existe fallback silencioso para o mock. Na inicialização, o log informa o modo ativo (`MOCK` ou `SERVIÇO EXTERNO`), a URL base e os timeouts de conexão/leitura configurados.

## CORS

O acesso de `/api/**` é restrito às origens configuradas em `CORS_ALLOWED_ORIGINS` (lista separada por vírgulas). Nos perfis `default`/`local`/`mysql`/`integration`, o padrão é `http://localhost:5173` (porta padrão do Vite). No perfil `oci`, **não há valor padrão** — a variável é obrigatória e a aplicação falha na inicialização se não for definida, para evitar liberar CORS acidentalmente em produção. Nunca configure `*` como origem.

```powershell
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173"
```

### Testando a integração real localmente (perfil `integration`)

Existe um perfil dedicado para validar a integração Java → serviço oficial de Ciência de Dados sem depender de OCI: usa H2 em memória (mesma migration do perfil `local`) e mantém o mock **sempre desabilitado**. `ML_SERVICE_URL` é obrigatório nesse perfil — a aplicação não sobe sem ele.

Arquitetura desse cenário: serviço de Ciência de Dados em `http://localhost:8000`, backend em `http://localhost:8080`, frontend em `http://localhost:5173`.

```powershell
$env:SPRING_PROFILES_ACTIVE="integration"
$env:ML_SERVICE_MOCK_ENABLED="false"
$env:ML_SERVICE_URL="http://localhost:8000"
$env:ML_SERVICE_CONNECT_TIMEOUT_MS="3000"
$env:ML_SERVICE_READ_TIMEOUT_MS="10000"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173"

mvn spring-boot:run
```

Com o serviço de ML (real ou o sidecar em `backend/ml-service`) escutando em `http://localhost:8000`, teste com:

```powershell
curl -X POST http://localhost:8080/api/artigos/classificar `
  -H "Content-Type: application/json" `
  -d '{\"titulo\":\"Spring Boot na prática\",\"texto\":\"Artigo sobre APIs REST com Java e Spring Boot\"}'
```

Se o serviço externo estiver indisponível, a chamada falha com `MlIntegrationException` e a API responde HTTP 502 — sem cair para o mock.

Frontend (arquivo `.env` local, nunca versionado — veja `frontend/.env.example`):

```
VITE_API_URL=http://localhost:8080
VITE_SWAGGER_URL=http://localhost:8080/swagger-ui.html
```

Durante `npm run dev`, deixe `VITE_API_URL` vazio/não definido para usar o proxy do Vite (evita depender de CORS). Se definir um valor absoluto, as chamadas passam a ir direto para o backend e dependem do CORS configurado acima.

### Oracle Autonomous Database OCI

O projeto possui um perfil `oci` que usa o Oracle Autonomous Database com o wallet mantido fora do projeto. Nunca copie o wallet para este diretório e nunca salve a senha em arquivos versionados.

Depois de extrair o wallet, configure as variáveis no PowerShell. O caminho abaixo corresponde ao ambiente local deste projeto:

```powershell
$env:OCI_DATABASE_ENABLED="true"
$env:OCI_DATABASE_URL="jdbc:oracle:thin:@techminddb_high"
$env:OCI_DATABASE_USERNAME="ADMIN"
$env:OCI_DATABASE_PASSWORD="SUA_SENHA_APENAS_NESTA_SESSAO"
$env:OCI_WALLET_PATH="C:/Users/Diego Pitoco/Documents/Alura/wallet-techmind"
$env:ML_SERVICE_MOCK_ENABLED="true"

mvn clean test
mvn spring-boot:run --spring.profiles.active=oci
```

O perfil OCI usa Flyway para criar o schema e `spring.jpa.hibernate.ddl-auto=validate` para impedir alterações automáticas nas tabelas. Como o schema `ADMIN` do Autonomous Database já pode conter objetos internos, a primeira execução cria um baseline Flyway na versão `0` e aplica a migration Oracle `V1`, que cria `artigos_classificados` e `artigos_informacoes`.

Para uma execução local sem OCI, basta usar:

```powershell
mvn spring-boot:run --spring.profiles.active=local
```

Nesse modo, o H2 usa a migration específica de H2 e o classificador mock permanece habilitado.

Para subir os dois serviços, gere o jar. O `docker-compose.yml` utiliza o modelo versionado no repositório da equipe de Ciência de Dados. Os artefatos esperados são `classificador_techmind.pkl`, `config_classificador.json`, `embeddings_techmind.npy` e `artigos_com_embeddings.json`:

```bash
mvn clean package
docker compose up --build
```

Exemplo: `POST /api/artigos/classificar` com `{ "titulo": "Spring Boot", "texto": "Artigo sobre APIs Java", "autores": "Equipe TechMind", "link": "https://exemplo.com/artigo", "ano": 2026 }`.

`titulo` e `texto` são obrigatórios. `autores`, `link` e `ano` são opcionais e servem apenas para exibição/persistência. A resposta retorna `confianca` entre 0 e 1, `status` como `APROVADO` quando a confiança é maior ou igual a 0,70, além de `palavrasChave` e até três `artigosRelacionados` (similaridade mínima de 0,40).
As credenciais OCI e o wallet são configurados por `OCI_DATABASE_URL`, `OCI_DATABASE_USERNAME`, `OCI_DATABASE_PASSWORD`, `OCI_WALLET_PATH` e `OCI_DATABASE_ENABLED=true`. Um modelo sem credenciais reais está disponível em `.env.example`; o projeto não carrega arquivos `.env` automaticamente.

## Persistência

Cada classificação é salva em `artigos_classificados`. Use `GET /api/artigos` para listar registros de forma paginada e `GET /api/artigos/{id}` para consultar um registro específico.

No H2 Console, consultas úteis:

```sql
SELECT * FROM ARTIGOS_CLASSIFICADOS ORDER BY CRIADO_EM DESC;
SELECT * FROM ARTIGOS_INFORMACOES WHERE ARTIGO_ID = 1;
```

### `GET /api/artigos` — listagem paginada, com filtros

> **Mudança de contrato:** este endpoint deixou de retornar um array simples e passou a retornar um envelope de paginação (`{ "conteudo": [...], "pagina": ..., ... }`). O `frontend/src/App.jsx` já foi adaptado para ler `response.conteudo`.

Parâmetros de query (todos opcionais e combináveis; vazios são tratados como ausentes):

| Parâmetro | Descrição | Padrão |
|---|---|---|
| `page` | Página, começando em 0. Deve ser ≥ 0 (senão HTTP 400). | `0` |
| `size` | Tamanho da página. Deve ser > 0 (senão HTTP 400); valores acima de 100 são limitados a 100 (configurável via `ARTIGOS_PAGINACAO_TAMANHO_MAXIMO`). | `10` |
| `sort` | `campo,asc|desc`. Campos aceitos: `criadoEm`, `titulo`, `categoria`, `status`, `confianca`. Campo ou direção inválidos → HTTP 400. | `criadoEm,desc` |
| `titulo` | Busca parcial, ignora maiúsculas/minúsculas. | — |
| `categoria` | Filtro exato (ignora maiúsculas/minúsculas) pelos valores já persistidos. | — |
| `status` | `APROVADO` ou `PENDENTE`. Qualquer outro valor → HTTP 400. | — |
| `palavraChave` | Busca parcial na tabela `artigos_informacoes`, ignora maiúsculas/minúsculas. | — |

Exemplos:

```
GET /api/artigos?page=0&size=10
GET /api/artigos?categoria=Backend
GET /api/artigos?status=APROVADO
GET /api/artigos?titulo=spring
GET /api/artigos?palavraChave=java
GET /api/artigos?page=0&size=10&sort=criadoEm,desc&categoria=Backend
```

Resposta:

```json
{
  "conteudo": [
    {
      "id": 1,
      "titulo": "Introdução ao Spring Boot",
      "texto": "Conteúdo...",
      "autores": "Equipe TechMind",
      "link": "https://exemplo.com",
      "ano": 2026,
      "categoria": "Backend",
      "confianca": 0.89,
      "status": "APROVADO",
      "palavrasChave": ["Java", "Spring Boot"],
      "criadoEm": "2026-08-06T10:00:00"
    }
  ],
  "pagina": 0,
  "tamanho": 10,
  "totalElementos": 25,
  "totalPaginas": 3,
  "primeira": true,
  "ultima": false
}
```

`artigosRelacionados` não é incluído nesta listagem nem no `GET /api/artigos/{id}`, pois ainda não é persistido.

Filtros e paginação são resolvidos com Spring Data Specifications direto no banco (sem carregar tudo em memória), compatível com H2 e Oracle.
