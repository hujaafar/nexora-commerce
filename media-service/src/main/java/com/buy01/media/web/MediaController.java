/*
 * File purpose: Exposes media HTTP endpoints.
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

// Learning annotation: @RestController combines @Controller and @ResponseBody so methods return serialized API data.
@RestController
// Learning annotation: @RequestMapping defines the shared base URL (and optionally other rules) for this controller.
@RequestMapping("/media/images")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // Learning annotation: @PostMapping maps HTTP POST requests to this create/action controller method.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.CREATED)
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    MediaResponse upload(
            // Learning annotation: @AuthenticationPrincipal injects the authenticated JWT principal so identity comes from the verified token.
            @AuthenticationPrincipal Jwt jwt,
            // Learning annotation: @RequestParam binds a query-string or multipart form field to this method parameter.
            @RequestParam("file") MultipartFile file,
            // Learning annotation: @RequestParam binds a query-string or multipart form field to this method parameter.
            @RequestParam(required = false) String productId,
            // Learning annotation: @RequestParam binds a query-string or multipart form field to this method parameter.
            @RequestParam(defaultValue = "PRODUCT_IMAGE") MediaPurpose purpose) {
        return mediaService.upload(
                jwt.getSubject(),
                file,
                productId,
                purpose);
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping("/mine")
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    // Learning annotation: @AuthenticationPrincipal supplies the verified JWT for the current seller.
    List<MediaResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return mediaService.listMine(jwt.getSubject());
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping("/moderation")
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('ADMIN')")
    List<MediaResponse> listForModeration() {
        return mediaService.listForModeration();
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping("/{id}")
    // Learning annotation: @PathVariable reads the requested media ID from the URL.
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

    // Learning annotation: @DeleteMapping maps HTTP DELETE requests to this controller method.
    @DeleteMapping("/{id}")
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('SELLER')")
    void delete(
            // Learning annotation: @PathVariable binds a dynamic URL path segment to this method parameter.
            @PathVariable String id,
            // Learning annotation: @AuthenticationPrincipal injects the authenticated JWT principal so identity comes from the verified token.
            @AuthenticationPrincipal Jwt jwt) {
        mediaService.delete(id, jwt.getSubject());
    }

    // Learning annotation: @DeleteMapping maps HTTP DELETE requests to this controller method.
    @DeleteMapping("/moderation/{id}")
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('ADMIN')")
    // Learning annotation: @PathVariable reads the media ID an administrator wants to remove.
    void deleteAsAdmin(@PathVariable String id) {
        mediaService.deleteAsAdmin(id);
    }
}
