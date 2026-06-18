package com.example.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "order-service");
    }

    @GetMapping("/pedidos")
    public List<OrderTypes.PedidoResponse> listarPedidos() {
        return service.listarPedidos();
    }

    @GetMapping("/pedidos/paginados")
    public OrderTypes.PedidoPageResponse listarPedidosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includeItens
    ) {
        return service.listarPedidosPaginados(page, size, includeItens);
    }

    @GetMapping("/pedidos/{id}")
    public OrderTypes.PedidoResponse buscarPedido(@PathVariable Long id) {
        return service.buscarPedido(id);
    }

    @GetMapping("/clientes/{clienteId}/pedidos")
    public List<OrderTypes.PedidoResponse> listarPedidosPorCliente(@PathVariable Long clienteId) {
        return service.listarPedidosPorCliente(clienteId);
    }

    @GetMapping("/clientes/{clienteId}/pedidos/paginados")
    public OrderTypes.PedidoPageResponse listarPedidosPorClientePaginados(
            @PathVariable Long clienteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "false") boolean includeItens
    ) {
        return service.listarPedidosPorClientePaginados(clienteId, page, size, includeItens);
    }

    @GetMapping("/pedidos/{id}/itens")
    public List<OrderTypes.ItemPedidoResponse> listarItensDoPedido(@PathVariable Long id) {
        return service.listarItensDoPedido(id);
    }

    @GetMapping("/vendas/resumo")
    public OrderTypes.ResumoVendasResponse resumoVendas(@RequestParam(defaultValue = "12") int limit) {
        return service.resumoVendas(limit);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderTypes.PedidoResponse criarPedido(@RequestBody OrderTypes.PedidoRequest request) {
        return service.criarPedido(request);
    }

    @PutMapping("/pedidos/{id}/status")
    public OrderTypes.PedidoResponse atualizarStatusPedido(
            @PathVariable Long id,
            @RequestBody OrderTypes.StatusRequest request
    ) {
        return service.atualizarStatusPedido(id, request);
    }
}
