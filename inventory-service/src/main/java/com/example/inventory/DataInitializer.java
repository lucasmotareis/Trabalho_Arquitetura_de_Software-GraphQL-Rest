package com.example.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final int TOTAL_PRODUTOS = 120;

    private final InventoryService service;
    private final boolean seedEnabled;

    public DataInitializer(
            InventoryService service,
            @Value("${app.seed.enabled:true}") boolean seedEnabled
    ) {
        this.service = service;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || service.hasSeedData()) {
            return;
        }
        for (long produtoId = 1; produtoId <= TOTAL_PRODUTOS; produtoId += 1) {
            service.salvarSeed(produtoId, 500 + (int) produtoId);
        }
    }
}
