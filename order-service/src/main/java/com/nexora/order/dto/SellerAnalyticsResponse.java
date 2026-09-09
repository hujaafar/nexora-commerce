/* File purpose: Returns seller revenue, units sold, and best-selling products. */
package com.nexora.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record SellerAnalyticsResponse(
        BigDecimal revenue,
        int orderCount,
        int unitsSold,
        List<ProductMetric> bestSellingProducts) {
}
