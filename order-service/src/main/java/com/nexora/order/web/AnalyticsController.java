/* File purpose: Exposes role-specific customer spending and seller revenue dashboards. */
package com.nexora.order.web;

import com.nexora.order.dto.CustomerAnalyticsResponse;
import com.nexora.order.dto.SellerAnalyticsResponse;
import com.nexora.order.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CLIENT')")
    CustomerAnalyticsResponse customer(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.customer(jwt.getSubject());
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    SellerAnalyticsResponse seller(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.seller(jwt.getSubject());
    }
}
