/*
 * File purpose: Defines the product request API data contract.
 */
package com.buy01.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Name is required")
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
        String name,

        // Learning annotation: @NotBlank rejects null, empty, and whitespace-only text during Bean Validation.
        @NotBlank(message = "Description is required")
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        // Learning annotation: @NotNull rejects a missing null value during Bean Validation.
        @NotNull(message = "Price is required")
        // Learning annotation: @DecimalMin requires the numeric value to be at least the configured decimal boundary.
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        // Learning annotation: @Digits limits how many integer and fractional digits the numeric value may contain.
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price,

        // Learning annotation: @Min requires the numeric value to be at least the configured integer boundary.
        @Min(value = 0, message = "Quantity cannot be negative")
        int quantity,

        // Learning annotation: @Valid triggers Bean Validation here; on collections it also validates nested values.
        @Valid
        // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
        @Size(max = 8, message = "A product can have at most 8 images")
        List<
                // Learning annotation: @Size enforces minimum/maximum length or collection-size limits.
                @Size(max = 500, message = "Image URL must be at most 500 characters")
                // Learning annotation: @Pattern requires the text to match the configured regular expression.
                @Pattern(
                        regexp = "^https?://.+$",
                        message = "Image URL must be an absolute HTTP(S) URL")
                String> imageUrls) {
}
