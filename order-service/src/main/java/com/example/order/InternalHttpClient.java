package com.example.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class InternalHttpClient {
    private final String source;
    private final String target;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public InternalHttpClient(String source, String target, String baseUrl, ObjectMapper objectMapper) {
        this.source = source;
        this.target = target;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public <T> T get(String path, Class<T> responseType) {
        return exchange(HttpMethod.GET, path, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(HttpMethod.POST, path, body, responseType);
    }

    private <T> T exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        String requestText = writeBody(body);
        long start = System.nanoTime();
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(path);
            if (!requestText.isEmpty()) {
                spec.contentType(MediaType.APPLICATION_JSON).body(requestText);
            }
            ResponseEntity<String> response = spec.retrieve().toEntity(String.class);
            String responseText = response.getBody() == null ? "" : response.getBody();
            record(method, path, response.getStatusCode().value(), elapsedMs(start), responseText, requestText, response);
            if (responseType == Void.class) {
                return null;
            }
            return objectMapper.readValue(responseText, responseType);
        } catch (RestClientResponseException exception) {
            String responseText = exception.getResponseBodyAsString();
            record(method, path, exception.getStatusCode().value(), elapsedMs(start), responseText, requestText, null);
            throw new IllegalArgumentException(extractErrorMessage(responseText, exception.getMessage()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao processar resposta interna de " + target + ".", exception);
        }
    }

    private void record(
            HttpMethod method,
            String path,
            int status,
            double clientMs,
            String responseText,
            String requestText,
            ResponseEntity<String> response
    ) {
        double backendMs = 0;
        if (response != null) {
            backendMs = parseDouble(response.getHeaders().getFirst(TimingFilter.BACKEND_TIME_HEADER));
            BackendTrace.mergeHeader(response.getHeaders().getFirst(BackendTrace.HEADER), objectMapper);
        }
        BackendTrace.record(new BackendTrace.TraceEntry(
                source,
                target,
                method.name(),
                path,
                status,
                clientMs,
                backendMs,
                bytes(requestText),
                bytes(responseText),
                BackendTrace.sample(requestText),
                BackendTrace.sample(responseText)
        ));
    }

    private String writeBody(Object body) {
        if (body == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Payload interno invalido.", exception);
        }
    }

    private String extractErrorMessage(String responseText, String fallback) {
        if (responseText == null || responseText.isBlank()) {
            return fallback;
        }
        try {
            OrderTypes.ErrorResponse error = objectMapper.readValue(responseText, OrderTypes.ErrorResponse.class);
            return error.message();
        } catch (JsonProcessingException ignored) {
            return responseText;
        }
    }

    private int bytes(String text) {
        return text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
    }

    private double elapsedMs(long start) {
        return round((System.nanoTime() - start) / 1_000_000.0);
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private double round(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.3f", value));
    }
}
