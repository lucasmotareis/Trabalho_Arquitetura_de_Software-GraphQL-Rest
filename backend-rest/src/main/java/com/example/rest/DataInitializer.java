package com.example.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final int TOTAL_PRODUTOS = 120;
    private static final int TOTAL_CLIENTES = 120;

    private static final String[] CATEGORIAS = {
            "Eletronicos",
            "Acessorios",
            "Moveis",
            "Casa",
            "Escritorio",
            "Games",
            "Livros",
            "Esporte"
    };

    private static final String[] CIDADES = {
            "Curitiba",
            "Sao Paulo",
            "Belo Horizonte",
            "Rio de Janeiro",
            "Porto Alegre",
            "Recife",
            "Salvador",
            "Florianopolis"
    };

    private final StoreService service;
    private final boolean seedEnabled;

    public DataInitializer(
            StoreService service,
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
        criarProdutos();
        criarClientes();
    }

    private List<Produto> criarProdutos() {
        List<Produto> produtos = new ArrayList<>();

        for (int index = 1; index <= TOTAL_PRODUTOS; index++) {
            String categoria = CATEGORIAS[(index - 1) % CATEGORIAS.length];
            BigDecimal preco = BigDecimal.valueOf(49.90 + (index * 13.75) % 4900)
                    .setScale(2, RoundingMode.HALF_UP);
            produtos.add(service.salvarProdutoSeed(
                    "Produto " + String.format("%03d", index),
                    "Item " + index + " da categoria " + categoria + " usado para comparacao REST vs GraphQL",
                    categoria,
                    preco
            ));
        }

        return produtos;
    }

    private List<Cliente> criarClientes() {
        List<Cliente> clientes = new ArrayList<>();

        for (int index = 1; index <= TOTAL_CLIENTES; index++) {
            clientes.add(service.salvarClienteSeed(
                    "Cliente " + String.format("%03d", index),
                    "cliente" + String.format("%03d", index) + "@email.com",
                    CIDADES[(index - 1) % CIDADES.length]
            ));
        }

        return clientes;
    }
}
