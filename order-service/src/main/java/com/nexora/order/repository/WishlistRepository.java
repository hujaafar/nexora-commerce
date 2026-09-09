/* File purpose: Provides one persisted wishlist per authenticated customer. */
package com.nexora.order.repository;

import com.nexora.order.domain.Wishlist;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    Optional<Wishlist> findByCustomerId(String customerId);
}
