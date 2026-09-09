/*
 * File purpose: Signals that a checkout cannot reserve the requested inventory.
 */
package com.nexora.product.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException() {
        super("The requested product quantity is no longer available");
    }
}
