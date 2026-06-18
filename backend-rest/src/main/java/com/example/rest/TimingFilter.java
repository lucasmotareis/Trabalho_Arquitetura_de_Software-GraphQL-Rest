package com.example.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Component
public class TimingFilter extends OncePerRequestFilter {
    static final String BACKEND_TIME_HEADER = "X-Backend-Time-Ms";
    static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    private final ObjectMapper objectMapper;

    public TimingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.nanoTime();
        BackendTrace.reset();
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            String trace = BackendTrace.headerValue(objectMapper);
            wrappedResponse.setHeader(BACKEND_TIME_HEADER, String.format(Locale.US, "%.3f", elapsedMs));
            if (!trace.isBlank()) {
                wrappedResponse.setHeader(BackendTrace.HEADER, trace);
            }
            wrappedResponse.setHeader(EXPOSE_HEADERS, BACKEND_TIME_HEADER + ", " + BackendTrace.HEADER);
            BackendTrace.clear();
            wrappedResponse.copyBodyToResponse();
        }
    }
}
