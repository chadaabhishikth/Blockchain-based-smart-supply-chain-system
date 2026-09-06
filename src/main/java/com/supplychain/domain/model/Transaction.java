package com.supplychain.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain Layer — Transaction (Value Object)
 * ===========================================
 *
 * A typed, immutable supply chain transaction. This replaces the old
 * untyped {@code Map<String, Object>} representation, giving the whole
 * system compile-time safety: a transaction either has a product id,
 * sender, receiver, location, timestamp and metadata — or it does not
 * compile.
 *
 * A transaction records one custody event:
 *
 *     sender --[product]--> receiver   (at a location, at a time)
 *
 * Being immutable, a transaction can never be modified after it is
 * embedded in a block — tampering has to be simulated explicitly
 * (see {@code Blockchain#simulateTampering}).
 */
public final class Transaction {

    private final String productId;
    private final String sender;
    private final String receiver;
    private final String location;
    private final String timestamp;
    private final Map<String, Object> metadata;

    /**
     * Create a fully-specified transaction.
     *
     * @param productId unique identifier of the product
     * @param sender    entity transferring custody
     * @param receiver  entity receiving custody
     * @param location  physical location of the event
     * @param timestamp ISO-8601 timestamp of the event
     * @param metadata  optional additional data (copied; never null)
     */
    public Transaction(String productId, String sender, String receiver,
                       String location, String timestamp, Map<String, Object> metadata) {
        this.productId = productId;
        this.sender = sender;
        this.receiver = receiver;
        this.location = location;
        this.timestamp = timestamp;
        this.metadata = metadata == null
                ? new LinkedHashMap<String, Object>()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /**
     * Factory that stamps the transaction with the current time.
     * This is how business code normally creates transactions.
     */
    public static Transaction create(String productId, String sender, String receiver,
                                     String location, Map<String, Object> metadata) {
        return new Transaction(productId, sender, receiver, location, Instant.now().toString(), metadata);
    }

    /**
     * Derive a tampered copy of this transaction with a different location.
     * Used exclusively by integrity demonstrations and tests.
     */
    public Transaction withLocation(String newLocation) {
        return new Transaction(productId, sender, receiver, newLocation, timestamp, metadata);
    }

    public String getProductId() {
        return productId;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getLocation() {
        return location;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Map representation with the canonical snake_case keys.
     * Used mainly by the SQL benchmark when inserting rows.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("product_id", productId);
        map.put("sender", sender);
        map.put("receiver", receiver);
        map.put("location", location);
        map.put("timestamp", timestamp);
        map.put("metadata", metadata);
        return map;
    }

    @Override
    public String toString() {
        return "Transaction{" + productId + ": " + sender + " → " + receiver + " @ " + location + "}";
    }
}
