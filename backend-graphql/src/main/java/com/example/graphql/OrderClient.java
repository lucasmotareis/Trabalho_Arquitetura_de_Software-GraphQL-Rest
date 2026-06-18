package com.example.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class OrderClient {
    private final InternalHttpClient httpClient;

    public OrderClient(
            @Value("${spring.application.name:backend-graphql}") String source,
            @Value("${app.services.order-url:http://localhost:8083/api}") String orderUrl,
            ObjectMapper objectMapper
    ) {
        this.httpClient = new InternalHttpClient(source, "order-service", orderUrl, objectMapper);
    }

    public List<PedidoResponse> listarPedidos() {
        return Arrays.asList(httpClient.get("/pedidos", PedidoResponse[].class));
    }

    public PedidoPageResponse listarPedidosPaginados(int page, int size, boolean includeItens) {
        return httpClient.get(
                "/pedidos/paginados?page=" + page + "&size=" + size + "&includeItens=" + includeItens,
                PedidoPageResponse.class
        );
    }

    public PedidoResponse buscarPedido(Long id) {
        return httpClient.get("/pedidos/" + id, PedidoResponse.class);
    }

    public List<PedidoResponse> listarPedidosPorCliente(Long clienteId) {
        return Arrays.asList(httpClient.get("/clientes/" + clienteId + "/pedidos", PedidoResponse[].class));
    }

    public PedidoPageResponse listarPedidosPorClientePaginados(Long clienteId, int page, int size, boolean includeItens) {
        return httpClient.get(
                "/clientes/" + clienteId + "/pedidos/paginados?page=" + page + "&size=" + size + "&includeItens=" + includeItens,
                PedidoPageResponse.class
        );
    }

    public ResumoVendasInternoResponse resumoVendas(int limit) {
        return httpClient.get("/vendas/resumo?limit=" + limit, ResumoVendasInternoResponse.class);
    }

    public PedidoResponse criarPedido(PedidoInternoRequest request) {
        return httpClient.post("/pedidos", request, PedidoResponse.class);
    }

    public PedidoResponse atualizarStatus(Long id, StatusRequest request) {
        return httpClient.put("/pedidos/" + id + "/status", request, PedidoResponse.class);
    }

    public record PedidoInternoRequest(Long clienteId, List<PedidoItemInternoRequest> itens) {
    }

    public record PedidoItemInternoRequest(Long produtoId, Integer quantidade, BigDecimal precoUnitario) {
    }

    public record StatusRequest(String status) {
    }

    public record PedidoResponse(
            Long id,
            Long clienteId,
            String status,
            LocalDateTime criadoEm,
            BigDecimal total,
            int quantidadeItens,
            List<ItemPedidoResponse> itens
    ) {
    }

    public record PedidoPageResponse(
            List<PedidoResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }

    public record ItemPedidoResponse(
            Long id,
            Long pedidoId,
            Long produtoId,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
    }

    public record ProdutoVendidoInternoResponse(
            Long produtoId,
            int quantidadeVendida,
            BigDecimal faturamento
    ) {
    }

    public record ResumoVendasInternoResponse(
            long totalPedidos,
            BigDecimal faturamentoTotal,
            BigDecimal ticketMedio,
            List<ProdutoVendidoInternoResponse> produtosMaisVendidos
    ) {
    }
}
