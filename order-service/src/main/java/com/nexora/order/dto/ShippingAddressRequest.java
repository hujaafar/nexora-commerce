/* File purpose: Validates checkout delivery details before they enter the order model. */
package com.nexora.order.dto;

import com.nexora.order.domain.ShippingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+0-9 ()-]{7,24}$", message = "Phone format is invalid") String phone,
        @NotBlank(message = "Address is required") @Size(max = 180) String addressLine,
        @NotBlank(message = "City is required") @Size(max = 80) String city,
        @NotBlank(message = "Country is required") @Size(max = 80) String country,
        @Size(max = 20) String postalCode) {

    public ShippingAddress toDomain() {
        return new ShippingAddress(
                fullName.trim(),
                phone.trim(),
                addressLine.trim(),
                city.trim(),
                country.trim(),
                postalCode == null ? "" : postalCode.trim());
    }
}
