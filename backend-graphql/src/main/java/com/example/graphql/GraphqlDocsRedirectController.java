package com.example.graphql;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphqlDocsRedirectController {
    @GetMapping(value = {"/", "/docs"}, produces = MediaType.TEXT_HTML_VALUE)
    public String docs() {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Documentacao GraphQL - Loja Virtual</title>
                  <style>
                    :root {
                      color-scheme: light;
                      font-family: Arial, Helvetica, sans-serif;
                      color: #152033;
                      background: #eef4f7;
                    }
                    body {
                      margin: 0;
                      padding: 32px;
                    }
                    main {
                      max-width: 1180px;
                      margin: 0 auto;
                    }
                    header {
                      margin-bottom: 24px;
                    }
                    h1 {
                      margin: 0 0 8px;
                      font-size: 40px;
                    }
                    h2 {
                      margin: 0 0 14px;
                      font-size: 22px;
                    }
                    p {
                      margin: 0;
                      color: #526172;
                      line-height: 1.5;
                    }
                    a {
                      color: #0f6f86;
                      font-weight: 700;
                    }
                    .grid {
                      display: grid;
                      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
                      gap: 18px;
                    }
                    section {
                      background: #ffffff;
                      border: 1px solid #d7e2ea;
                      border-radius: 8px;
                      padding: 18px;
                      box-shadow: 0 14px 35px rgba(21, 32, 51, 0.08);
                    }
                    .actions {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 8px;
                      margin-bottom: 12px;
                    }
                    button {
                      border: 0;
                      border-radius: 6px;
                      background: #156f86;
                      color: #ffffff;
                      cursor: pointer;
                      font-weight: 700;
                      padding: 10px 12px;
                    }
                    button.secondary {
                      background: #e7eef4;
                      color: #152033;
                    }
                    textarea,
                    pre {
                      box-sizing: border-box;
                      width: 100%;
                      min-height: 360px;
                      margin: 0;
                      border: 1px solid #cbd8e1;
                      border-radius: 6px;
                      background: #f8fbfd;
                      color: #152033;
                      font: 14px/1.45 Consolas, Monaco, monospace;
                      padding: 14px;
                      white-space: pre-wrap;
                      overflow: auto;
                    }
                    textarea {
                      resize: vertical;
                    }
                    .links {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 12px;
                      margin-top: 12px;
                    }
                    @media (max-width: 860px) {
                      body {
                        padding: 18px;
                      }
                      .grid {
                        grid-template-columns: 1fr;
                      }
                      h1 {
                        font-size: 30px;
                      }
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <header>
                      <h1>Documentacao GraphQL</h1>
                      <p>Backend GraphQL da loja virtual academica. Use os exemplos abaixo para demonstrar consultas compostas em um unico endpoint.</p>
                      <div class="links">
                        <a href="/graphql/schema">Schema SDL</a>
                        <a href="/graphiql">GraphiQL original</a>
                        <a href="/graphql/schema" download="schema.graphqls">Baixar schema</a>
                      </div>
                    </header>
                    <div class="grid">
                      <section>
                        <h2>Executar query</h2>
                        <div class="actions">
                          <button type="button" data-example="produtos">Produtos</button>
                          <button type="button" data-example="pedido">Pedido completo</button>
                          <button type="button" data-example="cliente">Cliente</button>
                          <button type="button" data-example="resumo">Resumo vendas</button>
                          <button type="button" id="run">Executar</button>
                        </div>
                        <textarea id="query" spellcheck="false"></textarea>
                      </section>
                      <section>
                        <h2>Resposta</h2>
                        <pre id="result">Clique em Executar para chamar POST /graphql.</pre>
                      </section>
                      <section>
                        <h2>Schema</h2>
                        <pre id="schema">Carregando schema...</pre>
                      </section>
                      <section>
                        <h2>Endpoints</h2>
                        <pre>POST /graphql
GET  /graphql/schema
GET  /docs
GET  /graphiql

Observacao:
/graphql e endpoint de API. No navegador, use /docs para documentacao visual.</pre>
                      </section>
                    </div>
                  </main>
                  <script>
                    const examples = {
                      produtos: `query Produtos {
  produtos {
    id
    nome
    preco
    estoque
  }
}`,
                      pedido: `query PedidoCompleto {
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
}`,
                      cliente: `query ClienteComPedidos {
  cliente(id: 1) {
    id
    nome
    email
    pedidos {
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
}`,
                      resumo: `query ResumoVendas {
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
}`
                    };

                    const query = document.querySelector("#query");
                    const result = document.querySelector("#result");
                    const schema = document.querySelector("#schema");

                    query.value = examples.pedido;

                    document.querySelectorAll("[data-example]").forEach((button) => {
                      button.addEventListener("click", () => {
                        query.value = examples[button.dataset.example];
                      });
                    });

                    document.querySelector("#run").addEventListener("click", async () => {
                      result.textContent = "Executando...";
                      try {
                        const response = await fetch("/graphql", {
                          method: "POST",
                          headers: { "Content-Type": "application/json" },
                          body: JSON.stringify({ query: query.value })
                        });
                        const payload = await response.json();
                        result.textContent = JSON.stringify(payload, null, 2);
                      } catch (error) {
                        result.textContent = String(error);
                      }
                    });

                    fetch("/graphql/schema")
                      .then((response) => response.text())
                      .then((text) => { schema.textContent = text; })
                      .catch((error) => { schema.textContent = String(error); });
                  </script>
                </body>
                </html>
                """;
    }
}
