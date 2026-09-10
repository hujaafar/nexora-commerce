/*
 * File purpose: Models ownership, totals, payment, and legal order-status transitions.
 */
package com.nexora.order.domain;

import com.nexora.order.exception.CommerceConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "orders")
@CompoundIndex(name = "customer_created", def = "{'customerId': 1, 'createdAt': -1}")
@CompoundIndex(name = "seller_created", def = "{'items.sellerId': 1, 'createdAt': -1}")
public class MarketplaceOrder {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNumber;

    private String customerId;
    private List<OrderLine> items = new ArrayList<>();
    private ShippingAddress shippingAddress;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant cancelledAt;

    protected MarketplaceOrder() {
    }

    public MarketplaceOrder(
            String orderNumber,
            String customerId,
            List<OrderLine> items,
            ShippingAddress shippingAddress,
            OrderPayment payment,
            OrderTotals totals,
            Instant now) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.shippingAddress = shippingAddress;
        this.paymentMethod = payment.method();
        this.paymentStatus = payment.status();
        this.status = OrderStatus.PLACED;
        this.subtotal = totals.subtotal();
        this.deliveryFee = totals.deliveryFee();
        this.total = totals.total();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        if (status != OrderStatus.PLACED && status != OrderStatus.CONFIRMED) {
            throw new CommerceConflictException("This order can no longer be cancelled");
        }
        status = OrderStatus.CANCELLED;
        paymentStatus = PaymentStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }

    public void moveTo(OrderStatus target, Instant now) {
        OrderStatus expected = switch (status) {
            case PLACED -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.PACKING;
            case PACKING -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            default -> null;
        };
        if (target != expected) {
            throw new CommerceConflictException("Order status must move to the next step");
        }
        status = target;
        updatedAt = now;
    }

    public boolean belongsToSeller(String sellerId) {
        return items.stream().anyMatch(item -> item.sellerId().equals(sellerId));
    }

    public String getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerId() { return customerId; }
    public List<OrderLine> getItems() { return List.copyOf(items); }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
