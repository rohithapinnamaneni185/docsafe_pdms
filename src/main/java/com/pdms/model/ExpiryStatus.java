package com.pdms.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Expiry status calculated based on current date.
 * - EXPIRED: expiry date is before today.
 * - EXPIRING_SOON: expiry date is today or within the next 30 days.
 * - VALID: expiry date is more than 30 days away.
 * - NO_EXPIRY: expiry date is not provided.
 */
public enum ExpiryStatus {
    VALID("Valid", "badge-success"),
    EXPIRING_SOON("Expiring Soon", "badge-warning"),
    EXPIRED("Expired", "badge-danger"),
    NO_EXPIRY("No Expiry Date", "badge-neutral");

    private final String label;
    private final String cssClass;

    ExpiryStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static ExpiryStatus calculate(LocalDate expiryDate) {
        if (expiryDate == null) {
            return NO_EXPIRY;
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            return EXPIRED;
        } else if (!expiryDate.isAfter(today.plusDays(30))) {
            return EXPIRING_SOON;
        } else {
            return VALID;
        }
    }

    public static Long calculateDaysRemaining(LocalDate expiryDate) {
        if (expiryDate == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return ChronoUnit.DAYS.between(today, expiryDate);
    }
}
