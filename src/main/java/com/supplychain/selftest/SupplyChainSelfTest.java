package com.supplychain.selftest;

import com.supplychain.domain.dto.BatchVerificationResult;
import com.supplychain.domain.dto.ManufactureResult;
import com.supplychain.domain.dto.TransferResult;
import com.supplychain.domain.dto.VerificationResult;
import com.supplychain.domain.model.SupplyChainStage;
import com.supplychain.domain.service.SupplyChainService;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;

/**
 * Self-Test Layer — Phase 4: Supply Chain Operations
 * ===================================================
 *
 * End-to-end tests of the business logic layer: the complete product
 * lifecycle, counterfeit detection and batch verification.
 *
 * Run from the menu (option 1) or directly:
 * java com.supplychain.selftest.SupplyChainSelfTest
 */
public final class SupplyChainSelfTest {

    private final PrintStream out;

    public SupplyChainSelfTest(PrintStream out) {
        this.out = out;
    }

    /**
     * Drive one product through the full lifecycle and verify it.
     */
    public void testCompleteProductLifecycle() {
        SupplyChainService service = new SupplyChainService();

        service.registerManufacturer("FACTORY-A");

        ManufactureResult manufactured = service.manufactureProduct(
                "FACTORY-A",
                Collections.singletonList("PROD-LIFE-001"),
                "Factory Floor",
                "BATCH-001",
                null
        );
        Check.that(manufactured.getProductsCreated() == 1, "One product must be created");

        TransferResult toDistributor = service.transferOwnership(
                "PROD-LIFE-001", "FACTORY-A", "DISTRIBUTOR-B", "Warehouse 1", SupplyChainStage.DISTRIBUTION, null);
        Check.that(toDistributor.isSuccess(), "Transfer to distributor must succeed");

        TransferResult toRetailer = service.transferOwnership(
                "PROD-LIFE-001", "DISTRIBUTOR-B", "RETAILER-C", "Store Front", SupplyChainStage.RETAIL, null);
        Check.that(toRetailer.isSuccess(), "Transfer to retailer must succeed");

        TransferResult sale = service.sellToConsumer(
                "PROD-LIFE-001", "RETAILER-C", "CONSUMER-X", "Store Front", 99.99, null);
        Check.that(sale.isSuccess(), "Sale to consumer must succeed");
        Check.that(sale.getProvenance() != null, "Sale must attach full provenance");

        VerificationResult verification = service.verifyProduct("PROD-LIFE-001");
        Check.that(verification.isVerified(), "Product must verify after lifecycle");
        Check.that(verification.isAuthentic(), "Product must be authentic after lifecycle");
        Check.that(verification.getJourneyLength() == 4, "Journey must contain 4 steps, got "
                + verification.getJourneyLength());

        out.println("✓ Complete product lifecycle verified (manufacture → sale)");
    }

    public void testCounterfeitDetection() {
        SupplyChainService service = new SupplyChainService();

        // A product that was never manufactured must be rejected as counterfeit
        TransferResult transfer = service.transferOwnership(
                "PROD-FAKE-999", "SOMEONE", "SOMEONE-ELSE", "Alley", SupplyChainStage.DISTRIBUTION, null);
        Check.that(!transfer.isSuccess(), "Transfer of unknown product must fail");
        Check.that(transfer.isCounterfeit(), "Unknown product must be flagged as counterfeit");
        Check.that("PRODUCT_NOT_FOUND".equals(transfer.getError()), "Error must be PRODUCT_NOT_FOUND");

        VerificationResult verification = service.verifyProduct("PROD-FAKE-999");
        Check.that(!verification.isAuthentic(), "Unknown product must not be authentic");
        Check.that("PRODUCT_NOT_IN_REGISTRY".equals(verification.getReason()),
                "Reason must be PRODUCT_NOT_IN_REGISTRY");

        out.println("✓ Counterfeit detection rejects unregistered products");
    }

    public void testUnauthorizedManufacturer() {
        SupplyChainService service = new SupplyChainService();

        boolean rejected = false;
        try {
            service.manufactureProduct(
                    "ROGUE-FACTORY",
                    Arrays.asList("PROD-ROGUE-1", "PROD-ROGUE-2"),
                    "Back Alley",
                    "BATCH-ROGUE",
                    null
            );
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        Check.that(rejected, "Unregistered manufacturer must be rejected");

        out.println("✓ Unauthorized manufacturers cannot create products");
    }

    public void testBatchVerification() {
        SupplyChainService service = new SupplyChainService();

        service.registerManufacturer("FACTORY-A");
        service.manufactureProduct("FACTORY-A", Arrays.asList("PROD-B-1", "PROD-B-2"), "Factory", "BATCH-B", null);

        BatchVerificationResult batch = service.verifyBatch(Arrays.asList("PROD-B-1", "PROD-B-2", "PROD-FAKE"));

        Check.that(batch.getTotalProducts() == 3, "Batch must count all products");
        Check.that(batch.getVerifiedProducts() == 2, "Two products must verify");
        Check.that(batch.getFailedProducts() == 1, "One product must fail");
        Check.that(!batch.isBatchAuthentic(), "Mixed batch must not be authentic");
        Check.that(batch.getSuccessRate() == 200.0 / 3.0, "Success rate must be 66.7%");

        out.println("✓ Batch verification reports " + batch.getVerifiedProducts()
                + "/" + batch.getTotalProducts() + " authentic");
    }

    public void runAll() {
        out.println("============================================================");
        out.println("PHASE 4: SUPPLY CHAIN OPERATIONS UNIT TESTS");
        out.println("============================================================\n");

        testCompleteProductLifecycle();
        testCounterfeitDetection();
        testUnauthorizedManufacturer();
        testBatchVerification();

        out.println("\n============================================================");
        out.println("ALL TESTS PASSED ✓");
        out.println("============================================================\n");
    }

    public static void main(String[] args) {
        new SupplyChainSelfTest(System.out).runAll();
    }
}
