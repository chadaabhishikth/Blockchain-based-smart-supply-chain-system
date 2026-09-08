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
mvn clean compile exec:java
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

**Run all unit tests (Phases 1-4)**
```bash
mvn exec:java
# Select option 1
```

**Demo the complete supply chain system (Phase 4)**
```bash
mvn exec:java
# Select option 2
```

**Run the SQL vs Blockchain benchmark (Phase 5)**
```bash
mvn exec:java
# Select option 3
```

### Option 3: Without Maven

```bash
# Compile all Java files
javac -d bin $(find src/main/java -name "*.java")

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

The project follows a layered architecture (`app → domain → crypto`):

```
src/main/java/com/supplychain/
├── Main.java                     # Thin entry point
├── app/                          # Console application & demos
│   ├── SupplyChainApplication.java
│   └── demo/SupplyChainDemo.java
├── crypto/                       # Hashing primitives
│   ├── HashFunction.java         # Hash abstraction (interface)
│   ├── Sha256Hasher.java         # SHA-256 implementation
│   └── TransactionSerializer.java
├── domain/                       # Business logic (pure, no I/O)
│   ├── model/                    # Transaction, Block, Product, enums
│   ├── merkle/                   # Merkle tree + proofs
│   ├── ledger/Blockchain.java    # Immutable ledger
│   ├── service/SupplyChainService.java
│   └── dto/                      # Typed results
├── benchmark/DatabaseBenchmark.java
└── selftest/                     # Menu-driven tests (Phases 1-4)

src/test/java/com/supplychain/    # JUnit 5 tests (mvn test)
```

## 🔧 Customization

### Add New Transaction Fields
Edit `src/main/java/com/supplychain/domain/model/Transaction.java` and
`src/main/java/com/supplychain/crypto/TransactionSerializer.java`:

```java
public Transaction(String productId, String sender, String receiver,
                   String location, String timestamp, Map<String, Object> metadata) {
    // add your field here — the canonical serializer picks it up automatically
}
```

### Modify Verification Logic
Edit `src/main/java/com/supplychain/domain/service/SupplyChainService.java`:
```java
public VerificationResult verifyProduct(String productId) {
    // Add custom verification rules here
}
```

### Change Hash Algorithm
Implement the `HashFunction` interface — no domain code changes needed:
```java
public class Sha3Hasher implements HashFunction {
    @Override
    public String hash(String data) {
        MessageDigest digest = MessageDigest.getInstance("SHA3-256");
        return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }
}

// Then wire it at the composition root (SupplyChainApplication):
HashFunction hasher = new Sha3Hasher();
Blockchain ledger = new Blockchain(hasher);
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
Reduce the transaction count in `src/main/java/com/supplychain/benchmark/DatabaseBenchmark.java`:
```java
benchmark.runBenchmark(1000, 100);  // transactions, products
```

**Need Help?**
Each package includes a `package-info.java` explaining its responsibility, and each class has:
- Detailed Javadoc comments
- Usage examples
- Complexity analysis
- Academic references

## ✅ Success Criteria

Your system is working if:

- ✓ All unit tests pass (option 1 in main menu)
- ✓ Demonstration runs without errors
- ✓ Benchmark completes and shows trade-offs
