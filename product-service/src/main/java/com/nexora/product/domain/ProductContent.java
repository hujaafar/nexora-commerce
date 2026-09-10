package com.nexora.product.domain;

import java.util.List;

/** The descriptive content a seller edits together, independent of inventory. */
public record ProductContent(String name, String description, String category, List<String> imageUrls) {
    public ProductContent {
        imageUrls = List.copyOf(imageUrls);
    }
}
