/* File purpose: Exposes ownership-safe customer and seller order history and management endpoints. */
package com.nexora.order.web;

import com.nexora.order.domain.OrderStatus;
import com.nexora.order.dto.OrderPageResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.dto.OrderStatusRequest;
import com.nexora.order.service.OrderService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    OrderPageResponse customerOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.listCustomer(jwt.getSubject(), q, status, from, to, page, size);
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    OrderPageResponse sellerOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.listSeller(jwt.getSubject(), q, status, from, to, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    OrderResponse customerOrder(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.getForCustomer(id, jwt.getSubject());
    }

    @GetMapping("/seller/{id}")
    @PreAuthorize("hasRole('SELLER')")
    OrderResponse sellerOrder(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.getForSeller(id, jwt.getSubject());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    OrderResponse cancel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.cancel(id, jwt.getSubject());
    }

    @PostMapping("/{id}/redo")
    @PreAuthorize("hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse redo(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.redo(id, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        orderService.remove(id, jwt.getSubject());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    OrderResponse updateStatus(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateStatus(id, jwt.getSubject(), request.status());
    }
}
