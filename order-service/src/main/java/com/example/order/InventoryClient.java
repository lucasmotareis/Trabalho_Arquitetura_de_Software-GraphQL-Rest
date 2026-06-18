package com.example.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InventoryClient {
    private final InternalHttpClient httpClient;

    public InventoryClient(
            @Value("${spring.application.name:order-service}") String source,
            @Value("${app.services.inventory-url:http://localhost:8084/api}") String inventoryUrl,
            ObjectMapper objectMapper
    ) {
        this.httpClient = new InternalHttpClient(source, "inventory-service", inventoryUrl, objectMapper);
    }

    public void baixarEstoque(Long produtoId, int quantidade) {
        httpClient.post(
                "/estoques/" + produtoId + "/baixar",
                new OrderTypes.BaixarEstoqueRequest(quantidade),
                InventoryResponse.class
        );
    }

    private record InventoryResponse(Long produtoId, int quantidade) {
    }
}
