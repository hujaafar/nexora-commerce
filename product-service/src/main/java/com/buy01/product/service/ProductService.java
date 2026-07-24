package com.buy01.product.service;

import com.buy01.product.domain.Product;
import com.buy01.product.dto.ProductRequest;
import com.buy01.product.dto.ProductResponse;
import com.buy01.product.exception.ProductNotFoundException;
import com.buy01.product.repository.ProductRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> listPublic() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getPublic(String productId) {
        return ProductResponse.from(find(productId));
    }

    public List<ProductResponse> listMine(String sellerId) {
        return repository.findAllBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse create(String sellerId, ProductRequest request) {
        Instant now = Instant.now();
        Product product = new Product(
                request.name().trim(),
                request.description().trim(),
                request.price(),
                request.quantity(),
                sellerId,
                normalizedImages(request.imageUrls()),
                now);
        return ProductResponse.from(repository.save(product));
    }

    public ProductResponse update(
            String productId,
            String sellerId,
            ProductRequest request) {
        Product product = findOwned(productId, sellerId);
        product.update(
                request.name().trim(),
                request.description().trim(),
                request.price(),
                request.quantity(),
                normalizedImages(request.imageUrls()),
                Instant.now());
        return ProductResponse.from(repository.save(product));
    }

    public void delete(String productId, String sellerId) {
        Product product = findOwned(productId, sellerId);
        repository.delete(product);
    }

    private Product findOwned(String productId, String sellerId) {
        Product product = find(productId);
        if (!product.getSellerId().equals(sellerId)) {
            throw new ProductNotFoundException();
        }
        return product;
    }

    private Product find(String productId) {
        return repository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private List<String> normalizedImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .map(String::trim)
                .distinct()
                .toList();
    }
}
