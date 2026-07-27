/* BUY-01 learning header
 * File purpose: Models the media asset domain concept persisted or used by the service.
 * Learning focus: Domain modeling, MongoDB documents, indexes, and explicit enums.
 */
package com.buy01.media.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "media_assets")
public class MediaAsset {

    @Id
    private String id;

    private String objectKey;
    private String originalFilename;
    private String contentType;
    private long size;

    @Indexed
    private String sellerId;

    @Indexed
    private String productId;

    private MediaPurpose purpose;
    private Instant createdAt;

    protected MediaAsset() {
    }

    public MediaAsset(
            String objectKey,
            String originalFilename,
            String contentType,
            long size,
            String sellerId,
            String productId,
            MediaPurpose purpose,
            Instant createdAt) {
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.sellerId = sellerId;
        this.productId = productId;
        this.purpose = purpose;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getProductId() {
        return productId;
    }

    public MediaPurpose getPurpose() {
        return purpose;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
