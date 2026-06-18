package com.example.inventory;

import java.util.List;

public final class InventoryTypes {
    private InventoryTypes() {
    }

    public record EstoqueRequest(Integer quantidade) {
    }

    public record BaixarEstoqueRequest(Integer quantidade) {
    }

    public record EstoqueResponse(Long produtoId, int quantidade) {
    }

    public record EstoquePageResponse(
            List<EstoqueResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }

    public record ErrorResponse(String message) {
    }
}
