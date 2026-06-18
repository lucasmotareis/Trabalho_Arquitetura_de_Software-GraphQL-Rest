# Trabalho_Arquitetura_de_Software-GraphQL-Rest

Projeto academico para comparar uma API REST granular com uma API GraphQL em um mesmo dominio de loja virtual.

## Estrutura

- `backend-rest`: Spring Boot REST-only na porta `8081`.
- `backend-graphql`: Spring Boot GraphQL-only na porta `8082`.
- `order-service`: Spring Boot interno na porta `8083`, dono de pedidos e itens.
- `inventory-service`: Spring Boot interno na porta `8084`, dono do estoque.
- `frontend`: React + Vite na porta `5173`.

No Docker Compose, a aplicacao usa tres bancos PostgreSQL separados:

- `core-db`: produtos e clientes;
- `order-db`: pedidos e itens;
- `inventory-db`: estoque dos produtos.

Em execucao local isolada, os servicos ainda usam H2 em memoria como fallback para testes.

A carga inicial possui:

- 120 produtos;
- 120 clientes;
- 150 pedidos;
- 525 itens de pedido, usados como vendas no dashboard;
- 120 registros de estoque, um por produto.

O frontend tambem mostra chamadas internas via header `X-Backend-Trace`, permitindo visualizar fluxos como `backend-graphql -> order-service` e `backend-graphql -> inventory-service`.

## Como executar

Forma recomendada, com a arquitetura completa e PostgreSQL:

```powershell
$env:BUILDX_NO_DEFAULT_ATTESTATIONS='1'
docker compose up --build -d
```

Depois acesse:

- Frontend: `http://localhost:5173`
- REST: `http://localhost:8081/api/health`
- GraphQL: `http://localhost:8082/graphql`
- Order service: `http://localhost:8083/api/health`
- Inventory service: `http://localhost:8084/api/health`

Para execucao manual com H2, suba os quatro servicos Spring em terminais separados:

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -pl inventory-service spring-boot:run
```

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -pl order-service spring-boot:run
```

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -pl backend-rest spring-boot:run
```

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -pl backend-graphql spring-boot:run
```

```powershell
cd frontend
pnpm install
.\start-dev.cmd
```

Depois acesse `http://localhost:5173`.

Se o seu ambiente nao tiver conflito de variaveis externas, `pnpm dev -- --port 5173` tambem funciona.
No Windows, se aparecer erro com `steam_master_ipc_name_override` e `null bytes`, use `.\start-dev.cmd`, pois ele limpa essa variavel antes de iniciar o Vite.

Para servir o build estatico:

```powershell
cd frontend
node .\node_modules\vite\bin\vite.js build
node .\serve-dist.mjs
```

No Windows, tambem e possivel iniciar o build estatico com:

```powershell
.\frontend\start-frontend.cmd
```

## Deploy com Docker Compose / Coolify

O projeto possui `docker-compose.yml` para deploy em VPS/Coolify.

Servicos:

- `backend-rest`: Spring Boot REST na porta interna `8081`;
- `backend-graphql`: Spring Boot GraphQL na porta interna `8082`;
- `order-service`: pedidos e itens na porta interna `8083`;
- `inventory-service`: estoque na porta interna `8084`;
- `core-db`, `order-db`, `inventory-db`: PostgreSQL 16;
- `frontend`: React buildado e servido por Nginx na porta interna `80`.

Em ambiente local:

```powershell
$env:BUILDX_NO_DEFAULT_ATTESTATIONS='1'
docker compose up --build -d
```

No Docker Desktop usado nos testes, `BUILDX_NO_DEFAULT_ATTESTATIONS=1` evita conflito de exportacao de imagem com manifest/provenance do BuildKit.

No Coolify, configure os dominios de cada servico e informe as URLs publicas no build do frontend:

```text
APP_CORS_ALLOWED_ORIGINS=https://SEU-DOMINIO-FRONTEND
VITE_REST_API_URL=https://SEU-DOMINIO-REST/api
VITE_GRAPHQL_API_URL=https://SEU-DOMINIO-GRAPHQL/graphql
```

Exemplo:

```text
APP_CORS_ALLOWED_ORIGINS=https://trabalho.seudominio.com.br
VITE_REST_API_URL=https://trabalho-rest.seudominio.com.br/api
VITE_GRAPHQL_API_URL=https://trabalho-graphql.seudominio.com.br/graphql
```

Para os dominios usados na VPS:

```text
APP_CORS_ALLOWED_ORIGINS=https://trabalho.pmto8bpm.com.br
VITE_REST_API_URL=https://trabalho-rest.pmto8bpm.com.br/api
VITE_GRAPHQL_API_URL=https://trabalho-graphql.pmto8bpm.com.br/graphql
```

Como o Vite injeta essas variaveis no build, altere esses valores antes de gerar a imagem do frontend e mande o Coolify reconstruir a imagem. Se informar apenas o dominio, como `https://trabalho-rest.pmto8bpm.com.br`, o frontend completa `/api` automaticamente.

## Endpoints principais

REST:

- `GET http://localhost:8081/api/produtos`
- `GET http://localhost:8081/api/clientes/1`
- `GET http://localhost:8081/api/clientes/1/pedidos`
- `GET http://localhost:8081/api/pedidos/1`
- `GET http://localhost:8081/api/pedidos/1/itens`

Servicos internos:

- `GET http://localhost:8083/api/pedidos`
- `GET http://localhost:8083/api/pedidos/1`
- `GET http://localhost:8083/api/clientes/1/pedidos`
- `GET http://localhost:8084/api/estoques/1`
- `GET http://localhost:8084/api/estoques?produtoIds=1,2,3`

GraphQL:

- `POST http://localhost:8082/graphql`
- Playground GraphiQL: `http://localhost:8082/graphiql`
- Schema SDL: `http://localhost:8082/graphql/schema`

## Documentacao dos backends

REST:

- Atalho da documentacao: `http://localhost:8081/docs`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`

GraphQL:

- Atalho da documentacao: `http://localhost:8082/docs`
- Documentacao local com exemplos executaveis: `http://localhost:8082/docs`
- GraphiQL original do Spring: `http://localhost:8082/graphiql`
- Schema SDL via HTTP: `http://localhost:8082/graphql/schema`
- Schema SDL no codigo: `backend-graphql/src/main/resources/graphql/schema.graphqls`

Observacao: `http://localhost:8082/graphql` e o endpoint da API e deve ser chamado com `POST`. Para abrir a interface visual no navegador, use `/docs`. Se o GraphiQL original abrir em branco no navegador, use `/docs`, que nao depende de CDN.

## Exemplos GraphQL

Catalogo de produtos:

```graphql
query Produtos {
  produtos {
    id
    nome
    preco
    estoque
  }
}
```

Detalhe completo do pedido:

```graphql
query PedidoCompleto {
  pedido(id: 1) {
    id
    status
    total
    cliente { nome email }
    itens {
      quantidade
      subtotal
      produto { nome preco }
    }
  }
}
```

Cliente com historico de pedidos:

```graphql
query ClienteComPedidos {
  cliente(id: 1) {
    id
    nome
    email
    cidade
    pedidos {
      id
      status
      total
      itens {
        quantidade
        subtotal
        produto {
          nome
          preco
        }
      }
    }
  }
}
```

Pedidos por cliente:

```graphql
query PedidosPorCliente {
  pedidosPorCliente(clienteId: 1) {
    id
    status
    total
    quantidadeItens
    itens {
      quantidade
      produto {
        nome
      }
    }
  }
}
```

Resumo de vendas:

```graphql
query ResumoVendas {
  resumoVendas {
    totalPedidos
    faturamentoTotal
    ticketMedio
    produtosMaisVendidos {
      quantidadeVendida
      faturamento
      produto {
        nome
        categoria
      }
    }
  }
}
```

Criar produto:

```graphql
mutation CriarProduto {
  criarProduto(input: {
    nome: "Monitor UltraWide 29"
    descricao: "Monitor para produtividade"
    categoria: "Eletronicos"
    preco: 1499.90
    estoque: 25
  }) {
    id
    nome
    preco
  }
}
```

Criar cliente:

```graphql
mutation CriarCliente {
  criarCliente(input: {
    nome: "Bruna Lima"
    email: "bruna.lima@email.com"
    cidade: "Sao Paulo"
  }) {
    id
    nome
    email
  }
}
```

Criar pedido:

```graphql
mutation CriarPedido {
  criarPedido(input: {
    clienteId: 1
    itens: [
      { produtoId: 1, quantidade: 1 }
      { produtoId: 2, quantidade: 2 }
    ]
  }) {
    id
    status
    total
    itens {
      quantidade
      produto {
        nome
      }
    }
  }
}
```

## Medicoes exibidas pelo frontend

- tempo total no cliente;
- soma do tempo de backend via header `X-Backend-Time-Ms`;
- quantidade de requisicoes do navegador;
- quantidade de chamadas internas entre servicos via `X-Backend-Trace`;
- tamanho aproximado do payload;
- trilha das requisicoes executadas.
