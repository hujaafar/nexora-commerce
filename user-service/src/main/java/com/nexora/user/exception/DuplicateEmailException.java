/*
 * File purpose: Represents the duplicate email exception domain failure.
 */
package com.nexora.user.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An account with this email already exists");
    }
}
