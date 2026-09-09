/* File purpose: Converts a validated cart into an order using pay-on-delivery or simulated card payment. */
package com.nexora.order.web;

import com.nexora.order.dto.CheckoutRequest;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
@PreAuthorize("hasRole('CLIENT')")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(jwt.getSubject(), request);
    }
}
