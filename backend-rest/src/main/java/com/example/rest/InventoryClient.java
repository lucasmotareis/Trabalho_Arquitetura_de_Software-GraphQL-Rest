package com.example.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
public class InventoryClient {
    private final InternalHttpClient httpClient;

    public InventoryClient(
            @Value("${spring.application.name:backend-rest}") String source,
            @Value("${app.services.inventory-url:http://localhost:8084/api}") String inventoryUrl,
            ObjectMapper objectMapper
    ) {
        this.httpClient = new InternalHttpClient(source, "inventory-service", inventoryUrl, objectMapper);
    }

    public int buscarQuantidade(Long produtoId) {
        return httpClient.get("/estoques/" + produtoId, EstoqueResponse.class).quantidade();
    }

    public List<EstoqueResponse> listar(Collection<Long> produtoIds) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            return List.of();
        }
        String ids = String.join(",", produtoIds.stream().map(String::valueOf).toList());
        EstoqueResponse[] response = httpClient.get("/estoques?produtoIds=" + ids, EstoqueResponse[].class);
        return Arrays.asList(response);
    }

    public List<EstoqueResponse> listarCriticos(int limit) {
        EstoqueResponse[] response = httpClient.get("/estoques/criticos?limit=" + limit, EstoqueResponse[].class);
        return Arrays.asList(response);
    }

    public void definirQuantidade(Long produtoId, int quantidade) {
        httpClient.put("/estoques/" + produtoId, new EstoqueRequest(quantidade), EstoqueResponse.class);
    }

    public record EstoqueRequest(Integer quantidade) {
    }

    public record EstoqueResponse(Long produtoId, int quantidade) {
    }
}
