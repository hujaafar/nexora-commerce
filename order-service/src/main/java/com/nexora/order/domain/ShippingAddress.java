/*
 * File purpose: Stores the delivery address and provides a privacy-safe seller view.
 */
package com.nexora.order.domain;

public record ShippingAddress(
        String fullName,
        String phone,
        String addressLine,
        String city,
        String country,
        String postalCode) {

    public ShippingAddress masked() {
        String maskedPhone = phone == null || phone.length() < 4
                ? "Hidden"
                : "*** *** " + phone.substring(phone.length() - 4);
        return new ShippingAddress("Customer", maskedPhone, "Hidden", city, country, postalCode);
    }
}
