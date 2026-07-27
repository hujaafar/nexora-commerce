/* BUY-01 learning header
 * File purpose: Represents the user not found exception domain failure.
 * Learning focus: Typed domain exceptions that map cleanly to meaningful HTTP responses.
 */
package com.buy01.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User account was not found");
    }
}
