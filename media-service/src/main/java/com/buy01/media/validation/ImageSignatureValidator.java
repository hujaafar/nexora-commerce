package com.buy01.media.validation;

import com.buy01.media.exception.InvalidMediaException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ImageSignatureValidator {

    public DetectedImageType validate(byte[] content, String declaredContentType) {
        DetectedImageType detected = detect(content);
        String normalizedDeclaredType = normalizeContentType(declaredContentType);
        if (!detected.contentType().equals(normalizedDeclaredType)) {
            throw new InvalidMediaException(
                    "The declared file type does not match the image content");
        }
        return detected;
    }

    private DetectedImageType detect(byte[] content) {
        if (startsWith(content, new int[] {0xFF, 0xD8, 0xFF})) {
            return DetectedImageType.JPEG;
        }
        if (startsWith(content, new int[] {
                0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        })) {
            return DetectedImageType.PNG;
        }
        if (hasAscii(content, 0, "GIF87a") || hasAscii(content, 0, "GIF89a")) {
            return DetectedImageType.GIF;
        }
        if (hasAscii(content, 0, "RIFF") && hasAscii(content, 8, "WEBP")) {
            return DetectedImageType.WEBP;
        }
        throw new InvalidMediaException(
                "Only genuine JPEG, PNG, GIF, or WebP images are accepted");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new InvalidMediaException("The upload must include an image content type");
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).trim();
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private boolean startsWith(byte[] content, int[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAscii(byte[] content, int offset, String expected) {
        byte[] signature = expected.getBytes(StandardCharsets.US_ASCII);
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[offset + index] != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
