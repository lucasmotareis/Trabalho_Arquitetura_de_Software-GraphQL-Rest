package com.example.graphql;

import java.util.List;

public final class GqlTypes {
    private GqlTypes() {
    }

    public record ProdutoInput(
            String nome,
            String descricao,
            String categoria,
            Double preco,
            Integer estoque
    ) {
    }

    public record ClienteInput(
            String nome,
            String email,
            String cidade
    ) {
    }

    public record PedidoItemInput(
            Long produtoId,
            Integer quantidade
    ) {
    }

    public record PedidoInput(
            Long clienteId,
            List<PedidoItemInput> itens
    ) {
    }

    public record ProdutoPayload(
            Long id,
            String nome,
            String descricao,
            String categoria,
            Double preco,
            int estoque
    ) {
    }

    public record PageInfoPayload(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }

    public record ProdutoPagePayload(
            List<ProdutoPayload> content,
            PageInfoPayload pageInfo
    ) {
    }

    public record ClienteResumoPayload(
            Long id,
            String nome,
            String email,
            String cidade
    ) {
    }

    public record ClientePayload(
            Long id,
            String nome,
            String email,
            String cidade,
            List<PedidoPayload> pedidos
    ) {
    }

    public record ItemPedidoPayload(
            Long id,
            int quantidade,
            Double precoUnitario,
            Double subtotal,
            ProdutoPayload produto
    ) {
    }

    public record PedidoPayload(
            Long id,
            String status,
            String criadoEm,
            Double total,
            int quantidadeItens,
            ClienteResumoPayload cliente,
            List<ItemPedidoPayload> itens
    ) {
    }

    public record PedidoPagePayload(
            List<PedidoPayload> content,
            PageInfoPayload pageInfo
    ) {
    }

    public record ProdutoMaisVendidoPayload(
            ProdutoPayload produto,
            int quantidadeVendida,
            Double faturamento
    ) {
    }

    public record ResumoVendasPayload(
            int totalPedidos,
            Double faturamentoTotal,
            Double ticketMedio,
            List<ProdutoMaisVendidoPayload> produtosMaisVendidos
    ) {
    }
}
