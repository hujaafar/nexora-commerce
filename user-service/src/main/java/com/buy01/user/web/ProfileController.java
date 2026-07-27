/*
 * File purpose: Exposes profile HTTP endpoints.
 */
package com.buy01.user.web;

import com.buy01.user.dto.UpdateProfileRequest;
import com.buy01.user.dto.UserResponse;
import com.buy01.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Learning annotation: @RestController combines @Controller and @ResponseBody so methods return serialized API data.
@RestController
// Learning annotation: @RequestMapping defines the shared base URL (and optionally other rules) for this controller.
@RequestMapping("/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping
    // Learning annotation: @AuthenticationPrincipal supplies the verified JWT for the current account.
    UserResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.get(jwt.getSubject());
    }

    // Learning annotation: @PutMapping maps HTTP PUT requests to this full-update controller method.
    @PutMapping
    UserResponse updateProfile(
            // Learning annotation: @AuthenticationPrincipal injects the authenticated JWT principal so identity comes from the verified token.
            @AuthenticationPrincipal Jwt jwt,
            // Learning annotation: @Valid triggers Bean Validation here; on collections it also validates nested values. @RequestBody deserializes the HTTP JSON body into this typed Java request object.
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(jwt.getSubject(), request);
    }
}
