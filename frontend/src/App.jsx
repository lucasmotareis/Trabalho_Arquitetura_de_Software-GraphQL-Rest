import { useEffect, useMemo, useRef, useState } from "react";
import { compareScenario, runScenario, scenarios } from "./api.js";
import accessoriesImage from "./assets/marketplace/acessorios.svg";
import homeImage from "./assets/marketplace/casa.svg";
import electronicsImage from "./assets/marketplace/eletronicos.svg";
import officeImage from "./assets/marketplace/escritorio.svg";
import sportImage from "./assets/marketplace/esporte.svg";
import gamesImage from "./assets/marketplace/games.svg";
import booksImage from "./assets/marketplace/livros.svg";
import furnitureImage from "./assets/marketplace/moveis.svg";

const INITIAL_SCENARIO_ID = "cliente-catalogo-mobile";
const INITIAL_MODE = "rest";

const seededCategories = [
  "Eletronicos",
  "Acessorios",
  "Moveis",
  "Casa",
  "Escritorio",
  "Games",
  "Livros",
  "Esporte",
];

const categoryImages = {
  Eletronicos: electronicsImage,
  Acessorios: accessoriesImage,
  Moveis: furnitureImage,
  Casa: homeImage,
  Escritorio: officeImage,
  Games: gamesImage,
  Livros: booksImage,
  Esporte: sportImage,
};

const currency = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const number = new Intl.NumberFormat("pt-BR", {
  maximumFractionDigits: 2,
});

const scenarioGroups = scenarios.reduce((groups, scenario) => {
  const group = groups.find((item) => item.actor === scenario.actor);
  if (group) {
    group.items.push(scenario);
  } else {
    groups.push({ actor: scenario.actor, items: [scenario] });
  }
  return groups;
}, []);

function formatMoney(value) {
  return currency.format(Number(value ?? 0));
}

function formatMs(value) {
  return `${number.format(Number(value ?? 0))} ms`;
}

function formatBytes(value) {
  if (value >= 1024) {
    return `${number.format(value / 1024)} KB`;
  }
  return `${number.format(value)} B`;
}

function formatDate(value) {
  if (!value) {
    return "Data nao informada";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function extractPageInfo(data) {
  return data?.pageInfo ?? data?.cliente?.pageInfo ?? data?.analiseCliente?.pageInfo ?? null;
}

export default function App() {
  const autoRunRef = useRef(false);
  const [mode, setMode] = useState(INITIAL_MODE);
  const [scenarioId, setScenarioId] = useState(INITIAL_SCENARIO_ID);
  const [result, setResult] = useState(null);
  const [comparison, setComparison] = useState({});
  const [scenarioPages, setScenarioPages] = useState({ [INITIAL_SCENARIO_ID]: 0 });
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("Pronto para executar");

  const activeScenario = useMemo(
    () => scenarios.find((scenario) => scenario.id === scenarioId),
    [scenarioId],
  );
  const activeComparison = comparison[scenarioId] ?? {};
  const activeResult = result?.scenarioId === scenarioId ? result : null;
  const activePage = scenarioPages[scenarioId] ?? 0;
  const activePageInfo = extractPageInfo(activeResult?.data);

  async function executeScenario(selectedScenarioId, selectedMode, loadingLabel = "Executando", pageOverride = null) {
    const selectedScenario = scenarios.find((scenario) => scenario.id === selectedScenarioId);
    const page = pageOverride ?? scenarioPages[selectedScenarioId] ?? 0;
    setLoading(true);
    setStatus(`${loadingLabel} ${selectedScenario.name} em ${selectedMode.toUpperCase()}`);
    try {
      const nextResult = await runScenario(selectedScenarioId, selectedMode, { page });
      setResult({ ...nextResult, scenarioId: selectedScenarioId, mode: selectedMode });
      setComparison((current) => ({
        ...current,
        [selectedScenarioId]: {
          ...(current[selectedScenarioId] ?? {}),
          [selectedMode]: nextResult.metrics,
        },
      }));
      setStatus("Execucao concluida");
    } catch (error) {
      setStatus(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function executeCurrent() {
    await executeScenario(scenarioId, mode, "Executando", activePage);
  }

  async function executeComparison(iterations = 1) {
    setLoading(true);
    setStatus(`Comparando REST e GraphQL (${iterations}x)`);
    try {
      const nextComparison = await compareScenario(scenarioId, iterations, { page: activePage });
      setComparison((current) => ({
        ...current,
        [scenarioId]: {
          rest: nextComparison.rest,
          graphql: nextComparison.graphql,
        },
      }));
      setResult({
        scenarioId,
        mode,
        data: mode === "rest" ? nextComparison.restData : nextComparison.graphqlData,
        metrics: mode === "rest" ? nextComparison.rest : nextComparison.graphql,
      });
      setStatus("Comparacao concluida");
    } catch (error) {
      setStatus(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function changePage(nextPage) {
    const safePage = Math.max(nextPage, 0);
    setScenarioPages((current) => ({ ...current, [scenarioId]: safePage }));
    await executeScenario(scenarioId, mode, "Carregando pagina", safePage);
  }

  useEffect(() => {
    if (autoRunRef.current) {
      return;
    }
    autoRunRef.current = true;
    executeScenario(INITIAL_SCENARIO_ID, INITIAL_MODE, "Carregando vitrine");
  }, []);

  return (
    <main className="app-shell">
      <section className="topbar">
        <div>
          <span className="eyebrow">Loja virtual academica</span>
          <h1>REST vs GraphQL</h1>
        </div>
        <div className="mode-toggle" aria-label="Selecionar API">
          <button className={mode === "rest" ? "active" : ""} onClick={() => setMode("rest")}>
            REST
          </button>
          <button className={mode === "graphql" ? "active" : ""} onClick={() => setMode("graphql")}>
            GraphQL
          </button>
        </div>
      </section>

      <section className="workspace">
        <aside className="scenario-panel">
          <div className="panel-heading">
            <span>Cenarios</span>
            <strong>{status}</strong>
          </div>
          <div className="scenario-list">
            {scenarioGroups.map((group) => (
              <section className="scenario-group" key={group.actor}>
                <div className="scenario-group-title">
                  <span>{group.actor}</span>
                  <small>{group.items.length} cenarios</small>
                </div>
                {group.items.map((scenario) => (
                  <button
                    type="button"
                    key={scenario.id}
                    className={scenarioId === scenario.id ? "scenario active" : "scenario"}
                    onClick={() => setScenarioId(scenario.id)}
                  >
                    <span>{scenario.name}</span>
                    <small>{scenario.description}</small>
                  </button>
                ))}
              </section>
            ))}
          </div>
          <div className="actions">
            <button className="primary" disabled={loading} onClick={executeCurrent}>
              Executar {mode.toUpperCase()}
            </button>
            <button disabled={loading} onClick={() => executeComparison(1)}>
              Comparar 1x
            </button>
            <button disabled={loading} onClick={() => executeComparison(10)}>
              Comparar 10x
            </button>
          </div>
        </aside>

        <section className="content">
          <MetricsStrip metrics={activeResult?.metrics} />
          <ScenarioDebate scenario={activeScenario} />
          <ResultView scenario={activeScenario} data={activeResult?.data} />
          <PaginationControls pageInfo={activePageInfo} loading={loading} onPageChange={changePage} />
          <ComparisonTable comparison={comparison} activeScenarioId={scenarioId} />
          <RequestTrace
            activeMode={mode}
            selectedMetrics={activeResult?.metrics}
            restMetrics={activeComparison.rest}
            graphqlMetrics={activeComparison.graphql}
          />
        </section>
      </section>
    </main>
  );
}

function PaginationControls({ pageInfo, loading, onPageChange }) {
  if (!pageInfo || pageInfo.totalPages <= 1) {
    return null;
  }

  return (
    <nav className="pagination-controls" aria-label="Paginacao">
      <button
        type="button"
        disabled={loading || pageInfo.first}
        onClick={() => onPageChange(pageInfo.page - 1)}
      >
        Anterior
      </button>
      <span>Pagina {pageInfo.page + 1} de {pageInfo.totalPages}</span>
      <button
        type="button"
        disabled={loading || pageInfo.last}
        onClick={() => onPageChange(pageInfo.page + 1)}
      >
        Proxima
      </button>
    </nav>
  );
}

function ScenarioDebate({ scenario }) {
  if (!scenario) {
    return null;
  }

  return (
    <section className="scenario-insight">
      <div>
        <span>{scenario.actor}</span>
        <strong>{scenario.name}</strong>
      </div>
      <p>{scenario.debate}</p>
    </section>
  );
}

function MetricsStrip({ metrics }) {
  const cards = [
    ["Tempo cliente", metrics ? formatMs(metrics.totalClientMs) : "-"],
    ["Tempo backend", metrics ? formatMs(metrics.totalBackendMs) : "-"],
    ["Req. navegador", metrics ? metrics.requestCount : "-"],
    ["Chamadas internas", metrics ? metrics.internalRequestCount : "-"],
    ["Payload", metrics ? formatBytes(metrics.payloadBytes) : "-"],
  ];

  return (
    <div className="metrics-grid">
      {cards.map(([label, value]) => (
        <article className="metric-card" key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </article>
      ))}
    </div>
  );
}

function ComparisonTable({ comparison, activeScenarioId }) {
  const rows = scenarios.map((scenario) => {
    const metrics = comparison[scenario.id] ?? {};
    return {
      scenario,
      rest: metrics.rest,
      graphql: metrics.graphql,
    };
  });

  return (
    <section className="table-section">
      <div className="section-title">
        <h2>Comparativo</h2>
        <span>{scenarios.find((scenario) => scenario.id === activeScenarioId)?.name}</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Cenario</th>
              <th>API</th>
              <th>Cliente</th>
              <th>Backend</th>
              <th>Req.</th>
              <th>Internas</th>
              <th>Payload</th>
              <th>Amostras</th>
            </tr>
          </thead>
          <tbody>
            {rows.flatMap(({ scenario, rest, graphql }) => [
              <ComparisonRow key={`${scenario.id}-rest`} scenario={scenario} label="REST" metrics={rest} />,
              <ComparisonRow key={`${scenario.id}-graphql`} scenario={scenario} label="GraphQL" metrics={graphql} />,
            ])}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function ComparisonRow({ scenario, label, metrics }) {
  return (
    <tr>
      <td>{scenario.actor} - {scenario.name}</td>
      <td><span className={`pill ${label.toLowerCase()}`}>{label}</span></td>
      <td>{metrics ? formatMs(metrics.totalClientMs) : "-"}</td>
      <td>{metrics ? formatMs(metrics.totalBackendMs) : "-"}</td>
      <td>{metrics ? metrics.requestCount : "-"}</td>
      <td>{metrics ? metrics.internalRequestCount : "-"}</td>
      <td>{metrics ? formatBytes(metrics.payloadBytes) : "-"}</td>
      <td>{metrics?.samples ?? (metrics ? 1 : "-")}</td>
    </tr>
  );
}

function ResultView({ scenario, data }) {
  if (!data) {
    return (
      <section className="result-empty">
        <h2>Resultado</h2>
        <p>Selecione um cenario e execute uma API.</p>
      </section>
    );
  }

  if (scenario?.resultType === "catalogo-mobile") {
    if (!data.produtos) {
      return <EmptyResult />;
    }
    return <CatalogoMobile produtos={data.produtos} pageInfo={data.pageInfo} />;
  }
  if (scenario?.resultType === "pedido") {
    if (!data.pedido) {
      return <EmptyResult />;
    }
    return <Pedido pedido={data.pedido} />;
  }
  if (scenario?.resultType === "historico") {
    if (!data.cliente) {
      return <EmptyResult />;
    }
    return <Historico cliente={data.cliente} />;
  }
  if (scenario?.resultType === "dashboard") {
    if (!data.resumoVendas) {
      return <EmptyResult />;
    }
    return <Dashboard resumo={data.resumoVendas} />;
  }
  if (scenario?.resultType === "estoque-critico") {
    if (!data.estoqueCritico) {
      return <EmptyResult />;
    }
    return <EstoqueCritico estoque={data.estoqueCritico} />;
  }
  if (scenario?.resultType === "analise-cliente") {
    if (!data.analiseCliente) {
      return <EmptyResult />;
    }
    return <AnaliseCliente analise={data.analiseCliente} />;
  }
  if (!data.produtos) {
    return <EmptyResult />;
  }
  return <Catalogo produtos={data.produtos} />;
}

function EmptyResult() {
  return (
    <section className="result-empty">
      <h2>Resultado</h2>
      <p>Selecione um cenario e execute uma API.</p>
    </section>
  );
}

function Catalogo({ produtos }) {
  return (
    <section className="result-section">
      <h2>Catalogo</h2>
      <div className="cards-grid">
        {produtos.map((produto) => (
          <article className="data-card" key={produto.id}>
            <strong>{produto.nome}</strong>
            <span>{produto.categoria}</span>
            <p>{produto.descricao}</p>
            <div className="card-footer">
              <b>{formatMoney(produto.preco)}</b>
              <small>{produto.estoque} em estoque</small>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function CatalogoMobile({ produtos, pageInfo }) {
  const visibleProducts = produtos;
  const totalProducts = pageInfo?.totalElements ?? produtos.length;

  return (
    <section className="result-section marketplace-section">
      <div className="marketplace-hero">
        <div>
          <span>Marketplace academico</span>
          <h2>Vitrine do cliente</h2>
          <p>Produtos prontos para comparar a experiencia REST e GraphQL em uma tela real de compra.</p>
        </div>
        <strong>{visibleProducts.length} de {totalProducts} produtos</strong>
      </div>
      <div className="marketplace-grid">
        {visibleProducts.map((produto) => (
          <article className="marketplace-card" key={produto.id}>
            <div className="marketplace-image">
              <img src={imageForProduct(produto)} alt="" loading="lazy" />
              <span>{categoryForProduct(produto)}</span>
            </div>
            <div className="marketplace-card-body">
              <div className="marketplace-card-top">
                <h3>{produto.nome}</h3>
                <small>{produto.estoque} un.</small>
              </div>
              <strong>{formatMoney(produto.preco)}</strong>
              <div className="marketplace-card-footer">
                <span>Entrega simulada</span>
                <button type="button">Adicionar</button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function categoryForProduct(produto) {
  if (produto.categoria) {
    return produto.categoria;
  }
  const id = Number(produto.id);
  if (!Number.isFinite(id) || id < 1) {
    return seededCategories[0];
  }
  return seededCategories[(id - 1) % seededCategories.length];
}

function imageForProduct(produto) {
  return categoryImages[categoryForProduct(produto)] ?? electronicsImage;
}

function Pedido({ pedido }) {
  const totalItens = pedido.quantidadeItens ?? pedido.itens.reduce((sum, item) => sum + Number(item.quantidade ?? 0), 0);

  return (
    <section className="result-section order-section">
      <div className="order-hero">
        <div>
          <span className="order-eyebrow">Acompanhamento de compra</span>
          <h2>Pedido #{pedido.id}</h2>
          <p>{pedido.cliente.nome} comprou {totalItens} itens nesta simulacao de marketplace.</p>
        </div>
        <div className="order-total">
          <span>Total do pedido</span>
          <strong>{formatMoney(pedido.total)}</strong>
        </div>
      </div>

      <div className="order-meta-grid">
        <article>
          <span>Status</span>
          <strong className={`order-status status-${String(pedido.status).toLowerCase()}`}>{pedido.status}</strong>
        </article>
        <article>
          <span>Cliente</span>
          <strong>{pedido.cliente.nome}</strong>
        </article>
        <article>
          <span>Criado em</span>
          <strong>{formatDate(pedido.criadoEm)}</strong>
        </article>
        <article>
          <span>Itens</span>
          <strong>{totalItens}</strong>
        </article>
      </div>

      <div className="order-items-header">
        <div>
          <h3>Itens do pedido</h3>
          <p>Produtos carregados dos servicos de pedidos, catalogo e estoque.</p>
        </div>
        <span>{pedido.itens.length} produtos</span>
      </div>

      <div className="order-items-grid">
        {pedido.itens.map((item) => (
          <article className="order-item-card" key={item.id}>
            <div className="order-item-image">
              <img src={imageForProduct(item.produto)} alt="" loading="lazy" />
              <span>{categoryForProduct(item.produto)}</span>
            </div>
            <div className="order-item-body">
              <div>
                <h3>{item.produto.nome}</h3>
                <small>{item.quantidade} x {formatMoney(item.precoUnitario ?? item.produto.preco)}</small>
              </div>
              <strong>{formatMoney(item.subtotal)}</strong>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function Historico({ cliente }) {
  const totalGasto = cliente.pedidos.reduce((sum, pedido) => sum + Number(pedido.total ?? 0), 0);
  const totalItens = cliente.pedidos.reduce(
    (sum, pedido) =>
      sum + Number(pedido.quantidadeItens ?? pedido.itens.reduce((inner, item) => inner + Number(item.quantidade ?? 0), 0)),
    0,
  );
  const totalPedidos = cliente.pageInfo?.totalElements ?? cliente.pedidos.length;

  return (
    <section className="result-section customer-history-section">
      <div className="customer-hero">
        <div className="customer-avatar" aria-hidden="true">
          {cliente.nome.slice(0, 2).toUpperCase()}
        </div>
        <div>
          <span className="order-eyebrow">Area do cliente</span>
          <h2>{cliente.nome}</h2>
          <p>{cliente.email} - {cliente.cidade}</p>
        </div>
        <div className="customer-total">
          <span>Total da pagina</span>
          <strong>{formatMoney(totalGasto)}</strong>
        </div>
      </div>

      <div className="order-meta-grid">
        <article>
          <span>Pedidos totais</span>
          <strong>{totalPedidos}</strong>
        </article>
        <article>
          <span>Itens na pagina</span>
          <strong>{totalItens}</strong>
        </article>
        <article>
          <span>Ticket da pagina</span>
          <strong>{formatMoney(cliente.pedidos.length ? totalGasto / cliente.pedidos.length : 0)}</strong>
        </article>
        <article>
          <span>Cidade</span>
          <strong>{cliente.cidade}</strong>
        </article>
      </div>

      <div className="customer-orders-header">
        <div>
          <h3>Historico de compras</h3>
          <p>Pedidos montados a partir de cliente, pedidos, itens e produtos relacionados.</p>
        </div>
        <span>{cliente.pedidos.length} pedidos nesta pagina</span>
      </div>

      <div className="customer-order-list">
        {cliente.pedidos.map((pedido) => (
          <article className="customer-order-card" key={pedido.id}>
            <div className="customer-order-summary">
              <div>
                <span className={`order-status status-${String(pedido.status).toLowerCase()}`}>{pedido.status}</span>
                <h3>Pedido #{pedido.id}</h3>
                <small>{formatDate(pedido.criadoEm)}</small>
              </div>
              <strong>{formatMoney(pedido.total)}</strong>
            </div>
            <div className="customer-order-items">
              {pedido.itens.map((item) => (
                <article className="customer-order-item" key={item.id}>
                  <img src={imageForProduct(item.produto)} alt="" loading="lazy" />
                  <div>
                    <strong>{item.produto.nome}</strong>
                    <span>{item.quantidade} x {formatMoney(item.precoUnitario ?? item.produto.preco)}</span>
                  </div>
                  <b>{formatMoney(item.subtotal)}</b>
                </article>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function Dashboard({ resumo }) {
  const topProducts = resumo.produtosMaisVendidos.slice(0, 12);

  return (
    <section className="result-section sales-dashboard-section">
      <div className="sales-hero">
        <div>
          <span className="order-eyebrow">Painel administrativo</span>
          <h2>Resumo de vendas</h2>
          <p>Indicadores consolidados e ranking de produtos em uma visao compacta.</p>
        </div>
        <strong>{resumo.produtosMaisVendidos.length} produtos ranqueados</strong>
      </div>

      <div className="sales-kpi-grid">
        <article>
          <span>Pedidos</span>
          <strong>{resumo.totalPedidos}</strong>
          <small>pedidos processados</small>
        </article>
        <article>
          <span>Faturamento</span>
          <strong>{formatMoney(resumo.faturamentoTotal)}</strong>
          <small>valor bruto simulado</small>
        </article>
        <article>
          <span>Ticket medio</span>
          <strong>{formatMoney(resumo.ticketMedio)}</strong>
          <small>media por pedido</small>
        </article>
      </div>

      <div className="sales-board">
        <div className="sales-board-heading">
          <div>
            <h3>Produtos mais vendidos</h3>
            <p>Top {topProducts.length} por quantidade vendida.</p>
          </div>
          <span>{topProducts.reduce((sum, item) => sum + Number(item.quantidadeVendida ?? 0), 0)} unidades</span>
        </div>
        <div className="sales-ranking-grid">
          {topProducts.map((item, index) => (
            <article className="sales-product-card" key={item.produto.id}>
              <div className="sales-rank">#{index + 1}</div>
              <img src={imageForProduct(item.produto)} alt="" loading="lazy" />
              <div>
                <strong>{item.produto.nome}</strong>
                <span>{categoryForProduct(item.produto)}</span>
              </div>
              <small>{item.quantidadeVendida} un.</small>
              <b>{formatMoney(item.faturamento)}</b>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function EstoqueCritico({ estoque }) {
  const produtos = estoque.produtos ?? [];
  const minStock = Math.min(...produtos.map((produto) => Number(produto.estoque ?? 0)));
  const maxStock = Math.max(...produtos.map((produto) => Number(produto.estoque ?? 0)));
  const stockRange = Math.max(maxStock - minStock, 1);

  return (
    <section className="result-section critical-stock-section">
      <div className="critical-stock-hero">
        <div>
          <span className="order-eyebrow">Painel administrativo</span>
          <h2>Estoque critico</h2>
          <p>Produtos com menor disponibilidade retornados diretamente do inventory-service.</p>
        </div>
        <strong>{estoque.totalProdutos} itens analisados</strong>
      </div>

      <div className="critical-stock-kpis">
        <article>
          <span>Menor estoque</span>
          <strong>{estoque.menorEstoque}</strong>
          <small>unidades</small>
        </article>
        <article>
          <span>Limite exibido</span>
          <strong>{estoque.limiteExibido}</strong>
          <small>ultimo item do recorte</small>
        </article>
        <article>
          <span>Produtos criticos</span>
          <strong>{estoque.produtos.length}</strong>
          <small>retornados na tela</small>
        </article>
      </div>

      <div className="critical-stock-board">
        <div className="critical-stock-heading">
          <div>
            <h3>Fila de reposicao</h3>
            <p>Itens ordenados do menor para o maior estoque no recorte atual.</p>
          </div>
          <span>{produtos.length} produtos</span>
        </div>

        <div className="critical-stock-grid">
          {produtos.map((produto, index) => {
            const stock = Number(produto.estoque ?? 0);
            const barWidth = 100 - ((stock - minStock) / stockRange) * 52;

            return (
              <article className="critical-stock-card" key={produto.id}>
                <div className="critical-stock-image">
                  <img src={imageForProduct(produto)} alt="" loading="lazy" />
                  <span>{categoryForProduct(produto)}</span>
                </div>
                <div className="critical-stock-body">
                  <div className="critical-stock-topline">
                    <small>#{index + 1}</small>
                    <b>{formatMoney(produto.preco)}</b>
                  </div>
                  <h3>{produto.nome}</h3>
                  <div className="stock-meter" aria-hidden="true">
                    <span style={{ width: `${barWidth}%` }} />
                  </div>
                  <div className="critical-stock-footer">
                    <strong>{stock} un.</strong>
                    <span>{stock === minStock ? "Reposicao primeiro" : "Monitorar"}</span>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}

function AnaliseCliente({ analise }) {
  const totalPedidos = analise.pageInfo?.totalElements ?? analise.totalPedidos;
  const produtos = analise.produtosMaisComprados ?? [];
  const pedidos = analise.pedidosRecentes ?? [];
  const maxQuantidade = Math.max(...produtos.map((item) => Number(item.quantidade ?? 0)), 1);

  return (
    <section className="result-section customer-analysis-section">
      <div className="customer-analysis-hero">
        <div className="customer-avatar large" aria-hidden="true">
          {analise.nome.slice(0, 2).toUpperCase()}
        </div>
        <div>
          <span className="order-eyebrow">Painel administrativo</span>
          <h2>{analise.nome}</h2>
          <p>{analise.email} - {analise.cidade}</p>
        </div>
        <div className="customer-analysis-total">
          <span>Total da pagina</span>
          <strong>{formatMoney(analise.totalGasto)}</strong>
        </div>
      </div>

      <div className="customer-analysis-kpis">
        <article>
          <span>Pedidos totais</span>
          <strong>{totalPedidos}</strong>
          <small>historico paginado</small>
        </article>
        <article>
          <span>Itens na pagina</span>
          <strong>{analise.totalItens}</strong>
          <small>recorte atual</small>
        </article>
        <article>
          <span>Ticket da pagina</span>
          <strong>{formatMoney(analise.ticketMedio)}</strong>
          <small>media dos pedidos exibidos</small>
        </article>
      </div>

      <div className="customer-analysis-board">
        <section className="customer-analysis-panel">
          <div className="customer-analysis-heading">
            <div>
              <h3>Produtos recorrentes</h3>
              <p>Itens mais comprados no recorte carregado.</p>
            </div>
            <span>{produtos.length} produtos</span>
          </div>
          <div className="customer-product-list">
            {produtos.map((item, index) => {
              const percent = Math.max((Number(item.quantidade ?? 0) / maxQuantidade) * 100, 18);

              return (
                <article className="customer-product-card" key={item.produto.id}>
                  <img src={imageForProduct(item.produto)} alt="" loading="lazy" />
                  <div className="customer-product-info">
                    <div>
                      <small>#{index + 1} - {categoryForProduct(item.produto)}</small>
                      <h3>{item.produto.nome}</h3>
                    </div>
                    <div className="customer-product-meter" aria-hidden="true">
                      <span style={{ width: `${percent}%` }} />
                    </div>
                  </div>
                  <div className="customer-product-values">
                    <strong>{item.quantidade} un.</strong>
                    <b>{formatMoney(item.valor)}</b>
                  </div>
                </article>
              );
            })}
          </div>
        </section>

        <section className="customer-analysis-panel">
          <div className="customer-analysis-heading">
            <div>
              <h3>Pedidos recentes</h3>
              <p>Compras usadas para montar a analise da pagina.</p>
            </div>
            <span>{pedidos.length} pedidos</span>
          </div>
          <div className="customer-recent-orders">
            {pedidos.map((pedido) => (
              <article className="customer-recent-order" key={pedido.id}>
                <div>
                  <span className={`order-status status-${String(pedido.status).toLowerCase()}`}>{pedido.status}</span>
                  <h3>Pedido #{pedido.id}</h3>
                  <small>{formatDate(pedido.criadoEm)}</small>
                </div>
                <div>
                  <strong>{formatMoney(pedido.total)}</strong>
                  <small>{pedido.quantidadeItens ?? pedido.itens?.length ?? 0} itens</small>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    </section>
  );
}

function ItemList({ itens, compact = false }) {
  return (
    <div className={compact ? "item-list compact" : "item-list"}>
      {itens.map((item) => (
        <article className="item-row" key={item.id}>
          <span>{item.produto.nome}</span>
          <small>{item.quantidade} x {formatMoney(item.precoUnitario ?? item.produto.preco)}</small>
          <strong>{formatMoney(item.subtotal)}</strong>
        </article>
      ))}
    </div>
  );
}

function RequestTrace({ activeMode, selectedMetrics, restMetrics, graphqlMetrics }) {
  const groups = [];
  if (restMetrics && graphqlMetrics) {
    groups.push({ label: "REST", kind: "rest", metrics: restMetrics });
    groups.push({ label: "GraphQL", kind: "graphql", metrics: graphqlMetrics });
  } else if (selectedMetrics) {
    groups.push({
      label: activeMode === "rest" ? "REST" : "GraphQL",
      kind: activeMode,
      metrics: selectedMetrics,
    });
  }

  const totalRequests = groups.reduce((sum, group) => sum + group.metrics.requests.length, 0);
  const totalInternalRequests = groups.reduce((sum, group) => sum + (group.metrics.internalRequestCount ?? 0), 0);

  return (
    <section className="table-section">
      <div className="section-title">
        <h2>Requisicoes</h2>
        <span>{totalRequests} navegador | {totalInternalRequests} internas</span>
      </div>
      {groups.length === 0 && <p>Nenhuma requisicao executada.</p>}
      {groups.map((group) => (
        <div className="trace-group" key={group.kind}>
          <div className="trace-group-title">
            <span className={`pill ${group.kind}`}>{group.label}</span>
            <small>
              {group.metrics.requests.length} chamadas | {formatMs(group.metrics.totalClientMs)} cliente |{" "}
              {group.metrics.internalRequestCount ?? 0} internas | {formatBytes(group.metrics.payloadBytes)}
            </small>
          </div>
          <div className="trace-list">
            {group.metrics.requests.map((request, index) => (
              <article className="trace-row" key={`${group.kind}-${request.label}-${index}`}>
                <span>{request.label}</span>
                <small>{request.method} - {request.status}</small>
                <b>{formatMs(request.clientMs)}</b>
                <em>{formatBytes(request.responsePayloadBytes ?? request.payloadBytes)}</em>
                <InternalTrace requests={request.internalRequests ?? []} />
                <details className="payload-details">
                  <summary>Ver payloads</summary>
                  <div className="payload-grid">
                    <div className="payload-block">
                      <strong>Enviado ({formatBytes(request.requestPayloadBytes ?? 0)})</strong>
                      <pre>{request.requestPayloadText || "Sem corpo na requisicao"}</pre>
                    </div>
                    <div className="payload-block">
                      <strong>Recebido ({formatBytes(request.responsePayloadBytes ?? request.payloadBytes)})</strong>
                      <pre>{request.responsePayloadText || "Sem corpo na resposta"}</pre>
                    </div>
                  </div>
                </details>
              </article>
            ))}
          </div>
        </div>
      ))}
    </section>
  );
}

function InternalTrace({ requests }) {
  if (!requests.length) {
    return null;
  }

  return (
    <div className="internal-trace">
      <strong>Chamadas internas</strong>
      {requests.map((request, index) => (
        <article className="internal-row" key={`${request.source}-${request.target}-${request.path}-${index}`}>
          <span>{request.source}{" -> "}{request.target}</span>
          <small>{request.method} {request.path} - {request.status}</small>
          <b>{formatMs(request.clientMs)}</b>
          <em>{formatBytes(request.responsePayloadBytes ?? 0)}</em>
          <details className="payload-details">
            <summary>Payload interno</summary>
            <div className="payload-grid">
              <div className="payload-block">
                <strong>Enviado ({formatBytes(request.requestPayloadBytes ?? 0)})</strong>
                <pre>{request.requestPayloadText || "Sem corpo na requisicao"}</pre>
              </div>
              <div className="payload-block">
                <strong>Recebido ({formatBytes(request.responsePayloadBytes ?? 0)})</strong>
                <pre>{request.responsePayloadText || "Sem corpo na resposta"}</pre>
              </div>
            </div>
          </details>
        </article>
      ))}
    </div>
  );
}
