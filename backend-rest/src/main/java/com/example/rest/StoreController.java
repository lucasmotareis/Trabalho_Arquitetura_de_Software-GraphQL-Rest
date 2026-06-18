package com.example.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StoreController {
    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @GetMapping("/health")
    @Tag(name = "Health")
    @Operation(summary = "Verifica disponibilidade da API REST", responses = {
            @ApiResponse(responseCode = "200", description = "API REST disponivel")
    })
    public Map<String, String> health() {
        return Map.of("status", "UP", "api", "REST");
    }

    @GetMapping("/produtos")
    @Tag(name = "Produtos")
    @Operation(summary = "Lista produtos", description = "Retorna todos os produtos cadastrados no banco H2 de demonstracao.")
    public List<ApiTypes.ProdutoResponse> listarProdutos() {
        return service.listarProdutos();
    }

    @GetMapping("/produtos/{id}")
    @Tag(name = "Produtos")
    @Operation(summary = "Busca produto por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public ApiTypes.ProdutoResponse buscarProduto(
            @Parameter(description = "ID do produto", example = "1") @PathVariable Long id
    ) {
        return service.buscarProduto(id);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @Tag(name = "Produtos")
    @Operation(summary = "Cria produto", description = "Cadastra um novo produto no catalogo REST.", responses = {
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(responseCode = "400", description = "Payload invalido")
    })
    public ApiTypes.ProdutoResponse criarProduto(@RequestBody ApiTypes.ProdutoRequest request) {
        return service.criarProduto(request);
    }

    @PutMapping("/produtos/{id}")
    @Tag(name = "Produtos")
    @Operation(summary = "Atualiza produto", responses = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public ApiTypes.ProdutoResponse atualizarProduto(
            @Parameter(description = "ID do produto", example = "1") @PathVariable Long id,
            @RequestBody ApiTypes.ProdutoRequest request
    ) {
        return service.atualizarProduto(id, request);
    }

    @DeleteMapping("/produtos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Tag(name = "Produtos")
    @Operation(summary = "Remove produto", responses = {
            @ApiResponse(responseCode = "204", description = "Produto removido"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public void removerProduto(
            @Parameter(description = "ID do produto", example = "1") @PathVariable Long id
    ) {
        service.removerProduto(id);
    }

    @GetMapping("/clientes")
    @Tag(name = "Clientes")
    @Operation(summary = "Lista clientes", description = "Retorna todos os clientes cadastrados.")
    public List<ApiTypes.ClienteResponse> listarClientes() {
        return service.listarClientes();
    }

    @GetMapping("/clientes/{id}")
    @Tag(name = "Clientes")
    @Operation(summary = "Busca cliente por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public ApiTypes.ClienteResponse buscarCliente(
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id
    ) {
        return service.buscarCliente(id);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    @Tag(name = "Clientes")
    @Operation(summary = "Cria cliente", responses = {
            @ApiResponse(responseCode = "201", description = "Cliente criado"),
            @ApiResponse(responseCode = "400", description = "Payload invalido")
    })
    public ApiTypes.ClienteResponse criarCliente(@RequestBody ApiTypes.ClienteRequest request) {
        return service.criarCliente(request);
    }

    @PutMapping("/clientes/{id}")
    @Tag(name = "Clientes")
    @Operation(summary = "Atualiza cliente", responses = {
            @ApiResponse(responseCode = "200", description = "Cliente atualizado"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public ApiTypes.ClienteResponse atualizarCliente(
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id,
            @RequestBody ApiTypes.ClienteRequest request
    ) {
        return service.atualizarCliente(id, request);
    }

    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Tag(name = "Clientes")
    @Operation(summary = "Remove cliente", responses = {
            @ApiResponse(responseCode = "204", description = "Cliente removido"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public void removerCliente(
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id
    ) {
        service.removerCliente(id);
    }

    @GetMapping("/clientes/{id}/pedidos")
    @Tag(name = "Pedidos")
    @Operation(summary = "Lista pedidos de um cliente", description = "Endpoint granular usado pelo frontend REST para montar historico do cliente.")
    public List<ApiTypes.PedidoResponse> listarPedidosPorCliente(
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id
    ) {
        return service.listarPedidosPorCliente(id);
    }

    @GetMapping("/pedidos")
    @Tag(name = "Pedidos")
    @Operation(summary = "Lista pedidos", description = "Retorna resumos de todos os pedidos.")
    public List<ApiTypes.PedidoResponse> listarPedidos() {
        return service.listarPedidos();
    }

    @GetMapping("/pedidos/{id}")
    @Tag(name = "Pedidos")
    @Operation(summary = "Busca pedido por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado")
    })
    public ApiTypes.PedidoResponse buscarPedido(
            @Parameter(description = "ID do pedido", example = "1") @PathVariable Long id
    ) {
        return service.buscarPedido(id);
    }

    @GetMapping("/pedidos/{id}/itens")
    @Tag(name = "Pedidos")
    @Operation(summary = "Lista itens de um pedido", description = "Endpoint granular usado para buscar produtos relacionados ao pedido.")
    public List<ApiTypes.ItemPedidoResponse> listarItensDoPedido(
            @Parameter(description = "ID do pedido", example = "1") @PathVariable Long id
    ) {
        return service.listarItensDoPedido(id);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    @Tag(name = "Pedidos")
    @Operation(summary = "Cria pedido", description = "Cria um pedido com um ou mais itens e baixa estoque dos produtos.", responses = {
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Payload invalido ou estoque insuficiente")
    })
    public ApiTypes.PedidoResponse criarPedido(@RequestBody ApiTypes.PedidoRequest request) {
        return service.criarPedido(request);
    }

    @PutMapping("/pedidos/{id}/status")
    @Tag(name = "Pedidos")
    @Operation(summary = "Atualiza status do pedido", responses = {
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado")
    })
    public ApiTypes.PedidoResponse atualizarStatusPedido(
            @Parameter(description = "ID do pedido", example = "1") @PathVariable Long id,
            @RequestBody ApiTypes.StatusRequest request
    ) {
        return service.atualizarStatusPedido(id, request);
    }
}
