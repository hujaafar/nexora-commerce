/* File purpose: Returns resolved products from the optional save-for-later list. */
package com.nexora.order.dto;

import com.nexora.order.client.CatalogProduct;
import java.time.Instant;
import java.util.List;

public record WishlistResponse(List<CatalogProduct> items, Instant updatedAt) {
}
