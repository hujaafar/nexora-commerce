/*
 * File purpose: Represents the media not found exception domain failure.
 */
package com.nexora.media.exception;

public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException() {
        super("Media asset was not found");
    }
}
