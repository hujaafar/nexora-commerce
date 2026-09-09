/*
 * File purpose: Implements media service business rules.
 */
package com.nexora.media.service;

import com.nexora.media.domain.MediaAsset;
import com.nexora.media.domain.MediaPurpose;
import com.nexora.media.dto.MediaDownload;
import com.nexora.media.dto.MediaResponse;
import com.nexora.media.event.MediaEvent;
import com.nexora.media.event.MediaEventPublisher;
import com.nexora.media.exception.InvalidMediaException;
import com.nexora.media.exception.MediaNotFoundException;
import com.nexora.media.repository.MediaAssetRepository;
import com.nexora.media.storage.ObjectStorage;
import com.nexora.media.validation.DetectedImageType;
import com.nexora.media.validation.FilenameSanitizer;
import com.nexora.media.validation.ImageSignatureValidator;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Learning annotation: @Service marks business-logic code as a Spring-managed service-layer component.
@Service
public class MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private final MediaAssetRepository repository;
    private final ObjectStorage objectStorage;
    private final ImageSignatureValidator imageSignatureValidator;
    private final FilenameSanitizer filenameSanitizer;
    private final MediaEventPublisher eventPublisher;
    private final long maxBytes;
    private final String publicBaseUrl;

    public MediaService(
            MediaAssetRepository repository,
            ObjectStorage objectStorage,
            ImageSignatureValidator imageSignatureValidator,
            FilenameSanitizer filenameSanitizer,
            MediaEventPublisher eventPublisher,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.media.max-bytes}") long maxBytes,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.media.public-base-url}") String publicBaseUrl) {
        this.repository = repository;
        this.objectStorage = objectStorage;
        this.imageSignatureValidator = imageSignatureValidator;
        this.filenameSanitizer = filenameSanitizer;
        this.eventPublisher = eventPublisher;
        this.maxBytes = maxBytes;
        this.publicBaseUrl = trimTrailingSlashes(publicBaseUrl);
    }

    public MediaResponse upload(
            String sellerId,
            MultipartFile file,
            String productId,
            MediaPurpose purpose) {
        validateSize(file);
        byte[] content = read(file);
        DetectedImageType imageType =
                imageSignatureValidator.validate(content, file.getContentType());
        String safeFilename = filenameSanitizer.sanitize(file.getOriginalFilename());
        String objectKey = sellerId
                + "/"
                + UUID.randomUUID()
                + "."
                + imageType.extension();
        String normalizedProductId = normalizeNullable(productId);

        objectStorage.put(objectKey, imageType.contentType(), content);
        try {
            MediaAsset asset = new MediaAsset(
                    objectKey,
                    safeFilename,
                    imageType.contentType(),
                    content.length,
                    sellerId,
                    normalizedProductId,
                    purpose);
            MediaAsset saved = repository.save(asset);
            eventPublisher.publish(MediaEvent.uploaded(
                    saved.getId(),
                    sellerId,
                    normalizedProductId,
                    purpose));
            return MediaResponse.from(saved, publicBaseUrl);
        } catch (RuntimeException persistenceFailure) {
            try {
                objectStorage.delete(objectKey);
            } catch (RuntimeException cleanupFailure) {
                persistenceFailure.addSuppressed(cleanupFailure);
                LOGGER.error("Could not clean up object {}", objectKey, cleanupFailure);
            }
            throw persistenceFailure;
        }
    }

    public List<MediaResponse> listMine(String sellerId) {
        return repository.findAllBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(asset -> MediaResponse.from(asset, publicBaseUrl))
                .toList();
    }

    public List<MediaResponse> listForModeration() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(asset -> MediaResponse.from(asset, publicBaseUrl))
                .toList();
    }

    public MediaDownload download(String mediaId) {
        MediaAsset asset = find(mediaId);
        byte[] content = objectStorage.get(asset.getObjectKey());
        String eTag = "\"" + asset.getId() + "-" + asset.getSize() + "\"";
        return new MediaDownload(
                content,
                asset.getContentType(),
                asset.getOriginalFilename(),
                eTag);
    }

    public void delete(String mediaId, String sellerId) {
        MediaAsset asset = findOwned(mediaId, sellerId);
        deleteAsset(asset);
    }

    public void deleteAsAdmin(String mediaId) {
        deleteAsset(find(mediaId));
    }

    private void deleteAsset(MediaAsset asset) {
        objectStorage.delete(asset.getObjectKey());
        repository.delete(asset);
        eventPublisher.publish(MediaEvent.deleted(
                asset.getId(),
                asset.getSellerId(),
                asset.getProductId(),
                asset.getPurpose()));
    }

    private void validateSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("An image file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new InvalidMediaException("The image must be 2 MB or smaller");
        }
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new InvalidMediaException("The uploaded image could not be read");
        }
    }

    private MediaAsset findOwned(String mediaId, String sellerId) {
        MediaAsset asset = find(mediaId);
        if (!asset.getSellerId().equals(sellerId)) {
            throw new MediaNotFoundException();
        }
        return asset;
    }

    private MediaAsset find(String mediaId) {
        return repository.findById(mediaId)
                .orElseThrow(MediaNotFoundException::new);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimTrailingSlashes(String value) {
        // A simple character scan has linear runtime. It avoids a backtracking
        // regular expression on an externally configured URL.
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
