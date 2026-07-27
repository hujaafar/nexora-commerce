/* BUY-01 learning header
 * File purpose: Represents the media not found exception domain failure.
 * Learning focus: Typed domain exceptions that map cleanly to meaningful HTTP responses.
 */
package com.buy01.media.exception;

public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException() {
        super("Media asset was not found");
    }
}
