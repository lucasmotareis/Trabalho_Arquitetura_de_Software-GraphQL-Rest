package com.example.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiTypes {
    private ApiTypes() {
    }

    @Schema(description = "Payload para criar ou atualizar um produto.")
    public record ProdutoRequest(
            @Schema(description = "Nome comercial do produto.", example = "Notebook Pro") String nome,
            @Schema(description = "Descricao curta exibida no catalogo.", example = "Notebook para trabalho e estudos com 16GB RAM") String descricao,
            @Schema(description = "Categoria do produto.", example = "Eletronicos") String categoria,
            @Schema(description = "Preco unitario do produto.", example = "4799.90") BigDecimal preco,
            @Schema(description = "Quantidade disponivel em estoque.", example = "120") Integer estoque
    ) {
    }

    @Schema(description = "Produto retornado pela API REST.")
    public record ProdutoResponse(
            @Schema(description = "Identificador do produto.", example = "1") Long id,
            @Schema(description = "Nome comercial do produto.", example = "Notebook Pro") String nome,
            @Schema(description = "Descricao curta exibida no catalogo.", example = "Notebook para trabalho e estudos com 16GB RAM") String descricao,
            @Schema(description = "Categoria do produto.", example = "Eletronicos") String categoria,
            @Schema(description = "Preco unitario do produto.", example = "4799.90") BigDecimal preco,
            @Schema(description = "Quantidade disponivel em estoque.", example = "120") int estoque
    ) {
    }

    @Schema(description = "Pagina de produtos retornada pela API REST.")
    public record ProdutoPageResponse(
            @Schema(description = "Produtos da pagina atual.") List<ProdutoResponse> content,
            @Schema(description = "Indice da pagina atual, com inicio em zero.", example = "0") int page,
            @Schema(description = "Quantidade solicitada por pagina.", example = "12") int size,
            @Schema(description = "Total de produtos encontrados.", example = "120") long totalElements,
            @Schema(description = "Total de paginas disponiveis.", example = "10") int totalPages,
            @Schema(description = "Indica se esta e a primeira pagina.", example = "true") boolean first,
            @Schema(description = "Indica se esta e a ultima pagina.", example = "false") boolean last
    ) {
    }

    @Schema(description = "Payload para criar ou atualizar um cliente.")
    public record ClienteRequest(
            @Schema(description = "Nome completo do cliente.", example = "Ana Souza") String nome,
            @Schema(description = "Email do cliente.", example = "ana.souza@email.com") String email,
            @Schema(description = "Cidade do cliente.", example = "Curitiba") String cidade
    ) {
    }

    @Schema(description = "Cliente retornado pela API REST.")
    public record ClienteResponse(
            @Schema(description = "Identificador do cliente.", example = "1") Long id,
            @Schema(description = "Nome completo do cliente.", example = "Ana Souza") String nome,
            @Schema(description = "Email do cliente.", example = "ana.souza@email.com") String email,
            @Schema(description = "Cidade do cliente.", example = "Curitiba") String cidade
    ) {
    }

    @Schema(description = "Item solicitado na criacao de um pedido.")
    public record PedidoItemRequest(
            @Schema(description = "Identificador do produto comprado.", example = "1") Long produtoId,
            @Schema(description = "Quantidade comprada.", example = "2") Integer quantidade
    ) {
    }

    @Schema(description = "Payload para criar um pedido.")
    public record PedidoRequest(
            @Schema(description = "Identificador do cliente comprador.", example = "1") Long clienteId,
            @Schema(description = "Itens comprados no pedido.") List<PedidoItemRequest> itens
    ) {
    }

    @Schema(description = "Payload para atualizar o status de um pedido.")
    public record StatusRequest(
            @Schema(description = "Novo status do pedido.", example = "ENVIADO") String status
    ) {
    }

    @Schema(description = "Resumo de pedido retornado pela API REST.")
    public record PedidoResponse(
            @Schema(description = "Identificador do pedido.", example = "1") Long id,
            @Schema(description = "Identificador do cliente do pedido.", example = "1") Long clienteId,
            @Schema(description = "Status atual do pedido.", example = "PAGO") String status,
            @Schema(description = "Data e hora de criacao do pedido.", example = "2026-01-01T15:00:00") LocalDateTime criadoEm,
            @Schema(description = "Valor total do pedido.", example = "4949.80") BigDecimal total,
            @Schema(description = "Soma das quantidades dos itens do pedido.", example = "3") int quantidadeItens
    ) {
    }

    @Schema(description = "Pagina de pedidos retornada pela API REST.")
    public record PedidoPageResponse(
            @Schema(description = "Pedidos da pagina atual.") List<PedidoResponse> content,
            @Schema(description = "Indice da pagina atual, com inicio em zero.", example = "0") int page,
            @Schema(description = "Quantidade solicitada por pagina.", example = "5") int size,
            @Schema(description = "Total de pedidos encontrados.", example = "150") long totalElements,
            @Schema(description = "Total de paginas disponiveis.", example = "30") int totalPages,
            @Schema(description = "Indica se esta e a primeira pagina.", example = "true") boolean first,
            @Schema(description = "Indica se esta e a ultima pagina.", example = "false") boolean last
    ) {
    }

    @Schema(description = "Item de pedido retornado pela API REST granular.")
    public record ItemPedidoResponse(
            @Schema(description = "Identificador do item.", example = "1") Long id,
            @Schema(description = "Identificador do pedido.", example = "1") Long pedidoId,
            @Schema(description = "Identificador do produto.", example = "1") Long produtoId,
            @Schema(description = "Quantidade comprada.", example = "2") int quantidade,
            @Schema(description = "Preco unitario capturado no momento da compra.", example = "149.90") BigDecimal precoUnitario,
            @Schema(description = "Subtotal do item.", example = "299.80") BigDecimal subtotal
    ) {
    }

    @Schema(description = "Produto ranqueado no resumo de vendas.")
    public record ProdutoMaisVendidoResponse(
            @Schema(description = "Produto vendido.") ProdutoResponse produto,
            @Schema(description = "Quantidade total vendida.", example = "32") int quantidadeVendida,
            @Schema(description = "Faturamento gerado pelo produto.", example = "44716.80") BigDecimal faturamento
    ) {
    }

    @Schema(description = "Resumo agregado de vendas usado no painel administrativo.")
    public record ResumoVendasResponse(
            @Schema(description = "Total de pedidos processados.", example = "150") long totalPedidos,
            @Schema(description = "Faturamento total.", example = "1177383.65") BigDecimal faturamentoTotal,
            @Schema(description = "Ticket medio por pedido.", example = "7849.22") BigDecimal ticketMedio,
            @Schema(description = "Produtos mais vendidos.") List<ProdutoMaisVendidoResponse> produtosMaisVendidos
    ) {
    }

    @Schema(description = "Resposta padronizada para erros simples.")
    public record ErrorResponse(
            @Schema(description = "Mensagem de erro.", example = "Produto 999 nao encontrado.") String message
    ) {
    }
}
