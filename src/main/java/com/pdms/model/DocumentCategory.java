package com.pdms.model;

/**
 * Standard categories for organizing personal documents.
 */
public enum DocumentCategory {
    IDENTITY("Identity"),
    VEHICLE("Vehicle"),
    INSURANCE("Insurance"),
    EDUCATION("Education"),
    FINANCE("Finance"),
    MEDICAL("Medical"),
    WARRANTY("Warranty"),
    OTHER("Other");

    private final String displayName;

    DocumentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DocumentCategory fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OTHER;
        }
        for (DocumentCategory category : DocumentCategory.values()) {
            if (category.name().equalsIgnoreCase(text.trim()) || 
                category.displayName.equalsIgnoreCase(text.trim())) {
                return category;
            }
        }
        return OTHER;
    }
}
