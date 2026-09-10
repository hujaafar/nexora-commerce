package com.nexora.order.domain;

public record OrderPayment(PaymentMethod method, PaymentStatus status) {
}
