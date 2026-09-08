package com.supplychain.domain.model;

/**
 * Domain Layer — Supply Chain Stage
 * ===================================
 *
 * Enumerates the stages a product passes through on its journey from
 * the factory to the consumer:
 *
 *     MANUFACTURING → DISTRIBUTION → WHOLESALING → RETAIL → CONSUMER
 */
public enum SupplyChainStage {

    MANUFACTURING("manufacturing"),
    DISTRIBUTION("distribution"),
    WHOLESALING("wholesaling"),
    RETAIL("retail"),
    CONSUMER("consumer");

    private final String value;

    SupplyChainStage(String value) {
        this.value = value;
    }

    /**
     * Lowercase value persisted in transaction metadata.
     */
    public String getValue() {
        return value;
    }
}
