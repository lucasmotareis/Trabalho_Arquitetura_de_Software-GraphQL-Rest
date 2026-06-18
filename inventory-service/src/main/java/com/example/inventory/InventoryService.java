package com.example.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class InventoryService {
    private static final int MAX_PAGE_SIZE = 50;

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

    @Transactional(readOnly = true)
    public InventoryTypes.EstoquePageResponse listarPaginado(int page, int size) {
        Page<Estoque> estoques = repository.findAll(PageRequest.of(
                Math.max(page, 0),
                safeSize(size),
                Sort.by(Sort.Direction.ASC, "produtoId")
        ));
        return new InventoryTypes.EstoquePageResponse(
                estoques.getContent().stream().map(this::toResponse).toList(),
                estoques.getNumber(),
                estoques.getSize(),
                estoques.getTotalElements(),
                estoques.getTotalPages(),
                estoques.isFirst(),
                estoques.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<InventoryTypes.EstoqueResponse> listarCriticos(int limit) {
        return repository.findAll(PageRequest.of(
                        0,
                        safeLimit(limit),
                        Sort.by(Sort.Direction.ASC, "quantidade").and(Sort.by(Sort.Direction.ASC, "produtoId"))
                ))
                .getContent()
                .stream()
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

    private int safeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 12;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }
}
