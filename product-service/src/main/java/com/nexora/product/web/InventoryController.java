/*
 * File purpose: Exposes token-protected stock operations only to trusted backend services.
 */
package com.nexora.product.web;

import com.nexora.product.dto.ProductResponse;
import com.nexora.product.dto.StockAdjustmentRequest;
import com.nexora.product.exception.InternalAccessDeniedException;
import com.nexora.product.service.InventoryService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
public class InventoryController {

    private final InventoryService inventoryService;
    private final byte[] expectedToken;

    public InventoryController(
            InventoryService inventoryService,
            @Value("${app.internal-token}") String internalToken) {
        this.inventoryService = inventoryService;
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/{id}/reserve")
    ProductResponse reserve(
            @PathVariable String id,
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody StockAdjustmentRequest request) {
        verify(token);
        return inventoryService.reserve(id, request.quantity());
    }

    @PostMapping("/{id}/release")
    ProductResponse release(
            @PathVariable String id,
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody StockAdjustmentRequest request) {
        verify(token);
        return inventoryService.release(id, request.quantity());
    }

    private void verify(String actualToken) {
        if (!MessageDigest.isEqual(
                expectedToken,
                actualToken.getBytes(StandardCharsets.UTF_8))) {
            throw new InternalAccessDeniedException();
        }
    }
}
