package com.example.rest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreService {
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public StoreService(
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemPedidoRepository
    ) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.ProdutoResponse> listarProdutos() {
        return produtoRepository.findAll().stream().map(this::toProdutoResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApiTypes.ProdutoResponse buscarProduto(Long id) {
        return toProdutoResponse(findProduto(id));
    }

    @Transactional
    public ApiTypes.ProdutoResponse criarProduto(ApiTypes.ProdutoRequest request) {
        Produto produto = new Produto(
                requiredText(request.nome(), "Nome do produto"),
                requiredText(request.descricao(), "Descricao do produto"),
                requiredText(request.categoria(), "Categoria do produto"),
                requiredMoney(request.preco()),
                requiredPositiveOrZero(request.estoque(), "Estoque")
        );
        return toProdutoResponse(produtoRepository.save(produto));
    }

    @Transactional
    public ApiTypes.ProdutoResponse atualizarProduto(Long id, ApiTypes.ProdutoRequest request) {
        Produto produto = findProduto(id);
        produto.setNome(requiredText(request.nome(), "Nome do produto"));
        produto.setDescricao(requiredText(request.descricao(), "Descricao do produto"));
        produto.setCategoria(requiredText(request.categoria(), "Categoria do produto"));
        produto.setPreco(requiredMoney(request.preco()));
        produto.setEstoque(requiredPositiveOrZero(request.estoque(), "Estoque"));
        return toProdutoResponse(produto);
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
        return pedidoRepository.findAll().stream().map(this::toPedidoResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApiTypes.PedidoResponse buscarPedido(Long id) {
        return toPedidoResponse(findPedido(id));
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.PedidoResponse> listarPedidosPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente " + clienteId + " nao encontrado.");
        }
        return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(this::toPedidoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiTypes.ItemPedidoResponse> listarItensDoPedido(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new ResourceNotFoundException("Pedido " + pedidoId + " nao encontrado.");
        }
        return itemPedidoRepository.findByPedidoId(pedidoId).stream()
                .map(this::toItemPedidoResponse)
                .toList();
    }

    @Transactional
    public ApiTypes.PedidoResponse criarPedido(ApiTypes.PedidoRequest request) {
        Cliente cliente = findCliente(request.clienteId());
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("Pedido deve possuir ao menos um item.");
        }

        Pedido pedido = new Pedido(cliente, "CRIADO", LocalDateTime.now());
        for (ApiTypes.PedidoItemRequest itemRequest : request.itens()) {
            Produto produto = findProduto(itemRequest.produtoId());
            int quantidade = requiredPositive(itemRequest.quantidade(), "Quantidade");
            produto.baixarEstoque(quantidade);
            pedido.adicionarItem(new ItemPedido(produto, quantidade, produto.getPreco()));
        }

        return toPedidoResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public ApiTypes.PedidoResponse atualizarStatusPedido(Long id, ApiTypes.StatusRequest request) {
        Pedido pedido = findPedido(id);
        pedido.setStatus(requiredText(request.status(), "Status do pedido").toUpperCase());
        return toPedidoResponse(pedido);
    }

    Produto salvarProdutoSeed(String nome, String descricao, String categoria, BigDecimal preco, int estoque) {
        return produtoRepository.save(new Produto(nome, descricao, categoria, preco, estoque));
    }

    Cliente salvarClienteSeed(String nome, String email, String cidade) {
        return clienteRepository.save(new Cliente(nome, email, cidade));
    }

    ApiTypes.PedidoResponse criarPedidoSeed(Long clienteId, LocalDateTime criadoEm, String status, List<ApiTypes.PedidoItemRequest> itens) {
        Cliente cliente = findCliente(clienteId);
        Pedido pedido = new Pedido(cliente, status, criadoEm);
        for (ApiTypes.PedidoItemRequest item : itens) {
            Produto produto = findProduto(item.produtoId());
            int quantidade = requiredPositive(item.quantidade(), "Quantidade");
            produto.baixarEstoque(quantidade);
            pedido.adicionarItem(new ItemPedido(produto, quantidade, produto.getPreco()));
        }
        return toPedidoResponse(pedidoRepository.save(pedido));
    }

    private Produto findProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto " + id + " nao encontrado."));
    }

    private Cliente findCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + id + " nao encontrado."));
    }

    private Pedido findPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + id + " nao encontrado."));
    }

    private ApiTypes.ProdutoResponse toProdutoResponse(Produto produto) {
        return new ApiTypes.ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getCategoria(),
                produto.getPreco(),
                produto.getEstoque()
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

    private ApiTypes.PedidoResponse toPedidoResponse(Pedido pedido) {
        return new ApiTypes.PedidoResponse(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getStatus(),
                pedido.getCriadoEm(),
                pedido.getTotal(),
                pedido.getItens().stream().mapToInt(ItemPedido::getQuantidade).sum()
        );
    }

    private ApiTypes.ItemPedidoResponse toItemPedidoResponse(ItemPedido item) {
        return new ApiTypes.ItemPedidoResponse(
                item.getId(),
                item.getPedido().getId(),
                item.getProduto().getId(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
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

