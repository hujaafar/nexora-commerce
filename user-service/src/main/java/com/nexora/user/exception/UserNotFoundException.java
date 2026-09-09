/*
 * File purpose: Represents the user not found exception domain failure.
 */
package com.nexora.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User account was not found");
    }
}
