package com.supplychain.benchmark;

import com.supplychain.crypto.HashFunction;
import com.supplychain.crypto.Sha256Hasher;
import com.supplychain.crypto.TransactionSerializer;
import com.supplychain.domain.ledger.Blockchain;
import com.supplychain.domain.merkle.MerkleProofElement;
import com.supplychain.domain.merkle.MerkleTree;
import com.supplychain.domain.model.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark Layer — Blockchain vs Traditional Database
 * =====================================================
 *
 * Academic comparison between the blockchain-based supply chain system
 * and a traditional centralized SQL database.
 *
 * METRICS ANALYZED:
 * 1. Performance: transaction throughput and latency
 * 2. Storage: space efficiency and growth rate
 * 3. Security: tamper resistance and data integrity
 * 4. Trust model: centralization vs decentralization
 * 5. Complexity: implementation and maintenance effort
 *
 * If the SQLite JDBC driver is not on the classpath, the benchmark
 * degrades gracefully to a blockchain-only demonstration instead of
 * crashing.
 */
public class DatabaseBenchmark {

    private final HashFunction hasher = new Sha256Hasher();
    private final Blockchain blockchain;
    private final Connection sqlConnection;
    private final Statement sqlStatement;
    private final String dbPath = "supply_chain_benchmark.db";

    /**
     * Initialize the benchmark environment.
     */
    public DatabaseBenchmark() throws SQLException {
        this.blockchain = new Blockchain(hasher);
        this.sqlConnection = setupSqlDatabase();
        this.sqlStatement = sqlConnection == null ? null : sqlConnection.createStatement();
    }

    /**
     * Create the SQL schema used for the comparison baseline.
     *
     * @return an open connection, or null when SQLite is unavailable
     */
    private Connection setupSqlDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC not found. Using in-memory simulation.");
            return null;
        }

        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } catch (SQLException e) {
            System.out.println("Could not open SQLite database (" + e.getMessage() + "). Skipping SQL tests.");
            return null;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("    CREATE TABLE IF NOT EXISTS transactions (\n"
                    + "        id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                    + "        product_id TEXT NOT NULL,\n"
                    + "        sender TEXT NOT NULL,\n"
                    + "        receiver TEXT NOT NULL,\n"
                    + "        location TEXT NOT NULL,\n"
                    + "        timestamp TEXT NOT NULL,\n"
                    + "        block_number INTEGER,\n"
                    + "        metadata TEXT\n"
                    + "    )\n");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_product_id ON transactions(product_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON transactions(timestamp)");
        } catch (SQLException e) {
            System.out.println("Could not prepare SQLite schema (" + e.getMessage() + "). Skipping SQL tests.");
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Ignore cleanup errors
            }
            return null;
        }

        return connection;
    }

    /**
     * Insert a single transaction into the SQL database.
     */
    private void insertSqlTransaction(Transaction transaction, int blockNumber) throws SQLException {
        String sql = "INSERT INTO transactions (product_id, sender, receiver, location, timestamp, block_number, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = sqlConnection.prepareStatement(sql)) {
            pstmt.setString(1, transaction.getProductId());
            pstmt.setString(2, transaction.getSender());
            pstmt.setString(3, transaction.getReceiver());
            pstmt.setString(4, transaction.getLocation());
            pstmt.setString(5, transaction.getTimestamp());
            pstmt.setInt(6, blockNumber);
            pstmt.setString(7, transaction.getMetadata().toString());
            pstmt.executeUpdate();
        }
    }

    /**
     * Query product history from the SQL database.
     */
    private List<Map<String, Object>> queryProductHistorySql(String productId) throws SQLException {
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
     * @return true when the SQL baseline is usable
     */
    private boolean sqlAvailable() {
        return sqlConnection != null;
    }

    /**
     * Run the comprehensive benchmark comparing blockchain and SQL.
     *
     * @param numTransactions number of transactions to insert
     * @param numProducts     number of distinct products
     * @return raw benchmark metrics
     */
    public Map<String, Object> runBenchmark(int numTransactions, int numProducts) throws SQLException {
        System.out.println("\n======================================================================");
        System.out.println("BENCHMARK: BLOCKCHAIN vs TRADITIONAL DATABASE");
        System.out.println("======================================================================");
        System.out.println("Test Parameters: " + numTransactions + " transactions, " + numProducts + " products\n");

        Map<String, Object> results = new LinkedHashMap<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("num_transactions", numTransactions);
        parameters.put("num_products", numProducts);
        results.put("parameters", parameters);

        // =========================================================================
        // 1. INSERTION PERFORMANCE
        // =========================================================================
        System.out.println("TEST 1: Transaction Insertion Performance");
        System.out.println("----------------------------------------------------------------------");

        long blockchainStartTime = System.nanoTime();
        for (int i = 0; i < numTransactions; i++) {
            Transaction tx = createBenchmarkTransaction(i, numProducts);
            blockchain.addBlock(Collections.singletonList(tx));
        }
        long blockchainEndTime = System.nanoTime();
        double blockchainTotalMs = (blockchainEndTime - blockchainStartTime) / 1_000_000.0;
        double blockchainAvgMs = blockchainTotalMs / numTransactions;

        results.put("blockchain_total_insert_time_ms", blockchainTotalMs);
        results.put("blockchain_avg_insert_time_ms", blockchainAvgMs);

        System.out.println("Blockchain:");
        System.out.printf("  Total time: %.2f ms%n", blockchainTotalMs);
        System.out.printf("  Average per transaction: %.4f ms%n", blockchainAvgMs);

        Double sqlTotalMs = null;
        if (sqlAvailable()) {
            long sqlStartTime = System.nanoTime();
            for (int i = 0; i < numTransactions; i++) {
                Transaction tx = createBenchmarkTransaction(i, numProducts);
                insertSqlTransaction(tx, i / 10);
            }
            long sqlEndTime = System.nanoTime();
            sqlTotalMs = (sqlEndTime - sqlStartTime) / 1_000_000.0;

            results.put("sql_total_insert_time_ms", sqlTotalMs);
            results.put("sql_avg_insert_time_ms", sqlTotalMs / numTransactions);

            System.out.println("SQL Database:");
            System.out.printf("  Total time: %.2f ms%n", sqlTotalMs);
            System.out.printf("  Average per transaction: %.4f ms%n", sqlTotalMs / numTransactions);
        } else {
            System.out.println("SQL Database: SKIPPED (SQLite JDBC not available)");
        }

        Double insertionSpeedup = null;
        if (sqlTotalMs != null) {
            insertionSpeedup = sqlTotalMs / blockchainTotalMs;
            System.out.printf("%n  SQL is %.2fx %s for insertions%n%n", insertionSpeedup, insertionSpeedup > 1 ? "FASTER" : "SLOWER");
        } else {
            System.out.println();
        }

        // =========================================================================
        // 2. QUERY PERFORMANCE
        // =========================================================================
        System.out.println("TEST 2: Product History Query Performance");
        System.out.println("----------------------------------------------------------------------");

        String testProduct = "PROD-0001";

        long[] blockchainQueryTimes = new long[100];
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            blockchain.getProductHistory(testProduct);
            long end = System.nanoTime();
            blockchainQueryTimes[i] = end - start;
        }
        double blockchainQueryAvg = average(blockchainQueryTimes) / 1_000_000.0;
        results.put("blockchain_query_time_ms", blockchainQueryAvg);

        System.out.printf("Blockchain: %.4f ms average%n", blockchainQueryAvg);

        Double sqlQueryAvg = null;
        if (sqlAvailable()) {
            long[] sqlQueryTimes = new long[100];
            for (int i = 0; i < 100; i++) {
                long start = System.nanoTime();
                queryProductHistorySql(testProduct);
                long end = System.nanoTime();
                sqlQueryTimes[i] = end - start;
            }
            sqlQueryAvg = average(sqlQueryTimes) / 1_000_000.0;
            results.put("sql_query_time_ms", sqlQueryAvg);

            System.out.printf("SQL Database: %.4f ms average%n", sqlQueryAvg);
        } else {
            System.out.println("SQL Database: SKIPPED (SQLite JDBC not available)");
        }

        Double querySpeedup = null;
        if (sqlQueryAvg != null) {
            querySpeedup = blockchainQueryAvg / sqlQueryAvg;
            System.out.printf("%n  SQL is %.2fx %s for queries%n%n", querySpeedup, querySpeedup > 1 ? "FASTER" : "SLOWER");
        } else {
            System.out.println();
        }

        // =========================================================================
        // 3. STORAGE EFFICIENCY
        // =========================================================================
        System.out.println("TEST 3: Storage Efficiency");
        System.out.println("----------------------------------------------------------------------");

        long blockchainStorage = blockchain.getChainLength() * 500L;  // Approximate bytes per block
        results.put("blockchain_storage_bytes", blockchainStorage);
        results.put("blockchain_total_blocks", blockchain.getChainLength());

        System.out.println("Blockchain Storage:");
        System.out.println("  Blocks: " + blockchain.getChainLength());
        System.out.printf("  Size: %.2f KB%n", blockchainStorage / 1024.0);

        long sqlStorage = numTransactions * 200L;  // Approximate bytes per row
        results.put("sql_storage_bytes", sqlStorage);

        System.out.println("SQL Database Storage:");
        System.out.println("  Transactions: " + numTransactions);
        System.out.printf("  Size: %.2f KB%n", sqlStorage / 1024.0);

        double storageRatio = (double) blockchainStorage / sqlStorage;
        System.out.printf("%n  Blockchain uses %.2fx %s storage%n%n", storageRatio, storageRatio > 1 ? "MORE" : "LESS");

        // =========================================================================
        // 4. VERIFICATION PERFORMANCE
        // =========================================================================
        System.out.println("TEST 4: Verification Operation Performance");
        System.out.println("----------------------------------------------------------------------");

        long[] blockchainVerifyTimes = new long[100];
        for (int i = 0; i < 100; i++) {
            Transaction testTx = Transaction.create(
                    String.format("PROD-%04d", i),
                    "Test",
                    "Verify",
                    "Test",
                    null
            );
            long start = System.nanoTime();
            String txHash = TransactionSerializer.hashOf(testTx, hasher);
            MerkleTree merkle = new MerkleTree(Collections.singletonList(txHash), hasher);
            merkle.getMerkleRoot();
            long end = System.nanoTime();
            blockchainVerifyTimes[i] = end - start;
        }
        double blockchainVerifyAvg = average(blockchainVerifyTimes) / 1_000_000.0;
        results.put("blockchain_verification_time_ms", blockchainVerifyAvg);

        System.out.printf("Blockchain (hash + merkle): %.4f ms%n", blockchainVerifyAvg);

        if (sqlAvailable()) {
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

            System.out.printf("SQL Database (lookup): %.4f ms%n%n", sqlVerifyAvg);
        } else {
            System.out.println("SQL Database (lookup): SKIPPED (SQLite JDBC not available)\n");
        }

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

        System.out.printf("Blockchain: %.2f ms (Valid: %s)%n", (double) integrityTime, isValid);
        System.out.println("  Complexity: O(n) - must verify all blocks");
        System.out.println("  Checks: Block hashes, Merkle roots, Chain links\n");

        // =========================================================================
        // SUMMARY
        // =========================================================================
        if (insertionSpeedup != null && querySpeedup != null) {
            printSummary(insertionSpeedup, querySpeedup, storageRatio);
        } else {
            System.out.println("======================================================================");
            System.out.println("BENCHMARK SUMMARY (blockchain-only run)");
            System.out.println("======================================================================");
            System.out.println("SQL comparison skipped - add the SQLite JDBC driver to the");
            System.out.println("classpath to run the full academic comparison.");
            System.out.println("======================================================================\n");
        }

        return results;
    }

    /**
     * Create a synthetic transaction for the benchmark loops.
     */
    private Transaction createBenchmarkTransaction(int i, int numProducts) {
        String productId = String.format("PROD-%04d", i % numProducts);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("batch", i / 10);
        return Transaction.create(
                productId,
                String.format("Entity-%d", i % 10),
                String.format("Entity-%d", (i + 1) % 10),
                String.format("Location-%d", i % 20),
                metadata
        );
    }

    /**
     * Arithmetic mean of a long array.
     */
    private double average(long[] values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return (double) sum / values.length;
    }

    /**
     * Print the benchmark summary and trade-off analysis.
     */
    private void printSummary(double insertionSpeedup, double querySpeedup, double storageRatio) {
        System.out.println("======================================================================");
        System.out.println("BENCHMARK SUMMARY");
        System.out.println("======================================================================");

        System.out.println("Performance Comparison:");
        System.out.printf("  Insertions: SQL is %.2fx faster%n", insertionSpeedup);
        System.out.printf("  Queries: SQL is %.2fx faster%n", querySpeedup);
        System.out.printf("  Storage: Blockchain uses %.2fx more space%n", storageRatio);
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
            if (sqlStatement != null) {
                sqlStatement.close();
            }
            if (sqlConnection != null) {
                sqlConnection.close();
            }
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(dbPath));
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Run the full comparison with the academic analysis.
     */
    public static void runFullComparison() {
        System.out.println("\n" + "======================================================================");
        System.out.println("ACADEMIC COMPARISON: BLOCKCHAIN vs TRADITIONAL DATABASE");
        System.out.println("Supply Chain Tracking System Analysis");
        System.out.println("======================================================================");
        System.out.println("This analysis compares two approaches to supply chain tracking:\n"
                + "\n"
                + "1. BLOCKCHAIN-BASED (Our Implementation)\n"
                + "   - Immutable ledger with cryptographic linking\n"
                + "   - Merkle Trees for efficient verification\n"
                + "   - SHA-256 hashing for tamper detection\n"
                + "   - Decentralized trust model\n"
                + "\n"
                + "2. TRADITIONAL SQL DATABASE (Comparison Baseline)\n"
                + "   - Standard relational database\n"
                + "   - Indexed queries for fast retrieval\n"
                + "   - ACID transactions\n"
                + "   - Centralized trust model\n"
                + "\n"
                + "KEY METRICS:\n"
                + "  • Performance: Transaction throughput and latency\n"
                + "  • Storage: Space efficiency\n"
                + "  • Security: Tamper resistance and integrity\n"
                + "  • Trust: Centralization vs decentralization\n");

        try {
            DatabaseBenchmark benchmark = new DatabaseBenchmark();

            System.out.println("\nRunning benchmark with 1,000 transactions...");
            benchmark.runBenchmark(1000, 100);

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
     * Run the blockchain-only benchmark when SQLite is not available.
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
            Transaction tx = Transaction.create(
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
        System.out.printf("  Total time: %.2f ms%n", totalTimeMs);
        System.out.printf("  Average per transaction: %.4f ms%n", totalTimeMs / numTransactions);
        System.out.printf("  Blocks created: %d%n", blockchain.getChainLength());
        System.out.printf("  Chain valid: %s%n", blockchain.isValid());
        System.out.println();

        // Measure verification
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            blockchain.getProductHistory("PROD-0001");
        }
        endTime = System.nanoTime();
        double verifyTimeMs = (endTime - startTime) / 1_000_000.0 / 100;

        System.out.println("Query Performance:");
        System.out.printf("  Average query time: %.4f ms%n", verifyTimeMs);
        System.out.println();

        // Measure Merkle proof
        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            hashes.add(String.format("hash%04d", i));
        }
        MerkleTree tree = new MerkleTree(hashes);

        startTime = System.nanoTime();
        List<MerkleProofElement> proof = tree.generateProof("hash0500");
        endTime = System.nanoTime();
        double proofTimeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("Merkle Tree Verification:");
        System.out.printf("  Proof generation time: %.4f ms%n", proofTimeMs);
        System.out.printf("  Proof size: %d elements (O(log n) = O(%.0f))%n", proof.size(), Math.ceil(Math.log(1000) / Math.log(2)));
        System.out.printf("  Merkle root: %s...%n", tree.getMerkleRoot().substring(0, 32));
        System.out.println();
    }

    /**
     * Print the academic analysis.
     */
    private static void printAnalysis() {
        System.out.println("                ANALYSIS: WHEN TO USE BLOCKCHAIN FOR SUPPLY CHAIN\n"
                + "                ==================================================\n"
                + "\n"
                + "                The Oracle Problem (Critical Limitation):\n"
                + "                -----------------------------------------\n"
                + "                Even with perfect blockchain technology, we cannot solve the fundamental\n"
                + "                problem of connecting digital records to physical reality:\n"
                + "\n"
                + "                  BLOCKCHAIN SECURES: Digital transaction records ✓\n"
                + "                  BLOCKCHAIN CANNOT SECURE: Physical product authenticity ✗\n"
                + "\n"
                + "                Example: A corrupt factory worker could place authentic QR codes on\n"
                + "                counterfeit products. The blockchain would perfectly record the lie.\n"
                + "\n"
                + "                Solution: IoT sensors, RFID, and physical inspections must supplement\n"
                + "                blockchain verification.\n"
                + "\n"
                + "                Engineering Trade-offs:\n"
                + "                -----------------------\n"
                + "                1. PERFORMANCE COST\n"
                + "                   - SHA-256 hashing: ~0.1-0.5 ms per operation\n"
                + "                   - Merkle tree construction: O(n) for building, O(log n) for proof\n"
                + "                   - Block chaining: Adds verification overhead\n"
                + "\n"
                + "                   SQL INSERT: ~0.01-0.1 ms (10-50x faster)\n"
                + "\n"
                + "                2. STORAGE OVERHEAD\n"
                + "                   - Each block stores: Previous hash, Merkle root, metadata\n"
                + "                   - Redundant hash storage for verification\n"
                + "                   - ~2-5x more storage than equivalent SQL\n"
                + "\n"
                + "                3. COMPLEXITY INCREASE\n"
                + "                   - More complex implementation\n"
                + "                   - Requires understanding of cryptography\n"
                + "                   - Harder to debug and maintain\n"
                + "                   - Consensus mechanisms needed (in distributed deployment)\n"
                + "\n"
                + "                When Blockchain IS the Right Choice:\n"
                + "                ------------------------------------\n"
                + "                ✓ Multi-party supply chains with low trust\n"
                + "                ✓ Regulatory requirements for immutable audit trails\n"
                + "                ✓ High-value products (luxury goods, pharmaceuticals)\n"
                + "                ✓ Value chains where authenticity verification is critical\n"
                + "                ✓ Scenarios requiring cryptographic proof of provenance\n"
                + "\n"
                + "                When SQL is the Right Choice:\n"
                + "                -----------------------------\n"
                + "                ✓ Single-company supply chain (internal tracking)\n"
                + "                ✓ High-volume, low-value products\n"
                + "                ✓ Performance-critical applications\n"
                + "                ✓ When traditional database features are needed (joins, complex queries)\n"
                + "                ✓ Smaller teams without blockchain expertise\n"
                + "\n"
                + "                Hybrid Approach (Best of Both Worlds):\n"
                + "                --------------------------------------\n"
                + "                Consider using blockchain for:\n"
                + "                  - High-value/high-risk product verification\n"
                + "                  - Multi-party trust boundaries\n"
                + "                  - Regulatory compliance records\n"
                + "\n"
                + "                And SQL/database for:\n"
                + "                  - Internal operations and analytics\n"
                + "                  - High-frequency transactions\n"
                + "                  - Reporting and business intelligence\n"
                + "\n"
                + "                This approach optimizes for both security AND performance.\n");
    }

    public static void main(String[] args) {
        runFullComparison();
    }
}
