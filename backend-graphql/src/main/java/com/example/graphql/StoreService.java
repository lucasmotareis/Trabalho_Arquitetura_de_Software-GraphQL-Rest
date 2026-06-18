package com.example.graphql;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreService {
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public StoreService(
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository
    ) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<GqlTypes.ProdutoPayload> produtos() {
        return produtoRepository.findAll().stream().map(this::toProdutoPayload).toList();
    }

    @Transactional(readOnly = true)
    public GqlTypes.ProdutoPayload produto(Long id) {
        return toProdutoPayload(findProduto(id));
    }

    @Transactional(readOnly = true)
    public GqlTypes.ClientePayload cliente(Long id) {
        Cliente cliente = findCliente(id);
        List<GqlTypes.PedidoPayload> pedidos = pedidoRepository.findByClienteIdOrderByCriadoEmDesc(id).stream()
                .map(this::toPedidoPayload)
                .toList();
        return new GqlTypes.ClientePayload(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCidade(),
                pedidos
        );
    }

    @Transactional(readOnly = true)
    public GqlTypes.PedidoPayload pedido(Long id) {
        return toPedidoPayload(findPedido(id));
    }

    @Transactional(readOnly = true)
    public List<GqlTypes.PedidoPayload> pedidosPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente " + clienteId + " nao encontrado.");
        }
        return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(this::toPedidoPayload)
                .toList();
    }

    @Transactional(readOnly = true)
    public GqlTypes.ResumoVendasPayload resumoVendas() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        double faturamentoTotal = pedidos.stream()
                .map(Pedido::getTotal)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        Map<Long, ProdutoVendido> vendidos = new HashMap<>();
        for (Pedido pedido : pedidos) {
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = item.getProduto();
                ProdutoVendido acumulado = vendidos.computeIfAbsent(
                        produto.getId(),
                        ignored -> new ProdutoVendido(produto, 0, 0.0)
                );
                acumulado.quantidade += item.getQuantidade();
                acumulado.faturamento += item.getSubtotal().doubleValue();
            }
        }

        List<GqlTypes.ProdutoMaisVendidoPayload> produtosMaisVendidos = vendidos.values().stream()
                .sorted(Comparator.comparingInt(ProdutoVendido::quantidade).reversed())
                .map(produtoVendido -> new GqlTypes.ProdutoMaisVendidoPayload(
                        toProdutoPayload(produtoVendido.produto),
                        produtoVendido.quantidade,
                        produtoVendido.faturamento
                ))
                .toList();

        return new GqlTypes.ResumoVendasPayload(
                pedidos.size(),
                faturamentoTotal,
                pedidos.isEmpty() ? 0.0 : faturamentoTotal / pedidos.size(),
                produtosMaisVendidos
        );
    }

    @Transactional
    public GqlTypes.ProdutoPayload criarProduto(GqlTypes.ProdutoInput input) {
        Produto produto = new Produto(
                requiredText(input.nome(), "Nome do produto"),
                requiredText(input.descricao(), "Descricao do produto"),
                requiredText(input.categoria(), "Categoria do produto"),
                requiredMoney(input.preco()),
                requiredPositiveOrZero(input.estoque(), "Estoque")
        );
        return toProdutoPayload(produtoRepository.save(produto));
    }

    @Transactional
    public GqlTypes.ProdutoPayload atualizarProduto(Long id, GqlTypes.ProdutoInput input) {
        Produto produto = findProduto(id);
        produto.setNome(requiredText(input.nome(), "Nome do produto"));
        produto.setDescricao(requiredText(input.descricao(), "Descricao do produto"));
        produto.setCategoria(requiredText(input.categoria(), "Categoria do produto"));
        produto.setPreco(requiredMoney(input.preco()));
        produto.setEstoque(requiredPositiveOrZero(input.estoque(), "Estoque"));
        return toProdutoPayload(produto);
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

        Pedido pedido = new Pedido(cliente, "CRIADO", LocalDateTime.now());
        for (GqlTypes.PedidoItemInput itemInput : input.itens()) {
            Produto produto = findProduto(itemInput.produtoId());
            int quantidade = requiredPositive(itemInput.quantidade(), "Quantidade");
            produto.baixarEstoque(quantidade);
            pedido.adicionarItem(new ItemPedido(produto, quantidade, produto.getPreco()));
        }
        return toPedidoPayload(pedidoRepository.save(pedido));
    }

    @Transactional
    public GqlTypes.PedidoPayload atualizarStatusPedido(Long id, String status) {
        Pedido pedido = findPedido(id);
        pedido.setStatus(requiredText(status, "Status do pedido").toUpperCase());
        return toPedidoPayload(pedido);
    }

    Produto salvarProdutoSeed(String nome, String descricao, String categoria, BigDecimal preco, int estoque) {
        return produtoRepository.save(new Produto(nome, descricao, categoria, preco, estoque));
    }

    Cliente salvarClienteSeed(String nome, String email, String cidade) {
        return clienteRepository.save(new Cliente(nome, email, cidade));
    }

    GqlTypes.PedidoPayload criarPedidoSeed(Long clienteId, LocalDateTime criadoEm, String status, List<GqlTypes.PedidoItemInput> itens) {
        Cliente cliente = findCliente(clienteId);
        Pedido pedido = new Pedido(cliente, status, criadoEm);
        for (GqlTypes.PedidoItemInput item : itens) {
            Produto produto = findProduto(item.produtoId());
            int quantidade = requiredPositive(item.quantidade(), "Quantidade");
            produto.baixarEstoque(quantidade);
            pedido.adicionarItem(new ItemPedido(produto, quantidade, produto.getPreco()));
        }
        return toPedidoPayload(pedidoRepository.save(pedido));
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

    private GqlTypes.ProdutoPayload toProdutoPayload(Produto produto) {
        return new GqlTypes.ProdutoPayload(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getCategoria(),
                produto.getPreco().doubleValue(),
                produto.getEstoque()
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

    private GqlTypes.PedidoPayload toPedidoPayload(Pedido pedido) {
        List<GqlTypes.ItemPedidoPayload> itens = pedido.getItens().stream()
                .map(this::toItemPedidoPayload)
                .toList();
        return new GqlTypes.PedidoPayload(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getCriadoEm().toString(),
                pedido.getTotal().doubleValue(),
                pedido.getItens().stream().mapToInt(ItemPedido::getQuantidade).sum(),
                toClienteResumoPayload(pedido.getCliente()),
                itens
        );
    }

    private GqlTypes.ItemPedidoPayload toItemPedidoPayload(ItemPedido item) {
        return new GqlTypes.ItemPedidoPayload(
                item.getId(),
                item.getQuantidade(),
                item.getPrecoUnitario().doubleValue(),
                item.getSubtotal().doubleValue(),
                toProdutoPayload(item.getProduto())
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

    private static final class ProdutoVendido {
        private final Produto produto;
        private int quantidade;
        private double faturamento;

        private ProdutoVendido(Produto produto, int quantidade, double faturamento) {
            this.produto = produto;
            this.quantidade = quantidade;
            this.faturamento = faturamento;
        }

        private int quantidade() {
            return quantidade;
        }
    }
}
