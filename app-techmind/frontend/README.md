# TechMind Frontend

Interface React para classificação, busca e consulta de artigos pelo backend Java do TechMind.

## Execução local

Com o backend executando em `http://localhost:8080`:

```bash
npm install
npm run dev
```

Abra o endereço exibido pelo Vite, normalmente `http://localhost:5173`.

Durante o desenvolvimento, o Vite encaminha as chamadas `/api` para o backend local (proxy configurado em `vite.config.js`), evitando problemas de CORS. Nesse cenário, **deixe `VITE_API_URL` sem definir** (ou vazio) — é assim que o cliente HTTP em `src/api/artigosApi.js` sabe para usar caminhos relativos e passar pelo proxy.

## Variáveis de ambiente

Copie `.env.example` para `.env` e ajuste se necessário:

```
VITE_API_URL=http://localhost:8080
VITE_SWAGGER_URL=http://localhost:8080/swagger-ui.html
```

- `VITE_API_URL`: só é necessário quando o front **não** passa pelo proxy do Vite (ex.: build de produção servido separadamente do backend). Se definido durante `npm run dev`, as chamadas passam a ir direto para essa URL — o backend atual não tem CORS habilitado, então isso só funciona se front e backend estiverem na mesma origem ou atrás de um proxy reverso.
- `VITE_SWAGGER_URL`: usada pelo link "Swagger" no topo da página.

## Build e lint

```bash
npm run build
npm run lint
```

## Funcionalidades

- **Classificação**: formulário envia `POST /api/artigos/classificar` e exibe categoria, confiança, status e palavras-chave retornados pelo backend.
- **Conteúdos relacionados**: exibidos apenas no resultado imediato da classificação (com `scoreSimilaridade` formatado em %), pois o backend ainda não os persiste. Eles **não aparecem** ao recarregar a listagem ou ao abrir os detalhes de um artigo salvo.
- **Listagem paginada**: consome `GET /api/artigos`, que retorna um envelope `{ conteudo, pagina, tamanho, totalElementos, totalPaginas, primeira, ultima }` — não mais um array simples. Os controles "Anterior"/"Próxima" desabilitam nos limites e preservam os filtros ativos.
- **Filtros**: título (parcial), categoria, status (`APROVADO`/`PENDENTE`) e palavra-chave, combináveis, sempre resolvidos pelo backend via query string (nunca filtrados só em memória). Aplicar filtros volta para a primeira página; "Limpar filtros" também.
- **Detalhes**: clicar em um item do histórico busca `GET /api/artigos/{id}` e mostra título, texto, autores, link, ano, categoria, confiança, status, palavras-chave e data de criação em um painel expansível (sem rotas).
- **Status da API**: o indicador "API online/offline" verifica apenas `GET /api/artigos/health`, isto é, a disponibilidade do backend Java — não indica se o serviço de Ciência de Dados ou o Oracle estão disponíveis.
