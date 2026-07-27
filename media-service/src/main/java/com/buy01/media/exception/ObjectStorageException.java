/* BUY-01 learning header
 * File purpose: Represents the object storage exception domain failure.
 * Learning focus: Typed domain exceptions that map cleanly to meaningful HTTP responses.
 */
package com.buy01.media.exception;

public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
