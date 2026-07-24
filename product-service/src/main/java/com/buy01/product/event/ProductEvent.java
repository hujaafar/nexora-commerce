package com.buy01.product.event;

import java.time.Instant;
import java.util.UUID;

public record ProductEvent(
        UUID eventId,
        EventType type,
        String productId,
        String sellerId,
        Instant occurredAt) {

    public static ProductEvent of(
            EventType type,
            String productId,
            String sellerId) {
        return new ProductEvent(
                UUID.randomUUID(),
                type,
                productId,
                sellerId,
                Instant.now());
    }

    public enum EventType {
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED
    }
}
