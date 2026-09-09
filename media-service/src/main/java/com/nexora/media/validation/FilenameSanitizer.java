/*
 * File purpose: Validates or normalizes uploaded image data through filename sanitizer.
 */
package com.nexora.media.validation;

import com.nexora.media.exception.InvalidMediaException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
public class FilenameSanitizer {

    public String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image";
        }
        try {
            String normalizedSeparators = originalFilename.replace('\\', '/');
            String basename = Path.of(normalizedSeparators).getFileName().toString();
            if (basename.contains("..") || basename.chars().anyMatch(Character::isISOControl)) {
                throw new InvalidMediaException("The filename is invalid");
            }
            String safe = basename.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
            return safe.isBlank() ? "image" : safe;
        } catch (InvalidPathException exception) {
            throw new InvalidMediaException("The filename is invalid");
        }
    }
}
