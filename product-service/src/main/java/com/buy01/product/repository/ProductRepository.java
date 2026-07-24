package com.buy01.product.repository;

import com.buy01.product.domain.Product;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);
}
