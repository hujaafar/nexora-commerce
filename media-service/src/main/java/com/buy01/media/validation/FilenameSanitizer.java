package com.buy01.media.validation;

import com.buy01.media.exception.InvalidMediaException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

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
