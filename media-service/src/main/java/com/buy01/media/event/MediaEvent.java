/* BUY-01 learning header
 * File purpose: Defines the immutable Kafka event envelope.
 * Learning focus: Stable event contracts for asynchronous consumers.
 */
package com.buy01.media.event;

import com.buy01.media.domain.MediaPurpose;
import java.time.Instant;
import java.util.UUID;

public record MediaEvent(
        UUID eventId,
        EventType type,
        String mediaId,
        String sellerId,
        String productId,
        MediaPurpose purpose,
        Instant occurredAt) {

    public static MediaEvent uploaded(
            String mediaId,
            String sellerId,
            String productId,
            MediaPurpose purpose) {
        return new MediaEvent(
                UUID.randomUUID(),
                EventType.IMAGE_UPLOADED,
                mediaId,
                sellerId,
                productId,
                purpose,
                Instant.now());
    }

    public static MediaEvent deleted(
            String mediaId,
            String sellerId,
            String productId,
            MediaPurpose purpose) {
        return new MediaEvent(
                UUID.randomUUID(),
                EventType.IMAGE_DELETED,
                mediaId,
                sellerId,
                productId,
                purpose,
                Instant.now());
    }

    public enum EventType {
        IMAGE_UPLOADED,
        IMAGE_DELETED
    }
}
