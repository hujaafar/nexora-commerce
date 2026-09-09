/*
 * File purpose: Returns one product search page together with reusable filter facets.
 */
package com.nexora.product.dto;

import java.util.List;

public record ProductSearchResponse(
        List<ProductResponse> items,
        long totalItems,
        int page,
        int size,
        int totalPages,
        List<String> categories) {
}
