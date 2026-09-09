/* File purpose: Produces a controlled 409 for stock or order-state conflicts. */
package com.nexora.order.exception;

public class CommerceConflictException extends RuntimeException {
    public CommerceConflictException(String message) {
        super(message);
    }
}
