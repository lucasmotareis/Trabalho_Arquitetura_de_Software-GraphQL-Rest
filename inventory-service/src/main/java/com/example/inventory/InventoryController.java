package com.example.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "inventory-service");
    }

    @GetMapping("/estoques")
    public List<InventoryTypes.EstoqueResponse> listar(@RequestParam(required = false) List<Long> produtoIds) {
        return service.listar(produtoIds);
    }

    @GetMapping("/estoques/{produtoId}")
    public InventoryTypes.EstoqueResponse buscar(@PathVariable Long produtoId) {
        return service.buscar(produtoId);
    }

    @PutMapping("/estoques/{produtoId}")
    public InventoryTypes.EstoqueResponse definir(
            @PathVariable Long produtoId,
            @RequestBody InventoryTypes.EstoqueRequest request
    ) {
        return service.definir(produtoId, request);
    }

    @PostMapping("/estoques/{produtoId}/baixar")
    public InventoryTypes.EstoqueResponse baixar(
            @PathVariable Long produtoId,
            @RequestBody InventoryTypes.BaixarEstoqueRequest request
    ) {
        return service.baixar(produtoId, request);
    }
}
