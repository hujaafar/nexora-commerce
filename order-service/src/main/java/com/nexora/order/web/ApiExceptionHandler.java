/* File purpose: Converts validation, ownership, state, and unexpected failures into stable JSON. */
package com.nexora.order.web;

import com.nexora.order.exception.CommerceConflictException;
import com.nexora.order.exception.ResourceNotFoundException;
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
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                details.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "VALIDATION_FAILED", "Request validation failed",
                request.getRequestURI(), details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request data is invalid", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(CommerceConflictException.class)
    ResponseEntity<ApiError> conflict(CommerceConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled commerce error for {}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), code, message, request.getRequestURI()));
    }
}
