/*
 * File purpose: Performs atomic inventory changes so concurrent checkouts cannot oversell stock.
 */
package com.nexora.product.service;

import com.nexora.product.domain.Product;
import com.nexora.product.dto.ProductResponse;
import com.nexora.product.exception.InsufficientStockException;
import com.nexora.product.exception.ProductNotFoundException;
import java.time.Instant;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private static final String QUANTITY_FIELD = "quantity";

    private final MongoTemplate mongoTemplate;

    public InventoryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ProductResponse reserve(String productId, int quantity) {
        Query query = Query.query(Criteria.where("id").is(productId).and(QUANTITY_FIELD).gte(quantity));
        Update update = new Update()
                .inc(QUANTITY_FIELD, -quantity)
                .set("updatedAt", Instant.now());
        Product product = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class);
        if (product == null) {
            if (!mongoTemplate.exists(Query.query(Criteria.where("id").is(productId)), Product.class)) {
                throw new ProductNotFoundException();
            }
            throw new InsufficientStockException();
        }
        return ProductResponse.from(product);
    }

    public ProductResponse release(String productId, int quantity) {
        Product product = mongoTemplate.findAndModify(
                Query.query(Criteria.where("id").is(productId)),
                new Update().inc(QUANTITY_FIELD, quantity).set("updatedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                Product.class);
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ProductResponse.from(product);
    }
}
