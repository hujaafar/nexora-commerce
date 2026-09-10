/*
 * File purpose: Coordinates checkout, inventory, ownership, search, cancellation, redo, and status rules.
 */
package com.nexora.order.service;

import com.nexora.order.client.CatalogProduct;
import com.nexora.order.client.ProductClient;
import com.nexora.order.domain.Cart;
import com.nexora.order.domain.CartLine;
import com.nexora.order.domain.MarketplaceOrder;
import com.nexora.order.domain.OrderPayment;
import com.nexora.order.domain.OrderTotals;
import com.nexora.order.domain.OrderLine;
import com.nexora.order.domain.OrderStatus;
import com.nexora.order.domain.PaymentMethod;
import com.nexora.order.domain.PaymentStatus;
import com.nexora.order.domain.ShippingAddress;
import com.nexora.order.dto.CheckoutRequest;
import com.nexora.order.dto.OrderPageResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.event.OrderEvent;
import com.nexora.order.event.OrderEventPublisher;
import com.nexora.order.exception.CommerceConflictException;
import com.nexora.order.exception.ResourceNotFoundException;
import com.nexora.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal FREE_DELIVERY_MINIMUM = new BigDecimal("100.00");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("4.90");
    private final OrderRepository repository;
    private final CartService cartService;
    private final ProductClient productClient;
    private final OrderEventPublisher eventPublisher;

    public OrderService(
            OrderRepository repository,
            CartService cartService,
            ProductClient productClient,
            OrderEventPublisher eventPublisher) {
        this.repository = repository;
        this.cartService = cartService;
        this.productClient = productClient;
        this.eventPublisher = eventPublisher;
    }

    public OrderResponse checkout(String customerId, CheckoutRequest request) {
        Cart cart = cartService.requireCart(customerId);
        MarketplaceOrder order = place(customerId, cart.getItems(), request.shippingAddress().toDomain(),
                request.paymentMethod());
        cartService.clear(customerId);
        return OrderResponse.forCustomer(order);
    }

    public OrderPageResponse listCustomer(
            String customerId, String query, OrderStatus status, Instant from, Instant to, int page, int size) {
        return page(repository.findAllByCustomerIdOrderByCreatedAtDesc(customerId), new OrderFilters(query, status, from, to),
                page, size, OrderResponse::forCustomer);
    }

    public OrderPageResponse listSeller(
            String sellerId, String query, OrderStatus status, Instant from, Instant to, int page, int size) {
        return page(repository.findAllByItemsSellerIdOrderByCreatedAtDesc(sellerId), new OrderFilters(query, status, from, to),
                page, size, order -> OrderResponse.forSeller(order, sellerId));
    }

    public OrderResponse getForCustomer(String orderId, String customerId) {
        MarketplaceOrder order = find(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Order was not found");
        }
        return OrderResponse.forCustomer(order);
    }

    public OrderResponse getForSeller(String orderId, String sellerId) {
        MarketplaceOrder order = find(orderId);
        if (!order.belongsToSeller(sellerId)) {
            throw new ResourceNotFoundException("Order was not found");
        }
        return OrderResponse.forSeller(order, sellerId);
    }

    public OrderResponse cancel(String orderId, String customerId) {
        MarketplaceOrder order = findOwned(orderId, customerId);
        order.cancel(Instant.now());
        MarketplaceOrder saved = repository.save(order);
        releaseStock(saved.getItems());
        eventPublisher.publish(OrderEvent.of("ORDER_CANCELLED", saved.getId(), customerId));
        return OrderResponse.forCustomer(saved);
    }

    public void remove(String orderId, String customerId) {
        MarketplaceOrder order = findOwned(orderId, customerId);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new CommerceConflictException("Only cancelled orders can be removed from history");
        }
        repository.delete(order);
    }

    public OrderResponse redo(String orderId, String customerId) {
        MarketplaceOrder previous = findOwned(orderId, customerId);
        List<CartLine> lines = previous.getItems().stream()
                .map(item -> new CartLine(
                        item.productId(), item.name(), item.category(), item.sellerId(), item.unitPrice(),
                        item.quantity(), item.quantity(), item.imageUrl()))
                .toList();
        MarketplaceOrder repeated = place(
                customerId, lines, previous.getShippingAddress(), previous.getPaymentMethod());
        return OrderResponse.forCustomer(repeated);
    }

    public OrderResponse updateStatus(String orderId, String sellerId, OrderStatus status) {
        MarketplaceOrder order = find(orderId);
        if (!order.belongsToSeller(sellerId)) {
            throw new ResourceNotFoundException("Order was not found");
        }
        order.moveTo(status, Instant.now());
        MarketplaceOrder saved = repository.save(order);
        eventPublisher.publish(OrderEvent.of("ORDER_STATUS_CHANGED", saved.getId(), saved.getCustomerId()));
        return OrderResponse.forSeller(saved, sellerId);
    }

    private MarketplaceOrder place(
            String customerId,
            List<CartLine> requestedLines,
            ShippingAddress address,
            PaymentMethod paymentMethod) {
        List<Reservation> reservations = new ArrayList<>();
        try {
            List<OrderLine> orderLines = new ArrayList<>();
            for (CartLine requested : requestedLines) {
                CatalogProduct current = productClient.reserve(requested.productId(), requested.quantity());
                reservations.add(new Reservation(current.id(), requested.quantity()));
                BigDecimal lineTotal = current.price().multiply(BigDecimal.valueOf(requested.quantity()));
                orderLines.add(new OrderLine(
                        current.id(), current.name(), current.category(), current.sellerId(), current.price(),
                        requested.quantity(), lineTotal, current.primaryImage()));
            }
            BigDecimal subtotal = orderLines.stream()
                    .map(OrderLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal deliveryFee = subtotal.compareTo(FREE_DELIVERY_MINIMUM) >= 0
                    ? BigDecimal.ZERO : DELIVERY_FEE;
            PaymentStatus paymentStatus = paymentMethod == PaymentMethod.PAY_ON_DELIVERY
                    ? PaymentStatus.DUE_ON_DELIVERY : PaymentStatus.AUTHORIZED;
            Instant now = Instant.now();
            MarketplaceOrder saved = repository.save(new MarketplaceOrder(
                    newOrderNumber(now), customerId, orderLines, address, new OrderPayment(paymentMethod, paymentStatus),
                    new OrderTotals(subtotal, deliveryFee), now));
            eventPublisher.publish(OrderEvent.of("ORDER_CREATED", saved.getId(), customerId));
            return saved;
        } catch (RuntimeException exception) {
            reservations.forEach(reservation -> safeRelease(reservation.productId(), reservation.quantity()));
            throw exception;
        }
    }

    private OrderPageResponse page(
            List<MarketplaceOrder> orders,
            OrderFilters filters,
            int requestedPage,
            int requestedSize,
            Function<MarketplaceOrder, OrderResponse> mapper) {
        int page = Math.max(0, requestedPage);
        int size = Math.min(50, Math.max(1, requestedSize));
        String needle = filters.query() == null ? "" : filters.query().trim().toLowerCase(Locale.ROOT);
        List<MarketplaceOrder> filtered = orders.stream()
                .filter(order -> filters.status() == null || order.getStatus() == filters.status())
                .filter(order -> filters.from() == null || !order.getCreatedAt().isBefore(filters.from()))
                .filter(order -> filters.to() == null || !order.getCreatedAt().isAfter(filters.to()))
                .filter(order -> needle.isBlank()
                        || order.getOrderNumber().toLowerCase(Locale.ROOT).contains(needle)
                        || order.getItems().stream().anyMatch(item ->
                                item.name().toLowerCase(Locale.ROOT).contains(needle)))
                .sorted(Comparator.comparing(MarketplaceOrder::getCreatedAt).reversed())
                .toList();
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<OrderResponse> items = filtered.subList(fromIndex, toIndex).stream().map(mapper).toList();
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new OrderPageResponse(items, filtered.size(), page, size, totalPages);
    }

    private record OrderFilters(String query, OrderStatus status, Instant from, Instant to) {}

    private MarketplaceOrder findOwned(String orderId, String customerId) {
        MarketplaceOrder order = find(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Order was not found");
        }
        return order;
    }

    private MarketplaceOrder find(String orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found"));
    }

    private void releaseStock(List<OrderLine> lines) {
        lines.forEach(item -> safeRelease(item.productId(), item.quantity()));
    }

    private void safeRelease(String productId, int quantity) {
        try {
            productClient.release(productId, quantity);
        } catch (RuntimeException exception) {
            LOGGER.error("Stock compensation failed for product {}", productId, exception);
        }
    }

    private String newOrderNumber(Instant now) {
        String date = now.toString().substring(0, 10).replace("-", "");
        return "BZ-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private record Reservation(String productId, int quantity) {
    }
}
