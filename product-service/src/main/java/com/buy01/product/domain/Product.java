/* BUY-01 learning header
 * File purpose: Models the product domain concept persisted or used by the service.
 * Learning focus: Domain modeling, MongoDB documents, indexes, and explicit enums.
 */
package com.buy01.product.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String description;
    private BigDecimal price;
    private int quantity;

    @Indexed
    private String sellerId;

    private List<String> imageUrls = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    protected Product() {
    }

    public Product(
            String name,
            String description,
            BigDecimal price,
            int quantity,
            String sellerId,
            List<String> imageUrls,
            Instant now) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.imageUrls = new ArrayList<>(imageUrls);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String name,
            String description,
            BigDecimal price,
            int quantity,
            List<String> imageUrls,
            Instant now) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.imageUrls = new ArrayList<>(imageUrls);
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getSellerId() {
        return sellerId;
    }

    public List<String> getImageUrls() {
        return List.copyOf(imageUrls);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
