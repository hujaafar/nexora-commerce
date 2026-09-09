/* File purpose: Exposes customer-only shopping cart endpoints. */
package com.nexora.order.web;

import com.nexora.order.dto.CartItemRequest;
import com.nexora.order.dto.CartResponse;
import com.nexora.order.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@PreAuthorize("hasRole('CLIENT')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return cartService.get(jwt.getSubject());
    }

    @PutMapping("/items")
    CartResponse put(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemRequest request) {
        return cartService.put(jwt.getSubject(), request.productId(), request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String productId) {
        return cartService.remove(jwt.getSubject(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@AuthenticationPrincipal Jwt jwt) {
        cartService.clear(jwt.getSubject());
    }
}
