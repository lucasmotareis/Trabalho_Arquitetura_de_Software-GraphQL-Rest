package com.example.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public final class BackendTrace {
    static final String HEADER = "X-Backend-Trace";
    private static final int SAMPLE_LIMIT = 300;
    private static final ThreadLocal<List<TraceEntry>> ENTRIES = ThreadLocal.withInitial(ArrayList::new);

    private BackendTrace() {
    }

    static void reset() {
        ENTRIES.get().clear();
    }

    static void clear() {
        ENTRIES.remove();
    }

    static void record(TraceEntry entry) {
        ENTRIES.get().add(entry);
    }

    static void mergeHeader(String headerValue, ObjectMapper objectMapper) {
        if (headerValue == null || headerValue.isBlank()) {
            return;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(headerValue);
            TraceEntry[] entries = objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), TraceEntry[].class);
            ENTRIES.get().addAll(Arrays.asList(entries));
        } catch (IllegalArgumentException | JsonProcessingException ignored) {
            // Trace is diagnostic only; application flow should not fail because of it.
        }
    }

    static String headerValue(ObjectMapper objectMapper) {
        List<TraceEntry> entries = ENTRIES.get();
        if (entries.isEmpty()) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(entries);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            return "";
        }
    }

    static String sample(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= SAMPLE_LIMIT ? text : text.substring(0, SAMPLE_LIMIT) + "...";
    }

    public record TraceEntry(
            String source,
            String target,
            String method,
            String path,
            int status,
            double clientMs,
            double backendMs,
            int requestPayloadBytes,
            int responsePayloadBytes,
            String requestPayloadText,
            String responsePayloadText
    ) {
    }
}
