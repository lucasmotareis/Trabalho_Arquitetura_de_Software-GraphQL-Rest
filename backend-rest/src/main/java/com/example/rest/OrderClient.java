package com.example.rest;

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
            @Value("${spring.application.name:backend-rest}") String source,
            @Value("${app.services.order-url:http://localhost:8083/api}") String orderUrl,
            ObjectMapper objectMapper
    ) {
        this.httpClient = new InternalHttpClient(source, "order-service", orderUrl, objectMapper);
    }

    public List<ApiTypes.PedidoResponse> listarPedidos() {
        return Arrays.asList(httpClient.get("/pedidos", ApiTypes.PedidoResponse[].class));
    }

    public ApiTypes.PedidoPageResponse listarPedidosPaginados(int page, int size) {
        return httpClient.get("/pedidos/paginados?page=" + page + "&size=" + size, ApiTypes.PedidoPageResponse.class);
    }

    public ApiTypes.PedidoResponse buscarPedido(Long id) {
        return httpClient.get("/pedidos/" + id, ApiTypes.PedidoResponse.class);
    }

    public List<ApiTypes.PedidoResponse> listarPedidosPorCliente(Long clienteId) {
        return Arrays.asList(httpClient.get("/clientes/" + clienteId + "/pedidos", ApiTypes.PedidoResponse[].class));
    }

    public ApiTypes.PedidoPageResponse listarPedidosPorClientePaginados(Long clienteId, int page, int size) {
        return httpClient.get(
                "/clientes/" + clienteId + "/pedidos/paginados?page=" + page + "&size=" + size,
                ApiTypes.PedidoPageResponse.class
        );
    }

    public List<ApiTypes.ItemPedidoResponse> listarItensDoPedido(Long pedidoId) {
        return Arrays.asList(httpClient.get("/pedidos/" + pedidoId + "/itens", ApiTypes.ItemPedidoResponse[].class));
    }

    public ResumoVendasInternoResponse resumoVendas(int limit) {
        return httpClient.get("/vendas/resumo?limit=" + limit, ResumoVendasInternoResponse.class);
    }

    public ApiTypes.PedidoResponse criarPedido(PedidoInternoRequest request) {
        return httpClient.post("/pedidos", request, ApiTypes.PedidoResponse.class);
    }

    public ApiTypes.PedidoResponse atualizarStatus(Long id, ApiTypes.StatusRequest request) {
        return httpClient.put("/pedidos/" + id + "/status", request, ApiTypes.PedidoResponse.class);
    }

    public record PedidoInternoRequest(Long clienteId, List<PedidoItemInternoRequest> itens) {
    }

    public record PedidoItemInternoRequest(Long produtoId, Integer quantidade, BigDecimal precoUnitario) {
    }

    public record PedidoComItensResponse(
            Long id,
            Long clienteId,
            String status,
            LocalDateTime criadoEm,
            BigDecimal total,
            int quantidadeItens,
            List<ApiTypes.ItemPedidoResponse> itens
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
