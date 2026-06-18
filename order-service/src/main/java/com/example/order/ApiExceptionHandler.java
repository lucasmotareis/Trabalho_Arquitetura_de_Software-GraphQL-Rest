package com.example.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public OrderTypes.ErrorResponse notFound(ResourceNotFoundException exception) {
        return new OrderTypes.ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public OrderTypes.ErrorResponse badRequest(IllegalArgumentException exception) {
        return new OrderTypes.ErrorResponse(exception.getMessage());
    }
}
