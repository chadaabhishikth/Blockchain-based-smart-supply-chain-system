package com.supplychain.domain.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Layer — Batch Verification Result
 * =========================================
 *
 * Aggregated result of verifying an entire shipment of products at once.
 */
public final class BatchVerificationResult {

    private final int totalProducts;
    private final int verifiedProducts;
    private final int failedProducts;
    private final List<ProductVerification> results;
    private final boolean batchAuthentic;
    private final double successRate;

    public BatchVerificationResult(int totalProducts, int verifiedProducts, int failedProducts,
                                   List<ProductVerification> results, boolean batchAuthentic, double successRate) {
        this.totalProducts = totalProducts;
        this.verifiedProducts = verifiedProducts;
        this.failedProducts = failedProducts;
        this.results = new ArrayList<>(results);
        this.batchAuthentic = batchAuthentic;
        this.successRate = successRate;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getVerifiedProducts() {
        return verifiedProducts;
    }

    public int getFailedProducts() {
        return failedProducts;
    }

    public List<ProductVerification> getResults() {
        return Collections.unmodifiableList(results);
    }

    public boolean isBatchAuthentic() {
        return batchAuthentic;
    }

    public double getSuccessRate() {
        return successRate;
    }
}
