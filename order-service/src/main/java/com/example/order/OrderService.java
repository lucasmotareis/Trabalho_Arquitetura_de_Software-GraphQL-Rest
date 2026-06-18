package com.example.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final int MAX_PAGE_SIZE = 50;

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
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm")).stream()
                .map(this::toPedidoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderTypes.PedidoPageResponse listarPedidosPaginados(int page, int size, boolean includeItens) {
        Page<Pedido> pedidos = pedidoRepository.findAll(pageRequest(page, size));
        return toPageResponse(pedidos, includeItens);
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
    public OrderTypes.PedidoPageResponse listarPedidosPorClientePaginados(
            Long clienteId,
            int page,
            int size,
            boolean includeItens
    ) {
        Page<Pedido> pedidos = pedidoRepository.findByClienteId(clienteId, pageRequest(page, size));
        return toPageResponse(pedidos, includeItens);
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

    @Transactional(readOnly = true)
    public OrderTypes.ResumoVendasResponse resumoVendas(int limit) {
        long totalPedidos = pedidoRepository.count();
        List<ItemPedido> itens = itemPedidoRepository.findAll();
        BigDecimal faturamentoTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, ProdutoVendido> vendidos = new HashMap<>();
        for (ItemPedido item : itens) {
            ProdutoVendido atual = vendidos.computeIfAbsent(
                    item.getProdutoId(),
                    produtoId -> new ProdutoVendido(produtoId, 0, BigDecimal.ZERO)
            );
            atual.quantidadeVendida += item.getQuantidade();
            atual.faturamento = atual.faturamento.add(item.getSubtotal());
        }

        List<OrderTypes.ProdutoVendidoResponse> produtosMaisVendidos = vendidos.values().stream()
                .sorted(Comparator.comparingInt(ProdutoVendido::quantidadeVendida).reversed())
                .limit(safeLimit(limit))
                .map(item -> new OrderTypes.ProdutoVendidoResponse(
                        item.produtoId,
                        item.quantidadeVendida,
                        item.faturamento
                ))
                .toList();

        BigDecimal ticketMedio = totalPedidos == 0
                ? BigDecimal.ZERO
                : faturamentoTotal.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);

        return new OrderTypes.ResumoVendasResponse(
                totalPedidos,
                faturamentoTotal,
                ticketMedio,
                produtosMaisVendidos
        );
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
        return toPedidoResponse(pedido, true);
    }

    private OrderTypes.PedidoResponse toPedidoResponse(Pedido pedido, boolean includeItens) {
        List<OrderTypes.ItemPedidoResponse> itens = includeItens
                ? pedido.getItens().stream().map(this::toItemPedidoResponse).toList()
                : List.of();
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

    private OrderTypes.PedidoPageResponse toPageResponse(Page<Pedido> pedidos, boolean includeItens) {
        return new OrderTypes.PedidoPageResponse(
                pedidos.getContent().stream()
                        .map(pedido -> toPedidoResponse(pedido, includeItens))
                        .toList(),
                pedidos.getNumber(),
                pedidos.getSize(),
                pedidos.getTotalElements(),
                pedidos.getTotalPages(),
                pedidos.isFirst(),
                pedidos.isLast()
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

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                safeSize(size),
                Sort.by(Sort.Direction.DESC, "criadoEm")
        );
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 12;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private static final class ProdutoVendido {
        private final Long produtoId;
        private int quantidadeVendida;
        private BigDecimal faturamento;

        private ProdutoVendido(Long produtoId, int quantidadeVendida, BigDecimal faturamento) {
            this.produtoId = produtoId;
            this.quantidadeVendida = quantidadeVendida;
            this.faturamento = faturamento;
        }

        private int quantidadeVendida() {
            return quantidadeVendida;
        }
    }
}
