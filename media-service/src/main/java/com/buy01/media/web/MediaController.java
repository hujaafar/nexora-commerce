/* BUY-01 learning header
 * File purpose: Exposes media HTTP endpoints.
 * Learning focus: Thin REST controllers, request validation, status codes, and delegated business logic.
 */
package com.buy01.media.web;

import com.buy01.media.domain.MediaPurpose;
import com.buy01.media.dto.MediaDownload;
import com.buy01.media.dto.MediaResponse;
import com.buy01.media.service.MediaService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media/images")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SELLER')")
    MediaResponse upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String productId,
            @RequestParam(defaultValue = "PRODUCT_IMAGE") MediaPurpose purpose) {
        return mediaService.upload(
                jwt.getSubject(),
                file,
                productId,
                purpose);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SELLER')")
    List<MediaResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return mediaService.listMine(jwt.getSubject());
    }

    @GetMapping("/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    List<MediaResponse> listForModeration() {
        return mediaService.listForModeration();
    }

    @GetMapping("/{id}")
    ResponseEntity<byte[]> download(@PathVariable String id) {
        MediaDownload media = mediaService.download(id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(media.originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .contentLength(media.content().length)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365))
                        .cachePublic()
                        .immutable())
                .eTag(media.eTag())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(media.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SELLER')")
    void delete(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        mediaService.delete(id, jwt.getSubject());
    }

    @DeleteMapping("/moderation/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deleteAsAdmin(@PathVariable String id) {
        mediaService.deleteAsAdmin(id);
    }
}
