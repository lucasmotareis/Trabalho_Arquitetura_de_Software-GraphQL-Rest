package com.example.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final int TOTAL_CLIENTES = 120;
    private static final int TOTAL_PRODUTOS = 120;
    private static final int TOTAL_PEDIDOS = 150;

    private static final String[] STATUS = {
            "CRIADO",
            "PAGO",
            "ENVIADO",
            "ENTREGUE",
            "CANCELADO"
    };

    private final OrderService service;
    private final boolean seedEnabled;

    public DataInitializer(
            OrderService service,
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
        LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 8, 0);
        for (int index = 1; index <= TOTAL_PEDIDOS; index += 1) {
            Long clienteId = (long) (((index - 1) % TOTAL_CLIENTES) + 1);
            service.criarPedidoSeed(
                    clienteId,
                    inicio.plusHours(index * 7L),
                    STATUS[(index - 1) % STATUS.length],
                    criarItens(index)
            );
        }
    }

    private List<OrderTypes.PedidoItemRequest> criarItens(int pedidoIndex) {
        List<OrderTypes.PedidoItemRequest> itens = new ArrayList<>();
        int totalItens = 2 + (pedidoIndex % 4);
        for (int itemIndex = 0; itemIndex < totalItens; itemIndex += 1) {
            long produtoId = (((long) pedidoIndex * 3) + itemIndex * 7) % TOTAL_PRODUTOS + 1;
            int quantidade = 1 + ((pedidoIndex + itemIndex) % 4);
            itens.add(new OrderTypes.PedidoItemRequest(
                    produtoId,
                    quantidade,
                    precoProduto((int) produtoId)
            ));
        }
        return itens;
    }

    private BigDecimal precoProduto(int index) {
        return BigDecimal.valueOf(49.90 + (index * 13.75) % 4900)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
