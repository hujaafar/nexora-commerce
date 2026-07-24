package com.buy01.product.web;

import com.buy01.product.dto.ProductRequest;
import com.buy01.product.dto.ProductResponse;
import com.buy01.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    List<ProductResponse> list() {
        return productService.listPublic();
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable String id) {
        return productService.getPublic(id);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SELLER')")
    List<ProductResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return productService.listMine(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SELLER')")
    ProductResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductRequest request) {
        return productService.create(jwt.getSubject(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    ProductResponse update(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductRequest request) {
        return productService.update(id, jwt.getSubject(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SELLER')")
    void delete(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        productService.delete(id, jwt.getSubject());
    }
}
