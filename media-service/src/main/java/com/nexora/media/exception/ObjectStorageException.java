/*
 * File purpose: Represents the object storage exception domain failure.
 */
package com.nexora.media.exception;

public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
