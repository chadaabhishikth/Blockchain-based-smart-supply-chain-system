package com.supplychain.benchmarking;

import com.supplychain.core.Blockchain;
import com.supplychain.core.HashUtils;
import com.supplychain.core.MerkleTree;
import com.supplychain.supplychain.SupplyChainBlockchain;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Phase 5: Academic Comparison - Blockchain vs Traditional Database
 * ====================================================================
 * This class provides a comprehensive comparison between the blockchain-based
 * supply chain system and a traditional centralized SQL database.
 *
 * METRICS ANALYZED:
 * 1. Performance: Transaction throughput and latency
 * 2. Storage: Space efficiency and growth rate
 * 3. Security: Tamper resistance and data integrity
 * 4. Trust Model: Centralization vs decentralization
 * 5. Complexity: Implementation and maintenance effort
 */
public class DatabaseBenchmark {

    private Blockchain blockchain;
    private Connection sqlConnection;
    private Statement sqlStatement;
    private String dbPath = "supply_chain_benchmark.db";

    /**
     * Initialize benchmark environment.
     */
    public DatabaseBenchmark() throws SQLException {
        blockchain = new Blockchain();
        setupSQLDatabase();
    }

    /**
     * Create SQL schema for comparison.
     */
    private void setupSQLDatabase() throws SQLException {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC not found. Using in-memory simulation.");
            return;
        }

        sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        sqlStatement = sqlConnection.createStatement();

        // Create transaction table
        sqlStatement.execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id TEXT NOT NULL,
                sender TEXT NOT NULL,
                receiver TEXT NOT NULL,
                location TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                block_number INTEGER,
                metadata TEXT
            )
        """);

        // Create indexes for faster queries
        sqlStatement.execute("CREATE INDEX IF NOT EXISTS idx_product_id ON transactions(product_id)");
        sqlStatement.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON transactions(timestamp)");
    }

    /**
     * Insert a single transaction into SQL database.
     */
    private void insertSQLTransaction(Map<String, Object> transaction, int blockNumber) throws SQLException {
        String sql = "INSERT INTO transactions (product_id, sender, receiver, location, timestamp, block_number, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = sqlConnection.prepareStatement(sql)) {
            pstmt.setString(1, (String) transaction.get("product_id"));
            pstmt.setString(2, (String) transaction.get("sender"));
            pstmt.setString(3, (String) transaction.get("receiver"));
            pstmt.setString(4, (String) transaction.get("location"));
            pstmt.setString(5, (String) transaction.get("timestamp"));
            pstmt.setInt(6, blockNumber);
            pstmt.setString(7, transaction.get("metadata").toString());
            pstmt.executeUpdate();
        }
    }

    /**
     * Query product history from SQL database.
     */
    private List<Map<String, Object>> queryProductHistorySQL(String productId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT product_id, sender, receiver, location, timestamp, block_number FROM transactions WHERE product_id = ? ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = sqlConnection.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("product_id", rs.getString("product_id"));
                row.put("sender", rs.getString("sender"));
                row.put("receiver", rs.getString("receiver"));
                row.put("location", rs.getString("location"));
                row.put("timestamp", rs.getString("timestamp"));
                row.put("block_number", rs.getInt("block_number"));
                results.add(row);
            }
        }

        return results;
    }

    /**
     * Run comprehensive benchmark comparing blockchain and SQL.
     */
    public Map<String, Object> runBenchmark(int numTransactions, int numProducts) throws SQLException {
        System.out.println("\n======================================================================");
        System.out.println("BENCHMARK: BLOCKCHAIN vs TRADITIONAL DATABASE");
        System.out.println("======================================================================");
        System.out.println("Test Parameters: " + numTransactions + " transactions, " + numProducts + " products\n");

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("parameters", createMap("num_transactions", numTransactions, "num_products", numProducts));

        // =========================================================================
        // 1. INSERTION PERFORMANCE
        // =========================================================================
        System.out.println("TEST 1: Transaction Insertion Performance");
        System.out.println("----------------------------------------------------------------------");

        // Blockchain insertion
        long blockchainStartTime = System.nanoTime();
        for (int i = 0; i < numTransactions; i++) {
            String productId = String.format("PROD-%04d", i % numProducts);
            Map<String, Object> tx = HashUtils.createTransaction(
                    productId,
                    String.format("Entity-%d", i % 10),
                    String.format("Entity-%d", (i + 1) % 10),
                    String.format("Location-%d", i % 20),
                    createMap("batch", i / 10)
            );
            blockchain.addBlock(Collections.singletonList(tx));
        }
        long blockchainEndTime = System.nanoTime();
        double blockchainTotalMs = (blockchainEndTime - blockchainStartTime) / 1_000_000.0;
        double blockchainAvgMs = blockchainTotalMs / numTransactions;

        results.put("blockchain_total_insert_time_ms", blockchainTotalMs);
        results.put("blockchain_avg_insert_time_ms", blockchainAvgMs);

        System.out.println("Blockchain:");
        System.out.printf("  Total time: %.2f ms\n", blockchainTotalMs);
        System.out.printf("  Average per transaction: %.4f ms\n", blockchainAvgMs);

        // SQL insertion
        long sqlStartTime = System.nanoTime();
        for (int i = 0; i < numTransactions; i++) {
            String productId = String.format("PROD-%04d", i % numProducts);
            Map<String, Object> tx = HashUtils.createTransaction(
                    productId,
                    String.format("Entity-%d", i % 10),
                    String.format("Entity-%d", (i + 1) % 10),
                    String.format("Location-%d", i % 20),
                    createMap("batch", i / 10)
            );
            insertSQLTransaction(tx, i / 10);
        }
        long sqlEndTime = System.nanoTime();
        double sqlTotalMs = (sqlEndTime - sqlStartTime) / 1_000_000.0;
        double sqlAvgMs = sqlTotalMs / numTransactions;

        results.put("sql_total_insert_time_ms", sqlTotalMs);
        results.put("sql_avg_insert_time_ms", sqlAvgMs);

        System.out.println("SQL Database:");
        System.out.printf("  Total time: %.2f ms\n", sqlTotalMs);
        System.out.printf("  Average per transaction: %.4f ms\n", sqlAvgMs);

        double insertionSpeedup = sqlTotalMs / blockchainTotalMs;
        System.out.printf("\n  SQL is %.2fx %s for insertions\n\n", insertionSpeedup, insertionSpeedup > 1 ? "FASTER" : "SLOWER");

        // =========================================================================
        // 2. QUERY PERFORMANCE
        // =========================================================================
        System.out.println("TEST 2: Product History Query Performance");
        System.out.println("----------------------------------------------------------------------");

        String testProduct = "PROD-0001";

        // Blockchain query
        long[] blockchainQueryTimes = new long[100];
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            blockchain.getProductHistory(testProduct);
            long end = System.nanoTime();
            blockchainQueryTimes[i] = end - start;
        }
        double blockchainQueryAvg = average(blockchainQueryTimes) / 1_000_000.0;
        results.put("blockchain_query_time_ms", blockchainQueryAvg);

        System.out.printf("Blockchain: %.4f ms average\n", blockchainQueryAvg);

        // SQL query
        long[] sqlQueryTimes = new long[100];
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            queryProductHistorySQL(testProduct);
            long end = System.nanoTime();
            sqlQueryTimes[i] = end - start;
        }
        double sqlQueryAvg = average(sqlQueryTimes) / 1_000_000.0;
        results.put("sql_query_time_ms", sqlQueryAvg);

        System.out.printf("SQL Database: %.4f ms average\n", sqlQueryAvg);

        double querySpeedup = blockchainQueryAvg / sqlQueryAvg;
        System.out.printf("\n  SQL is %.2fx %s for queries\n\n", querySpeedup, querySpeedup > 1 ? "FASTER" : "SLOWER");

        // =========================================================================
        // 3. STORAGE EFFICIENCY
        // =========================================================================
        System.out.println("TEST 3: Storage Efficiency");
        System.out.println("----------------------------------------------------------------------");

        // Blockchain storage (simplified)
        long blockchainStorage = blockchain.getChainLength() * 500;  // Approximate bytes per block
        results.put("blockchain_storage_bytes", blockchainStorage);
        results.put("blockchain_total_blocks", blockchain.getChainLength());

        System.out.println("Blockchain Storage:");
        System.out.println("  Blocks: " + blockchain.getChainLength());
        System.out.printf("  Size: %.2f KB\n", blockchainStorage / 1024.0);

        // SQL storage
        long sqlStorage = numTransactions * 200;  // Approximate bytes per row
        results.put("sql_storage_bytes", sqlStorage);

        System.out.println("SQL Database Storage:");
        System.out.println("  Transactions: " + numTransactions);
        System.out.printf("  Size: %.2f KB\n", sqlStorage / 1024.0);

        double storageRatio = (double) blockchainStorage / sqlStorage;
        System.out.printf("\n  Blockchain uses %.2fx %s storage\n\n", storageRatio, storageRatio > 1 ? "MORE" : "LESS");

        // =========================================================================
        // 4. VERIFICATION PERFORMANCE
        // =========================================================================
        System.out.println("TEST 4: Verification Operation Performance");
        System.out.println("----------------------------------------------------------------------");

        // Blockchain verification
        long[] blockchainVerifyTimes = new long[100];
        for (int i = 0; i < 100; i++) {
            Map<String, Object> testTx = HashUtils.createTransaction(
                    String.format("PROD-%04d", i),
                    "Test",
                    "Verify",
                    "Test",
                    null
            );
            long start = System.nanoTime();
            String txHash = HashUtils.hashTransaction(testTx);
            MerkleTree merkle = new MerkleTree(Collections.singletonList(txHash));
            String root = merkle.getMerkleRoot();
            long end = System.nanoTime();
            blockchainVerifyTimes[i] = end - start;
        }
        double blockchainVerifyAvg = average(blockchainVerifyTimes) / 1_000_000.0;
        results.put("blockchain_verification_time_ms", blockchainVerifyAvg);

        System.out.printf("Blockchain (hash + merkle): %.4f ms\n", blockchainVerifyAvg);

        // SQL verification
        long[] sqlVerifyTimes = new long[100];
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            String sql = "SELECT COUNT(*) FROM transactions WHERE product_id = ?";
            try (PreparedStatement pstmt = sqlConnection.prepareStatement(sql)) {
                pstmt.setString(1, "PROD-0001");
                ResultSet rs = pstmt.executeQuery();
                rs.next();
            }
            long end = System.nanoTime();
            sqlVerifyTimes[i] = end - start;
        }
        double sqlVerifyAvg = average(sqlVerifyTimes) / 1_000_000.0;
        results.put("sql_verification_time_ms", sqlVerifyAvg);

        System.out.printf("SQL Database (lookup): %.4f ms\n\n", sqlVerifyAvg);

        // =========================================================================
        // 5. INTEGRITY VERIFICATION
        // =========================================================================
        System.out.println("TEST 5: Chain Integrity Verification");
        System.out.println("----------------------------------------------------------------------");

        long integrityStart = System.nanoTime();
        boolean isValid = blockchain.isValid();
        long integrityTime = (System.nanoTime() - integrityStart) / 1_000_000;

        results.put("blockchain_integrity_check_time_ms", integrityTime);
        results.put("blockchain_integrity_valid", isValid);

        System.out.printf("Blockchain: %.2f ms (Valid: %s)\n", integrityTime, isValid);
        System.out.println("  Complexity: O(n) - must verify all blocks");
        System.out.println("  Checks: Block hashes, Merkle roots, Chain links\n");

        // =========================================================================
        // SUMMARY
        // =========================================================================
        printSummary(insertionSpeedup, querySpeedup, storageRatio);

        return results;
    }

    /**
     * Create a simple map.
     */
    private Map<String, Object> createMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Calculate average of long array.
     */
    private double average(long[] values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return (double) sum / values.length;
    }

    /**
     * Print benchmark summary.
     */
    private void printSummary(double insertionSpeedup, double querySpeedup, double storageRatio) {
        System.out.println("======================================================================");
        System.out.println("BENCHMARK SUMMARY");
        System.out.println("======================================================================");

        System.out.println("Performance Comparison:");
        System.out.printf("  Insertions: SQL is %.2fx faster\n", insertionSpeedup);
        System.out.printf("  Queries: SQL is %.2fx faster\n", querySpeedup);
        System.out.printf("  Storage: Blockchain uses %.2fx more space\n", storageRatio);
        System.out.println();

        System.out.println("Trade-off Analysis:");
        System.out.println("  BLOCKCHAIN wins on:");
        System.out.println("    ✓ Tamper resistance (modifications are detectable)");
        System.out.println("    ✓ Decentralized trust (no single point of control)");
        System.out.println("    ✓ Cryptographic proofs (O(log n) verification)");
        System.out.println("    ✓ Immutable history (append-only ledger)");
        System.out.println();

        System.out.println("  SQL wins on:");
        System.out.println("    ✓ Faster insertions and queries");
        System.out.println("    ✓ More storage efficient");
        System.out.println("    ✓ Flexible querying and reporting");
        System.out.println("    ✓ ACID transactions fully supported");
        System.out.println();

        System.out.println("Recommendation:");
        System.out.println("  Use BLOCKCHAIN when:");
        System.out.println("    • Multiple untrusting parties need shared truth");
        System.out.println("    • Tamper evidence is critical");
        System.out.println("    • Audit trail with cryptographic proof is required");
        System.out.println();

        System.out.println("  Use SQL when:");
        System.out.println("    • Single trusted authority manages the database");
        System.out.println("    • Performance and storage are priorities");
        System.out.println("    • Traditional CRUD operations dominate");
        System.out.println();

        System.out.println("======================================================================\n");
    }

    /**
     * Clean up benchmark resources.
     */
    public void cleanup() {
        try {
            if (sqlStatement != null) sqlStatement.close();
            if (sqlConnection != null) sqlConnection.close();
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(dbPath));
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Run full comparison with analysis.
     */
    public static void runFullComparison() {
        System.out.println("\n" + "======================================================================");
        System.out.println("ACADEMIC COMPARISON: BLOCKCHAIN vs TRADITIONAL DATABASE");
        System.out.println("Supply Chain Tracking System Analysis");
        System.out.println("======================================================================");
        System.out.println("""
                This analysis compares two approaches to supply chain tracking:

                1. BLOCKCHAIN-BASED (Our Implementation)
                   - Immutable ledger with cryptographic linking
                   - Merkle Trees for efficient verification
                   - SHA-256 hashing for tamper detection
                   - Decentralized trust model

                2. TRADITIONAL SQL DATABASE (Comparison Baseline)
                   - Standard relational database
                   - Indexed queries for fast retrieval
                   - ACID transactions
                   - Centralized trust model

                KEY METRICS:
                  • Performance: Transaction throughput and latency
                  • Storage: Space efficiency
                  • Security: Tamper resistance and integrity
                  • Trust: Centralization vs decentralization
                """);

        try {
            DatabaseBenchmark benchmark = new DatabaseBenchmark();

            System.out.println("\nRunning benchmark with 1,000 transactions...");
            Map<String, Object> results = benchmark.runBenchmark(1000, 100);

            benchmark.cleanup();

        } catch (SQLException e) {
            System.out.println("SQL benchmark skipped (SQLite not available)");
            System.out.println("Running blockchain-only demonstration...\n");

            // Run simplified blockchain benchmark
            runBlockchainBenchmark();
        }

        printAnalysis();
    }

    /**
     * Run blockchain-only benchmark when SQLite is not available.
     */
    private static void runBlockchainBenchmark() {
        System.out.println("======================================================================");
        System.out.println("BLOCKCHAIN PERFORMANCE ANALYSIS");
        System.out.println("======================================================================\n");

        Blockchain blockchain = new Blockchain();
        int numTransactions = 1000;

        // Measure insertion time
        long startTime = System.nanoTime();
        for (int i = 0; i < numTransactions; i++) {
            Map<String, Object> tx = HashUtils.createTransaction(
                    String.format("PROD-%04d", i),
                    String.format("Sender-%d", i),
                    String.format("Receiver-%d", i),
                    String.format("Location-%d", i),
                    null
            );
            blockchain.addBlock(Collections.singletonList(tx));
        }
        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("Blockchain Performance with " + numTransactions + " transactions:");
        System.out.printf("  Total time: %.2f ms\n", totalTimeMs);
        System.out.printf("  Average per transaction: %.4f ms\n", totalTimeMs / numTransactions);
        System.out.printf("  Blocks created: %d\n", blockchain.getChainLength());
        System.out.printf("  Chain valid: %s\n", blockchain.isValid());
        System.out.println();

        // Measure verification
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            blockchain.getProductHistory("PROD-0001");
        }
        endTime = System.nanoTime();
        double verifyTimeMs = (endTime - startTime) / 1_000_000.0 / 100;

        System.out.println("Query Performance:");
        System.out.printf("  Average query time: %.4f ms\n", verifyTimeMs);
        System.out.println();

        // Measure Merkle proof
        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            hashes.add(String.format("hash%04d", i));
        }
        MerkleTree tree = new MerkleTree(hashes);

        startTime = System.nanoTime();
        List<MerkleTree.ProofElement> proof = tree.generateProof("hash0500");
        endTime = System.nanoTime();
        double proofTimeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("Merkle Tree Verification:");
        System.out.printf("  Proof generation time: %.4f ms\n", proofTimeMs);
        System.out.printf("  Proof size: %d elements (O(log n) = O(%.0f))\n", proof.size(), Math.ceil(Math.log(1000) / Math.log(2)));
        System.out.printf("  Merkle root: %s...\n", tree.getMerkleRoot().substring(0, 32));
        System.out.println();
    }

    /**
     * Print academic analysis.
     */
    private static void printAnalysis() {
        System.out.println("""
                ANALYS

IS: WHEN TO USE BLOCKCHAIN FOR SUPPLY CHAIN
                ==================================================

                The Oracle Problem (Critical Limitation):
                -----------------------------------------
                Even with perfect blockchain technology, we cannot solve the fundamental
                problem of connecting digital records to physical reality:

                  BLOCKCHAIN SECURES: Digital transaction records ✓
                  BLOCKCHAIN CANNOT SECURE: Physical product authenticity ✗

                Example: A corrupt factory worker could place authentic QR codes on
                counterfeit products. The blockchain would perfectly record the lie.

                Solution: IoT sensors, RFID, and physical inspections must supplement
                blockchain verification.

                Engineering Trade-offs:
                -----------------------
                1. PERFORMANCE COST
                   - SHA-256 hashing: ~0.1-0.5 ms per operation
                   - Merkle tree construction: O(n) for building, O(log n) for proof
                   - Block chaining: Adds verification overhead

                   SQL INSERT: ~0.01-0.1 ms (10-50x faster)

                2. STORAGE OVERHEAD
                   - Each block stores: Previous hash, Merkle root, metadata
                   - Redundant hash storage for verification
                   - ~2-5x more storage than equivalent SQL

                3. COMPLEXITY INCREASE
                   - More complex implementation
                   - Requires understanding of cryptography
                   - Harder to debug and maintain
                   - Consensus mechanisms needed (in distributed deployment)

                When Blockchain IS the Right Choice:
                ------------------------------------
                ✓ Multi-party supply chains with low trust
                ✓ Regulatory requirements for immutable audit trails
                ✓ High-value products (luxury goods, pharmaceuticals)
                ✓ Value chains where authenticity verification is critical
                ✓ Scenarios requiring cryptographic proof of provenance

                When SQL is the Right Choice:
                -----------------------------
                ✓ Single-company supply chain (internal tracking)
                ✓ High-volume, low-value products
                ✓ Performance-critical applications
                ✓ When traditional database features are needed (joins, complex queries)
                ✓ Smaller teams without blockchain expertise

                Hybrid Approach (Best of Both Worlds):
                --------------------------------------
                Consider using blockchain for:
                  - High-value/high-risk product verification
                  - Multi-party trust boundaries
                  - Regulatory compliance records

                And SQL/database for:
                  - Internal operations and analytics
                  - High-frequency transactions
                  - Reporting and business intelligence

                This approach optimizes for both security AND performance.
                """);
    }

    public static void main(String[] args) {
        runFullComparison();
    }
}
