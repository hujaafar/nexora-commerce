/* File purpose: Exposes the optional customer save-for-later feature. */
package com.nexora.order.web;

import com.nexora.order.dto.WishlistResponse;
import com.nexora.order.service.WishlistService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wishlist")
@PreAuthorize("hasRole('CLIENT')")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    WishlistResponse get(@AuthenticationPrincipal Jwt jwt) {
        return wishlistService.get(jwt.getSubject());
    }

    @PutMapping("/{productId}")
    WishlistResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable String productId) {
        return wishlistService.add(jwt.getSubject(), productId);
    }

    @DeleteMapping("/{productId}")
    WishlistResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String productId) {
        return wishlistService.remove(jwt.getSubject(), productId);
    }
}
