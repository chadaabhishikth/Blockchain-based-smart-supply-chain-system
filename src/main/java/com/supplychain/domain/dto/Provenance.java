package com.supplychain.domain.dto;

import com.supplychain.domain.model.ProductHistoryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Layer — Provenance Report
 * =================================
 *
 * The complete verified history of a product: where it was made, when,
 * and every custody step in between. This is what a consumer effectively
 * receives when scanning a product's QR code.
 */
public final class Provenance {

    private final String productId;
    private final String manufacturer;
    private final String manufacturingDate;
    private final String originLocation;
    private final List<ProductHistoryEntry> journey;
    private final int totalTransactions;
    private final boolean blockchainVerified;

    public Provenance(String productId, String manufacturer, String manufacturingDate,
                      String originLocation, List<ProductHistoryEntry> journey) {
        this.productId = productId;
        this.manufacturer = manufacturer;
        this.manufacturingDate = manufacturingDate;
        this.originLocation = originLocation;
        this.journey = new ArrayList<>(journey);
        this.totalTransactions = this.journey.size();
        this.blockchainVerified = true;
    }

    public String getProductId() {
        return productId;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getManufacturingDate() {
        return manufacturingDate;
    }

    public String getOriginLocation() {
        return originLocation;
    }

    public List<ProductHistoryEntry> getJourney() {
        return Collections.unmodifiableList(journey);
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public boolean isBlockchainVerified() {
        return blockchainVerified;
    }
}
