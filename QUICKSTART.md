# 🚀 Quick Start Guide

## Blockchain-Based Smart Supply Chain System (Java)

Get up and running with your blockchain supply chain project in 5 minutes!

## 📋 Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- SQLite JDBC driver (downloaded automatically by Maven)

## 🎯 Quick Start

### Option 1: Run Everything (Recommended for First Time)

```bash
cd Blockchain-based-smart-supply-chain-system
mvn clean compile exec:java -Dexec.mainClass="com.supplychain.Main"
```

Then enter `4` when prompted. This will:
1. ✓ Run all unit tests
2. ✓ Demonstrate the complete supply chain system
3. ✓ Run performance benchmarks

Or simply use the build script:

```bash
./build.sh all
```

### Option 2: Run Individual Components

**Compile the project**
```bash
mvn clean compile
```

**Run all unit tests (Phases 1-3)**
```bash
mvn exec:java -Dexec.mainClass="com.supplychain.Main"
# Select option 1
```

**Demo the complete supply chain system (Phase 4)**
```bash
mvn exec:java -Dexec.mainClass="com.supplychain.Main"
# Select option 2
```

**Run the SQL vs Blockchain benchmark (Phase 5)**
```bash
mvn exec:java -Dexec.mainClass="com.supplychain.Main"
# Select option 3
```

### Option 3: Without Maven

```bash
# Compile all Java files
javac -d bin src/main/java/com/supplychain/**/*.java

# Run the main class
java -cp bin com.supplychain.Main
```

## ✅ What You Should See

### Phase 1: SHA-256 Hashing
```
✓ Hash consistency test passed
✓ Avalanche effect test passed: 95.3% of hash characters changed
✓ Hash uniqueness test passed
✓ Hash length test passed: 64 hexadecimal characters
```

### Phase 2: Merkle Tree
```
✓ Merkle root generated
✓ Tree depth: 3
✓ All 8 transactions verified with proof length O(log n)
✓ Batch verification: 1000 items verified at O(log n) each
```

### Phase 3: Blockchain Ledger
```
✓ Genesis block created correctly
✓ Blocks properly chained
✓ Chain validation detects tampering
✓ Product journey tracked: 4 steps
```

### Supply Chain Demo
```
STEP 1: Registering authorized manufacturers...
STEP 2: Manufacturing products...
STEP 3-6: Transferring through supply chain...
STEP 7: Verifying product authenticity...
STEP 8: Testing counterfeit detection...
```

### Benchmark Results
```
Blockchain is ~107x slower for insertions
SQL is ~20x faster for queries
Blockchain uses ~4x more storage

BUT blockchain provides:
✓ Tamper resistance
✓ Decentralized trust
✓ Cryptographic verification
```

## 📊 Key Metrics

| Metric | Value | Notes |
|--------|-------|-------|
| Hash length | 64 chars | SHA-256 produces 256-bit (64 hex) |
| Proof complexity | O(log n) | For n items, only ~10 hashes needed |
| Tree depth (1000 items) | 10 levels | log₂(1000) ≈ 10 |
| Insertion overhead | ~107x | Compared to SQL |
| Storage overhead | ~4x | Compared to SQL |
| Query overhead | ~20x | Compared to SQL |

## 🔍 What This System Does

### 1. Product Lifecycle Tracking
```
FACTORY → DISTRIBUTOR → WHOLESALER → RETAILER → CONSUMER
   |           |              |            |           |
   └───────────┴──────────────┴────────────┴───────────┘
                         |
                    BLOCKCHAIN
                    (Immutable Ledger)
```

### 2. Verification Flow
```
Consumer scans QR code
       ↓
System queries blockchain
       ↓
Verifies Merkle proof
       ↓
Confirms product authenticity
       ↓
Shows complete journey
```

### 3. Tamper Detection
```
If anyone changes even ONE character:
   - Transaction hash changes completely
   - Merkle root becomes invalid
   - Chain validation fails
   - System rejects the product
```

## 🎓 Learning Objectives

After running this system, you should understand:

1. **Cryptographic Hashing**
   - How SHA-256 creates digital fingerprints
   - Why avalanche effect matters
   - Applications in tamper detection

2. **Merkle Trees**
   - Binary tree structure for hash verification
   - O(log n) verification complexity
   - Space-optimized proofs

3. **Blockchain Technology**
   - Chaining blocks with cryptographic links
   - Immutability through hash dependencies
   - When to use vs traditional databases

4. **Supply Chain Applications**
   - Real-world implementation challenges
   - The Oracle Problem (digital ↔ physical gap)
   - Hybrid architecture recommendations

## 📝 Code Structure

```
src/main/java/com/supplychain/
├── core/
│   ├── HashUtils.java        # SHA-256 implementation
│   ├── MerkleTree.java       # Merkle Tree with proofs
│   └── Blockchain.java       # Blockchain ledger
├── supplychain/
│   └── SupplyChainBlockchain.java  # Supply chain operations
├── benchmarking/
│   └── DatabaseBenchmark.java      # SQL vs blockchain analysis
└── Main.java                 # Entry point with menu
```

## 🔧 Customization

### Add New Transaction Types
Edit `src/main/java/com/supplychain/core/HashUtils.java`:
```java
public static Map<String, Object> createTransaction(
        String productId, String sender, String receiver,
        String location, Map<String, Object> metadata) {
    Map<String, Object> transaction = new LinkedHashMap<>();
    transaction.put("product_id", productId);
    transaction.put("sender", sender);
    transaction.put("receiver", receiver);
    transaction.put("location", location);
    transaction.put("timestamp", Instant.now().toString());
    transaction.put("metadata", metadata != null ? metadata : new LinkedHashMap<>());
    return transaction;
}
```

### Modify Verification Logic
Edit `src/main/java/com/supplychain/supplychain/SupplyChainBlockchain.java`:
```java
public boolean verifyProduct(String productId) {
    // Add custom verification rules here
    return verifyProductInternal(productId);
}
```

### Change Hash Algorithm
Edit `src/main/java/com/supplychain/core/HashUtils.java`:
```java
public static String calculateSHA256(String data) {
    // Replace with SHA-512, SHA3-256, etc.
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(hash);
}
```

## 🎯 Next Steps

1. **Understand the Code**: Read through each class's Javadoc comments
2. **Run the Tests**: Verify each component works independently
3. **Analyze the Benchmark**: Understand the trade-offs
4. **Read the README**: Get detailed academic context
5. **Extend the System**: Add your own features!

## 📚 Documentation

- **Main README**: Comprehensive project documentation
- **Code Comments**: Each method has detailed Javadoc
- **Academic Context**: Why these DSA concepts matter
- **Benchmark Analysis**: When to use blockchain vs SQL

## 🐛 Troubleshooting

**Build Errors?**
```bash
# Make sure Java 11+ and Maven are installed
java -version
mvn -version

# Clean and rebuild
mvn clean compile
```

**Maven not found?**
The `build.sh` script can install Maven for you:
```bash
./build.sh compile
```

**Slow Benchmark?**
Reduce the transaction count in `src/main/java/com/supplychain/benchmarking/DatabaseBenchmark.java`:
```java
runBenchmark(1000, 100);  // transactions, products
```

**Need Help?**
Each class includes:
- Detailed Javadoc comments
- Usage examples
- Complexity analysis
- Academic references

## ✅ Success Criteria

Your system is working if:

- ✓ All unit tests pass (option 1 in main menu)
- ✓ Demonstration runs without errors
- ✓ Benchmark completes and shows trade-offs
