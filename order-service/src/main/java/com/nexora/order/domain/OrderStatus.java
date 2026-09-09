/* File purpose: Lists the controlled states in the order lifecycle. */
package com.nexora.order.domain;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PACKING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
