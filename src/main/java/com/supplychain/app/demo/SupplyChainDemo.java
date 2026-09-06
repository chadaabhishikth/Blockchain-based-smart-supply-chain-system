package com.supplychain.app.demo;

import com.supplychain.domain.dto.BatchVerificationResult;
import com.supplychain.domain.dto.ManufactureResult;
import com.supplychain.domain.dto.SupplyChainSummary;
import com.supplychain.domain.dto.TransferResult;
import com.supplychain.domain.dto.VerificationResult;
import com.supplychain.domain.model.SupplyChainStage;
import com.supplychain.domain.service.SupplyChainService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application Layer — Supply Chain Demo
 * ======================================
 *
 * Console demonstration of a complete product lifecycle. Pure
 * presentation: it builds a {@link SupplyChainService}, drives it
 * through the business use cases and prints the typed results.
 *
 * STORY:
 *   Step 1  register authorized manufacturers
 *   Step 2  manufacture a batch of products
 *   Step 3  transfer products to a distributor
 *   Step 4  transfer to a wholesaler
 *   Step 5  transfer to a retailer
 *   Step 6  sell one product to a consumer
 *   Step 7  verify the sold product (authentic)
 *   Step 8  try to verify a counterfeit product
 *   Step 9  batch-verify a mixed shipment
 */
public final class SupplyChainDemo {

    /**
     * Run the demonstration.
     */
    public void run() {
        System.out.println("\n======================================================================");
        System.out.println("SUPPLY CHAIN BLOCKCHAIN DEMONSTRATION");
        System.out.println("======================================================================\n");

        // Initialize system
        SupplyChainService service = new SupplyChainService();

        // STEP 1: Register authorized manufacturers
        System.out.println("STEP 1: Registering authorized manufacturers...");
        service.registerManufacturer("AUTHENTIC-FACTORY-A");
        service.registerManufacturer("AUTHENTIC-FACTORY-B");
        System.out.println("✓ Factories registered\n");

        // STEP 2: Manufacture products
        System.out.println("STEP 2: Manufacturing products...");
        List<String> productIds = Arrays.asList("PROD-001", "PROD-002", "PROD-003");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("product_type", "Electronics");
        metadata.put("quality_grade", "A");

        ManufactureResult manufactured = service.manufactureProduct(
                "AUTHENTIC-FACTORY-A",
                productIds,
                "Shanghai Manufacturing Hub",
                "BATCH-2026-001",
                metadata
        );
        System.out.println("✓ " + manufactured.getProductsCreated() + " products manufactured");
        System.out.println("  Block #" + manufactured.getBlockIndex() + " created with Merkle root:");
        System.out.println("  " + manufactured.getMerkleRoot().substring(0, 32) + "...\n");

        // STEP 3: Transfer to distributor
        System.out.println("STEP 3: Transferring to distributor...");
        for (String productId : Arrays.asList("PROD-001", "PROD-002")) {
            service.transferOwnership(
                    productId,
                    "AUTHENTIC-FACTORY-A",
                    "LOGISTICS-DIST-B",
                    "Shanghai Distribution Center",
                    SupplyChainStage.DISTRIBUTION,
                    null
            );
            System.out.println("  ✓ " + productId + " → LOGISTICS-DIST-B");
        }
        System.out.println();

        // STEP 4: Transfer to wholesaler
        System.out.println("STEP 4: Transferring to wholesaler...");
        service.transferOwnership(
                "PROD-001",
                "LOGISTICS-DIST-B",
                "WHOLESALE-CENTRAL",
                "Beijing Wholesale Hub",
                SupplyChainStage.WHOLESALING,
                null
        );
        System.out.println("  ✓ PROD-001 → WHOLESALE-CENTRAL\n");

        // STEP 5: Transfer to retailer
        System.out.println("STEP 5: Transferring to retailer...");
        service.transferOwnership(
                "PROD-001",
                "WHOLESALE-CENTRAL",
                "RETAIL-STORE-SHANGHAI",
                "Shanghai Retail Store",
                SupplyChainStage.RETAIL,
                null
        );
        System.out.println("  ✓ PROD-001 → RETAIL-STORE-SHANGHAI\n");

        // STEP 6: Sell to consumer
        System.out.println("STEP 6: Selling to consumer...");
        TransferResult sale = service.sellToConsumer(
                "PROD-001",
                "RETAIL-STORE-SHANGHAI",
                "CONSUMER-XYZ-123",
                "Shanghai Retail Store",
                299.99,
                null
        );
        System.out.println("  ✓ PROD-001 sold to CONSUMER-XYZ-123 for $299.99");
        System.out.println("  Transaction hash: " + sale.getTransactionHash().substring(0, 32) + "...\n");

        // STEP 7: Verify the authentic product
        System.out.println("STEP 7: Verifying product authenticity...");
        VerificationResult verification = service.verifyProduct("PROD-001");
        System.out.println("\n  VERIFICATION RESULT:");
        System.out.println("  " + (verification.isVerified() ? "✓" : "✗") + " Verified: " + verification.isVerified());
        System.out.println("  " + (verification.isAuthentic() ? "✓" : "✗") + " Authentic: " + verification.isAuthentic());
        System.out.println("  Manufacturer: " + verification.getManufacturer());
        System.out.println("  Current Owner: " + verification.getCurrentOwner());
        System.out.println("  Journey Steps: " + verification.getJourneyLength());
        System.out.println("  Recommendation: " + verification.getRecommendation() + "\n");

        // STEP 8: Try to verify a counterfeit product
        System.out.println("STEP 8: Testing counterfeit detection...");
        VerificationResult fakeVerification = service.verifyProduct("PROD-FAKE-999");
        System.out.println("\n  VERIFICATION RESULT:");
        System.out.println("  " + (fakeVerification.isVerified() ? "✓" : "✗") + " Verified: " + fakeVerification.isVerified());
        System.out.println("  " + (fakeVerification.isAuthentic() ? "✓" : "✗") + " Authentic: " + fakeVerification.isAuthentic());
        System.out.println("  Reason: " + fakeVerification.getReason());
        System.out.println("  Recommendation: " + fakeVerification.getRecommendation() + "\n");

        // STEP 9: Batch verification
        System.out.println("STEP 9: Batch verification...");
        List<String> batchIds = Arrays.asList("PROD-001", "PROD-002", "PROD-FAKE");
        BatchVerificationResult batchResult = service.verifyBatch(batchIds);
        System.out.println("  Total Products: " + batchResult.getTotalProducts());
        System.out.println("  Verified: " + batchResult.getVerifiedProducts());
        System.out.println("  Failed: " + batchResult.getFailedProducts());
        System.out.println("  Success Rate: " + String.format("%.1f", batchResult.getSuccessRate()) + "%\n");

        // Summary
        SupplyChainSummary summary = service.getSummary();
        System.out.println("\n======================================================================");
        System.out.println("SUPPLY CHAIN SUMMARY");
        System.out.println("======================================================================");
        System.out.println("Total Products: " + summary.getTotalProducts());
        System.out.println("Active Products: " + summary.getActiveProducts());
        System.out.println("Sold Products: " + summary.getSoldProducts());
        System.out.println("Total Blocks: " + summary.getTotalBlocks());
        System.out.println("Blockchain Valid: " + summary.isBlockchainValid());
        System.out.println("======================================================================\n");
    }
}
