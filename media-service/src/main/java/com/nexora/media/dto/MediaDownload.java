/*
 * File purpose: Defines the media download API data contract.
 */
package com.nexora.media.dto;

import java.util.Arrays;
import java.util.Objects;

public record MediaDownload(
        byte[] content,
        String contentType,
        String originalFilename,
        String eTag) {

    public MediaDownload {
        // A defensive copy prevents callers from changing the downloaded data
        // after this immutable API value has been created.
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaDownload that)) {
            return false;
        }
        return Arrays.equals(content, that.content)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(originalFilename, that.originalFilename)
                && Objects.equals(eTag, that.eTag);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + Objects.hash(contentType, originalFilename, eTag);
        return result;
    }

    @Override
    public String toString() {
        // Report a content fingerprint rather than logging every image byte.
        return "MediaDownload[contentHash="
                + Arrays.hashCode(content)
                + ", contentType="
                + contentType
                + ", originalFilename="
                + originalFilename
                + ", eTag="
                + eTag
                + "]";
    }
}
