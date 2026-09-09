/* File purpose: Produces a controlled 404 when an owned commerce resource is unavailable. */
package com.nexora.order.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
