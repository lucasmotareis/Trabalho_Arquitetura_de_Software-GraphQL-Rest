package com.example.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final InventoryClient inventoryClient;

    public OrderService(
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemPedidoRepository,
            InventoryClient inventoryClient
    ) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.inventoryClient = inventoryClient;
    }

    @Transactional(readOnly = true)
    public List<OrderTypes.PedidoResponse> listarPedidos() {
        return pedidoRepository.findAll().stream()
                .sorted(Comparator.comparing(Pedido::getCriadoEm).reversed())
                .map(this::toPedidoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderTypes.PedidoResponse buscarPedido(Long id) {
        return toPedidoResponse(findPedido(id));
    }

    @Transactional(readOnly = true)
    public List<OrderTypes.PedidoResponse> listarPedidosPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(this::toPedidoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderTypes.ItemPedidoResponse> listarItensDoPedido(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new ResourceNotFoundException("Pedido " + pedidoId + " nao encontrado.");
        }
        return itemPedidoRepository.findByPedidoId(pedidoId).stream()
                .map(this::toItemPedidoResponse)
                .toList();
    }

    @Transactional
    public OrderTypes.PedidoResponse criarPedido(OrderTypes.PedidoRequest request) {
        validatePedidoRequest(request);
        Pedido pedido = new Pedido(request.clienteId(), "CRIADO", LocalDateTime.now());
        for (OrderTypes.PedidoItemRequest item : request.itens()) {
            Long produtoId = requiredId(item.produtoId(), "Produto");
            int quantidade = requiredPositive(item.quantidade(), "Quantidade");
            BigDecimal precoUnitario = requiredMoney(item.precoUnitario());
            inventoryClient.baixarEstoque(produtoId, quantidade);
            pedido.adicionarItem(new ItemPedido(produtoId, quantidade, precoUnitario));
        }
        return toPedidoResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public OrderTypes.PedidoResponse atualizarStatusPedido(Long id, OrderTypes.StatusRequest request) {
        Pedido pedido = findPedido(id);
        pedido.setStatus(requiredText(request.status(), "Status do pedido").toUpperCase());
        return toPedidoResponse(pedido);
    }

    boolean hasSeedData() {
        return pedidoRepository.count() > 0;
    }

    @Transactional
    void criarPedidoSeed(Long clienteId, LocalDateTime criadoEm, String status, List<OrderTypes.PedidoItemRequest> itens) {
        Pedido pedido = new Pedido(clienteId, status, criadoEm);
        for (OrderTypes.PedidoItemRequest item : itens) {
            pedido.adicionarItem(new ItemPedido(
                    item.produtoId(),
                    requiredPositive(item.quantidade(), "Quantidade"),
                    requiredMoney(item.precoUnitario())
            ));
        }
        pedidoRepository.save(pedido);
    }

    private Pedido findPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + id + " nao encontrado."));
    }

    private OrderTypes.PedidoResponse toPedidoResponse(Pedido pedido) {
        List<OrderTypes.ItemPedidoResponse> itens = pedido.getItens().stream()
                .map(this::toItemPedidoResponse)
                .toList();
        return new OrderTypes.PedidoResponse(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getStatus(),
                pedido.getCriadoEm(),
                pedido.getTotal(),
                pedido.getItens().stream().mapToInt(ItemPedido::getQuantidade).sum(),
                itens
        );
    }

    private OrderTypes.ItemPedidoResponse toItemPedidoResponse(ItemPedido item) {
        return new OrderTypes.ItemPedidoResponse(
                item.getId(),
                item.getPedido().getId(),
                item.getProdutoId(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }

    private void validatePedidoRequest(OrderTypes.PedidoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Pedido e obrigatorio.");
        }
        requiredId(request.clienteId(), "Cliente");
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("Pedido deve possuir ao menos um item.");
        }
    }

    private Long requiredId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " invalido.");
        }
        return value;
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
}
