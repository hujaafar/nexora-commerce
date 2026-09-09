/* File purpose: Returns customer spending and purchase insights. */
package com.nexora.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerAnalyticsResponse(
        BigDecimal totalSpent,
        int completedOrders,
        int purchasedUnits,
        List<ProductMetric> mostBoughtProducts,
        List<CategoryMetric> topCategories) {
}
