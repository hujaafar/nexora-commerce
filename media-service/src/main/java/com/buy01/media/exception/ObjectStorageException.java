/*
 * File purpose: Represents the object storage exception domain failure.
 */
package com.buy01.media.exception;

public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
