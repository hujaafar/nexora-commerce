/*
 * File purpose: Builds safe MongoDB queries for keyword, facet, price, sort, and pagination filters.
 */
package com.nexora.product.service;

import com.nexora.product.domain.Product;
import com.nexora.product.dto.ProductResponse;
import com.nexora.product.dto.ProductSearchResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

// Learning annotation: @Service keeps query-building rules outside the HTTP controller.
@Service
public class ProductSearchService {

    private static final int MAX_PAGE_SIZE = 48;
    private final MongoTemplate mongoTemplate;

    public ProductSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ProductSearchResponse search(
            String keyword,
            String category,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String sort,
            int requestedPage,
            int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, requestedSize));
        List<Criteria> filters = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern),
                    Criteria.where("description").regex(pattern),
                    Criteria.where("category").regex(pattern)));
        }
        if (category != null && !category.isBlank()) {
            filters.add(Criteria.where("category").regex(
                    Pattern.compile("^" + Pattern.quote(category.trim()) + "$", Pattern.CASE_INSENSITIVE)));
        }
        if (minimumPrice != null) {
            filters.add(Criteria.where("price").gte(minimumPrice));
        }
        if (maximumPrice != null) {
            filters.add(Criteria.where("price").lte(maximumPrice));
        }

        Criteria criteria = filters.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(filters.toArray(Criteria[]::new));
        Query countQuery = filters.isEmpty() ? new Query() : Query.query(criteria);
        long total = mongoTemplate.count(countQuery, Product.class);

        Query pageQuery = filters.isEmpty() ? new Query() : Query.query(criteria);
        pageQuery.with(PageRequest.of(page, size, toSort(sort)));
        List<ProductResponse> items = mongoTemplate.find(pageQuery, Product.class).stream()
                .map(ProductResponse::from)
                .toList();
        List<String> categories = mongoTemplate.query(Product.class)
                .distinct("category")
                .as(String.class)
                .all().stream()
                .filter(value -> value != null && !value.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new ProductSearchResponse(items, total, page, size, totalPages, categories);
    }

    private Sort toSort(String requestedSort) {
        String value = requestedSort == null ? "newest" : requestedSort.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
