# Contexto Atual: Comparacao REST-only vs GraphQL-only

## Mudanca de requisito

O professor alterou o foco do trabalho. Agora o objetivo nao e mais criar uma mesma aplicacao usando REST e GraphQL juntos.

O objetivo atual e comparar duas solucoes equivalentes:

- um backend usando apenas REST API;
- um backend usando apenas GraphQL;
- um unico frontend consumindo ambos para comparar pros, contras e performance.

## Solucao implementada

O projeto foi implementado como monorepo com tres aplicacoes:

- `backend-rest`: Spring Boot REST-only na porta `8081`;
- `backend-graphql`: Spring Boot GraphQL-only na porta `8082`;
- `frontend`: React + Vite, servido na porta `5173`.

## Cenario

O dominio escolhido continua sendo uma loja virtual simples.

Entidades principais:

- Produto;
- Cliente;
- Pedido;
- ItemPedido.

Os dois backends usam H2 em memoria e recebem a mesma carga inicial de dados.

Volume atual do seed:

- 120 produtos;
- 120 clientes;
- 150 pedidos;
- 525 itens de pedido, usados como vendas no dashboard;
- 1351 unidades vendidas no total.

## Comparacao principal

REST foi implementado de forma granular para evidenciar underfetching:

- `GET /api/pedidos/{id}`;
- `GET /api/clientes/{id}`;
- `GET /api/pedidos/{id}/itens`;
- `GET /api/produtos/{id}`.

GraphQL foi implementado com consultas compostas em um endpoint unico:

- `POST /graphql`;
- `pedido(id)`;
- `cliente(id)`;
- `pedidosPorCliente(clienteId)`;
- `resumoVendas`;
- `produtos`.

## Medicoes

O frontend mede:

- tempo total no cliente com `performance.now()`;
- soma do tempo de backend pelo header `X-Backend-Time-Ms`;
- quantidade de requisicoes;
- tamanho aproximado do payload;
- trilha de requisicoes executadas.

## Cenarios da demonstracao

- Catalogo de produtos;
- Detalhe completo do pedido;
- Historico do cliente;
- Resumo de vendas.

## Pontos para a apresentacao

- REST e simples, conhecido e bom para recursos/CRUD.
- REST granular pode exigir varias chamadas para montar telas compostas.
- GraphQL permite buscar dados relacionados em uma unica query.
- GraphQL reduz underfetching e permite selecionar campos.
- GraphQL tambem exige schema, resolvers e mais cuidado com complexidade de queries.
- A comparacao deve mostrar que a melhor escolha depende do tipo de uso.

## Documentacao dos backends

REST:

- Atalho da documentacao: `http://localhost:8081/docs`;
- Swagger UI: `http://localhost:8081/swagger-ui.html`;
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`;
- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`.

GraphQL:

- Atalho da documentacao: `http://localhost:8082/docs`;
- Documentacao local com exemplos executaveis: `http://localhost:8082/docs`;
- GraphiQL: `http://localhost:8082/graphiql`;
- Schema SDL via HTTP: `http://localhost:8082/graphql/schema`;
- Schema SDL no codigo: `backend-graphql/src/main/resources/graphql/schema.graphqls`.

Observacao: `http://localhost:8082/graphql` e endpoint de API via `POST`; para abrir documentacao no navegador, usar `/docs`. Se o GraphiQL original abrir em branco, `/docs` continua funcionando por ser uma pagina local.

## Comandos principais

Backends:

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -pl backend-rest spring-boot:run
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -pl backend-graphql spring-boot:run
```

Frontend:

```powershell
cd frontend
pnpm install
node .\node_modules\vite\bin\vite.js build
node .\serve-dist.mjs
```
