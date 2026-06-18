# Trabalho_Arquitetura_de_Software-GraphQL-Rest

Projeto academico para comparar uma API REST granular com uma API GraphQL em um mesmo dominio de loja virtual.

## Estrutura

- `backend-rest`: Spring Boot REST-only na porta `8081`.
- `backend-graphql`: Spring Boot GraphQL-only na porta `8082`.
- `frontend`: React + Vite na porta `5173`.

Os dois backends usam H2 em memoria e recebem a mesma carga inicial:

- 120 produtos;
- 120 clientes;
- 150 pedidos;
- 525 itens de pedido, usados como vendas no dashboard;
- 1351 unidades vendidas no total.

## Como executar

Em tres terminais:

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

## Endpoints principais

REST:

- `GET http://localhost:8081/api/produtos`
- `GET http://localhost:8081/api/clientes/1`
- `GET http://localhost:8081/api/clientes/1/pedidos`
- `GET http://localhost:8081/api/pedidos/1`
- `GET http://localhost:8081/api/pedidos/1/itens`

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
- quantidade de requisicoes;
- tamanho aproximado do payload;
- trilha das requisicoes executadas.
