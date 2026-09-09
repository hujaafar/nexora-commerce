/*
 * File purpose: Provides persistence queries for product data.
 */
package com.nexora.product.repository;

import com.nexora.product.domain.Product;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);
}
