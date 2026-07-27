/*
 * File purpose: Defines the media download API data contract.
 */
package com.buy01.media.dto;

public record MediaDownload(
        byte[] content,
        String contentType,
        String originalFilename,
        String eTag) {
}
