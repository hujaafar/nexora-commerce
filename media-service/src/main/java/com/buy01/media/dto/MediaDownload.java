/* BUY-01 learning header
 * File purpose: Defines the media download API data contract.
 * Learning focus: Immutable record DTOs, boundary validation, and avoiding domain-object exposure.
 */
package com.buy01.media.dto;

public record MediaDownload(
        byte[] content,
        String contentType,
        String originalFilename,
        String eTag) {
}
