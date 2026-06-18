# Contexto do Trabalho: Arquitetura REST + GraphQL

## Objetivo geral

Desenvolver um trabalho acadêmico aplicando uma arquitetura que combine REST e GraphQL.

O trabalho deve demonstrar a escolha de um cenário, a justificativa arquitetural, a implementação de um fluxo funcional e uma apresentação explicando o padrão, a implementação e a execução da solução.

## Requisitos do trabalho

## Cenário escolhido

O cenário definido para o trabalho será um sistema de pedidos para uma loja virtual simples.

A aplicação permitirá gerenciar produtos, clientes e pedidos. A parte REST será usada para operações padronizadas de CRUD, enquanto a parte GraphQL será usada para consultas flexíveis e agregadas, como detalhes completos de um pedido, histórico de pedidos de um cliente e resumo de vendas.

### Problema resolvido

Uma loja virtual precisa cadastrar produtos, registrar clientes, criar pedidos e consultar informações consolidadas para diferentes telas do sistema.

Exemplos de telas ou consumidores da API:

- Tela administrativa de cadastro de produtos.
- Tela administrativa de clientes.
- Tela de criação de pedidos.
- Tela de detalhes do pedido.
- Dashboard com resumo de vendas.
- Tela de histórico de compras do cliente.

### Dados principais

As principais entidades da aplicação serão:

- Produto.
- Cliente.
- Pedido.
- Item do pedido.

### Uso de REST no cenário

REST será usado para operações diretas e padronizadas sobre recursos.

Exemplos:

- `POST /api/produtos`: cadastrar produto.
- `GET /api/produtos`: listar produtos.
- `GET /api/produtos/{id}`: consultar produto por ID.
- `PUT /api/produtos/{id}`: atualizar produto.
- `DELETE /api/produtos/{id}`: remover produto.
- `POST /api/clientes`: cadastrar cliente.
- `GET /api/clientes`: listar clientes.
- `POST /api/pedidos`: criar pedido.
- `PUT /api/pedidos/{id}/status`: atualizar status do pedido.

REST foi escolhido para esses casos porque representa bem recursos do domínio, usa métodos HTTP conhecidos e facilita operações CRUD simples.

### Uso de GraphQL no cenário

GraphQL será usado para consultas que precisam combinar dados de várias entidades ou retornar apenas os campos necessários para cada tela.

Exemplos:

- Consultar um pedido com cliente, itens, produtos e total.
- Consultar o histórico de pedidos de um cliente.
- Consultar um dashboard com total de pedidos, faturamento e produtos mais vendidos.
- Consultar produtos retornando apenas os campos solicitados pelo cliente.

Exemplo conceitual de consulta GraphQL:

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
      produto {
        nome
        preco
      }
    }
  }
}
```

GraphQL foi escolhido para esses casos porque permite que o cliente solicite exatamente os dados necessários, reduzindo overfetching, underfetching e múltiplas chamadas REST para montar uma mesma tela.

### Justificativa da arquitetura híbrida

A combinação REST + GraphQL é adequada porque cada abordagem será usada onde tem mais vantagem.

REST ficará responsável pelas operações transacionais e previsíveis, principalmente criação, alteração e remoção de recursos.

GraphQL ficará responsável pelas consultas compostas, flexíveis e orientadas às necessidades das telas.

Na implementação, as duas interfaces podem existir na mesma aplicação:

- REST exposto em `/api`.
- GraphQL exposto em `/graphql`.
- Ambas usando a mesma camada de serviços.
- Ambas acessando a mesma base de dados.

Essa divisão evita duplicação de regra de negócio e mostra que REST e GraphQL não precisam competir. Eles podem ser usados como interfaces diferentes para necessidades diferentes.

### Exemplos reais de uso das duas abordagens

Existem projetos e produtos reais que oferecem REST e GraphQL no mesmo ecossistema de APIs.

Exemplos relevantes:

- GitHub possui documentação oficial para REST API e GraphQL API.
- GitLab possui documentação oficial para REST API e GraphQL API.
- Shopify possui Admin API em GraphQL e ainda documenta REST Admin API, embora REST tenha se tornado legado para novos aplicativos públicos.

Esses exemplos ajudam a justificar que a arquitetura híbrida não é apenas acadêmica. Ela aparece em plataformas reais, especialmente em cenários de migração, compatibilidade com clientes existentes e necessidade de consultas mais flexíveis.

### 1. Definição do cenário

É necessário definir qual será o cenário desenvolvido na solução.

O cenário deve deixar claro:

- Qual problema a aplicação resolve.
- Quem são os usuários ou sistemas envolvidos.
- Quais dados serão manipulados.
- Quais operações principais existirão.
- Onde REST será usado.
- Onde GraphQL será usado.

### 2. Justificativa da escolha do padrão

É necessário justificar a escolha da arquitetura REST + GraphQL considerando requisitos funcionais e não funcionais.

Devem ser explicados:

- Por que REST atende parte do cenário.
- Por que GraphQL atende parte do cenário.
- Por que a combinação dos dois padrões é adequada.
- Quais requisitos funcionais são atendidos pela arquitetura.
- Quais requisitos não funcionais são favorecidos pela arquitetura.

Exemplos de requisitos funcionais:

- Cadastro, consulta, atualização e remoção de entidades.
- Consulta agregada de dados.
- Filtros e buscas específicas.
- Exposição de endpoints para clientes externos.
- Consulta flexível por diferentes telas ou consumidores.

Exemplos de requisitos não funcionais:

- Manutenibilidade.
- Escalabilidade.
- Reutilização de APIs.
- Clareza na separação de responsabilidades.
- Redução de overfetching e underfetching com GraphQL.
- Simplicidade e padronização para operações CRUD com REST.

### 3. Implementação do fluxo arquitetural

É necessário implementar um fluxo que aplique o padrão arquitetural selecionado.

A implementação deve demonstrar, na prática:

- Endpoints REST para operações mais padronizadas.
- Uma API GraphQL para consultas mais flexíveis ou agregadas.
- Integração entre as camadas da aplicação.
- Pelo menos um fluxo completo de uso.

O fluxo pode envolver, por exemplo:

- Criar dados usando REST.
- Atualizar ou remover dados usando REST.
- Consultar dados relacionados ou agregados usando GraphQL.
- Executar a aplicação e demonstrar as chamadas funcionando.

### 4. Apresentação

A apresentação deve conter:

- Explicação sobre o padrão REST.
- Explicação sobre o padrão GraphQL.
- Explicação sobre a combinação REST + GraphQL.
- Motivos pelos quais o padrão atende ao cenário escolhido.
- Requisitos funcionais e não funcionais considerados.
- Detalhes da implementação.
- Trechos de código relevantes.
- Demonstração da execução da aplicação.
- Explicação do fluxo implementado.

## Entregáveis esperados

Os principais entregáveis do trabalho são:

- Código-fonte da aplicação.
- Implementação REST.
- Implementação GraphQL.
- Fluxo funcional demonstrável.
- Apresentação do trabalho.
- Explicação da arquitetura e justificativa técnica.

## Observações para o Codex

Ao continuar este trabalho, lembrar que o objetivo não é apenas criar uma API, mas demonstrar claramente o uso combinado de REST e GraphQL dentro de um cenário coerente.

O cenário ainda pode ser definido. A escolha deve facilitar a demonstração de:

- CRUD via REST.
- Consultas flexíveis ou agregadas via GraphQL.
- Comparação clara entre os papéis de REST e GraphQL.
- Justificativa com requisitos funcionais e não funcionais.

Um bom cenário deve ser simples o suficiente para implementar rapidamente, mas completo o bastante para mostrar valor arquitetural.
