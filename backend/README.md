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

Para testar o sidecar real, execute com `ML_SERVICE_MOCK_ENABLED=false` e disponibilize o `classificador_techmind.pkl`.

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

Para subir os dois serviços, gere o jar. O `docker-compose.yml` utiliza o modelo versionado em `../techmind-ai-service/models/classificador_techmind.pkl`:

```bash
mvn clean package
docker compose up --build
```

Exemplo: `POST /api/artigos/classificar` com `{ "titulo": "Spring Boot", "texto": "Artigo sobre APIs Java", "autores": "Equipe TechMind", "link": "https://exemplo.com/artigo", "ano": 2026 }`.

`titulo` e `texto` são obrigatórios. `autores`, `link` e `ano` são opcionais e servem apenas para exibição/persistência. A resposta retorna `confianca` entre 0 e 1, `status` como `APROVADO` quando a confiança é maior ou igual a 0,70, além de `palavrasChave` e até três `artigosRelacionados` (similaridade mínima de 0,40).
As credenciais OCI e o wallet são configurados por `OCI_DATABASE_URL`, `OCI_DATABASE_USERNAME`, `OCI_DATABASE_PASSWORD`, `OCI_WALLET_PATH` e `OCI_DATABASE_ENABLED=true`. Um modelo sem credenciais reais está disponível em `.env.example`; o projeto não carrega arquivos `.env` automaticamente.

## Persistência

Cada classificação é salva em `artigos_classificados`. Use `GET /api/artigos` para listar registros e `GET /api/artigos/{id}` para consultar um registro específico.

No H2 Console, consultas úteis:

```sql
SELECT * FROM ARTIGOS_CLASSIFICADOS ORDER BY CRIADO_EM DESC;
SELECT * FROM ARTIGOS_INFORMACOES WHERE ARTIGO_ID = 1;
```
