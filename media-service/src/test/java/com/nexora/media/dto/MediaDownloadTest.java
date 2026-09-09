/*
 * File purpose: Verifies byte-array value semantics and defensive copying in
 * the media download API contract.
 */
package com.nexora.media.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaDownloadTest {

    @Test
    void comparesByteContentAndProtectsItsInternalArray() {
        byte[] source = {1, 2, 3};
        MediaDownload first = new MediaDownload(source, "image/png", "item.png", "etag");
        MediaDownload same = new MediaDownload(
                new byte[] {1, 2, 3},
                "image/png",
                "item.png",
                "etag");

        source[0] = 9;
        byte[] returned = first.content();
        returned[1] = 9;

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(first.content()).containsExactly(1, 2, 3);
        assertThat(first.toString()).contains("contentHash=").doesNotContain("[1, 2, 3]");
    }
}
