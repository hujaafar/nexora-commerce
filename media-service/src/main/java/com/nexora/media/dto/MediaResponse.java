/*
 * File purpose: Defines the media response API data contract.
 */
package com.nexora.media.dto;

import com.nexora.media.domain.MediaAsset;
import com.nexora.media.domain.MediaPurpose;
import java.time.Instant;

public record MediaResponse(
        String id,
        String originalFilename,
        String contentType,
        long size,
        String productId,
        MediaPurpose purpose,
        String url,
        Instant createdAt) {

    public static MediaResponse from(MediaAsset asset, String publicBaseUrl) {
        return new MediaResponse(
                asset.getId(),
                asset.getOriginalFilename(),
                asset.getContentType(),
                asset.getSize(),
                asset.getProductId(),
                asset.getPurpose(),
                publicBaseUrl + "/" + asset.getId(),
                asset.getCreatedAt());
    }
}
