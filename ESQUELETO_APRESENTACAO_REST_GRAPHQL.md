# Esqueleto da Apresentação: REST + GraphQL

## Fontes-base

- IBM: https://www.ibm.com/br-pt/think/topics/graphql-vs-rest-api
- AWS: https://aws.amazon.com/pt/compare/the-difference-between-graphql-and-rest/

## Slide 1 - Título

**Arquitetura REST + GraphQL aplicada a um sistema de pedidos**

Conteúdo sugerido:

- Nome do trabalho.
- Nome dos integrantes.
- Disciplina.
- Professor.
- Data.

Objetivo do slide:

- Apresentar o tema e deixar claro que a solução usa as duas abordagens em conjunto.

## Slide 2 - Contexto do problema

**Cenário: loja virtual simples**

Conteúdo sugerido:

- A aplicação representa uma loja virtual que precisa gerenciar produtos, clientes e pedidos.
- O sistema deve permitir cadastros e atualizações.
- O sistema também deve permitir consultas mais completas, como detalhes de um pedido, histórico de compras e resumo de vendas.

Mensagem principal:

- Algumas operações são simples e padronizadas.
- Outras consultas precisam combinar dados de várias entidades.
- Por isso, a solução usa REST e GraphQL no mesmo projeto.

## Slide 3 - O que é REST

Conteúdo sugerido:

- REST é um estilo arquitetural para comunicação cliente-servidor.
- Usa recursos identificados por URLs.
- Usa métodos HTTP como `GET`, `POST`, `PUT`, `PATCH` e `DELETE`.
- Trabalha bem com formatos como JSON.
- É muito usado para operações CRUD.

Exemplo:

```http
GET /api/produtos
POST /api/produtos
PUT /api/produtos/1
DELETE /api/produtos/1
```

Mensagem principal:

- REST é simples, conhecido, compatível com HTTP e adequado para recursos bem definidos.

## Slide 4 - O que é GraphQL

Conteúdo sugerido:

- GraphQL é uma linguagem de consulta e um runtime para APIs.
- O cliente define exatamente quais campos deseja receber.
- Normalmente usa um único endpoint, como `/graphql`.
- Usa um esquema tipado para definir tipos, campos, queries, mutations e relacionamentos.
- Usa resolvers para buscar e montar os dados.

Exemplo:

```graphql
query {
  pedido(id: 1) {
    id
    status
    total
    cliente {
      nome
    }
    itens {
      quantidade
      produto {
        nome
        preco
      }
    }
  }
}
```

Mensagem principal:

- GraphQL é útil quando o cliente precisa buscar dados relacionados e controlar o formato da resposta.

## Slide 5 - Semelhanças entre REST e GraphQL

Conteúdo sugerido:

- Ambos permitem troca de dados entre cliente e servidor.
- Ambos podem usar HTTP.
- Ambos podem retornar JSON.
- Ambos podem ser usados para criar, consultar, atualizar e remover dados.
- Ambos ajudam a construir aplicações modulares, integráveis e escaláveis.

Mensagem principal:

- REST e GraphQL resolvem problemas parecidos, mas com modelos de uso diferentes.

## Slide 6 - Diferenças principais

Tabela sugerida:

| Aspecto | REST | GraphQL |
| --- | --- | --- |
| Modelo | Estilo arquitetural | Linguagem de consulta e runtime |
| Endpoints | Vários endpoints | Geralmente um endpoint |
| Dados retornados | Estrutura definida pelo servidor | Estrutura definida pelo cliente |
| Tipagem | Menos rígida por padrão | Esquema fortemente tipado |
| Versionamento | Comum usar `/v1`, `/v2` | Evolução por campos e depreciações |
| Erros | Códigos HTTP | Erros no corpo da resposta |
| Melhor uso | CRUD e recursos simples | Consultas flexíveis e dados relacionados |

Mensagem principal:

- REST favorece padronização por recurso.
- GraphQL favorece flexibilidade por consulta.

## Slide 7 - Problemas comuns em REST que GraphQL ajuda a reduzir

Conteúdo sugerido:

- **Overfetching:** o cliente recebe mais dados do que precisa.
- **Underfetching:** o cliente precisa fazer várias chamadas para montar uma tela.
- Estrutura fixa de resposta definida pelo servidor.
- Dificuldade maior quando a tela precisa combinar dados de vários recursos.

Exemplo no cenário:

- Para montar a tela de detalhes do pedido usando apenas REST, talvez fosse necessário chamar:
  - `GET /api/pedidos/1`
  - `GET /api/clientes/2`
  - `GET /api/produtos/10`
  - `GET /api/produtos/15`

Com GraphQL:

```graphql
query {
  pedido(id: 1) {
    id
    cliente { nome }
    itens {
      quantidade
      produto { nome preco }
    }
  }
}
```

Mensagem principal:

- GraphQL reduz múltiplas chamadas quando a tela precisa de dados relacionados.

## Slide 8 - Quando usar REST

Conteúdo sugerido:

REST é adequado quando:

- Os recursos são bem definidos.
- As operações são simples e previsíveis.
- A maioria dos clientes consome os dados de forma parecida.
- O sistema precisa de endpoints fáceis de entender e testar.
- O cache HTTP e os códigos de status são importantes.

No projeto:

- Cadastro de produtos.
- Cadastro de clientes.
- Criação de pedidos.
- Atualização de status do pedido.
- Remoção de produtos.

Mensagem principal:

- REST será usado para comandos e operações CRUD da loja virtual.

## Slide 9 - Quando usar GraphQL

Conteúdo sugerido:

GraphQL é adequado quando:

- O cliente precisa controlar os campos retornados.
- Existem dados relacionados entre várias entidades.
- As telas têm necessidades diferentes de dados.
- É importante reduzir o número de requisições.
- Há consultas agregadas ou dashboards.

No projeto:

- Detalhes completos do pedido.
- Histórico de pedidos de um cliente.
- Resumo de vendas.
- Produtos mais vendidos.

Mensagem principal:

- GraphQL será usado para consultas flexíveis e agregadas.

## Slide 10 - Arquitetura proposta

Conteúdo sugerido:

```text
Cliente Web / Mobile
        |
        | REST: /api/produtos, /api/clientes, /api/pedidos
        | GraphQL: /graphql
        |
Aplicação Backend
        |
Controllers REST + Resolvers GraphQL
        |
Camada de Serviços
        |
Repositórios / Banco de Dados
```

Mensagem principal:

- REST e GraphQL ficam como interfaces diferentes sobre a mesma regra de negócio.
- A camada de serviço evita duplicação de lógica.

## Slide 11 - Fluxo implementado

Fluxo sugerido para demonstrar:

1. Criar um cliente via REST.
2. Criar produtos via REST.
3. Criar um pedido via REST.
4. Consultar os detalhes completos do pedido via GraphQL.
5. Consultar um resumo de vendas via GraphQL.

Exemplo de chamadas REST:

```http
POST /api/clientes
POST /api/produtos
POST /api/pedidos
PUT /api/pedidos/1/status
```

Exemplo de consulta GraphQL:

```graphql
query {
  pedido(id: 1) {
    id
    status
    total
    cliente {
      nome
      email
    }
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
```

Mensagem principal:

- O fluxo mostra REST para alterar o estado do sistema e GraphQL para consultar dados compostos.

## Slide 12 - Requisitos funcionais atendidos

Conteúdo sugerido:

- Cadastrar produtos.
- Listar produtos.
- Atualizar produtos.
- Remover produtos.
- Cadastrar clientes.
- Criar pedidos.
- Atualizar status de pedidos.
- Consultar detalhes completos de pedidos.
- Consultar histórico de pedidos por cliente.
- Consultar resumo de vendas.

Mensagem principal:

- Os requisitos funcionais foram separados conforme o tipo de operação: CRUD em REST e consultas compostas em GraphQL.

## Slide 13 - Requisitos não funcionais atendidos

Conteúdo sugerido:

- **Manutenibilidade:** regras centralizadas na camada de serviços.
- **Escalabilidade:** interfaces separadas por tipo de consumo.
- **Reutilização:** REST e GraphQL usam os mesmos serviços internos.
- **Flexibilidade:** GraphQL permite respostas personalizadas por tela.
- **Padronização:** REST oferece endpoints claros para CRUD.
- **Eficiência de rede:** GraphQL reduz chamadas e dados desnecessários em consultas compostas.

Mensagem principal:

- A arquitetura híbrida melhora a organização técnica e atende necessidades diferentes da aplicação.

## Slide 14 - Detalhes da implementação REST

Conteúdo sugerido:

- Mostrar controllers REST.
- Mostrar rotas principais.
- Mostrar DTOs ou models.
- Mostrar exemplo de criação de produto, cliente ou pedido.

Placeholder de código:

```javascript
app.post("/api/produtos", produtoController.criar);
app.get("/api/produtos", produtoController.listar);
app.put("/api/produtos/:id", produtoController.atualizar);
app.delete("/api/produtos/:id", produtoController.remover);
```

Mensagem principal:

- A API REST expõe recursos claros e operações diretas.

## Slide 15 - Detalhes da implementação GraphQL

Conteúdo sugerido:

- Mostrar schema GraphQL.
- Mostrar types principais.
- Mostrar query de pedido completo.
- Mostrar resolver que chama a camada de serviço.

Placeholder de código:

```graphql
type Pedido {
  id: ID!
  status: String!
  total: Float!
  cliente: Cliente!
  itens: [ItemPedido!]!
}

type Query {
  pedido(id: ID!): Pedido
  resumoVendas: ResumoVendas!
}
```

Mensagem principal:

- A API GraphQL organiza os dados em tipos e permite consultas flexíveis.

## Slide 16 - Demonstração da execução

Conteúdo sugerido:

- Mostrar a aplicação rodando.
- Demonstrar chamadas REST em Postman, Insomnia, Thunder Client ou terminal.
- Demonstrar consulta GraphQL no playground, Apollo Sandbox ou ferramenta equivalente.

Roteiro da demo:

1. Subir a aplicação.
2. Cadastrar cliente.
3. Cadastrar produtos.
4. Criar pedido.
5. Consultar pedido completo via GraphQL.
6. Consultar resumo de vendas via GraphQL.

Mensagem principal:

- A execução comprova o uso conjunto dos dois padrões na mesma aplicação.

## Slide 17 - Conclusão

Conteúdo sugerido:

- REST e GraphQL são abordagens diferentes para projetar APIs.
- REST é adequado para operações simples, padronizadas e orientadas a recursos.
- GraphQL é adequado para consultas flexíveis, compostas e orientadas às necessidades do cliente.
- No cenário da loja virtual, a combinação das duas abordagens permite uma solução clara, didática e próxima de casos reais.

Mensagem principal:

- A arquitetura híbrida usa cada padrão onde ele faz mais sentido.

## Slide 18 - Referências

- IBM. API GraphQL versus REST: qual é a diferença?
  - https://www.ibm.com/br-pt/think/topics/graphql-vs-rest-api
- AWS. Qual é a diferença entre GraphQL e REST?
  - https://aws.amazon.com/pt/compare/the-difference-between-graphql-and-rest/
- GitHub REST API documentation.
  - https://docs.github.com/en/rest
- GitHub GraphQL API documentation.
  - https://docs.github.com/en/graphql
- GitLab REST API documentation.
  - https://docs.gitlab.com/api/rest/
- GitLab GraphQL API documentation.
  - https://docs.gitlab.com/api/graphql/

## Observações para evoluir depois

- Substituir placeholders de código pelo código real da implementação.
- Adicionar prints da execução da API REST.
- Adicionar prints da execução da consulta GraphQL.
- Ajustar quantidade de slides conforme o tempo disponível para apresentação.
- Transformar este esqueleto em PowerPoint quando a implementação estiver mais avançada.

