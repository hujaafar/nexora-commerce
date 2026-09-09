/*
 * File purpose: Rejects calls that do not carry the trusted service-to-service token.
 */
package com.nexora.product.exception;

public class InternalAccessDeniedException extends RuntimeException {

    public InternalAccessDeniedException() {
        super("Internal service authentication failed");
    }
}
