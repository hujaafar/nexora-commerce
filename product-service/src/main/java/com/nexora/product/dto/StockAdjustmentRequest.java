/*
 * File purpose: Validates an internal stock reservation or release request.
 */
package com.nexora.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StockAdjustmentRequest(
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100")
        int quantity) {
}
