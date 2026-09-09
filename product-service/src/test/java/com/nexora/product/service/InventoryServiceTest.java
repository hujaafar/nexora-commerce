/* File purpose: Verifies that atomic inventory reservation reports insufficient stock as a conflict. */
package com.nexora.product.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nexora.product.domain.Product;
import com.nexora.product.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private MongoTemplate mongoTemplate;

    @Test
    void reserveRejectsQuantityWhenProductStillExistsButStockIsTooLow() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Product.class)))
                .thenReturn(null);
        when(mongoTemplate.exists(any(Query.class), eq(Product.class))).thenReturn(true);

        assertThatThrownBy(() -> new InventoryService(mongoTemplate).reserve("product", 3))
                .isInstanceOf(InsufficientStockException.class);
    }
}
