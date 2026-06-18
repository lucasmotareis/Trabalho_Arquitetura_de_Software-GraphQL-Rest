package com.example.rest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final int TOTAL_PRODUTOS = 120;
    private static final int TOTAL_CLIENTES = 120;
    private static final int TOTAL_PEDIDOS = 150;

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

    private static final String[] STATUS = {
            "CRIADO",
            "PAGO",
            "ENVIADO",
            "ENTREGUE",
            "CANCELADO"
    };

    private final StoreService service;

    public DataInitializer(StoreService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        List<Produto> produtos = criarProdutos();
        List<Cliente> clientes = criarClientes();
        criarPedidos(clientes, produtos);
    }

    private List<Produto> criarProdutos() {
        List<Produto> produtos = new ArrayList<>();

        for (int index = 1; index <= TOTAL_PRODUTOS; index++) {
            String categoria = CATEGORIAS[(index - 1) % CATEGORIAS.length];
            BigDecimal preco = BigDecimal.valueOf(49.90 + (index * 13.75) % 4900)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            produtos.add(service.salvarProdutoSeed(
                    "Produto " + String.format("%03d", index),
                    "Item " + index + " da categoria " + categoria + " usado para comparacao REST vs GraphQL",
                    categoria,
                    preco,
                    500 + index
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

    private void criarPedidos(List<Cliente> clientes, List<Produto> produtos) {
        LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 8, 0);

        for (int index = 1; index <= TOTAL_PEDIDOS; index++) {
            Cliente cliente = clientes.get((index - 1) % clientes.size());
            List<ApiTypes.PedidoItemRequest> itens = new ArrayList<>();

            int totalItens = 2 + (index % 4);
            for (int itemIndex = 0; itemIndex < totalItens; itemIndex++) {
                Produto produto = produtos.get(((index * 3) + itemIndex * 7) % produtos.size());
                int quantidade = 1 + ((index + itemIndex) % 4);
                itens.add(new ApiTypes.PedidoItemRequest(produto.getId(), quantidade));
            }

            service.criarPedidoSeed(
                    cliente.getId(),
                    inicio.plusHours(index * 7L),
                    STATUS[(index - 1) % STATUS.length],
                    itens
            );
        }
    }
}

