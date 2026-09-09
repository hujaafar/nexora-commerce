/* File purpose: Validates address and payment choice for checkout. */
package com.nexora.order.dto;

import com.nexora.order.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @Valid @NotNull(message = "Shipping address is required") ShippingAddressRequest shippingAddress,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod) {
}
