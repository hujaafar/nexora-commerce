/* File purpose: Records whether payment is due later or simulated as authorized. */
package com.nexora.order.domain;

public enum PaymentStatus {
    DUE_ON_DELIVERY,
    AUTHORIZED,
    CANCELLED
}
