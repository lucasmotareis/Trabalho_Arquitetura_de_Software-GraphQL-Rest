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
const PRODUCT_PAGE_SIZE = 12;
const CUSTOMER_ORDER_PAGE_SIZE = 5;
const DASHBOARD_LIMIT = 12;

const encoder = new TextEncoder();
const decoder = new TextDecoder();
const TRACE_HEADER = "X-Backend-Trace";

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
    const parsed = JSON.parse(text);
    if (typeof parsed.query === "string") {
      return formatGraphqlPayload(parsed);
    }
    return JSON.stringify(parsed, null, 2);
  } catch {
    return text;
  }
}

function formatGraphqlPayload(payload) {
  const query = normalizeGraphqlQuery(payload.query);
  const variables = payload.variables && Object.keys(payload.variables).length > 0
    ? JSON.stringify(payload.variables, null, 2)
    : "{}";

  return `query:\n${query}\n\nvariables:\n${variables}`;
}

function normalizeGraphqlQuery(query) {
  const lines = query.replace(/\r\n/g, "\n").trim().split("\n");
  const indents = lines
    .filter((line) => line.trim())
    .map((line) => line.match(/^\s*/)?.[0].length ?? 0);
  const minIndent = indents.length ? Math.min(...indents) : 0;

  return lines.map((line) => line.slice(minIndent).trimEnd()).join("\n");
}

function emptyMetrics(scenario, mode) {
  return {
    scenario,
    mode,
    requestCount: 0,
    totalClientMs: 0,
    totalBackendMs: 0,
    totalInternalMs: 0,
    payloadBytes: 0,
    internalRequestCount: 0,
    requests: [],
  };
}

function addRequest(metrics, request) {
  const internalRequests = request.internalRequests ?? [];
  metrics.requestCount += 1;
  metrics.totalBackendMs += request.backendMs;
  metrics.totalInternalMs += internalRequests.reduce((sum, item) => sum + Number(item.clientMs ?? 0), 0);
  metrics.payloadBytes += request.payloadBytes;
  metrics.internalRequestCount += internalRequests.length;
  metrics.requests.push(request);
}

function finishMetrics(metrics, scenarioStart) {
  metrics.totalClientMs = round(performance.now() - scenarioStart);
  metrics.totalBackendMs = round(metrics.totalBackendMs);
  metrics.totalInternalMs = round(metrics.totalInternalMs);
  return metrics;
}

async function timedFetch(label, url, options = {}) {
  const requestText = typeof options.body === "string" ? options.body : "";
  const start = performance.now();
  const response = await fetch(url, options);
  const text = await response.text();
  const clientMs = round(performance.now() - start);
  const backendMs = Number.parseFloat(response.headers.get("X-Backend-Time-Ms") ?? "0") || 0;
  const internalRequests = decodeBackendTrace(response.headers.get(TRACE_HEADER));
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
      internalRequests,
    },
  };
}

function decodeBackendTrace(value) {
  if (!value) {
    return [];
  }

  try {
    const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
    const binary = atob(padded);
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
    const trace = JSON.parse(decoder.decode(bytes));
    return Array.isArray(trace)
      ? trace.map((item) => ({
          ...item,
          requestPayloadText: formatPayload(item.requestPayloadText),
          responsePayloadText: formatPayload(item.responsePayloadText),
        }))
      : [];
  } catch {
    return [];
  }
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
    id: "cliente-catalogo-mobile",
    actor: "Cliente",
    name: "Catalogo mobile",
    description: "Lista enxuta para compra em tela pequena.",
    resultType: "catalogo-mobile",
    debate: "REST retorna o recurso completo. GraphQL pede apenas os campos essenciais da vitrine para reduzir payload.",
  },
  {
    id: "cliente-detalhe-pedido",
    actor: "Cliente",
    name: "Detalhe do pedido",
    description: "Pedido, cliente, itens e produtos relacionados.",
    resultType: "pedido",
    debate: "REST monta a tela com varias chamadas. GraphQL entrega o grafo do pedido em uma unica requisicao do navegador.",
  },
  {
    id: "cliente-historico-compras",
    actor: "Cliente",
    name: "Historico de compras",
    description: "Cliente, pedidos, itens e produtos de cada pedido.",
    resultType: "historico",
    debate: "Historico exige dados aninhados. GraphQL reduz a montagem manual do frontend em relacoes cliente-pedido-produto.",
  },
  {
    id: "admin-painel-vendas",
    actor: "Administrador",
    name: "Painel de vendas",
    description: "Faturamento, ticket medio e produtos mais vendidos.",
    resultType: "dashboard",
    debate: "Dashboard e uma visao agregada. REST evidencia varias chamadas; GraphQL atua como camada de composicao.",
  },
  {
    id: "admin-estoque-critico",
    actor: "Administrador",
    name: "Estoque critico",
    description: "Produtos com menor disponibilidade no estoque.",
    resultType: "estoque-critico",
    debate: "A tela precisa de poucos campos. REST recebe o catalogo completo; GraphQL consulta apenas o necessario para decisao.",
  },
  {
    id: "admin-analise-cliente",
    actor: "Administrador",
    name: "Analise de cliente",
    description: "Total gasto, pedidos recentes e produtos comprados.",
    resultType: "analise-cliente",
    debate: "A analise combina cliente, pedidos e itens. GraphQL simplifica uma tela gerencial composta.",
  },
];

export async function runScenario(scenarioId, mode, options = {}) {
  const metrics = emptyMetrics(scenarioId, mode);
  const scenarioStart = performance.now();
  let data;

  if (scenarioId === "cliente-catalogo-mobile") {
    data = mode === "rest" ? await restCatalogoMobile(metrics, options) : await gqlCatalogoMobile(metrics, options);
  } else if (scenarioId === "cliente-detalhe-pedido") {
    data = mode === "rest" ? await restPedido(metrics, 1) : await gqlPedido(metrics, 1);
  } else if (scenarioId === "cliente-historico-compras") {
    data = mode === "rest" ? await restHistorico(metrics, 1, options) : await gqlHistorico(metrics, 1, options);
  } else if (scenarioId === "admin-painel-vendas") {
    data = mode === "rest" ? await restDashboard(metrics) : await gqlDashboard(metrics);
  } else if (scenarioId === "admin-estoque-critico") {
    data = mode === "rest" ? await restEstoqueCritico(metrics) : await gqlEstoqueCritico(metrics);
  } else if (scenarioId === "admin-analise-cliente") {
    data = mode === "rest" ? await restAnaliseCliente(metrics, 1, options) : await gqlAnaliseCliente(metrics, 1, options);
  }

  if (!data) {
    throw new Error("Cenario nao encontrado.");
  }
  return { data, metrics: finishMetrics(metrics, scenarioStart) };
}

export async function compareScenario(scenarioId, iterations = 1, options = {}) {
  const restRuns = [];
  const graphqlRuns = [];
  let restData = null;
  let graphqlData = null;

  for (let index = 0; index < iterations; index += 1) {
    const rest = await runScenario(scenarioId, "rest", options);
    const graphql = await runScenario(scenarioId, "graphql", options);
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
    totalInternalMs: round(average(runs.map((run) => run.totalInternalMs))),
    payloadBytes: Math.round(average(runs.map((run) => run.payloadBytes))),
    internalRequestCount: round(average(runs.map((run) => run.internalRequestCount))),
    requests: first.requests,
    samples: runs.length,
  };
}

function average(values) {
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function pageFromOptions(options) {
  return Math.max(Number(options?.page ?? 0), 0);
}

function normalizeRestPage(page) {
  return {
    page: page.page,
    size: page.size,
    totalElements: page.totalElements,
    totalPages: page.totalPages,
    first: page.first,
    last: page.last,
  };
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

async function restCatalogoMobile(metrics, options = {}) {
  const page = pageFromOptions(options);
  const produtos = await restGet(
    metrics,
    `GET /api/produtos/paginados?page=${page}&size=${PRODUCT_PAGE_SIZE}`,
    `/produtos/paginados?page=${page}&size=${PRODUCT_PAGE_SIZE}`,
  );
  return {
    produtos: produtos.content.map(toProdutoMobile),
    pageInfo: normalizeRestPage(produtos),
  };
}

async function gqlCatalogoMobile(metrics, options = {}) {
  const page = pageFromOptions(options);
  const data = await gql(
    metrics,
    "query produtosPaginados mobile",
    `query ProdutosPaginados($page: Int!, $size: Int!) {
      produtosPaginados(page: $page, size: $size) {
        content {
          id
          nome
          categoria
          preco
          estoque
        }
        pageInfo {
          page
          size
          totalElements
          totalPages
          first
          last
        }
      }
    }`,
    { page, size: PRODUCT_PAGE_SIZE },
  );
  return {
    produtos: data.produtosPaginados.content,
    pageInfo: data.produtosPaginados.pageInfo,
  };
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

async function restHistorico(metrics, clienteId, options = {}) {
  const page = pageFromOptions(options);
  const cliente = await restGet(metrics, `GET /api/clientes/${clienteId}`, `/clientes/${clienteId}`);
  const pedidosPage = await restGet(
    metrics,
    `GET /api/clientes/${clienteId}/pedidos/paginados?page=${page}&size=${CUSTOMER_ORDER_PAGE_SIZE}`,
    `/clientes/${clienteId}/pedidos/paginados?page=${page}&size=${CUSTOMER_ORDER_PAGE_SIZE}`,
  );
  const pedidos = pedidosPage.content;
  const itensPorPedido = await Promise.all(
    pedidos.map((pedido) => restGet(metrics, `GET /api/pedidos/${pedido.id}/itens`, `/pedidos/${pedido.id}/itens`)),
  );
  const produtoIds = itensPorPedido.flat().map((item) => item.produtoId);
  const produtos = await fetchProdutosUnicos(metrics, produtoIds);

  return {
    cliente: {
      ...cliente,
      pageInfo: normalizeRestPage(pedidosPage),
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

async function gqlHistorico(metrics, clienteId, options = {}) {
  const page = pageFromOptions(options);
  const data = await gql(
    metrics,
    "query pedidosPorClientePaginado",
    `query ClienteHistorico($id: ID!, $page: Int!, $size: Int!) {
      pedidosPorClientePaginado(clienteId: $id, page: $page, size: $size) {
        content {
          id
          status
          criadoEm
          total
          quantidadeItens
          cliente { id nome email cidade }
          itens {
            id
            quantidade
            subtotal
            produto { id nome preco categoria }
          }
        }
        pageInfo {
          page
          size
          totalElements
          totalPages
          first
          last
        }
      }
    }`,
    { id: String(clienteId), page, size: CUSTOMER_ORDER_PAGE_SIZE },
  );
  const pedidos = data.pedidosPorClientePaginado.content;
  const cliente = pedidos[0]?.cliente ?? {
    id: String(clienteId),
    nome: `Cliente ${clienteId}`,
    email: "cliente nao encontrado na pagina",
    cidade: "-",
  };
  return {
    cliente: {
      ...cliente,
      pedidos,
      pageInfo: data.pedidosPorClientePaginado.pageInfo,
    },
  };
}

async function restDashboard(metrics) {
  return {
    resumoVendas: await restGet(
      metrics,
      `GET /api/resumo-vendas?limit=${DASHBOARD_LIMIT}`,
      `/resumo-vendas?limit=${DASHBOARD_LIMIT}`,
    ),
  };
}

async function gqlDashboard(metrics) {
  const data = await gql(
    metrics,
    "query resumoVendas",
    `query ResumoVendas($limit: Int!) {
      resumoVendas(limit: $limit) {
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
    { limit: DASHBOARD_LIMIT },
  );
  return { resumoVendas: data.resumoVendas };
}

async function restEstoqueCritico(metrics) {
  const produtos = await restGet(
    metrics,
    `GET /api/estoque-critico?limit=${DASHBOARD_LIMIT}`,
    `/estoque-critico?limit=${DASHBOARD_LIMIT}`,
  );
  return { estoqueCritico: buildEstoqueCritico(produtos) };
}

async function gqlEstoqueCritico(metrics) {
  const data = await gql(
    metrics,
    "query estoque critico",
    `query EstoqueCritico($limit: Int!) {
      estoqueCritico(limit: $limit) {
        id
        nome
        categoria
        preco
        estoque
      }
    }`,
    { limit: DASHBOARD_LIMIT },
  );
  return { estoqueCritico: buildEstoqueCritico(data.estoqueCritico) };
}

async function restAnaliseCliente(metrics, clienteId, options = {}) {
  const { cliente } = await restHistorico(metrics, clienteId, options);
  return { analiseCliente: buildAnaliseCliente(cliente) };
}

async function gqlAnaliseCliente(metrics, clienteId, options = {}) {
  const { cliente } = await gqlHistorico(metrics, clienteId, options);
  return { analiseCliente: buildAnaliseCliente(cliente) };
}

async function fetchProdutosUnicos(metrics, produtoIds) {
  const uniqueIds = [...new Set(produtoIds)];
  const produtos = await Promise.all(
    uniqueIds.map((id) => restGet(metrics, `GET /api/produtos/${id}`, `/produtos/${id}`)),
  );
  return new Map(produtos.map((produto) => [produto.id, produto]));
}

function toProdutoMobile(produto) {
  return {
    id: produto.id,
    nome: produto.nome,
    categoria: produto.categoria,
    preco: produto.preco,
    estoque: produto.estoque,
  };
}

function buildEstoqueCritico(produtos) {
  const ordenados = [...produtos]
    .map(toProdutoMobile)
    .sort((a, b) => Number(a.estoque ?? 0) - Number(b.estoque ?? 0) || String(a.nome).localeCompare(String(b.nome)));
  const selecionados = ordenados.slice(0, 12);

  return {
    produtos: selecionados,
    totalProdutos: produtos.length,
    menorEstoque: selecionados[0]?.estoque ?? 0,
    limiteExibido: selecionados.at(-1)?.estoque ?? 0,
  };
}

function buildAnaliseCliente(cliente) {
  const pedidos = cliente?.pedidos ?? [];
  const produtos = new Map();

  for (const pedido of pedidos) {
    for (const item of pedido.itens ?? []) {
      const produto = item.produto;
      if (!produto) {
        continue;
      }
      const atual = produtos.get(produto.id) ?? {
        produto,
        quantidade: 0,
        valor: 0,
      };
      atual.quantidade += Number(item.quantidade ?? 0);
      atual.valor += Number(item.subtotal ?? 0);
      produtos.set(produto.id, atual);
    }
  }

  const totalGasto = pedidos.reduce((sum, pedido) => sum + Number(pedido.total ?? 0), 0);
  const totalItens = pedidos.reduce(
    (sum, pedido) =>
      sum + Number(pedido.quantidadeItens ?? (pedido.itens ?? []).reduce((inner, item) => inner + Number(item.quantidade ?? 0), 0)),
    0,
  );

  return {
    id: cliente.id,
    nome: cliente.nome,
    email: cliente.email,
    cidade: cliente.cidade,
    pageInfo: cliente.pageInfo,
    totalPedidos: pedidos.length,
    totalGasto,
    ticketMedio: pedidos.length ? totalGasto / pedidos.length : 0,
    totalItens,
    pedidosRecentes: pedidos.slice(0, 5),
    produtosMaisComprados: [...produtos.values()]
      .sort((a, b) => b.quantidade - a.quantidade || b.valor - a.valor)
      .slice(0, 5),
  };
}
