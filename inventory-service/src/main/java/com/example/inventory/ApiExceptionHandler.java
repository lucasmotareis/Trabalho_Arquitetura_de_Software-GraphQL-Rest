package com.example.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public InventoryTypes.ErrorResponse notFound(ResourceNotFoundException exception) {
        return new InventoryTypes.ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InventoryTypes.ErrorResponse badRequest(IllegalArgumentException exception) {
        return new InventoryTypes.ErrorResponse(exception.getMessage());
    }
}
