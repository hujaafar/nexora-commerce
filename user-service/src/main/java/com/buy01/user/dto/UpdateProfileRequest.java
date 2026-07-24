package com.buy01.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
        String name,

        @Size(max = 500, message = "Avatar URL must be at most 500 characters")
        @Pattern(
                regexp = "^$|^https?://.+$",
                message = "Avatar URL must be an absolute HTTP(S) URL")
        String avatarUrl) {
}
