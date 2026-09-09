/* File purpose: Defines auditable commerce lifecycle events published to Kafka. */
package com.nexora.order.event;

import java.time.Instant;

public record OrderEvent(String type, String orderId, String customerId, Instant occurredAt) {
    public static OrderEvent of(String type, String orderId, String customerId) {
        return new OrderEvent(type, orderId, customerId, Instant.now());
    }
}
