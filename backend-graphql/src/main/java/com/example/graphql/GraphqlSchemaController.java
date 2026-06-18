package com.example.graphql;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class GraphqlSchemaController {
    @GetMapping(value = "/graphql/schema", produces = MediaType.TEXT_PLAIN_VALUE)
    public String schema() throws IOException {
        Resource resource = new ClassPathResource("graphql/schema.graphqls");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
