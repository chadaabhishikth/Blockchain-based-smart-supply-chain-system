package com.supplychain.domain.model;

/**
 * Domain Layer — Product Status
 * ===============================
 *
 * Lifecycle status of a product tracked by the supply chain service.
 */
public enum ProductStatus {

    PENDING("pending"),
    ACTIVE("active"),
    SOLD("sold"),
    COUNTERFEIT("counterfeit");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    /**
     * Lowercase value persisted in the product registry.
     */
    public String getValue() {
        return value;
    }
}
