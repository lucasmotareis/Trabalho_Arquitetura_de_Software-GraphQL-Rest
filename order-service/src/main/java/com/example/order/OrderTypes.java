package com.example.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderTypes {
    private OrderTypes() {
    }

    public record PedidoItemRequest(Long produtoId, Integer quantidade, BigDecimal precoUnitario) {
    }

    public record PedidoRequest(Long clienteId, List<PedidoItemRequest> itens) {
    }

    public record StatusRequest(String status) {
    }

    public record BaixarEstoqueRequest(Integer quantidade) {
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

    public record ErrorResponse(String message) {
    }
}
