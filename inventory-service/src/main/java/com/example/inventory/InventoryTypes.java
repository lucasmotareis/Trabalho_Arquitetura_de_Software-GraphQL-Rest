package com.example.inventory;

public final class InventoryTypes {
    private InventoryTypes() {
    }

    public record EstoqueRequest(Integer quantidade) {
    }

    public record BaixarEstoqueRequest(Integer quantidade) {
    }

    public record EstoqueResponse(Long produtoId, int quantidade) {
    }

    public record ErrorResponse(String message) {
    }
}
