/*
 * File purpose: Represents the user not found exception domain failure.
 */
package com.buy01.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User account was not found");
    }
}
