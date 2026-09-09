/*
 * File purpose: Aggregates customer spending and seller revenue from trusted order snapshots.
 */
package com.nexora.order.service;

import com.nexora.order.domain.MarketplaceOrder;
import com.nexora.order.domain.OrderLine;
import com.nexora.order.domain.OrderStatus;
import com.nexora.order.dto.CategoryMetric;
import com.nexora.order.dto.CustomerAnalyticsResponse;
import com.nexora.order.dto.ProductMetric;
import com.nexora.order.dto.SellerAnalyticsResponse;
import com.nexora.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final OrderRepository repository;

    public AnalyticsService(OrderRepository repository) {
        this.repository = repository;
    }

    public CustomerAnalyticsResponse customer(String customerId) {
        List<MarketplaceOrder> orders = repository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().filter(this::countsInAnalytics).toList();
        Map<String, ProductCounter> products = new HashMap<>();
        Map<String, CategoryCounter> categories = new HashMap<>();
        int units = 0;
        for (MarketplaceOrder order : orders) {
            for (OrderLine item : order.getItems()) {
                units += item.quantity();
                products.computeIfAbsent(item.productId(), ignored -> new ProductCounter(item.name()))
                        .add(item.quantity(), item.lineTotal());
                categories.computeIfAbsent(item.category(), ignored -> new CategoryCounter())
                        .add(item.quantity(), item.lineTotal());
            }
        }
        BigDecimal spent = orders.stream().map(MarketplaceOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerAnalyticsResponse(
                spent,
                orders.size(),
                units,
                topProducts(products),
                categories.entrySet().stream()
                        .map(entry -> new CategoryMetric(
                                entry.getKey(), entry.getValue().units, entry.getValue().amount))
                        .sorted(Comparator.comparing(CategoryMetric::amount).reversed())
                        .limit(6)
                        .toList());
    }

    public SellerAnalyticsResponse seller(String sellerId) {
        List<MarketplaceOrder> orders = repository.findAllByItemsSellerIdOrderByCreatedAtDesc(sellerId)
                .stream().filter(this::countsInAnalytics).toList();
        Map<String, ProductCounter> products = new HashMap<>();
        int units = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (MarketplaceOrder order : orders) {
            for (OrderLine item : order.getItems()) {
                if (item.sellerId().equals(sellerId)) {
                    units += item.quantity();
                    revenue = revenue.add(item.lineTotal());
                    products.computeIfAbsent(item.productId(), ignored -> new ProductCounter(item.name()))
                            .add(item.quantity(), item.lineTotal());
                }
            }
        }
        return new SellerAnalyticsResponse(revenue, orders.size(), units, topProducts(products));
    }

    private boolean countsInAnalytics(MarketplaceOrder order) {
        return order.getStatus() != OrderStatus.CANCELLED;
    }

    private List<ProductMetric> topProducts(Map<String, ProductCounter> products) {
        return products.entrySet().stream()
                .map(entry -> new ProductMetric(
                        entry.getKey(), entry.getValue().name, entry.getValue().units, entry.getValue().amount))
                .sorted(Comparator.comparing(ProductMetric::units).reversed()
                        .thenComparing(ProductMetric::amount, Comparator.reverseOrder()))
                .limit(6)
                .toList();
    }

    private static final class ProductCounter {
        private final String name;
        private int units;
        private BigDecimal amount = BigDecimal.ZERO;

        private ProductCounter(String name) {
            this.name = name;
        }

        private void add(int addedUnits, BigDecimal addedAmount) {
            units += addedUnits;
            amount = amount.add(addedAmount);
        }
    }

    private static final class CategoryCounter {
        private int units;
        private BigDecimal amount = BigDecimal.ZERO;

        private void add(int addedUnits, BigDecimal addedAmount) {
            units += addedUnits;
            amount = amount.add(addedAmount);
        }
    }
}
