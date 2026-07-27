/* BUY-01 learning header
 * File purpose: Represents the invalid media exception domain failure.
 * Learning focus: Typed domain exceptions that map cleanly to meaningful HTTP responses.
 */
package com.buy01.media.exception;

public class InvalidMediaException extends RuntimeException {

    public InvalidMediaException(String message) {
        super(message);
    }
}
