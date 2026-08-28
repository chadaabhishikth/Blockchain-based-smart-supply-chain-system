package com.supplychain.core;

import java.time.Instant;
import java.util.*;

/**
 * Phase 3: Blockchain Ledger Implementation
 * ==========================================
 * This class implements the blockchain data structure that chains blocks
 * together using cryptographic hashes, creating an immutable ledger.
 *
 * STRUCTURE:
 * Each Block contains:
 * - Index: Position in the chain
 * - Timestamp: When the block was created
 * - Transactions: List of supply chain transactions
 * - Merkle Root: Hash summarizing all transactions in this block
 * - Previous Hash: Hash of the immediately preceding block
 * - Nonce: Proof of work counter (optional, for mining)
 * - Hash: SHA-256 hash of this entire block
 *
 * The chain is immutable because modifying any block would:
 * 1. Change its hash
 * 2. Break the "Previous Hash" link to the next block
 * 3. Invalidate the entire subsequent chain
 */
public class Blockchain {

    /**
     * A single block in the blockchain.
     *
     * Contains all supply chain transactions for a time period and
     * cryptographic links to the previous and next blocks.
     */
    public static class Block {
        public int index;
        public String timestamp;
        public List<Map<String, Object>> transactions;
        public String merkleRoot;
        public String previousHash;
        public int nonce;
        public String hash;

        public Block(int index, String timestamp, List<Map<String, Object>> transactions,
                     String merkleRoot, String previousHash, int nonce) {
            this.index = index;
            this.timestamp = timestamp;
            this.transactions = transactions;
            this.merkleRoot = merkleRoot;
            this.previousHash = previousHash;
            this.nonce = nonce;
            this.hash = calculateHash();
        }

        /**
         * Calculate SHA-256 hash of this block.
         *
         * Hash includes all critical data to ensure tamper evidence.
         * Any change to block contents will produce different hash.
         */
        public String calculateHash() {
            String blockData = index +
                    timestamp +
                    merkleRoot +
                    previousHash +
                    nonce;
            return HashUtils.calculateSHA256(blockData);
        }

        /**
         * Serialize block to map.
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("index", index);
            map.put("timestamp", timestamp);
            map.put("transactions", transactions);
            map.put("merkleRoot", merkleRoot);
            map.put("previousHash", previousHash);
            map.put("nonce", nonce);
            map.put("hash", hash);
            return map;
        }

        @Override
        public String toString() {
            return String.format("Block{#%d, txs=%d, hash=%s...}",
                    index, transactions.size(),
                    hash.length() > 16 ? hash.substring(0, 16) : hash);
        }
    }

    private static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);  // Hardcoded start for genesis block

    private List<Block> chain;
    private List<Map<String, Object>> pendingTransactions;

    /**
     * Initialize empty blockchain.
     */
    public Blockchain() {
        this.chain = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        createGenesisBlock();
    }

    /**
     * Create the first block in the chain (Genesis Block).
     *
     * The genesis block has:
     * - Index 0
     * - Previous hash of all zeros (conventional starting point)
     * - Minimal transaction (system initialization data)
     *
     * @return The genesis block that was added to the chain
     */
    public Block createGenesisBlock() {
        Map<String, Object> genesisTx = HashUtils.createTransaction(
                "GENESIS",
                "SYSTEM",
                "NETWORK",
                "Block 0",
                createMetadata("genesis_block", "Supply Chain Network Genesis")
        );

        // Hash the transaction for Merkle tree
        List<String> txHashes = new ArrayList<>();
        txHashes.add(HashUtils.hashTransaction(genesisTx));

        MerkleTree merkle = new MerkleTree(txHashes);

        Block genesisBlock = new Block(
                0,
                Instant.now().toString(),
                new ArrayList<>(Collections.singletonList(genesisTx)),
                merkle.getMerkleRoot(),
                GENESIS_PREVIOUS_HASH,
                0
        );

        chain.add(genesisBlock);
        return genesisBlock;
    }

    /**
     * Create metadata map with type and description.
     */
    private Map<String, Object> createMetadata(String type, String description) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", type);
        metadata.put("description", description);
        return metadata;
    }

    /**
     * Get the most recent block in the chain.
     */
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Add a new block to the chain containing the given transactions.
     *
     * This is the core operation of the blockchain:
     * 1. Create Merkle tree from transactions
     * 2. Link to previous block
     * 3. Calculate cryptographic hash
     * 4. Add to chain
     *
     * @param transactions List of transaction maps to include
     * @return The newly created and added block
     */
    public Block addBlock(List<Map<String, Object>> transactions) {
        if (transactions == null) {
            transactions = new ArrayList<>(pendingTransactions);
            pendingTransactions.clear();
        }

        // Hash transactions for Merkle tree
        List<String> txHashes = new ArrayList<>();
        for (Map<String, Object> tx : transactions) {
            txHashes.add(HashUtils.hashTransaction(tx));
        }

        MerkleTree merkleTree = new MerkleTree(txHashes);
        String merkleRoot = merkleTree.getMerkleRoot();

        // Create new block linked to previous
        Block newBlock = new Block(
                chain.size(),
                Instant.now().toString(),
                transactions,
                merkleRoot,
                getLatestBlock().hash,
                0
        );

        chain.add(newBlock);
        return newBlock;
    }

    /**
     * Add a transaction to the pending pool.
     *
     * Pending transactions are waiting to be mined (added to a block).
     *
     * @param transaction Transaction map to add to pending pool
     */
    public void addPendingTransaction(Map<String, Object> transaction) {
        pendingTransactions.add(transaction);
    }

    /**
     * Mine all pending transactions into a new block.
     *
     * @return The newly created block with pending transactions
     */
    public Block minePendingTransactions() {
        return addBlock(null);
    }

    /**
     * Validate the entire blockchain integrity.
     *
     * Checks performed:
     * 1. Each block's hash is correctly calculated
     * 2. Each block's "previous hash" matches the actual previous block's hash
     * 3. Merkle root matches the transactions in each block
     *
     * @return true if chain is valid and tamper-free, false if compromised
     *
     * COMPLEXITY: O(n) where n is number of blocks
     */
    public boolean isValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Check 1: Hash integrity
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("✗ Block " + i + " hash mismatch!");
                return false;
            }

            // Check 2: Chain integrity (previous hash link)
            if (!currentBlock.previousHash.equals(previousBlock.hash)) {
                System.out.println("✗ Block " + i + " previous hash doesn't match block " + (i-1) + "!");
                return false;
            }

            // Check 3: Merkle root validity
            List<String> txHashes = new ArrayList<>();
            for (Map<String, Object> tx : currentBlock.transactions) {
                txHashes.add(HashUtils.hashTransaction(tx));
            }
            MerkleTree merkle = new MerkleTree(txHashes);
            if (!merkle.getMerkleRoot().equals(currentBlock.merkleRoot)) {
                System.out.println("✗ Block " + i + " Merkle root invalid!");
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieve a specific block by index.
     *
     * @param index Block number to retrieve
     * @return Block if found, null if index out of range
     */
    public Block getBlock(int index) {
        if (index >= 0 && index < chain.size()) {
            return chain.get(index);
        }
        return null;
    }

    /**
     * Trace the complete journey of a product through the supply chain.
     *
     * Searches all blocks and transactions to find every occurrence
     * of the specified productId, returning them in chronological order.
     *
     * @param productId The product identifier to trace
     * @return List of transaction info maps involving this product
     */
    public List<Map<String, Object>> getProductHistory(String productId) {
        List<Map<String, Object>> history = new ArrayList<>();

        for (Block block : chain) {
            for (Map<String, Object> tx : block.transactions) {
                if (productId.equals(tx.get("product_id"))) {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("block_index", block.index);
                    record.put("block_hash", block.hash.substring(0, 16) + "...");
                    record.put("timestamp", tx.get("timestamp"));
                    record.put("sender", tx.get("sender"));
                    record.put("receiver", tx.get("receiver"));
                    record.put("location", tx.get("location"));
                    record.put("verified", true);  // Proven by blockchain
                    history.add(record);
                }
            }
        }

        return history;
    }

    /**
     * Verify product authenticity by checking its complete history.
     *
     * This is the end-to-end verification that consumers use when
     * scanning a product QR code.
     *
     * @param productId Product to verify
     * @return Map with verification results
     */
    public Map<String, Object> verifyProduct(String productId) {
        List<Map<String, Object>> history = getProductHistory(productId);

        if (history.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", false);
            result.put("reason", "Product not found in blockchain");
            result.put("authentic", false);
            return result;
        }

        // Check if product originated from valid genesis-linked manufacturer
        Map<String, Object> origin = history.get(0);
        if ("SYSTEM".equals(origin.get("sender"))) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", false);
            result.put("reason", "Invalid origin (genesis block only)");
            result.put("authentic", false);
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verified", true);
        result.put("authentic", true);
        result.put("origin", origin);
        result.put("current_location", history.get(history.size() - 1).get("location"));
        result.put("current_owner", history.get(history.size() - 1).get("receiver"));
        result.put("journey_length", history.size());
        result.put("journey", history);

        return result;
    }

    /**
     * Get the total number of blocks in the chain.
     */
    public int getChainLength() {
        return chain.size();
    }

    /**
     * Get pending transaction count.
     */
    public int getPendingCount() {
        return pendingTransactions.size();
    }

    // =========================================================================
    // UNIT TESTS FOR BLOCKCHAIN
    // =========================================================================

    public static void testGenesisBlock() {
        Blockchain chain = new Blockchain();

        assert chain.getChainLength() == 1 : "Should start with genesis block";
        assert chain.getBlock(0).index == 0 : "Genesis block index should be 0";
        assert chain.getBlock(0).previousHash.equals(GENESIS_PREVIOUS_HASH) : "Genesis previous hash should be zeros";
        assert !chain.getBlock(0).hash.isEmpty() : "Genesis block should have a hash";

        System.out.println("✓ Genesis block created correctly");
    }

    public static void testBlockChaining() {
        Blockchain chain = new Blockchain();

        Map<String, Object> tx1 = HashUtils.createTransaction("PROD-001", "Factory", "Distributor", "Shanghai", null);
        chain.addBlock(Collections.singletonList(tx1));

        Map<String, Object> tx2 = HashUtils.createTransaction("PROD-001", "Distributor", "Retailer", "Beijing", null);
        chain.addBlock(Collections.singletonList(tx2));

        assert chain.getChainLength() == 3 : "Should have 3 blocks (genesis + 2)";
        assert chain.getBlock(2).previousHash.equals(chain.getBlock(1).hash) : "Chain link broken";

        System.out.println("✓ Blocks properly chained");
    }

    public static void testChainValidation() {
        Blockchain chain = new Blockchain();

        // Add some blocks
        for (int i = 0; i < 5; i++) {
            Map<String, Object> tx = HashUtils.createTransaction(
                    String.format("PROD-%03d", i),
                    String.format("Factory %d", i),
                    String.format("Dist %d", i),
                    String.format("Loc %d", i),
                    null
            );
            chain.addBlock(Collections.singletonList(tx));
        }

        assert chain.isValid() : "Valid chain should pass validation";

        // Tamper with a block
        List<Map<String, Object>> tamperedTxs = new ArrayList<>(chain.getBlock(2).transactions);
        Map<String, Object> tamperedTx = new LinkedHashMap<>(tamperedTxs.get(0));
        tamperedTx.put("location", "Tampered Location");
        tamperedTxs.set(0, tamperedTx);

        // Recreate block with tampered data (simulating tampering)
        Block tamperedBlock = new Block(
                2,
                chain.getBlock(2).timestamp,
                tamperedTxs,
                chain.getBlock(2).merkleRoot,
                chain.getBlock(2).previousHash,
                chain.getBlock(2).nonce
        );
        chain.chain.set(2, tamperedBlock);

        assert !chain.isValid() : "Tampered chain should fail validation";

        System.out.println("✓ Chain validation detects tampering");
    }

    public static void testProductHistory() {
        Blockchain chain = new Blockchain();

        // Simulate product journey
        List<String[]> journey = Arrays.asList(
                new String[]{"Factory-A", "Distributor-B", "Shanghai"},
                new String[]{"Distributor-B", "Wholesaler-C", "Beijing"},
                new String[]{"Wholesaler-C", "Retailer-D", "Shanghai"},
                new String[]{"Retailer-D", "Consumer", "Customer Home"}
        );

        for (String[] step : journey) {
            Map<String, Object> tx = HashUtils.createTransaction(
                    "AUTHENTIC-001",
                    step[0],
                    step[1],
                    step[2],
                    null
            );
            chain.addBlock(Collections.singletonList(tx));
        }

        List<Map<String, Object>> history = chain.getProductHistory("AUTHENTIC-001");

        assert history.size() == 4 : "Should have 4 transactions, got " + history.size();
        assert "Factory-A".equals(history.get(0).get("sender")) : "Origin should be factory";
        assert "Consumer".equals(history.get(history.size() - 1).get("receiver")) : "Final destination should be consumer";

        System.out.println("✓ Product journey tracked: " + history.size() + " steps");
        for (Map<String, Object> step : history) {
            System.out.println("  " + step.get("sender") + " → " + step.get("receiver") + " (" + step.get("location") + ")");
        }
    }

    public static void testVerifyProduct() {
        Blockchain chain = new Blockchain();

        // Create authentic product
        Map<String, Object> tx1 = HashUtils.createTransaction("PROD-REAL", "Factory", "Dist", "Factory Location", null);
        chain.addBlock(Collections.singletonList(tx1));

        Map<String, Object> tx2 = HashUtils.createTransaction("PROD-REAL", "Dist", "Retailer", "Distribution Center", null);
        chain.addBlock(Collections.singletonList(tx2));

        // Verify authentic product
        Map<String, Object> result = chain.verifyProduct("PROD-REAL");
        assert (Boolean) result.get("verified") : "Real product should verify";
        assert (Boolean) result.get("authentic") : "Real product should be authentic";

        // Try to verify non-existent product
        result = chain.verifyProduct("PROD-FAKE");
        assert !(Boolean) result.get("verified") : "Fake product should not verify";
        assert !(Boolean) result.get("authentic") : "Fake product should not be authentic";

        System.out.println("✓ Product verification working correctly");
    }

    public static void testMerkleRootInBlock() {
        Blockchain chain = new Blockchain();

        // Add block with multiple transactions
        List<Map<String, Object>> transactions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            transactions.add(HashUtils.createTransaction(
                    String.format("PROD-%03d", i),
                    String.format("Sender %d", i),
                    String.format("Receiver %d", i),
                    String.format("Loc %d", i),
                    null
            ));
        }

        Block block = chain.addBlock(transactions);

        // Verify Merkle root matches (need to hash transactions)
        List<String> txHashes = new ArrayList<>();
        for (Map<String, Object> tx : transactions) {
            txHashes.add(HashUtils.hashTransaction(tx));
        }
        MerkleTree merkle = new MerkleTree(txHashes);

        assert block.merkleRoot.equals(merkle.getMerkleRoot()) : "Merkle root mismatch";

        System.out.println("✓ Merkle root correctly stored in block");
    }

    /**
     * Run all Blockchain tests.
     */
    public static void runAllTests() {
        System.out.println("============================================================");
        System.out.println("PHASE 3: BLOCKCHAIN LEDGER UNIT TESTS");
        System.out.println("============================================================\n");

        testGenesisBlock();
        System.out.println();
        testBlockChaining();
        System.out.println();
        testChainValidation();
        System.out.println();
        testProductHistory();
        System.out.println();
        testVerifyProduct();
        System.out.println();
        testMerkleRootInBlock();

        System.out.println("\n============================================================");
        System.out.println("ALL TESTS PASSED ✓");
        System.out.println("============================================================\n");
    }

    public static void main(String[] args) {
        runAllTests();
    }
}
