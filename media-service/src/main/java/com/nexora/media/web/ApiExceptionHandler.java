/*
 * File purpose: Translates validation and domain failures into stable JSON errors.
 */
package com.nexora.media.web;

import com.nexora.media.exception.InvalidMediaException;
import com.nexora.media.exception.MediaNotFoundException;
import com.nexora.media.exception.ObjectStorageException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

// Learning annotation: @RestControllerAdvice applies centralized exception handling to all REST controllers in this service.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler({
            InvalidMediaException.class,
            MaxUploadSizeExceededException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiError> handleBadRequest(
            Exception exception,
            HttpServletRequest request) {
        String message = exception instanceof MaxUploadSizeExceededException
                ? "The image must be 2 MB or smaller"
                : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    // Learning annotation: @ExceptionHandler routes the listed exception type(s) to this method for a controlled HTTP response.
    @ExceptionHandler(MediaNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
            MediaNotFoundException exception,
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
    @ExceptionHandler(ObjectStorageException.class)
    ResponseEntity<ApiError> handleStorage(
            ObjectStorageException exception,
            HttpServletRequest request) {
        LOGGER.error("Object storage failure while processing {}", request.getRequestURI(), exception);
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Image storage is temporarily unavailable",
                request);
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
