/*
 * File purpose: Represents the product not found exception domain failure.
 */
package com.nexora.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException() {
        super("Product was not found");
    }
}
