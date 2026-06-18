package com.example.graphql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StoreService {
    private static final int MAX_PAGE_SIZE = 50;

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;

    public StoreService(
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            OrderClient orderClient,
            InventoryClient inventoryClient
    ) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
    }

    @Transactional(readOnly = true)
    public List<GqlTypes.ProdutoPayload> produtos() {
        List<Produto> produtos = produtoRepository.findAll();
        Map<Long, Integer> estoques = estoquesPorProduto(produtos.stream().map(Produto::getId).toList());
        return produtos.stream()
                .map(produto -> toProdutoPayload(produto, estoques.getOrDefault(produto.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GqlTypes.ProdutoPagePayload produtosPaginados(Integer page, Integer size) {
        Page<Produto> produtos = produtoRepository.findAll(PageRequest.of(
                safePage(page),
                safeSize(size, 12),
                Sort.by(Sort.Direction.ASC, "id")
        ));
        Map<Long, Integer> estoques = estoquesPorProduto(produtos.getContent().stream().map(Produto::getId).toList());
        return new GqlTypes.ProdutoPagePayload(
                produtos.getContent().stream()
                        .map(produto -> toProdutoPayload(produto, estoques.getOrDefault(produto.getId(), 0)))
                        .toList(),
                toPageInfo(produtos)
        );
    }

    @Transactional(readOnly = true)
    public GqlTypes.ProdutoPayload produto(Long id) {
        Produto produto = findProduto(id);
        return toProdutoPayload(produto, inventoryClient.buscarQuantidade(produto.getId()));
    }

    @Transactional(readOnly = true)
    public GqlTypes.ClientePayload cliente(Long id) {
        Cliente cliente = findCliente(id);
        List<OrderClient.PedidoResponse> pedidos = orderClient.listarPedidosPorCliente(id);
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(produtoIds(pedidos));
        List<GqlTypes.PedidoPayload> pedidosPayload = pedidos.stream()
                .map(pedido -> toPedidoPayload(pedido, cliente, produtos))
                .toList();
        return new GqlTypes.ClientePayload(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCidade(),
                pedidosPayload
        );
    }

    @Transactional(readOnly = true)
    public GqlTypes.PedidoPayload pedido(Long id) {
        OrderClient.PedidoResponse pedido = orderClient.buscarPedido(id);
        Cliente cliente = findCliente(pedido.clienteId());
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(produtoIds(List.of(pedido)));
        return toPedidoPayload(pedido, cliente, produtos);
    }

    @Transactional(readOnly = true)
    public List<GqlTypes.PedidoPayload> pedidosPorCliente(Long clienteId) {
        Cliente cliente = findCliente(clienteId);
        List<OrderClient.PedidoResponse> pedidos = orderClient.listarPedidosPorCliente(clienteId);
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(produtoIds(pedidos));
        return pedidos.stream()
                .map(pedido -> toPedidoPayload(pedido, cliente, produtos))
                .toList();
    }

    @Transactional(readOnly = true)
    public GqlTypes.PedidoPagePayload pedidosPorClientePaginado(Long clienteId, Integer page, Integer size) {
        Cliente cliente = findCliente(clienteId);
        OrderClient.PedidoPageResponse pedidos = orderClient.listarPedidosPorClientePaginados(
                clienteId,
                safePage(page),
                safeSize(size, 5),
                true
        );
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(produtoIds(pedidos.content()));
        return new GqlTypes.PedidoPagePayload(
                pedidos.content().stream()
                        .map(pedido -> toPedidoPayload(pedido, cliente, produtos))
                        .toList(),
                new GqlTypes.PageInfoPayload(
                        pedidos.page(),
                        pedidos.size(),
                        pedidos.totalElements(),
                        pedidos.totalPages(),
                        pedidos.first(),
                        pedidos.last()
                )
        );
    }

    @Transactional(readOnly = true)
    public GqlTypes.ResumoVendasPayload resumoVendas(Integer limit) {
        OrderClient.ResumoVendasInternoResponse resumo = orderClient.resumoVendas(safeLimit(limit));
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(
                resumo.produtosMaisVendidos().stream()
                        .map(OrderClient.ProdutoVendidoInternoResponse::produtoId)
                        .toList()
        );
        List<GqlTypes.ProdutoMaisVendidoPayload> produtosMaisVendidos = resumo.produtosMaisVendidos().stream()
                .map(item -> new GqlTypes.ProdutoMaisVendidoPayload(
                        produtos.get(item.produtoId()),
                        item.quantidadeVendida(),
                        item.faturamento().doubleValue()
                ))
                .filter(item -> item.produto() != null)
                .toList();
        return new GqlTypes.ResumoVendasPayload(
                (int) resumo.totalPedidos(),
                resumo.faturamentoTotal().doubleValue(),
                resumo.ticketMedio().doubleValue(),
                produtosMaisVendidos
        );
    }

    @Transactional(readOnly = true)
    public List<GqlTypes.ProdutoPayload> estoqueCritico(Integer limit) {
        List<InventoryClient.EstoqueResponse> estoques = inventoryClient.listarCriticos(safeLimit(limit));
        Map<Long, Produto> produtos = produtoRepository.findAllById(
                        estoques.stream().map(InventoryClient.EstoqueResponse::produtoId).toList()
                )
                .stream()
                .collect(Collectors.toMap(Produto::getId, produto -> produto));
        return estoques.stream()
                .map(estoque -> {
                    Produto produto = produtos.get(estoque.produtoId());
                    return produto == null ? null : toProdutoPayload(produto, estoque.quantidade());
                })
                .filter(produto -> produto != null)
                .toList();
    }

    @Transactional
    public GqlTypes.ProdutoPayload criarProduto(GqlTypes.ProdutoInput input) {
        Produto produto = new Produto(
                requiredText(input.nome(), "Nome do produto"),
                requiredText(input.descricao(), "Descricao do produto"),
                requiredText(input.categoria(), "Categoria do produto"),
                requiredMoney(input.preco())
        );
        Produto salvo = produtoRepository.save(produto);
        int estoque = requiredPositiveOrZero(input.estoque(), "Estoque");
        inventoryClient.definirQuantidade(salvo.getId(), estoque);
        return toProdutoPayload(salvo, estoque);
    }

    @Transactional
    public GqlTypes.ProdutoPayload atualizarProduto(Long id, GqlTypes.ProdutoInput input) {
        Produto produto = findProduto(id);
        produto.setNome(requiredText(input.nome(), "Nome do produto"));
        produto.setDescricao(requiredText(input.descricao(), "Descricao do produto"));
        produto.setCategoria(requiredText(input.categoria(), "Categoria do produto"));
        produto.setPreco(requiredMoney(input.preco()));
        int estoque = requiredPositiveOrZero(input.estoque(), "Estoque");
        inventoryClient.definirQuantidade(produto.getId(), estoque);
        return toProdutoPayload(produto, estoque);
    }

    @Transactional
    public boolean removerProduto(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produto " + id + " nao encontrado.");
        }
        produtoRepository.deleteById(id);
        return true;
    }

    @Transactional
    public GqlTypes.ClienteResumoPayload criarCliente(GqlTypes.ClienteInput input) {
        Cliente cliente = new Cliente(
                requiredText(input.nome(), "Nome do cliente"),
                requiredText(input.email(), "Email do cliente"),
                requiredText(input.cidade(), "Cidade do cliente")
        );
        return toClienteResumoPayload(clienteRepository.save(cliente));
    }

    @Transactional
    public GqlTypes.PedidoPayload criarPedido(GqlTypes.PedidoInput input) {
        Cliente cliente = findCliente(input.clienteId());
        if (input.itens() == null || input.itens().isEmpty()) {
            throw new IllegalArgumentException("Pedido deve possuir ao menos um item.");
        }
        List<OrderClient.PedidoItemInternoRequest> itens = input.itens().stream()
                .map(item -> {
                    Produto produto = findProduto(item.produtoId());
                    int quantidade = requiredPositive(item.quantidade(), "Quantidade");
                    return new OrderClient.PedidoItemInternoRequest(produto.getId(), quantidade, produto.getPreco());
                })
                .toList();
        OrderClient.PedidoResponse pedido = orderClient.criarPedido(
                new OrderClient.PedidoInternoRequest(input.clienteId(), itens)
        );
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(produtoIds(List.of(pedido)));
        return toPedidoPayload(pedido, cliente, produtos);
    }

    @Transactional
    public GqlTypes.PedidoPayload atualizarStatusPedido(Long id, String status) {
        OrderClient.PedidoResponse pedido = orderClient.atualizarStatus(id, new OrderClient.StatusRequest(status));
        Cliente cliente = findCliente(pedido.clienteId());
        Map<Long, GqlTypes.ProdutoPayload> produtos = produtosPayloadMap(produtoIds(List.of(pedido)));
        return toPedidoPayload(pedido, cliente, produtos);
    }

    boolean hasSeedData() {
        return produtoRepository.count() > 0 || clienteRepository.count() > 0;
    }

    Produto salvarProdutoSeed(String nome, String descricao, String categoria, BigDecimal preco) {
        return produtoRepository.save(new Produto(nome, descricao, categoria, preco));
    }

    Cliente salvarClienteSeed(String nome, String email, String cidade) {
        return clienteRepository.save(new Cliente(nome, email, cidade));
    }

    private GqlTypes.PedidoPayload toPedidoPayload(
            OrderClient.PedidoResponse pedido,
            Cliente cliente,
            Map<Long, GqlTypes.ProdutoPayload> produtos
    ) {
        List<GqlTypes.ItemPedidoPayload> itens = pedido.itens().stream()
                .map(item -> toItemPedidoPayload(item, produtos.get(item.produtoId())))
                .toList();
        return new GqlTypes.PedidoPayload(
                pedido.id(),
                pedido.status(),
                pedido.criadoEm().toString(),
                pedido.total().doubleValue(),
                pedido.quantidadeItens(),
                toClienteResumoPayload(cliente),
                itens
        );
    }

    private GqlTypes.ItemPedidoPayload toItemPedidoPayload(
            OrderClient.ItemPedidoResponse item,
            GqlTypes.ProdutoPayload produto
    ) {
        return new GqlTypes.ItemPedidoPayload(
                item.id(),
                item.quantidade(),
                item.precoUnitario().doubleValue(),
                item.subtotal().doubleValue(),
                produto
        );
    }

    private Map<Long, GqlTypes.ProdutoPayload> produtosPayloadMap(Collection<Long> produtoIds) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            return Map.of();
        }
        List<Long> uniqueIds = produtoIds.stream().distinct().toList();
        List<Produto> produtos = produtoRepository.findAllById(uniqueIds);
        Map<Long, Integer> estoques = estoquesPorProduto(uniqueIds);
        return produtos.stream()
                .map(produto -> toProdutoPayload(produto, estoques.getOrDefault(produto.getId(), 0)))
                .collect(Collectors.toMap(GqlTypes.ProdutoPayload::id, produto -> produto));
    }

    private List<Long> produtoIds(List<OrderClient.PedidoResponse> pedidos) {
        return pedidos.stream()
                .flatMap(pedido -> pedido.itens().stream())
                .map(OrderClient.ItemPedidoResponse::produtoId)
                .distinct()
                .toList();
    }

    private Map<Long, Integer> estoquesPorProduto(Collection<Long> produtoIds) {
        return inventoryClient.listar(produtoIds).stream()
                .collect(Collectors.toMap(InventoryClient.EstoqueResponse::produtoId, InventoryClient.EstoqueResponse::quantidade));
    }

    private Produto findProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto " + id + " nao encontrado."));
    }

    private Cliente findCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + id + " nao encontrado."));
    }

    private GqlTypes.ProdutoPayload toProdutoPayload(Produto produto, int estoque) {
        return new GqlTypes.ProdutoPayload(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getCategoria(),
                produto.getPreco().doubleValue(),
                estoque
        );
    }

    private GqlTypes.ClienteResumoPayload toClienteResumoPayload(Cliente cliente) {
        return new GqlTypes.ClienteResumoPayload(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCidade()
        );
    }

    private GqlTypes.PageInfoPayload toPageInfo(Page<?> page) {
        return new GqlTypes.PageInfoPayload(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return value.trim();
    }

    private BigDecimal requiredMoney(Double value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Preco deve ser maior que zero.");
        }
        return BigDecimal.valueOf(value);
    }

    private int requiredPositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " deve ser maior que zero.");
        }
        return value;
    }

    private int requiredPositiveOrZero(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " nao pode ser negativo.");
        }
        return value;
    }

    private int safePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int safeSize(Integer size, int defaultSize) {
        if (size == null || size <= 0) {
            return defaultSize;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 12;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

}
