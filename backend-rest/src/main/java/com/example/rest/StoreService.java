package com.example.rest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoreService {
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
    public List<ApiTypes.ProdutoResponse> listarProdutos() {
        List<Produto> produtos = produtoRepository.findAll();
        Map<Long, Integer> estoques = estoquesPorProduto(produtos.stream().map(Produto::getId).toList());
        return produtos.stream()
                .map(produto -> toProdutoResponse(produto, estoques.getOrDefault(produto.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiTypes.ProdutoResponse buscarProduto(Long id) {
        Produto produto = findProduto(id);
        return toProdutoResponse(produto, inventoryClient.buscarQuantidade(produto.getId()));
    }

    @Transactional
    public ApiTypes.ProdutoResponse criarProduto(ApiTypes.ProdutoRequest request) {
        Produto produto = new Produto(
                requiredText(request.nome(), "Nome do produto"),
                requiredText(request.descricao(), "Descricao do produto"),
                requiredText(request.categoria(), "Categoria do produto"),
                requiredMoney(request.preco())
        );
        Produto salvo = produtoRepository.save(produto);
        int estoque = requiredPositiveOrZero(request.estoque(), "Estoque");
        inventoryClient.definirQuantidade(salvo.getId(), estoque);
        return toProdutoResponse(salvo, estoque);
    }

    @Transactional
    public ApiTypes.ProdutoResponse atualizarProduto(Long id, ApiTypes.ProdutoRequest request) {
        Produto produto = findProduto(id);
        produto.setNome(requiredText(request.nome(), "Nome do produto"));
        produto.setDescricao(requiredText(request.descricao(), "Descricao do produto"));
        produto.setCategoria(requiredText(request.categoria(), "Categoria do produto"));
        produto.setPreco(requiredMoney(request.preco()));
        int estoque = requiredPositiveOrZero(request.estoque(), "Estoque");
        inventoryClient.definirQuantidade(produto.getId(), estoque);
        return toProdutoResponse(produto, estoque);
    }

    @Transactional
    public void removerProduto(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produto " + id + " nao encontrado.");
        }
        produtoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.ClienteResponse> listarClientes() {
        return clienteRepository.findAll().stream().map(this::toClienteResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApiTypes.ClienteResponse buscarCliente(Long id) {
        return toClienteResponse(findCliente(id));
    }

    @Transactional
    public ApiTypes.ClienteResponse criarCliente(ApiTypes.ClienteRequest request) {
        Cliente cliente = new Cliente(
                requiredText(request.nome(), "Nome do cliente"),
                requiredText(request.email(), "Email do cliente"),
                requiredText(request.cidade(), "Cidade do cliente")
        );
        return toClienteResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ApiTypes.ClienteResponse atualizarCliente(Long id, ApiTypes.ClienteRequest request) {
        Cliente cliente = findCliente(id);
        cliente.setNome(requiredText(request.nome(), "Nome do cliente"));
        cliente.setEmail(requiredText(request.email(), "Email do cliente"));
        cliente.setCidade(requiredText(request.cidade(), "Cidade do cliente"));
        return toClienteResponse(cliente);
    }

    @Transactional
    public void removerCliente(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente " + id + " nao encontrado.");
        }
        clienteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.PedidoResponse> listarPedidos() {
        return orderClient.listarPedidos();
    }

    @Transactional(readOnly = true)
    public ApiTypes.PedidoResponse buscarPedido(Long id) {
        return orderClient.buscarPedido(id);
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.PedidoResponse> listarPedidosPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente " + clienteId + " nao encontrado.");
        }
        return orderClient.listarPedidosPorCliente(clienteId);
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.ItemPedidoResponse> listarItensDoPedido(Long pedidoId) {
        return orderClient.listarItensDoPedido(pedidoId);
    }

    @Transactional
    public ApiTypes.PedidoResponse criarPedido(ApiTypes.PedidoRequest request) {
        findCliente(request.clienteId());
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("Pedido deve possuir ao menos um item.");
        }
        List<OrderClient.PedidoItemInternoRequest> itens = request.itens().stream()
                .map(item -> {
                    Produto produto = findProduto(item.produtoId());
                    int quantidade = requiredPositive(item.quantidade(), "Quantidade");
                    return new OrderClient.PedidoItemInternoRequest(produto.getId(), quantidade, produto.getPreco());
                })
                .toList();
        return orderClient.criarPedido(new OrderClient.PedidoInternoRequest(request.clienteId(), itens));
    }

    @Transactional
    public ApiTypes.PedidoResponse atualizarStatusPedido(Long id, ApiTypes.StatusRequest request) {
        return orderClient.atualizarStatus(id, request);
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

    private ApiTypes.ProdutoResponse toProdutoResponse(Produto produto, int estoque) {
        return new ApiTypes.ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getCategoria(),
                produto.getPreco(),
                estoque
        );
    }

    private ApiTypes.ClienteResponse toClienteResponse(Cliente cliente) {
        return new ApiTypes.ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCidade()
        );
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return value.trim();
    }

    private BigDecimal requiredMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preco deve ser maior que zero.");
        }
        return value;
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
}
