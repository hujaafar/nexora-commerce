package com.buy01.media.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buy01.media.domain.MediaAsset;
import com.buy01.media.domain.MediaPurpose;
import com.buy01.media.event.MediaEventPublisher;
import com.buy01.media.exception.InvalidMediaException;
import com.buy01.media.exception.MediaNotFoundException;
import com.buy01.media.repository.MediaAssetRepository;
import com.buy01.media.storage.ObjectStorage;
import com.buy01.media.validation.FilenameSanitizer;
import com.buy01.media.validation.ImageSignatureValidator;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaAssetRepository repository;

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private MediaEventPublisher eventPublisher;

    @Mock
    private MultipartFile multipartFile;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(
                repository,
                objectStorage,
                new ImageSignatureValidator(),
                new FilenameSanitizer(),
                eventPublisher,
                2_097_152,
                "http://localhost:8080/media/images");
    }

    @Test
    void rejectsOversizedImagesBeforeReadingOrStoringThem() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(2_097_153L);

        assertThatThrownBy(() -> mediaService.upload(
                "seller-id",
                multipartFile,
                null,
                MediaPurpose.PRODUCT_IMAGE))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessage("The image must be 2 MB or smaller");

        verify(objectStorage, never()).put(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anotherSellerCannotDeleteOrDiscoverAnAsset() {
        MediaAsset asset = new MediaAsset(
                "owner/object.png",
                "image.png",
                "image/png",
                100,
                "owner",
                null,
                MediaPurpose.PRODUCT_IMAGE,
                Instant.now());
        when(repository.findById("media-id")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> mediaService.delete("media-id", "attacker"))
                .isInstanceOf(MediaNotFoundException.class)
                .hasMessage("Media asset was not found");

        verify(objectStorage, never()).delete(asset.getObjectKey());
    }
}
