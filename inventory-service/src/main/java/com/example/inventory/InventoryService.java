package com.example.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class InventoryService {
    private final EstoqueRepository repository;

    public InventoryService(EstoqueRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public InventoryTypes.EstoqueResponse buscar(Long produtoId) {
        return toResponse(findByProdutoId(produtoId));
    }

    @Transactional(readOnly = true)
    public List<InventoryTypes.EstoqueResponse> listar(List<Long> produtoIds) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            return repository.findAll().stream()
                    .sorted(Comparator.comparing(Estoque::getProdutoId))
                    .map(this::toResponse)
                    .toList();
        }
        return repository.findByProdutoIdIn(produtoIds).stream()
                .sorted(Comparator.comparing(Estoque::getProdutoId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryTypes.EstoqueResponse definir(Long produtoId, InventoryTypes.EstoqueRequest request) {
        int quantidade = requiredPositiveOrZero(request.quantidade());
        Estoque estoque = repository.findByProdutoId(produtoId)
                .orElseGet(() -> new Estoque(produtoId, quantidade));
        estoque.setQuantidade(quantidade);
        return toResponse(repository.save(estoque));
    }

    @Transactional
    public InventoryTypes.EstoqueResponse baixar(Long produtoId, InventoryTypes.BaixarEstoqueRequest request) {
        Estoque estoque = findByProdutoId(produtoId);
        estoque.baixar(requiredPositive(request.quantidade()));
        return toResponse(estoque);
    }

    boolean hasSeedData() {
        return repository.count() > 0;
    }

    void salvarSeed(Long produtoId, int quantidade) {
        if (repository.findByProdutoId(produtoId).isEmpty()) {
            repository.save(new Estoque(produtoId, quantidade));
        }
    }

    private Estoque findByProdutoId(Long produtoId) {
        return repository.findByProdutoId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque do produto " + produtoId + " nao encontrado."));
    }

    private InventoryTypes.EstoqueResponse toResponse(Estoque estoque) {
        return new InventoryTypes.EstoqueResponse(estoque.getProdutoId(), estoque.getQuantidade());
    }

    private int requiredPositive(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        return value;
    }

    private int requiredPositiveOrZero(Integer value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Quantidade nao pode ser negativa.");
        }
        return value;
    }
}
