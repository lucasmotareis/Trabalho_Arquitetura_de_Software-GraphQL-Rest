function withoutTrailingSlash(value) {
  return value.replace(/\/+$/, "");
}

function normalizeRestUrl(value) {
  const baseUrl = withoutTrailingSlash(value ?? "http://localhost:8081/api");
  return baseUrl.endsWith("/api") ? baseUrl : `${baseUrl}/api`;
}

function normalizeGraphqlUrl(value) {
  const baseUrl = withoutTrailingSlash(value ?? "http://localhost:8082/graphql");
  return baseUrl.endsWith("/graphql") ? baseUrl : `${baseUrl}/graphql`;
}

const REST_BASE_URL = normalizeRestUrl(import.meta.env.VITE_REST_API_URL);
const GRAPHQL_URL = normalizeGraphqlUrl(import.meta.env.VITE_GRAPHQL_API_URL);

const encoder = new TextEncoder();

function payloadBytes(text) {
  return encoder.encode(text ?? "").length;
}

function round(value) {
  return Number(value.toFixed(2));
}

function formatPayload(text) {
  if (!text) {
    return "";
  }
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

function emptyMetrics(scenario, mode) {
  return {
    scenario,
    mode,
    requestCount: 0,
    totalClientMs: 0,
    totalBackendMs: 0,
    payloadBytes: 0,
    requests: [],
  };
}

function addRequest(metrics, request) {
  metrics.requestCount += 1;
  metrics.totalBackendMs += request.backendMs;
  metrics.payloadBytes += request.payloadBytes;
  metrics.requests.push(request);
}

function finishMetrics(metrics, scenarioStart) {
  metrics.totalClientMs = round(performance.now() - scenarioStart);
  metrics.totalBackendMs = round(metrics.totalBackendMs);
  return metrics;
}

async function timedFetch(label, url, options = {}) {
  const requestText = typeof options.body === "string" ? options.body : "";
  const start = performance.now();
  const response = await fetch(url, options);
  const text = await response.text();
  const clientMs = round(performance.now() - start);
  const backendMs = Number.parseFloat(response.headers.get("X-Backend-Time-Ms") ?? "0") || 0;
  const requestBytes = payloadBytes(requestText);
  const responseBytes = payloadBytes(text);

  let body = null;
  if (text) {
    body = JSON.parse(text);
  }
  if (!response.ok) {
    throw new Error(body?.message ?? body?.errors?.[0]?.message ?? `Erro HTTP ${response.status}`);
  }

  return {
    body,
    metric: {
      label,
      method: options.method ?? "GET",
      url,
      status: response.status,
      clientMs,
      backendMs: round(backendMs),
      payloadBytes: responseBytes,
      requestPayloadBytes: requestBytes,
      responsePayloadBytes: responseBytes,
      requestPayloadText: formatPayload(requestText),
      responsePayloadText: formatPayload(text),
    },
  };
}

async function restGet(metrics, label, path) {
  const result = await timedFetch(label, `${REST_BASE_URL}${path}`);
  addRequest(metrics, result.metric);
  return result.body;
}

async function gql(metrics, label, query, variables = {}) {
  const result = await timedFetch(label, GRAPHQL_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  result.metric.label = `POST /graphql - ${label}`;
  addRequest(metrics, result.metric);

  if (result.body.errors?.length) {
    throw new Error(result.body.errors[0].message);
  }
  return result.body.data;
}

export const scenarios = [
  {
    id: "catalogo",
    name: "Catalogo de produtos",
    description: "Lista simples de produtos disponiveis.",
  },
  {
    id: "pedido",
    name: "Detalhe do pedido #1",
    description: "Pedido, cliente, itens e produtos relacionados.",
  },
  {
    id: "historico",
    name: "Historico da cliente #1",
    description: "Cliente, pedidos, itens e produtos de cada pedido.",
  },
  {
    id: "dashboard",
    name: "Resumo de vendas",
    description: "Faturamento, ticket medio e produtos mais vendidos.",
  },
];

export async function runScenario(scenarioId, mode) {
  const metrics = emptyMetrics(scenarioId, mode);
  const scenarioStart = performance.now();
  let data;

  if (scenarioId === "catalogo") {
    data = mode === "rest" ? await restCatalogo(metrics) : await gqlCatalogo(metrics);
  }
  if (scenarioId === "pedido") {
    data = mode === "rest" ? await restPedido(metrics, 1) : await gqlPedido(metrics, 1);
  }
  if (scenarioId === "historico") {
    data = mode === "rest" ? await restHistorico(metrics, 1) : await gqlHistorico(metrics, 1);
  }
  if (scenarioId === "dashboard") {
    data = mode === "rest" ? await restDashboard(metrics) : await gqlDashboard(metrics);
  }

  if (!data) {
    throw new Error("Cenario nao encontrado.");
  }
  return { data, metrics: finishMetrics(metrics, scenarioStart) };
}

export async function compareScenario(scenarioId, iterations = 1) {
  const restRuns = [];
  const graphqlRuns = [];
  let restData = null;
  let graphqlData = null;

  for (let index = 0; index < iterations; index += 1) {
    const rest = await runScenario(scenarioId, "rest");
    const graphql = await runScenario(scenarioId, "graphql");
    restRuns.push(rest.metrics);
    graphqlRuns.push(graphql.metrics);
    restData = rest.data;
    graphqlData = graphql.data;
  }

  return {
    restData,
    graphqlData,
    rest: averageMetrics(restRuns),
    graphql: averageMetrics(graphqlRuns),
  };
}

function averageMetrics(runs) {
  const first = runs[0];
  return {
    scenario: first.scenario,
    mode: first.mode,
    requestCount: round(average(runs.map((run) => run.requestCount))),
    totalClientMs: round(average(runs.map((run) => run.totalClientMs))),
    totalBackendMs: round(average(runs.map((run) => run.totalBackendMs))),
    payloadBytes: Math.round(average(runs.map((run) => run.payloadBytes))),
    requests: first.requests,
    samples: runs.length,
  };
}

function average(values) {
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

async function restCatalogo(metrics) {
  return {
    produtos: await restGet(metrics, "GET /api/produtos", "/produtos"),
  };
}

async function gqlCatalogo(metrics) {
  const data = await gql(
    metrics,
    "query produtos",
    `query {
      produtos {
        id
        nome
        descricao
        categoria
        preco
        estoque
      }
    }`,
  );
  return { produtos: data.produtos };
}

async function restPedido(metrics, pedidoId) {
  const pedido = await restGet(metrics, `GET /api/pedidos/${pedidoId}`, `/pedidos/${pedidoId}`);
  const [cliente, itens] = await Promise.all([
    restGet(metrics, `GET /api/clientes/${pedido.clienteId}`, `/clientes/${pedido.clienteId}`),
    restGet(metrics, `GET /api/pedidos/${pedidoId}/itens`, `/pedidos/${pedidoId}/itens`),
  ]);
  const produtos = await fetchProdutosUnicos(metrics, itens.map((item) => item.produtoId));

  return {
    pedido: {
      ...pedido,
      cliente,
      itens: itens.map((item) => ({
        ...item,
        produto: produtos.get(item.produtoId),
      })),
    },
  };
}

async function gqlPedido(metrics, pedidoId) {
  const data = await gql(
    metrics,
    "query pedido",
    `query Pedido($id: ID!) {
      pedido(id: $id) {
        id
        status
        criadoEm
        total
        quantidadeItens
        cliente { id nome email cidade }
        itens {
          id
          quantidade
          precoUnitario
          subtotal
          produto { id nome descricao categoria preco estoque }
        }
      }
    }`,
    { id: String(pedidoId) },
  );
  return { pedido: data.pedido };
}

async function restHistorico(metrics, clienteId) {
  const cliente = await restGet(metrics, `GET /api/clientes/${clienteId}`, `/clientes/${clienteId}`);
  const pedidos = await restGet(metrics, `GET /api/clientes/${clienteId}/pedidos`, `/clientes/${clienteId}/pedidos`);
  const itensPorPedido = await Promise.all(
    pedidos.map((pedido) => restGet(metrics, `GET /api/pedidos/${pedido.id}/itens`, `/pedidos/${pedido.id}/itens`)),
  );
  const produtoIds = itensPorPedido.flat().map((item) => item.produtoId);
  const produtos = await fetchProdutosUnicos(metrics, produtoIds);

  return {
    cliente: {
      ...cliente,
      pedidos: pedidos.map((pedido, index) => ({
        ...pedido,
        itens: itensPorPedido[index].map((item) => ({
          ...item,
          produto: produtos.get(item.produtoId),
        })),
      })),
    },
  };
}

async function gqlHistorico(metrics, clienteId) {
  const data = await gql(
    metrics,
    "query cliente",
    `query Cliente($id: ID!) {
      cliente(id: $id) {
        id
        nome
        email
        cidade
        pedidos {
          id
          status
          criadoEm
          total
          quantidadeItens
          itens {
            id
            quantidade
            subtotal
            produto { id nome preco categoria }
          }
        }
      }
    }`,
    { id: String(clienteId) },
  );
  return { cliente: data.cliente };
}

async function restDashboard(metrics) {
  const pedidos = await restGet(metrics, "GET /api/pedidos", "/pedidos");
  const itensPorPedido = await Promise.all(
    pedidos.map((pedido) => restGet(metrics, `GET /api/pedidos/${pedido.id}/itens`, `/pedidos/${pedido.id}/itens`)),
  );
  const todosItens = itensPorPedido.flat();
  const produtos = await fetchProdutosUnicos(metrics, todosItens.map((item) => item.produtoId));
  const vendidos = new Map();

  for (const item of todosItens) {
    const produto = produtos.get(item.produtoId);
    const atual = vendidos.get(item.produtoId) ?? {
      produto,
      quantidadeVendida: 0,
      faturamento: 0,
    };
    atual.quantidadeVendida += item.quantidade;
    atual.faturamento += Number(item.subtotal);
    vendidos.set(item.produtoId, atual);
  }

  const faturamentoTotal = pedidos.reduce((sum, pedido) => sum + Number(pedido.total), 0);
  return {
    resumoVendas: {
      totalPedidos: pedidos.length,
      faturamentoTotal,
      ticketMedio: pedidos.length ? faturamentoTotal / pedidos.length : 0,
      produtosMaisVendidos: [...vendidos.values()].sort((a, b) => b.quantidadeVendida - a.quantidadeVendida),
    },
  };
}

async function gqlDashboard(metrics) {
  const data = await gql(
    metrics,
    "query resumoVendas",
    `query {
      resumoVendas {
        totalPedidos
        faturamentoTotal
        ticketMedio
        produtosMaisVendidos {
          quantidadeVendida
          faturamento
          produto { id nome preco categoria }
        }
      }
    }`,
  );
  return { resumoVendas: data.resumoVendas };
}

async function fetchProdutosUnicos(metrics, produtoIds) {
  const uniqueIds = [...new Set(produtoIds)];
  const produtos = await Promise.all(
    uniqueIds.map((id) => restGet(metrics, `GET /api/produtos/${id}`, `/produtos/${id}`)),
  );
  return new Map(produtos.map((produto) => [produto.id, produto]));
}
