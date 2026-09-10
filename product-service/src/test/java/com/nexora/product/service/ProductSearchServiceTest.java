package com.nexora.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nexora.product.domain.Product;
import com.nexora.product.domain.ProductContent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class ProductSearchServiceTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class, RETURNS_DEEP_STUBS);
    private final ProductSearchService service = new ProductSearchService(mongo);
    @BeforeEach void setup() {
        when(mongo.query(Product.class).distinct("category").as(String.class).all())
                .thenReturn(Arrays.asList("Home", "Art", "", null));
        when(mongo.find(any(Query.class), eq(Product.class))).thenReturn(List.of());
    }
    @Test void quotesSearchInputAppliesAllFacetsAndPaginatesWithoutLosingTotals() {
        when(mongo.count(any(Query.class), eq(Product.class))).thenReturn(49L);
        Product lamp = new Product(new ProductContent("Lamp", "Desk light", "Home", List.of()),
                BigDecimal.TEN, 5, "seller", Instant.parse("2026-01-15T12:00:00Z"));
        when(mongo.find(any(Query.class), eq(Product.class))).thenReturn(List.of(lamp));
        var result = service.search(" .* ", " Home ", BigDecimal.ONE, BigDecimal.TEN, "price-asc", 1, 12);
        assertThat(result.totalItems()).isEqualTo(49);
        assertThat(result.totalPages()).isEqualTo(5);
        assertThat(result.items()).singleElement().extracting("name").isEqualTo("Lamp");
        assertThat(result.categories()).containsExactly("Art", "Home");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Product.class));
        assertThat(query.getValue().getSkip()).isEqualTo(12);
        assertThat(query.getValue().getLimit()).isEqualTo(12);
        assertThat(query.getValue().getQueryObject().toString()).contains("\\Q.*\\E", "$gte", "$lte", "category");
    }
    @ParameterizedTest
    @CsvSource({"price-asc,price,1", "price-desc,price,-1", "name,name,1", "unknown,createdAt,-1"})
    void boundsPageSizeAndUsesOnlyAllowlistedSortFields(String sort, String field, int direction) {
        var result = service.search(null, " ", null, null, sort, -5, 10000);
        assertThat(result.page()).isZero(); assertThat(result.size()).isEqualTo(48);
        assertThat(result.totalPages()).isZero();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Product.class));
        assertThat(query.getValue().getSortObject().get(field)).isEqualTo(direction);
        assertThat(query.getValue().getQueryObject()).isEmpty();
    }
}
