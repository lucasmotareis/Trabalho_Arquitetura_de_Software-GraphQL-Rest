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

    public PedidoResponse buscarPedido(Long id) {
        return httpClient.get("/pedidos/" + id, PedidoResponse.class);
    }

    public List<PedidoResponse> listarPedidosPorCliente(Long clienteId) {
        return Arrays.asList(httpClient.get("/clientes/" + clienteId + "/pedidos", PedidoResponse[].class));
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

    public record ItemPedidoResponse(
            Long id,
            Long pedidoId,
            Long produtoId,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
    }
}
