/* BUY-01 learning header
 * File purpose: Represents the duplicate email exception domain failure.
 * Learning focus: Typed domain exceptions that map cleanly to meaningful HTTP responses.
 */
package com.buy01.user.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An account with this email already exists");
    }
}
