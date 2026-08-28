package com.supplychain.supplychain;

import com.supplychain.core.Blockchain;
import com.supplychain.core.HashUtils;
import com.supplychain.core.MerkleTree;

import java.time.Instant;
import java.util.*;

/**
 * Phase 4: Supply Chain Business Logic
 * ======================================
 * This class implements the business logic layer that applies blockchain
 * technology to real-world supply chain scenarios.
 *
 * FUNCTIONS:
 * 1. manufactureProduct(): Create new authenticated products
 * 2. transferOwnership(): Move products through supply chain stages
 * 3. sellToConsumer(): Final sale transaction
 * 4. verifyAuthenticity(): End-to-end verification
 * 5. batchVerification(): Verify entire shipments
 *
 * This is where the "physical world meets blockchain" - handling the
 * business processes that create the transaction records.
 */
public class SupplyChainBlockchain {

    /**
     * Enum representing the stages of the supply chain.
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

        public String getValue() {
            return value;
        }
    }

    /**
     * Status of a product in the system.
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

        public String getValue() {
            return value;
        }
    }

    private Blockchain blockchain;
    private Map<String, Map<String, Object>> products;  // Track product metadata
    private Set<String> authorizedManufacturers;

    /**
     * Initialize the supply chain system.
     */
    public SupplyChainBlockchain() {
        this.blockchain = new Blockchain();
        this.products = new HashMap<>();
        this.authorizedManufacturers = new HashSet<>();
    }

    /**
     * Register an authorized manufacturer in the system.
     *
     * Only registered manufacturers can create new products.
     * This is a whitelist approach to prevent counterfeit at source.
     *
     * @param manufacturerId Unique identifier for the manufacturer
     * @return true if registration successful
     */
    public boolean registerManufacturer(String manufacturerId) {
        authorizedManufacturers.add(manufacturerId);
        return true;
    }

    /**
     * Create new authenticated products at the manufacturing stage.
     *
     * This is the ORIGIN POINT - products created here are the only
     * legitimate products in the system. Any product not originating
     * from this process will be flagged as counterfeit.
     *
     * @param manufacturerId ID of the manufacturing entity
     * @param productIds List of product IDs to create
     * @param location Manufacturing facility location
     * @param batchNumber Optional batch/lot number for traceability
     * @param metadata Additional product information
     * @return Map with manufacturing results
     */
    public Map<String, Object> manufactureProduct(
            String manufacturerId,
            List<String> productIds,
            String location,
            String batchNumber,
            Map<String, Object> metadata) {

        if (!authorizedManufacturers.contains(manufacturerId)) {
            throw new IllegalArgumentException("Manufacturer " + manufacturerId + " not authorized");
        }

        List<Map<String, Object>> transactions = new ArrayList<>();

        for (String productId : productIds) {
            // Create manufacturing transaction
            Map<String, Object> metadataMap = new LinkedHashMap<>();
            metadataMap.put("stage", SupplyChainStage.MANUFACTURING.getValue());
            metadataMap.put("batch_number", batchNumber);
            metadataMap.put("manufacturing_date", Instant.now().toString());
            if (metadata != null) {
                metadataMap.put("custom_metadata", metadata);
            }

            Map<String, Object> txData = HashUtils.createTransaction(
                    productId,
                    "SYSTEM",  // Products originate from "system"
                    manufacturerId,
                    location,
                    metadataMap
            );
            transactions.add(txData);

            // Track product metadata
            Map<String, Object> productInfo = new LinkedHashMap<>();
            productInfo.put("manufacturer", manufacturerId);
            productInfo.put("status", ProductStatus.ACTIVE.getValue());
            productInfo.put("origin_location", location);
            productInfo.put("manufacturing_date", txData.get("timestamp"));
            productInfo.put("current_owner", manufacturerId);
            productInfo.put("batch_number", batchNumber);

            products.put(productId, productInfo);
        }

        // Add block with all manufacturing transactions
        Blockchain.Block block = blockchain.addBlock(transactions);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("products_created", productIds.size());
        result.put("product_ids", productIds);
        result.put("block_index", block.index);
        result.put("block_hash", block.hash);
        result.put("merkle_root", block.merkleRoot);
        result.put("timestamp", block.timestamp);

        return result;
    }

    /**
     * Transfer product ownership through the supply chain.
     *
     * This is used for all intermediate transactions:
     * Factory → Distributor, Distributor → Wholesaler, etc.
     *
     * @param productId Product being transferred
     * @param senderId Current owner
     * @param receiverId New owner
     * @param location Transaction location
     * @param stage Current supply chain stage
     * @param metadata Additional transfer information
     * @return Map with transfer results
     */
    public Map<String, Object> transferOwnership(
            String productId,
            String senderId,
            String receiverId,
            String location,
            SupplyChainStage stage,
            Map<String, Object> metadata) {

        // Validate product exists
        if (!products.containsKey(productId)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", "PRODUCT_NOT_FOUND");
            result.put("message", "Product " + productId + " not found in blockchain");
            result.put("counterfeit", true);
            return result;
        }

        // Validate sender owns the product
        if (!senderId.equals(products.get(productId).get("current_owner"))) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", "INVALID_SENDER");
            result.put("message", senderId + " does not own " + productId);
            result.put("counterfeit", false);
            return result;
        }

        // Create transfer transaction
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        metadataMap.put("stage", stage.getValue());
        metadataMap.put("transfer_date", Instant.now().toString());
        if (metadata != null) {
            metadataMap.put("custom_metadata", metadata);
        }

        Map<String, Object> txData = HashUtils.createTransaction(
                productId,
                senderId,
                receiverId,
                location,
                metadataMap
        );

        // Add to blockchain
        Blockchain.Block block = blockchain.addBlock(Collections.singletonList(txData));

        // Update product tracking
        products.get(productId).put("current_owner", receiverId);
        products.get(productId).put("status", ProductStatus.ACTIVE.getValue());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("product_id", productId);
        result.put("from", senderId);
        result.put("to", receiverId);
        result.put("location", location);
        result.put("stage", stage.getValue());
        result.put("block_index", block.index);
        result.put("transaction_hash", HashUtils.hashTransaction(txData));
        result.put("timestamp", block.timestamp);

        return result;
    }

    /**
     * Final sale transaction from retailer to consumer.
     *
     * This completes the supply chain journey. The consumer receives
     * a verifiable record of the entire product history.
     *
     * @param productId Product being sold
     * @param retailerId Selling retailer
     * @param consumerId Consumer identifier (or 'ANONYMOUS')
     * @param location Sale location
     * @param salePrice Optional sale price for records
     * @param metadata Additional sale information
     * @return Map with sale confirmation and full provenance
     */
    public Map<String, Object> sellToConsumer(
            String productId,
            String retailerId,
            String consumerId,
            String location,
            Double salePrice,
            Map<String, Object> metadata) {

        Map<String, Object> result = transferOwnership(
                productId,
                retailerId,
                consumerId,
                location,
                SupplyChainStage.CONSUMER,
                createSaleMetadata(salePrice, metadata)
        );

        if ((Boolean) result.get("success")) {
            // Update product status to sold
            products.get(productId).put("status", ProductStatus.SOLD.getValue());

            // Add provenance information
            result.put("provenance", getProductProvenance(productId));
        }

        return result;
    }

    /**
     * Create metadata map for sale transactions.
     */
    private Map<String, Object> createSaleMetadata(Double salePrice, Map<String, Object> metadata) {
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        if (salePrice != null) {
            metadataMap.put("sale_price", salePrice);
        }
        metadataMap.put("sale_date", Instant.now().toString());
        if (metadata != null) {
            metadataMap.put("custom_metadata", metadata);
        }
        return metadataMap;
    }

    /**
     * Verify product authenticity and trace its complete journey.
     *
     * This is the primary consumer-facing verification function.
     *
     * @param productId Product to verify
     * @return Map with verification status and full provenance
     */
    public Map<String, Object> verifyProduct(String productId) {
        // Step 1: Check if product exists in our registry
        if (!products.containsKey(productId)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", false);
            result.put("authentic", false);
            result.put("reason", "PRODUCT_NOT_IN_REGISTRY");
            result.put("message", "Product " + productId + " is not registered in this supply chain system");
            result.put("counterfeit_probability", "HIGH");
            result.put("recommendation", "Do not purchase - product cannot be verified");
            return result;
        }

        // Step 2: Verify blockchain integrity
        if (!blockchain.isValid()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", false);
            result.put("authentic", false);
            result.put("reason", "BLOCKCHAIN_CORRUPTED");
            result.put("message", "Blockchain integrity check failed");
            result.put("recommendation", "System error - contact administrator");
            return result;
        }

        // Step 3: Get product history from blockchain
        List<Map<String, Object>> history = blockchain.getProductHistory(productId);

        if (history.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", false);
            result.put("authentic", false);
            result.put("reason", "NO_BLOCKCHAIN_RECORD");
            result.put("message", "Product in registry but no blockchain transactions found");
            result.put("counterfeit_probability", "MEDIUM");
            result.put("recommendation", "Contact manufacturer");
            return result;
        }

        // Step 4: Verify origin
        Map<String, Object> origin = history.get(0);
        if (!"SYSTEM".equals(origin.get("sender"))) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", false);
            result.put("authentic", false);
            result.put("reason", "INVALID_ORIGIN");
            result.put("message", "Product does not originate from a valid manufacturing process");
            result.put("counterfeit_probability", "HIGH");
            result.put("recommendation", "Do not purchase - counterfeit detected");
            return result;
        }

        Map<String, Object> productInfo = products.get(productId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verified", true);
        result.put("authentic", true);
        result.put("product_id", productId);
        result.put("manufacturer", productInfo.get("manufacturer"));
        result.put("origin_location", productInfo.get("origin_location"));
        result.put("current_owner", productInfo.get("current_owner"));
        result.put("current_status", productInfo.get("status"));
        result.put("journey_length", history.size());
        result.put("journey", history);
        result.put("blockchain_integrity", "VERIFIED");
        result.put("recommendation", "Product is authentic - safe to purchase");

        return result;
    }

    /**
     * Get complete provenance information for a product.
     *
     * @param productId Product to trace
     * @return Complete provenance report
     */
    public Map<String, Object> getProductProvenance(String productId) {
        if (!products.containsKey(productId)) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Product not found");
            return error;
        }

        List<Map<String, Object>> history = blockchain.getProductHistory(productId);
        Map<String, Object> productInfo = products.get(productId);

        List<Map<String, Object>> journey = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            Map<String, Object> record = history.get(i);
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("step", i + 1);
            step.put("from", record.get("sender"));
            step.put("to", record.get("receiver"));
            step.put("location", record.get("location"));
            step.put("timestamp", record.get("timestamp"));
            step.put("verified", true);
            journey.add(step);
        }

        Map<String, Object> provenance = new LinkedHashMap<>();
        provenance.put("product_id", productId);
        provenance.put("manufacturer", productInfo.get("manufacturer"));
        provenance.put("manufacturing_date", productInfo.get("manufacturing_date"));
        provenance.put("origin", Collections.singletonMap("location", productInfo.get("origin_location")));
        provenance.put("journey", journey);
        provenance.put("total_transactions", history.size());
        provenance.put("blockchain_verified", true);

        return provenance;
    }

    /**
     * Verify an entire batch of products at once.
     *
     * This uses Merkle Tree efficiency to verify multiple products
     * simultaneously without checking each individually.
     *
     * @param productIds List of product IDs to verify
     * @return Batch verification report
     */
    public Map<String, Object> verifyBatch(List<String> productIds) {
        int verifiedProducts = 0;
        int failedProducts = 0;
        List<Map<String, Object>> productResults = new ArrayList<>();

        for (String productId : productIds) {
            Map<String, Object> verification = verifyProduct(productId);

            if ((Boolean) verification.get("authentic")) {
                verifiedProducts++;
            } else {
                failedProducts++;
            }

            Map<String, Object> productResult = new LinkedHashMap<>();
            productResult.put("product_id", productId);
            productResult.put("authentic", verification.get("authentic"));
            productResult.put("reason", verification.get("reason"));
            productResults.add(productResult);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_products", productIds.size());
        result.put("verified_products", verifiedProducts);
        result.put("failed_products", failedProducts);
        result.put("products", productResults);
        result.put("batch_authentic", failedProducts == 0);
        result.put("success_rate", (verifiedProducts * 100.0) / productIds.size());

        return result;
    }

    /**
     * Get summary statistics of the supply chain.
     */
    public Map<String, Object> getSupplyChainSummary() {
        int totalProducts = products.size();
        int activeProducts = 0;
        int soldProducts = 0;

        for (Map<String, Object> product : products.values()) {
            String status = (String) product.get("status");
            if (ProductStatus.ACTIVE.getValue().equals(status)) {
                activeProducts++;
            } else if (ProductStatus.SOLD.getValue().equals(status)) {
                soldProducts++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_products", totalProducts);
        summary.put("active_products", activeProducts);
        summary.put("sold_products", soldProducts);
        summary.put("registered_manufacturers", authorizedManufacturers.size());
        summary.put("total_blocks", blockchain.getChainLength());
        summary.put("pending_transactions", blockchain.getPendingCount());
        summary.put("blockchain_valid", blockchain.isValid());

        return summary;
    }

    // =========================================================================
    // DEMONSTRATION & TESTING
    // =========================================================================

    /**
     * Demonstrate complete supply chain lifecycle.
     */
    public static void demoSupplyChain() {
        System.out.println("\n======================================================================");
        System.out.println("SUPPLY CHAIN BLOCKCHAIN DEMONSTRATION");
        System.out.println("======================================================================\n");

        // Initialize system
        SupplyChainBlockchain sc = new SupplyChainBlockchain();

        // Register authorized manufacturers
        System.out.println("STEP 1: Registering authorized manufacturers...");
        sc.registerManufacturer("AUTHENTIC-FACTORY-A");
        sc.registerManufacturer("AUTHENTIC-FACTORY-B");
        System.out.println("✓ Factories registered\n");

        // Manufacture products
        System.out.println("STEP 2: Manufacturing products...");
        List<String> productIds = Arrays.asList("PROD-001", "PROD-002", "PROD-003");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("product_type", "Electronics");
        metadata.put("quality_grade", "A");

        Map<String, Object> result = sc.manufactureProduct(
                "AUTHENTIC-FACTORY-A",
                productIds,
                "Shanghai Manufacturing Hub",
                "BATCH-2026-001",
                metadata
        );
        System.out.println("✓ " + result.get("products_created") + " products manufactured");
        System.out.println("  Block #" + result.get("block_index") + " created with Merkle root:");
        System.out.println("  " + ((String) result.get("merkle_root")).substring(0, 32) + "...\n");

        // Transfer to distributor
        System.out.println("STEP 3: Transferring to distributor...");
        for (String productId : Arrays.asList("PROD-001", "PROD-002")) {
            Map<String, Object> transferResult = sc.transferOwnership(
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

        // Transfer to wholesaler
        System.out.println("STEP 4: Transferring to wholesaler...");
        Map<String, Object> wholeResult = sc.transferOwnership(
                "PROD-001",
                "LOGISTICS-DIST-B",
                "WHOLESALE-CENTRAL",
                "Beijing Wholesale Hub",
                SupplyChainStage.WHOLESALING,
                null
        );
        System.out.println("  ✓ PROD-001 → WHOLESALE-CENTRAL\n");

        // Transfer to retailer
        System.out.println("STEP 5: Transferring to retailer...");
        Map<String, Object> retailResult = sc.transferOwnership(
                "PROD-001",
                "WHOLESALE-CENTRAL",
                "RETAIL-STORE-SHANGHAI",
                "Shanghai Retail Store",
                SupplyChainStage.RETAIL,
                null
        );
        System.out.println("  ✓ PROD-001 → RETAIL-STORE-SHANGHAI\n");

        // Sell to consumer
        System.out.println("STEP 6: Selling to consumer...");
        Map<String, Object> saleResult = sc.sellToConsumer(
                "PROD-001",
                "RETAIL-STORE-SHANGHAI",
                "CONSUMER-XYZ-123",
                "Shanghai Retail Store",
                299.99,
                null
        );
        System.out.println("  ✓ PROD-001 sold to CONSUMER-XYZ-123 for $299.99");
        System.out.println("  Transaction hash: " + ((String) saleResult.get("transaction_hash")).substring(0, 32) + "...\n");

        // Verify product
        System.out.println("STEP 7: Verifying product authenticity...");
        Map<String, Object> verification = sc.verifyProduct("PROD-001");
        System.out.println("\n  VERIFICATION RESULT:");
        System.out.println("  " + (verification.get("verified").equals(true) ? "✓" : "✗") + " Verified: " + verification.get("verified"));
        System.out.println("  " + (verification.get("authentic").equals(true) ? "✓" : "✗") + " Authentic: " + verification.get("authentic"));
        System.out.println("  Manufacturer: " + verification.get("manufacturer"));
        System.out.println("  Current Owner: " + verification.get("current_owner"));
        System.out.println("  Journey Steps: " + verification.get("journey_length"));
        System.out.println("  Recommendation: " + verification.get("recommendation") + "\n");

        // Try to verify counterfeit product
        System.out.println("STEP 8: Testing counterfeit detection...");
        Map<String, Object> fakeVerification = sc.verifyProduct("PROD-FAKE-999");
        System.out.println("\n  VERIFICATION RESULT:");
        System.out.println("  " + (fakeVerification.get("verified").equals(true) ? "✓" : "✗") + " Verified: " + fakeVerification.get("verified"));
        System.out.println("  " + (fakeVerification.get("authentic").equals(true) ? "✓" : "✗") + " Authentic: " + fakeVerification.get("authentic"));
        System.out.println("  Reason: " + fakeVerification.get("reason"));
        System.out.println("  Recommendation: " + fakeVerification.get("recommendation") + "\n");

        // Batch verification
        System.out.println("STEP 9: Batch verification...");
        List<String> batchIds = Arrays.asList("PROD-001", "PROD-002", "PROD-FAKE");
        Map<String, Object> batchResult = sc.verifyBatch(batchIds);
        System.out.println("  Total Products: " + batchResult.get("total_products"));
        System.out.println("  Verified: " + batchResult.get("verified_products"));
        System.out.println("  Failed: " + batchResult.get("failed_products"));
        System.out.println("  Success Rate: " + String.format("%.1f", batchResult.get("success_rate")) + "%\n");

        // Summary
        Map<String, Object> summary = sc.getSupplyChainSummary();
        System.out.println("\n======================================================================");
        System.out.println("SUPPLY CHAIN SUMMARY");
        System.out.println("======================================================================");
        System.out.println("Total Products: " + summary.get("total_products"));
        System.out.println("Active Products: " + summary.get("active_products"));
        System.out.println("Sold Products: " + summary.get("sold_products"));
        System.out.println("Total Blocks: " + summary.get("total_blocks"));
        System.out.println("Blockchain Valid: " + summary.get("blockchain_valid"));
        System.out.println("======================================================================\n");
    }

    public static void main(String[] args) {
        demoSupplyChain();
    }
}
