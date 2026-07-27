/*
 * File purpose: Exposes product HTTP endpoints.
 */
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

// Learning annotation: @RestController combines @Controller and @ResponseBody so methods return serialized API data.
@RestController
// Learning annotation: @RequestMapping defines the shared base URL (and optionally other rules) for this controller.
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping
    List<ProductResponse> list() {
        return productService.listPublic();
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping("/{id}")
    // Learning annotation: @PathVariable reads the requested product ID from the URL.
    ProductResponse get(@PathVariable String id) {
        return productService.getPublic(id);
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping("/mine")
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    // Learning annotation: @AuthenticationPrincipal supplies the verified JWT for the current seller.
    List<ProductResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return productService.listMine(jwt.getSubject());
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping("/moderation")
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('ADMIN')")
    List<ProductResponse> listForModeration() {
        return productService.listPublic();
    }

    // Learning annotation: @PostMapping maps HTTP POST requests to this create/action controller method.
    @PostMapping
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.CREATED)
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    ProductResponse create(
            // Learning annotation: @AuthenticationPrincipal injects the authenticated JWT principal so identity comes from the verified token.
            @AuthenticationPrincipal Jwt jwt,
            // Learning annotation: @Valid triggers Bean Validation here; on collections it also validates nested values. @RequestBody deserializes the HTTP JSON body into this typed Java request object.
            @Valid @RequestBody ProductRequest request) {
        return productService.create(jwt.getSubject(), request);
    }

    // Learning annotation: @PutMapping maps HTTP PUT requests to this full-update controller method.
    @PutMapping("/{id}")
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    ProductResponse update(
            // Learning annotation: @PathVariable binds a dynamic URL path segment to this method parameter.
            @PathVariable String id,
            // Learning annotation: @AuthenticationPrincipal injects the authenticated JWT principal so identity comes from the verified token.
            @AuthenticationPrincipal Jwt jwt,
            // Learning annotation: @Valid triggers Bean Validation here; on collections it also validates nested values. @RequestBody deserializes the HTTP JSON body into this typed Java request object.
            @Valid @RequestBody ProductRequest request) {
        return productService.update(id, jwt.getSubject(), request);
    }

    // Learning annotation: @DeleteMapping maps HTTP DELETE requests to this controller method.
    @DeleteMapping("/{id}")
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    void delete(
            // Learning annotation: @PathVariable binds a dynamic URL path segment to this method parameter.
            @PathVariable String id,
            // Learning annotation: @AuthenticationPrincipal injects the authenticated JWT principal so identity comes from the verified token.
            @AuthenticationPrincipal Jwt jwt) {
        productService.delete(id, jwt.getSubject());
    }

    // Learning annotation: @DeleteMapping maps HTTP DELETE requests to this controller method.
    @DeleteMapping("/moderation/{id}")
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('ADMIN')")
    // Learning annotation: @PathVariable reads the product ID an administrator wants to remove.
    void deleteAsAdmin(@PathVariable String id) {
        productService.deleteAsAdmin(id);
    }
}
