package com.supplychain.domain.service;

import com.supplychain.crypto.HashFunction;
import com.supplychain.crypto.Sha256Hasher;
import com.supplychain.crypto.TransactionSerializer;
import com.supplychain.domain.dto.BatchVerificationResult;
import com.supplychain.domain.dto.ManufactureResult;
import com.supplychain.domain.dto.ProductVerification;
import com.supplychain.domain.dto.Provenance;
import com.supplychain.domain.dto.SupplyChainSummary;
import com.supplychain.domain.dto.TransactionMerkleProof;
import com.supplychain.domain.dto.TransferResult;
import com.supplychain.domain.dto.VerificationResult;
import com.supplychain.domain.ledger.Blockchain;
import com.supplychain.domain.merkle.MerkleTree;
import com.supplychain.domain.model.Block;
import com.supplychain.domain.model.Product;
import com.supplychain.domain.model.ProductHistoryEntry;
import com.supplychain.domain.model.ProductStatus;
import com.supplychain.domain.model.SupplyChainStage;
import com.supplychain.domain.model.Transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * Domain Layer — Supply Chain Service
 * =====================================
 *
 * The business logic that applies the blockchain ledger to real-world
 * supply chain scenarios. This is where "the physical world meets the
 * blockchain": the business processes that create the transaction
 * records.
 *
 * Responsibilities:
 * 1. {@link #registerManufacturer}  — whitelist who may create products
 * 2. {@link #manufactureProduct}    — the legitimate origin of products
 * 3. {@code #transferOwnership}     — custody changes between parties
 * 4. {@link #sellToConsumer}        — the final sale
 * 5. {@link #verifyProduct}         — end-to-end authenticity check
 * 6. {@code #verifyBatch}           — verify entire shipments
 *
 * The service keeps an in-memory registry of the products it created;
 * the authoritative provenance always lives on the blockchain.
 */
public class SupplyChainService {

    private final Blockchain ledger;
    private final Map<String, Product> products = new HashMap<>();
    private final Set<String> authorizedManufacturers = new HashSet<>();
    private final HashFunction hasher = new Sha256Hasher();

    public SupplyChainService() {
        this(new Blockchain());
    }

    /**
     * @param ledger the blockchain ledger this service commits to
     */
    public SupplyChainService(Blockchain ledger) {
        this.ledger = ledger;
    }

    /**
     * Register an authorized manufacturer.
     *
     * Only registered manufacturers can create new products — a
     * whitelist approach that prevents counterfeiting at the source.
     *
     * @param manufacturerId unique identifier for the manufacturer
     * @return true if the manufacturer was newly registered
     */
    public boolean registerManufacturer(String manufacturerId) {
        return authorizedManufacturers.add(manufacturerId);
    }

    /**
     * @return true if the given manufacturer id is registered
     */
    public boolean isAuthorizedManufacturer(String manufacturerId) {
        return authorizedManufacturers.contains(manufacturerId);
    }

    /**
     * Create new authenticated products at the manufacturing stage.
     *
     * This is the ORIGIN POINT: products created here are the only
     * legitimate products in the system. Any product that did not
     * originate from this process is flagged as counterfeit.
     *
     * @param manufacturerId id of the manufacturing entity (must be registered)
     * @param productIds     product ids to create
     * @param location       manufacturing facility location
     * @param batchNumber    batch/lot number for traceability
     * @param metadata       optional additional product information
     * @return typed result describing the committed block
     * @throws IllegalArgumentException if the manufacturer is not registered
     */
    public ManufactureResult manufactureProduct(String manufacturerId, List<String> productIds,
                                                String location, String batchNumber,
                                                Map<String, Object> metadata) {
        if (!authorizedManufacturers.contains(manufacturerId)) {
            throw new IllegalArgumentException("Manufacturer " + manufacturerId + " not authorized");
        }

        List<Transaction> transactions = new ArrayList<>();

        for (String productId : productIds) {
            Map<String, Object> metadataMap = new LinkedHashMap<>();
            metadataMap.put("stage", SupplyChainStage.MANUFACTURING.getValue());
            metadataMap.put("batch_number", batchNumber);
            metadataMap.put("manufacturing_date", Instant.now().toString());
            if (metadata != null) {
                metadataMap.put("custom_metadata", new LinkedHashMap<>(metadata));
            }

            Transaction tx = Transaction.create(productId, "SYSTEM", manufacturerId, location, metadataMap);
            transactions.add(tx);

            products.put(productId, new Product(productId, manufacturerId, location, tx.getTimestamp(), batchNumber));
        }

        Block block = ledger.addBlock(transactions);
        return new ManufactureResult(productIds, block.getIndex(), block.getHash(),
                block.getMerkleRoot(), block.getTimestamp());
    }

    /**
     * Transfer product ownership through the supply chain.
     *
     * Used for all intermediate custody changes:
     * Factory → Distributor → Wholesaler → Retailer.
     *
     * @param productId  product being transferred
     * @param senderId   current owner
     * @param receiverId new owner
     * @param location   transaction location
     * @param stage      current supply chain stage
     * @param metadata   optional transfer information
     * @return typed result; never throws for business failures
     */
    public TransferResult transferOwnership(String productId, String senderId, String receiverId,
                                            String location, SupplyChainStage stage,
                                            Map<String, Object> metadata) {
        Product product = products.get(productId);
        if (product == null) {
            return TransferResult.failure("PRODUCT_NOT_FOUND",
                    "Product " + productId + " not found in blockchain", true);
        }

        if (!senderId.equals(product.getCurrentOwner())) {
            return TransferResult.failure("INVALID_SENDER",
                    senderId + " does not own " + productId, false);
        }

        Map<String, Object> metadataMap = new LinkedHashMap<>();
        metadataMap.put("stage", stage.getValue());
        metadataMap.put("transfer_date", Instant.now().toString());
        if (metadata != null) {
            metadataMap.put("custom_metadata", new LinkedHashMap<>(metadata));
        }

        Transaction tx = Transaction.create(productId, senderId, receiverId, location, metadataMap);
        Block block = ledger.addBlock(Collections.singletonList(tx));
        product.transferTo(receiverId);

        String transactionHash = hasher.hash(TransactionSerializer.serialize(tx));
        return TransferResult.success(productId, senderId, receiverId, location,
                stage.getValue(), block.getIndex(), transactionHash, block.getTimestamp());
    }

    /**
     * Final sale transaction from a retailer to a consumer.
     *
     * Completes the supply chain journey: the consumer receives a
     * verifiable record of the entire product history.
     *
     * @param productId  product being sold
     * @param retailerId selling retailer
     * @param consumerId consumer identifier (or "ANONYMOUS")
     * @param location   sale location
     * @param salePrice  optional sale price for the records
     * @param metadata   optional sale information
     * @return transfer result; on success the full provenance is attached
     */
    public TransferResult sellToConsumer(String productId, String retailerId, String consumerId,
                                         String location, Double salePrice, Map<String, Object> metadata) {
        TransferResult result = transferOwnership(productId, retailerId, consumerId, location,
                SupplyChainStage.CONSUMER, createSaleMetadata(salePrice, metadata));

        if (result.isSuccess()) {
            products.get(productId).markSold();
            result = result.withProvenance(getProductProvenance(productId).orElse(null));
        }

        return result;
    }

    /**
     * Verify product authenticity and trace its complete journey.
     *
     * This is the primary consumer-facing verification — the code behind
     * "scan a product and see if it is genuine". Performs, in order:
     *
     * 1. registry check          — is this product known to this system?
     * 2. ledger integrity check  — has the blockchain itself been tampered with?
     * 3. blockchain record check — does provenance exist for the product?
     * 4. origin check            — does the product stem from manufacturing?
     *
     * @param productId product to verify
     * @return typed verification result (never null)
     */
    public VerificationResult verifyProduct(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            return VerificationResult.failure("PRODUCT_NOT_IN_REGISTRY",
                    "Product " + productId + " is not registered in this supply chain system",
                    "HIGH", "Do not purchase - product cannot be verified");
        }

        if (!ledger.isValid()) {
            return VerificationResult.failure("BLOCKCHAIN_CORRUPTED",
                    "Blockchain integrity check failed",
                    null, "System error - contact administrator");
        }

        List<ProductHistoryEntry> history = ledger.getProductHistory(productId);
        if (history.isEmpty()) {
            return VerificationResult.failure("NO_BLOCKCHAIN_RECORD",
                    "Product in registry but no blockchain transactions found",
                    "MEDIUM", "Contact manufacturer");
        }

        if (!"SYSTEM".equals(history.get(0).getSender())) {
            return VerificationResult.failure("INVALID_ORIGIN",
                    "Product does not originate from a valid manufacturing process",
                    "HIGH", "Do not purchase - counterfeit detected");
        }

        return VerificationResult.success(productId, product, history);
    }

    /**
     * Build the complete provenance report for a product.
     *
     * @param productId product to trace
     * @return the provenance, or empty if the product is unknown
     */
    public Optional<Provenance> getProductProvenance(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            return Optional.empty();
        }

        List<ProductHistoryEntry> journey = ledger.getProductHistory(productId);
        return Optional.of(new Provenance(productId, product.getManufacturer(),
                product.getManufacturingDate(), product.getOriginLocation(), journey));
    }

    /**
     * Verify an entire batch of products at once.
     *
     * @param productIds product ids to verify
     * @return aggregated batch report
     */
    public BatchVerificationResult verifyBatch(List<String> productIds) {
        List<ProductVerification> results = new ArrayList<>();
        int verifiedProducts = 0;

        for (String productId : productIds) {
            VerificationResult verification = verifyProduct(productId);
            boolean authentic = verification.isAuthentic();
            if (authentic) {
                verifiedProducts++;
            }
            results.add(new ProductVerification(productId, authentic, verification.getReason()));
        }

        int failedProducts = productIds.size() - verifiedProducts;
        double successRate = (verifiedProducts * 100.0) / productIds.size();

        return new BatchVerificationResult(productIds.size(), verifiedProducts, failedProducts,
                results, failedProducts == 0, successRate);
    }

    /**
     * Aggregate statistics of the supply chain.
     */
    public SupplyChainSummary getSummary() {
        int activeProducts = 0;
        int soldProducts = 0;

        for (Product product : products.values()) {
            if (product.getStatus() == ProductStatus.ACTIVE) {
                activeProducts++;
            } else if (product.getStatus() == ProductStatus.SOLD) {
                soldProducts++;
            }
        }

        return new SupplyChainSummary(products.size(), activeProducts, soldProducts,
                authorizedManufacturers.size(), ledger.getChainLength(),
                ledger.getPendingCount(), ledger.isValid());
    }

    /**
     * Build the metadata map attached to sale transactions.
     */
    private Map<String, Object> createSaleMetadata(Double salePrice, Map<String, Object> metadata) {
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        if (salePrice != null) {
            metadataMap.put("sale_price", salePrice);
        }
        metadataMap.put("sale_date", Instant.now().toString());
        if (metadata != null) {
            metadataMap.put("custom_metadata", new LinkedHashMap<>(metadata));
        }
        return metadataMap;
    }

    /**
     * Generate an O(log n) cryptographic Merkle proof for a transaction.
     *
     * @param transactionHash hash of the transaction to prove
     * @return typed Merkle proof or null
     */
    public TransactionMerkleProof getTransactionMerkleProof(String transactionHash) {
        return ledger.generateProofForTransaction(transactionHash);
    }

    /**
     * Find the most recent transaction hash for a product and generate its Merkle proof.
     *
     * @param productId product identifier
     * @return proof or null
     */
    public TransactionMerkleProof getLatestProductMerkleProof(String productId) {
        List<Block> chain = ledger.getChain();
        for (int b = chain.size() - 1; b >= 0; b--) {
            Block block = chain.get(b);
            List<Transaction> txs = block.getTransactions();
            for (int t = txs.size() - 1; t >= 0; t--) {
                Transaction tx = txs.get(t);
                if (productId.equals(tx.getProductId())) {
                    String txHash = TransactionSerializer.hashOf(tx, hasher);
                    return ledger.generateProofForTransaction(txHash);
                }
            }
        }
        return null;
    }

    /**
     * LIGHTWEIGHT CLIENT VERIFICATION (SPV Mode)
     * ==========================================
     *
     * Verifies that a transaction is authentic and committed to a block
     * using ONLY the transaction hash, the Merkle proof siblings, and the
     * block's Merkle root.
     *
     * COMPLEXITY: O(log n) time, O(1) space.
     * Leaves the client free from having to download or store the full blockchain!
     *
     * @param proof the cryptographic Merkle proof
     * @return true if the proof reconstructs the block's Merkle root
     */
    public boolean verifyLightweightProof(TransactionMerkleProof proof) {
        if (proof == null || proof.getTransactionHash() == null || proof.getBlockMerkleRoot() == null) {
            return false;
        }
        MerkleTree verifier = new MerkleTree(Collections.emptyList(), hasher);
        return verifier.verifyProof(proof.getTransactionHash(), proof.getProofElements(), proof.getBlockMerkleRoot());
    }

    /**
     * Simulate tampering for presentation demonstrations.
     */
    public void simulateTampering(int blockIndex, int transactionIndex, String forgedLocation) {
        ledger.simulateTampering(blockIndex, transactionIndex, forgedLocation);
    }

    /**
     * Restore the blockchain after a tampering demonstration.
     */
    public boolean restoreChain() {
        return ledger.restoreChain();
    }

    public Blockchain getLedger() {
        return ledger;
    }

    public Map<String, Product> getProducts() {
        return Collections.unmodifiableMap(products);
    }

    public Set<String> getAuthorizedManufacturers() {
        return Collections.unmodifiableSet(authorizedManufacturers);
    }
}
