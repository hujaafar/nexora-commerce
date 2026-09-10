/* File purpose: Verifies user spend and seller revenue aggregations from order line snapshots. */
package com.nexora.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nexora.order.domain.MarketplaceOrder;
import com.nexora.order.domain.OrderPayment;
import com.nexora.order.domain.OrderTotals;
import com.nexora.order.domain.OrderLine;
import com.nexora.order.domain.PaymentMethod;
import com.nexora.order.domain.PaymentStatus;
import com.nexora.order.domain.ShippingAddress;
import com.nexora.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Instant TEST_TIME = Instant.parse("2026-01-15T12:00:00Z");

    @Mock private OrderRepository repository;

    @Test
    void aggregatesCustomerAndSellerMetrics() {
        MarketplaceOrder order = order();
        when(repository.findAllByCustomerIdOrderByCreatedAtDesc("customer")).thenReturn(List.of(order));
        when(repository.findAllByItemsSellerIdOrderByCreatedAtDesc("seller")).thenReturn(List.of(order));
        AnalyticsService service = new AnalyticsService(repository);

        var customer = service.customer("customer");
        var seller = service.seller("seller");

        assertThat(customer.totalSpent()).isEqualByComparingTo("44.88");
        assertThat(customer.mostBoughtProducts().get(0).units()).isEqualTo(2);
        assertThat(seller.revenue()).isEqualByComparingTo("39.98");
        assertThat(seller.unitsSold()).isEqualTo(2);
    }

    private MarketplaceOrder order() {
        OrderLine line = new OrderLine(
                "product", "Desk lamp", "Home", "seller", new BigDecimal("19.99"),
                2, new BigDecimal("39.98"), null);
        return new MarketplaceOrder(
                "BZ-TEST", "customer", List.of(line),
                new ShippingAddress("Buyer", "+97333333333", "Road 1", "Manama", "Bahrain", ""),
                new OrderPayment(PaymentMethod.PAY_ON_DELIVERY, PaymentStatus.DUE_ON_DELIVERY),
                new OrderTotals(new BigDecimal("39.98"), new BigDecimal("4.90")), TEST_TIME);
    }
}
