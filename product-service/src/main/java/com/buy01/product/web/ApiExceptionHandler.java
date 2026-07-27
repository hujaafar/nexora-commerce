/*
 * File purpose: Translates validation and domain failures into stable JSON errors.
 */
package com.buy01.product.web;

import com.buy01.product.exception.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Learning annotation: @RestControllerAdvice applies centralized exception handling to all REST controllers in this service.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed",
                request.getRequestURI(),
                errors);
        return ResponseEntity.badRequest().body(body);
    }

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid or contains an unsupported value",
                request);
    }

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
            ProductNotFoundException exception,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleForbidden(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access is denied", request);
    }

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Unhandled error while processing {}", request.getRequestURI(), exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()));
    }
}
