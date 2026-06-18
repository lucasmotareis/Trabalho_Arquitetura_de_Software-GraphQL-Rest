import { useMemo, useState } from "react";
import { compareScenario, runScenario, scenarios } from "./api.js";

const currency = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const number = new Intl.NumberFormat("pt-BR", {
  maximumFractionDigits: 2,
});

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

export default function App() {
  const [mode, setMode] = useState("rest");
  const [scenarioId, setScenarioId] = useState("pedido");
  const [result, setResult] = useState(null);
  const [comparison, setComparison] = useState({});
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("Pronto para executar");

  const activeScenario = useMemo(
    () => scenarios.find((scenario) => scenario.id === scenarioId),
    [scenarioId],
  );
  const activeComparison = comparison[scenarioId] ?? {};
  const activeResult = result?.scenarioId === scenarioId ? result : null;

  async function executeCurrent() {
    setLoading(true);
    setStatus(`Executando ${activeScenario.name} em ${mode.toUpperCase()}`);
    try {
      const nextResult = await runScenario(scenarioId, mode);
      setResult({ ...nextResult, scenarioId, mode });
      setComparison((current) => ({
        ...current,
        [scenarioId]: {
          ...(current[scenarioId] ?? {}),
          [mode]: nextResult.metrics,
        },
      }));
      setStatus("Execucao concluida");
    } catch (error) {
      setStatus(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function executeComparison(iterations = 1) {
    setLoading(true);
    setStatus(`Comparando REST e GraphQL (${iterations}x)`);
    try {
      const nextComparison = await compareScenario(scenarioId, iterations);
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
            {scenarios.map((scenario) => (
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
          <ComparisonTable comparison={comparison} activeScenarioId={scenarioId} />
          <ResultView scenarioId={scenarioId} data={activeResult?.data} />
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
      <td>{scenario.name}</td>
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

function ResultView({ scenarioId, data }) {
  if (!data) {
    return (
      <section className="result-empty">
        <h2>Resultado</h2>
        <p>Selecione um cenario e execute uma API.</p>
      </section>
    );
  }

  if (scenarioId === "catalogo") {
    if (!data.produtos) {
      return <EmptyResult />;
    }
    return <Catalogo produtos={data.produtos} />;
  }
  if (scenarioId === "pedido") {
    if (!data.pedido) {
      return <EmptyResult />;
    }
    return <Pedido pedido={data.pedido} />;
  }
  if (scenarioId === "historico") {
    if (!data.cliente) {
      return <EmptyResult />;
    }
    return <Historico cliente={data.cliente} />;
  }
  if (!data.resumoVendas) {
    return <EmptyResult />;
  }
  return <Dashboard resumo={data.resumoVendas} />;
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

function Pedido({ pedido }) {
  return (
    <section className="result-section">
      <h2>Pedido #{pedido.id}</h2>
      <div className="summary-line">
        <span>{pedido.status}</span>
        <span>{pedido.cliente.nome}</span>
        <strong>{formatMoney(pedido.total)}</strong>
      </div>
      <ItemList itens={pedido.itens} />
    </section>
  );
}

function Historico({ cliente }) {
  return (
    <section className="result-section">
      <h2>{cliente.nome}</h2>
      <div className="summary-line">
        <span>{cliente.email}</span>
        <span>{cliente.cidade}</span>
        <strong>{cliente.pedidos.length} pedidos</strong>
      </div>
      <div className="history-list">
        {cliente.pedidos.map((pedido) => (
          <article className="history-order" key={pedido.id}>
            <div>
              <strong>Pedido #{pedido.id}</strong>
              <span>{pedido.status}</span>
            </div>
            <b>{formatMoney(pedido.total)}</b>
            <ItemList itens={pedido.itens} compact />
          </article>
        ))}
      </div>
    </section>
  );
}

function Dashboard({ resumo }) {
  return (
    <section className="result-section">
      <h2>Resumo de vendas</h2>
      <div className="metrics-grid compact">
        <article className="metric-card">
          <span>Pedidos</span>
          <strong>{resumo.totalPedidos}</strong>
        </article>
        <article className="metric-card">
          <span>Faturamento</span>
          <strong>{formatMoney(resumo.faturamentoTotal)}</strong>
        </article>
        <article className="metric-card">
          <span>Ticket medio</span>
          <strong>{formatMoney(resumo.ticketMedio)}</strong>
        </article>
      </div>
      <div className="ranking">
        {resumo.produtosMaisVendidos.map((item) => (
          <article className="ranking-row" key={item.produto.id}>
            <span>{item.produto.nome}</span>
            <strong>{item.quantidadeVendida} un.</strong>
            <b>{formatMoney(item.faturamento)}</b>
          </article>
        ))}
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
