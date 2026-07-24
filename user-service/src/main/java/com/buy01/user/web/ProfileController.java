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

@RestController
@RequestMapping("/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    UserResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.get(jwt.getSubject());
    }

    @PutMapping
    UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(jwt.getSubject(), request);
    }
}
