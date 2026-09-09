/* File purpose: Provides ownership-aware order queries used by history and analytics. */
package com.nexora.order.repository;

import com.nexora.order.domain.MarketplaceOrder;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<MarketplaceOrder, String> {
    List<MarketplaceOrder> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<MarketplaceOrder> findAllByItemsSellerIdOrderByCreatedAtDesc(String sellerId);
}
