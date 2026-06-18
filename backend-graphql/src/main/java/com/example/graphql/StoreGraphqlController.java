package com.example.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class StoreGraphqlController {
    private final StoreService service;

    public StoreGraphqlController(StoreService service) {
        this.service = service;
    }

    @QueryMapping
    public List<GqlTypes.ProdutoPayload> produtos() {
        return service.produtos();
    }

    @QueryMapping
    public GqlTypes.ProdutoPagePayload produtosPaginados(
            @Argument Integer page,
            @Argument Integer size
    ) {
        return service.produtosPaginados(page, size);
    }

    @QueryMapping
    public GqlTypes.ProdutoPayload produto(@Argument Long id) {
        return service.produto(id);
    }

    @QueryMapping
    public GqlTypes.ClientePayload cliente(@Argument Long id) {
        return service.cliente(id);
    }

    @QueryMapping
    public GqlTypes.PedidoPayload pedido(@Argument Long id) {
        return service.pedido(id);
    }

    @QueryMapping
    public List<GqlTypes.PedidoPayload> pedidosPorCliente(@Argument Long clienteId) {
        return service.pedidosPorCliente(clienteId);
    }

    @QueryMapping
    public GqlTypes.PedidoPagePayload pedidosPorClientePaginado(
            @Argument Long clienteId,
            @Argument Integer page,
            @Argument Integer size
    ) {
        return service.pedidosPorClientePaginado(clienteId, page, size);
    }

    @QueryMapping
    public GqlTypes.ResumoVendasPayload resumoVendas(@Argument Integer limit) {
        return service.resumoVendas(limit);
    }

    @QueryMapping
    public List<GqlTypes.ProdutoPayload> estoqueCritico(@Argument Integer limit) {
        return service.estoqueCritico(limit);
    }

    @MutationMapping
    public GqlTypes.ProdutoPayload criarProduto(@Argument GqlTypes.ProdutoInput input) {
        return service.criarProduto(input);
    }

    @MutationMapping
    public GqlTypes.ProdutoPayload atualizarProduto(
            @Argument Long id,
            @Argument GqlTypes.ProdutoInput input
    ) {
        return service.atualizarProduto(id, input);
    }

    @MutationMapping
    public Boolean removerProduto(@Argument Long id) {
        return service.removerProduto(id);
    }

    @MutationMapping
    public GqlTypes.ClienteResumoPayload criarCliente(@Argument GqlTypes.ClienteInput input) {
        return service.criarCliente(input);
    }

    @MutationMapping
    public GqlTypes.PedidoPayload criarPedido(@Argument GqlTypes.PedidoInput input) {
        return service.criarPedido(input);
    }

    @MutationMapping
    public GqlTypes.PedidoPayload atualizarStatusPedido(
            @Argument Long id,
            @Argument String status
    ) {
        return service.atualizarStatusPedido(id, status);
    }
}
