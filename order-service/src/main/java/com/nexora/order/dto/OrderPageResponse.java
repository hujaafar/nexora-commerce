/* File purpose: Returns a bounded order-history page and total counts. */
package com.nexora.order.dto;

import java.util.List;

public record OrderPageResponse(
        List<OrderResponse> items,
        long totalItems,
        int page,
        int size,
        int totalPages) {
}
