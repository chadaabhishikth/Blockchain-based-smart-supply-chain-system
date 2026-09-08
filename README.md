# Blockchain-Based Smart Supply Chain System

## 📋 Project Overview

A comprehensive **Java** implementation of blockchain technology for supply chain provenance tracking, counterfeit detection, and product authentication using cryptographic hashing and Merkle Trees — organized in a clean, layered architecture.

## 🏗️ Architecture

The codebase follows a **layered architecture** with a strict dependency rule:
dependencies point inwards/downwards only (`app → domain → crypto`), never the
other way around.

```
┌─────────────────────────────────────────────────────────────────┐
│  app          SupplyChainApplication (composition root),        │
│               SupplyChainDemo — console presentation & wiring   │
├─────────────────────────────────────────────────────────────────┤
│  domain       The heart of the system:                          │
│    ├─ model    Transaction, Block, Product (typed, immutable)   │
│    ├─ merkle   MerkleTree, MerkleNode, MerkleProofElement       │
│    ├─ ledger   Blockchain (immutable ledger + integrity check)  │
│    ├─ service  SupplyChainService (business use cases)          │
│    └─ dto      Typed results: TransferResult, Verification-    │
│                Result, Provenance, Batch/Summary reports        │
├─────────────────────────────────────────────────────────────────┤
│  crypto       HashFunction (interface), Sha256Hasher,           │
│               TransactionSerializer (canonical serialization)   │
├─────────────────────────────────────────────────────────────────┤
│  benchmark    DatabaseBenchmark — SQL vs blockchain comparison  │
│  selftest     Menu-driven unit tests (Phases 1-4)               │
└─────────────────────────────────────────────────────────────────┘
        src/test/java  →  JUnit 5 tests for CI (Maven Surefire)
```

### Design principles applied

| Principle | Where you see it |
|-----------|------------------|
| **Layered architecture** | `app` / `domain` / `crypto` packages with one-way dependencies |
| **Type safety** | Typed `Transaction`, `Block`, `Product` and DTOs replace `Map<String, Object>` |
| **Program to interfaces** | `HashFunction` abstracts SHA-256; inject it anywhere |
| **Dependency injection** | `Blockchain(HashFunction)`, `SupplyChainService(Blockchain)` |
| **Immutability** | `Transaction` and `Block` are immutable value objects |
| **Encapsulation** | Private fields + getters everywhere (no more public mutable fields) |
| **Separation of concerns** | Domain code never prints; console I/O lives in `app` |
| **Tests separated** | Menu self-tests in `selftest`, CI tests in `src/test/java` (JUnit 5) |
| **Self-documenting packages** | Every layer has a `package-info.java` explaining its responsibility |

### The DSA Components

#### 1. SHA-256 Cryptographic Hashing (`crypto/Sha256Hasher.java`)
- **Purpose**: Create unforgeable digital fingerprints for transactions
- **Properties**:
  - Deterministic: Same input always produces same output
  - Avalanche effect: Tiny input changes cause massive output changes
  - One-way function: Practically impossible to reverse
- **Location**: `src/main/java/com/supplychain/crypto/`

#### 2. Merkle Trees (`domain/merkle/MerkleTree.java`)
- **Purpose**: Optimize storage and enable O(log n) verification
- **Structure**:
  - Leaves: Individual transaction hashes
  - Internal nodes: Hash of concatenated child hashes
  - Root: Single hash summarizing all transactions (Merkle Root)
- **Key Operations**:
  - `buildTree()`: O(n) construction
  - `generateProof()`: O(log n) proof generation
  - `verifyProof()`: O(log n) verification
- **Location**: `src/main/java/com/supplychain/domain/merkle/`

#### 3. Blockchain Ledger (`domain/ledger/Blockchain.java`)
- **Purpose**: Chain blocks together using cryptographic links
- **Structure**: Each `Block` (see `domain/model/Block.java`) contains:
  - Block index
  - Timestamp
  - Transactions (typed `Transaction` objects)
  - Merkle Root of transactions
  - Previous block's hash (creates the chain)
  - Current block's hash
- **Immutability**: Modifying any block invalidates all subsequent blocks
- **Location**: `src/main/java/com/supplychain/domain/ledger/`

#### 4. Business Logic (`domain/service/SupplyChainService.java`)
- Manufacturer whitelisting, manufacturing, ownership transfers, consumer
  sales, product verification and batch verification
- Returns typed results (`dto` package) instead of raw maps

## 📁 Project Structure

```
Blockchain-based-smart-supply-chain-system/
├── pom.xml                                        # Maven configuration
├── build.sh                                       # Build & run helper script
├── README.md                                      # This file
├── QUICKSTART.md                                  # Quick start guide
├── PROJECT_SUMMARY.md                             # Project summary
└── src/
    ├── main/java/com/supplychain/
    │   ├── Main.java                              # Thin entry point
    │   ├── app/                                   # ── Application layer
    │   │   ├── SupplyChainApplication.java        #    Composition root + console menu
    │   │   └── demo/SupplyChainDemo.java          #    Product lifecycle demo
    │   ├── crypto/                                # ── Crypto layer
    │   │   ├── HashFunction.java                  #    Hash abstraction (interface)
    │   │   ├── Sha256Hasher.java                  #    SHA-256 implementation
    │   │   └── TransactionSerializer.java         #    Canonical serialization
    │   ├── domain/                                # ── Domain layer
    │   │   ├── model/                             #    Typed value objects
    │   │   │   ├── Transaction.java
    │   │   │   ├── Block.java
    │   │   │   ├── Product.java
    │   │   │   ├── ProductHistoryEntry.java
    │   │   │   ├── ProductStatus.java
    │   │   │   └── SupplyChainStage.java
    │   │   ├── merkle/
    │   │   │   ├── MerkleTree.java
    │   │   │   ├── MerkleNode.java
    │   │   │   ├── MerkleProofElement.java
    │   │   │   └── MerkleSide.java
    │   │   ├── ledger/
    │   │   │   └── Blockchain.java
    │   │   ├── service/
    │   │   │   └── SupplyChainService.java
    │   │   └── dto/                               #    Typed results
    │   │       ├── TransferResult.java
    │   │       ├── VerificationResult.java
    │   │       ├── ManufactureResult.java
    │   │       ├── Provenance.java
    │   │       ├── BatchVerificationResult.java
    │   │       ├── ProductVerification.java
    │   │       └── SupplyChainSummary.java
    │   ├── benchmark/                             # ── Benchmark layer
    │   │   └── DatabaseBenchmark.java
    │   └── selftest/                              # ── Menu-driven tests
    │       ├── SelfTestSuite.java
    │       ├── Check.java
    │       ├── HashingSelfTest.java
    │       ├── MerkleSelfTest.java
    │       ├── LedgerSelfTest.java
    │       └── SupplyChainSelfTest.java
    └── test/java/com/supplychain/                 # ── JUnit 5 tests (CI)
        ├── crypto/Sha256HasherTest.java
        ├── domain/merkle/MerkleTreeTest.java
        ├── domain/ledger/BlockchainTest.java
        └── domain/service/SupplyChainServiceTest.java
```

## 🚀 Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- SQLite JDBC driver (included via Maven)

### Installation & Running

```bash
# Clone the repository
cd Blockchain-based-smart-supply-chain-system

# Compile the project
mvn clean compile

# Run the application (interactive menu)
mvn exec:java

# Or run tests
mvn test

# Package as runnable JAR
mvn package
java -jar target/blockchain-supply-chain-1.0.0.jar
```

### Alternative: Build Script

```bash
./build.sh compile   # Compile the project
./build.sh test      # Run tests
./build.sh run       # Run the application
./build.sh package   # Package as JAR
./build.sh all       # Compile + test + package + run
```

### Alternative: Direct Compilation

```bash
# Compile all Java files
javac -d bin $(find src/main/java -name "*.java")

# Run the main class
java -cp bin com.supplychain.Main

# Run an individual self test phase, e.g.:
java -cp bin com.supplychain.selftest.MerkleSelfTest
```

### Interactive Menu

Running the application shows:

```
Select an option:
  1. Run all unit tests
  2. Demo complete supply chain system
  3. Run performance benchmark
  4. Run everything
  5. Exit
```

## 🎓 Educational Content

### Time & Space Complexity

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|-------------------|
| SHA-256 Hash | O(1) | O(1) |
| Merkle Tree Construction | O(n) | O(n) |
| Merkle Proof Generation | O(log n) | O(log n) |
| Merkle Proof Verification | O(log n) | O(1) |
| Blockchain Validation | O(n) | O(1) |
| Product History Query | O(n) | O(k)* |
| Batch Verification | O(k log n) | O(k log n) |

*k = number of transactions for the product

### Why O(log n) Matters

For a supply chain with **1 million products**:
- Linear verification (O(n)): 1,000,000 operations
- Merkle Tree verification (O(log n)): ~20 operations

This enables lightweight consumer verification without downloading entire transaction history.

## 📊 Performance Analysis

Based on benchmark results with 10,000 transactions:

### Insertion Performance
- **SQL Database**: ~1.1 ms per transaction
- **Blockchain**: ~0.01 ms per transaction
- **Result**: SQL is **~107x faster** for insertions

### Query Performance
- **SQL Database**: ~0.05 ms average
- **Blockchain**: ~1.0 ms average
- **Result**: SQL is **~20x faster** for queries

### Storage Efficiency
- **SQL Database**: ~1.8 MB
- **Blockchain**: ~7.2 MB
- **Result**: Blockchain uses **~4x more storage**

### Trade-offs

**Blockchain Advantages:**
✓ Tamper-evident (modifications are detectable)
✓ Decentralized trust (no single point of control)
✓ Cryptographic proofs (O(log n) verification)
✓ Immutable history (append-only ledger)

**SQL Advantages:**
✓ Faster insertions and queries
✓ More storage efficient
✓ Flexible querying and reporting
✓ ACID transactions fully supported

## 🔐 Security Model

### What Blockchain Secures

✅ **Digital Transaction Records**
- Every transaction is hashed with SHA-256
- Tampering with any transaction changes its hash
- Merkle Tree structure ensures any change is detectable
- Chain linking ensures historical immutability

✅ **Cryptographic Proofs**
- Merkle proofs allow O(log n) verification
- No need to download entire history
- Suitable for mobile/IoT verification

### What Blockchain CANNOT Secure

❌ **Physical Reality (The Oracle Problem)**
- A corrupt factory worker could place authentic QR codes on counterfeit products
- The blockchain would perfectly record the lie
- **Solution**: IoT sensors, RFID, physical inspections supplement blockchain

## 💡 Use Cases

### When to Use Blockchain
- Multi-party supply chains with low trust
- Regulatory requirements for immutable audit trails
- High-value products (luxury goods, pharmaceuticals)
- Counterfeit-sensitive industries
- Regulatory compliance requirements

### When to Use SQL/Database
- Single-company internal tracking
- High-volume, low-value products
- Performance-critical applications
- Complex queries and reporting
- Smaller teams without blockchain expertise

### Hybrid Approach (Recommended)
```
Blockchain Layer:    High-value verification, multi-party trust, compliance
Database Layer:      Internal operations, analytics, reporting
```

## 🧪 Testing

Testing is organized in two complementary layers:

### 1. Menu-driven self tests (`selftest` package)
Run inside the application (menu option 1) — no test framework needed:

**Phase 1 — SHA-256 hashing:** hash consistency, avalanche effect, uniqueness, length
**Phase 2 — Merkle tree:** construction, proof generation/verification, invalid proof rejection, single item, odd counts, batch of 1000
**Phase 3 — Blockchain ledger:** genesis block, chaining, tamper detection, product history
**Phase 4 — Supply chain operations:** complete lifecycle, counterfeit detection, unauthorized manufacturers, batch verification

### 2. JUnit 5 tests (`src/test/java`)
Run by Maven Surefire in CI:

```bash
mvn test
```

Covers the crypto, merkle, ledger and service layers with the standard
`org.junit.jupiter.api.Assertions` API (including the known SHA-256 test
vector for `"abc"`).

## 🔧 Implementation Details

### Transaction (typed value object)

```java
Transaction tx = Transaction.create(
        "PROD-001",          // productId
        "Factory-A",         // sender
        "Distributor-B",     // receiver
        "Shanghai Manufacturing Hub",
        metadataMap);        // Map<String, Object> metadata

String fingerprint = TransactionSerializer.hashOf(tx, hasher);
```

### Block (immutable entity)

```java
public final class Block {
    // All fields private final — computed once, never mutated
    public Block(int index, String timestamp, List<Transaction> transactions,
                 String merkleRoot, String previousHash, int nonce, HashFunction hasher)
    public String calculateHash()   // re-verifiable integrity
    public List<Transaction> getTransactions()  // unmodifiable
}
```

### Wiring (composition root)

```java
HashFunction hasher = new Sha256Hasher();
Blockchain ledger = new Blockchain(hasher);
SupplyChainService service = new SupplyChainService(ledger);
```

Swap the hash algorithm (e.g., SHA3-256) by providing another
`HashFunction` implementation — zero domain code changes.

## 📚 Documentation

All code includes:
- Comprehensive Javadoc comments
- `package-info.java` per package explaining each layer's responsibility
- Complexity analysis in the Javadoc of every data structure
- Usage examples

## 🔄 Future Enhancements

Potential improvements for production systems:

1. **Consensus Mechanism**: Implement proof-of-stake or proof-of-authority
2. **Smart Contracts**: Add business logic as executable contracts
3. **Privacy**: Implement zero-knowledge proofs for sensitive data
4. **Scalability**: Layer 2 solutions for high throughput
5. **IoT Integration**: Connect with physical sensors and RFID
6. **Persistence**: Ledger repository backed by a database
7. **REST API**: Web interface for consumers and administrators

## 📄 License

This is an educational project for demonstrating blockchain technology, DSA concepts, and supply chain applications.

## 👥 Author

Blockchain Supply Chain Team
- Implemented for educational purposes
- Demonstrates core computer science principles and clean architecture
- Provides real-world context for theoretical concepts

---

**Remember**: Blockchain secures digital records but cannot verify physical reality. The "Oracle Problem" remains the fundamental challenge connecting digital systems to the physical world. The solution requires IoT, RFID, and physical inspections in addition to blockchain technology.
