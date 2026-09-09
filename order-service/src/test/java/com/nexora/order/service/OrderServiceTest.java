/* File purpose: Verifies checkout inventory reservation, cart clearing, cancellation, and ownership. */
package com.nexora.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.order.client.CatalogProduct;
import com.nexora.order.client.ProductClient;
import com.nexora.order.domain.Cart;
import com.nexora.order.domain.CartLine;
import com.nexora.order.domain.MarketplaceOrder;
import com.nexora.order.domain.OrderLine;
import com.nexora.order.domain.OrderStatus;
import com.nexora.order.domain.PaymentMethod;
import com.nexora.order.domain.PaymentStatus;
import com.nexora.order.domain.ShippingAddress;
import com.nexora.order.dto.CheckoutRequest;
import com.nexora.order.dto.ShippingAddressRequest;
import com.nexora.order.event.OrderEventPublisher;
import com.nexora.order.exception.ResourceNotFoundException;
import com.nexora.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository repository;
    @Mock private CartService cartService;
    @Mock private ProductClient productClient;
    @Mock private OrderEventPublisher eventPublisher;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(repository, cartService, productClient, eventPublisher);
    }

    @Test
    void checkoutReservesStockAndClearsTheAuthenticatedCart() {
        when(repository.save(any(MarketplaceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Cart cart = new Cart("customer", Instant.now());
        cart.addOrReplace(new CartLine(
                "product", "Desk lamp", "Home", "seller", new BigDecimal("19.99"),
                2, 5, null), Instant.now());
        when(cartService.requireCart("customer")).thenReturn(cart);
        when(productClient.reserve("product", 2)).thenReturn(product());

        var response = orderService.checkout("customer", checkoutRequest());

        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.DUE_ON_DELIVERY);
        assertThat(response.total()).isEqualByComparingTo("44.88");
        verify(cartService).clear("customer");
        verify(productClient).reserve("product", 2);
    }

    @Test
    void cancellationChangesStateAndRestoresStock() {
        when(repository.save(any(MarketplaceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MarketplaceOrder order = order("customer");
        when(repository.findById("order")).thenReturn(Optional.of(order));

        var response = orderService.cancel("order", "customer");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(productClient).release("product", 2);
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        when(repository.findById("order")).thenReturn(Optional.of(order("owner")));

        assertThatThrownBy(() -> orderService.getForCustomer("order", "attacker"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CheckoutRequest checkoutRequest() {
        return new CheckoutRequest(
                new ShippingAddressRequest("Demo Buyer", "+973 3333 3333", "Road 1", "Manama", "Bahrain", ""),
                PaymentMethod.PAY_ON_DELIVERY);
    }

    private CatalogProduct product() {
        return new CatalogProduct(
                "product", "Desk lamp", "Warm light", "Home", new BigDecimal("19.99"),
                3, "seller", List.of(), Instant.now(), Instant.now());
    }

    private MarketplaceOrder order(String customerId) {
        OrderLine line = new OrderLine(
                "product", "Desk lamp", "Home", "seller", new BigDecimal("19.99"),
                2, new BigDecimal("39.98"), null);
        return new MarketplaceOrder(
                "BZ-TEST", customerId, List.of(line),
                new ShippingAddress("Buyer", "+97333333333", "Road 1", "Manama", "Bahrain", ""),
                PaymentMethod.PAY_ON_DELIVERY, PaymentStatus.DUE_ON_DELIVERY,
                new BigDecimal("39.98"), new BigDecimal("4.90"), Instant.now());
    }
}
