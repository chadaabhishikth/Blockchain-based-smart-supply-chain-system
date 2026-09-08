package com.supplychain.domain.dto;

/**
 * Domain Layer — Product Verification (Batch Item)
 * =================================================
 *
 * The per-product line item inside a {@link BatchVerificationResult}.
 */
public final class ProductVerification {

    private final String productId;
    private final boolean authentic;
    private final String reason;

    public ProductVerification(String productId, boolean authentic, String reason) {
        this.productId = productId;
        this.authentic = authentic;
        this.reason = reason;
    }

    public String getProductId() {
        return productId;
    }

    public boolean isAuthentic() {
        return authentic;
    }

    public String getReason() {
        return reason;
    }
}
