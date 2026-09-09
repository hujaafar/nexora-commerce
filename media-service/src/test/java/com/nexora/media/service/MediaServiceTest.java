/*
 * File purpose: Verifies media service test behavior.
 */
package com.nexora.media.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.media.domain.MediaAsset;
import com.nexora.media.domain.MediaPurpose;
import com.nexora.media.event.MediaEventPublisher;
import com.nexora.media.exception.InvalidMediaException;
import com.nexora.media.exception.MediaNotFoundException;
import com.nexora.media.repository.MediaAssetRepository;
import com.nexora.media.storage.ObjectStorage;
import com.nexora.media.validation.FilenameSanitizer;
import com.nexora.media.validation.ImageSignatureValidator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

// Learning annotation: @ExtendWith connects JUnit 5 to the named extension; MockitoExtension creates and injects mocks.
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private MediaAssetRepository repository;

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private ObjectStorage objectStorage;

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private MediaEventPublisher eventPublisher;

    // Learning annotation: @Mock creates a Mockito test double so the unit test can isolate one class.
    @Mock
    private MultipartFile multipartFile;

    private MediaService mediaService;

    // Learning annotation: @BeforeEach runs this setup method before every JUnit test to keep tests isolated.
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

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
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

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void anotherSellerCannotDeleteOrDiscoverAnAsset() {
        MediaAsset asset = new MediaAsset(
                "owner/object.png",
                "image.png",
                "image/png",
                100,
                "owner",
                null,
                MediaPurpose.PRODUCT_IMAGE);
        when(repository.findById("media-id")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> mediaService.delete("media-id", "attacker"))
                .isInstanceOf(MediaNotFoundException.class)
                .hasMessage("Media asset was not found");

        verify(objectStorage, never()).delete(asset.getObjectKey());
    }

    // Learning annotation: @Test marks this method as an independently executable JUnit 5 test case.
    @Test
    void administratorCanDeleteAnyAssetForModeration() {
        MediaAsset asset = new MediaAsset(
                "seller/object.png",
                "image.png",
                "image/png",
                100,
                "seller",
                null,
                MediaPurpose.PRODUCT_IMAGE);
        when(repository.findById("media-id")).thenReturn(Optional.of(asset));

        mediaService.deleteAsAdmin("media-id");

        verify(objectStorage).delete(asset.getObjectKey());
        verify(repository).delete(asset);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any());
    }
}
