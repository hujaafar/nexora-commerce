/* BUY-01 learning header
 * File purpose: Represents the product not found exception domain failure.
 * Learning focus: Typed domain exceptions that map cleanly to meaningful HTTP responses.
 */
package com.buy01.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException() {
        super("Product was not found");
    }
}
