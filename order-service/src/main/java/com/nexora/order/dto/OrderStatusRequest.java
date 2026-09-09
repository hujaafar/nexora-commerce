/* File purpose: Validates the seller's requested next order status. */
package com.nexora.order.dto;

import com.nexora.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull(message = "Order status is required") OrderStatus status) {
}
