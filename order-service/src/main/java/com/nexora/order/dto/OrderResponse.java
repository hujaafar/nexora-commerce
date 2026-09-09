/*
 * File purpose: Returns full customer orders or privacy-filtered seller order views.
 */
package com.nexora.order.dto;

import com.nexora.order.domain.MarketplaceOrder;
import com.nexora.order.domain.OrderLine;
import com.nexora.order.domain.OrderStatus;
import com.nexora.order.domain.PaymentMethod;
import com.nexora.order.domain.PaymentStatus;
import com.nexora.order.domain.ShippingAddress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String orderNumber,
        String customerId,
        List<OrderLine> items,
        ShippingAddress shippingAddress,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        Instant cancelledAt) {

    public static OrderResponse forCustomer(MarketplaceOrder order) {
        return from(order, order.getCustomerId(), order.getItems(), order.getShippingAddress(),
                order.getSubtotal(), order.getDeliveryFee(), order.getTotal());
    }

    public static OrderResponse forSeller(MarketplaceOrder order, String sellerId) {
        List<OrderLine> sellerItems = order.getItems().stream()
                .filter(item -> item.sellerId().equals(sellerId))
                .toList();
        BigDecimal sellerSubtotal = sellerItems.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return from(order, mask(order.getCustomerId()), sellerItems,
                order.getShippingAddress().masked(), sellerSubtotal, BigDecimal.ZERO, sellerSubtotal);
    }

    private static OrderResponse from(
            MarketplaceOrder order,
            String customerId,
            List<OrderLine> items,
            ShippingAddress address,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal total) {
        return new OrderResponse(
                order.getId(), order.getOrderNumber(), customerId, items, address,
                order.getPaymentMethod(), order.getPaymentStatus(), order.getStatus(),
                subtotal, deliveryFee, total, order.getCreatedAt(), order.getUpdatedAt(),
                order.getCancelledAt());
    }

    private static String mask(String value) {
        return value == null || value.length() <= 4
                ? "Customer"
                : "***" + value.substring(value.length() - 4);
    }
}
