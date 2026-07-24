package com.buy01.media.dto;

public record MediaDownload(
        byte[] content,
        String contentType,
        String originalFilename,
        String eTag) {
}
