package com.example.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI lojaVirtualOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backend REST - Loja Virtual")
                        .version("1.0.0")
                        .description("""
                                API REST granular usada no trabalho comparativo REST vs GraphQL.
                                A documentacao mostra endpoints por recurso para produtos, clientes e pedidos.
                                """)
                        .license(new License().name("Uso academico")))
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("Servidor local do backend REST")))
                .tags(List.of(
                        new Tag().name("Health").description("Verificacao simples de disponibilidade da API REST."),
                        new Tag().name("Produtos").description("Operacoes REST de cadastro, consulta, atualizacao e remocao de produtos."),
                        new Tag().name("Clientes").description("Operacoes REST de cadastro, consulta, atualizacao e remocao de clientes."),
                        new Tag().name("Pedidos").description("Operacoes REST granulares para pedidos, status e itens.")
                ));
    }
}
