/* File purpose: Sends a validated inventory amount to product-service. */
package com.nexora.order.client;

public record StockAdjustment(int quantity) {
}
