/* BUY-01 learning header
 * File purpose: Validates or normalizes uploaded image data through detected image type.
 * Learning focus: Secure file handling with allowlists, magic bytes, and path-safe names.
 */
package com.buy01.media.validation;

public enum DetectedImageType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    DetectedImageType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}
