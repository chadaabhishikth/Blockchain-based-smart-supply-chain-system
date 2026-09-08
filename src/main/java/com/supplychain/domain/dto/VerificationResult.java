package com.supplychain.domain.dto;

import com.supplychain.domain.model.Product;
import com.supplychain.domain.model.ProductHistoryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Layer — Verification Result
 * ===================================
 *
 * Typed result of the consumer-facing product authenticity check
 * ({@code SupplyChainService#verifyProduct}). A result is either a
 * failure (with a machine-readable reason and a recommendation) or a
 * success carrying the full journey.
 */
public final class VerificationResult {

    private final boolean verified;
    private final boolean authentic;
    private final String reason;
    private final String message;
    private final String counterfeitProbability;
    private final String recommendation;

    private final String productId;
    private final String manufacturer;
    private final String originLocation;
    private final String currentOwner;
    private final String currentStatus;
    private final int journeyLength;
    private final List<ProductHistoryEntry> journey;

    private VerificationResult(boolean verified, boolean authentic, String reason, String message,
                               String counterfeitProbability, String recommendation, String productId,
                               String manufacturer, String originLocation, String currentOwner,
                               String currentStatus, int journeyLength, List<ProductHistoryEntry> journey) {
        this.verified = verified;
        this.authentic = authentic;
        this.reason = reason;
        this.message = message;
        this.counterfeitProbability = counterfeitProbability;
        this.recommendation = recommendation;
        this.productId = productId;
        this.manufacturer = manufacturer;
        this.originLocation = originLocation;
        this.currentOwner = currentOwner;
        this.currentStatus = currentStatus;
        this.journeyLength = journeyLength;
        this.journey = journey == null ? null : new ArrayList<>(journey);
    }

    /**
     * A failed verification.
     *
     * @param reason                 machine-readable failure reason
     * @param message                human-readable explanation
     * @param counterfeitProbability HIGH / MEDIUM / null
     * @param recommendation         advice to the consumer
     */
    public static VerificationResult failure(String reason, String message,
                                             String counterfeitProbability, String recommendation) {
        return new VerificationResult(false, false, reason, message, counterfeitProbability, recommendation,
                null, null, null, null, null, 0, null);
    }

    /**
     * A successful verification of an authentic product.
     */
    public static VerificationResult success(String productId, Product product, List<ProductHistoryEntry> journey) {
        return new VerificationResult(true, true, null, null, null,
                "Product is authentic - safe to purchase",
                productId, product.getManufacturer(), product.getOriginLocation(),
                product.getCurrentOwner(), product.getStatus().getValue(), journey.size(), journey);
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isAuthentic() {
        return authentic;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    public String getCounterfeitProbability() {
        return counterfeitProbability;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getProductId() {
        return productId;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getOriginLocation() {
        return originLocation;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public int getJourneyLength() {
        return journeyLength;
    }

    public List<ProductHistoryEntry> getJourney() {
        return journey == null ? null : Collections.unmodifiableList(journey);
    }
}
