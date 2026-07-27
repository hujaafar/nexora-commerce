/* BUY-01 learning header
 * File purpose: Verifies image signature validator test behavior.
 * Learning focus: Adversarial file-signature and MIME validation tests.
 */
package com.buy01.media.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buy01.media.exception.InvalidMediaException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ImageSignatureValidatorTest {

    private final ImageSignatureValidator validator = new ImageSignatureValidator();

    @Test
    void recognizesPngFromBytesRatherThanTheFilename() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        assertThat(validator.validate(png, "image/png"))
                .isEqualTo(DetectedImageType.PNG);
    }

    @Test
    void rejectsExecutableContentRenamedAsAnImage() {
        byte[] executable = "MZ fake executable".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> validator.validate(executable, "image/png"))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("genuine");
    }

    @Test
    void rejectsADeclaredTypeThatDoesNotMatchTheSignature() {
        byte[] gif = "GIF89a".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> validator.validate(gif, "image/jpeg"))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("does not match");
    }
}
