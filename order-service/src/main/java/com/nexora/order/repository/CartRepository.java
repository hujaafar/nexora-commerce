/* File purpose: Provides persistent cart lookup by authenticated customer ID. */
package com.nexora.order.repository;

import com.nexora.order.domain.Cart;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByCustomerId(String customerId);
}
