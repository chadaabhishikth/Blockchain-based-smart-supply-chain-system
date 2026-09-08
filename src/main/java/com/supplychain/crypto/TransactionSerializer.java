package com.supplychain.crypto;

import com.supplychain.domain.model.Transaction;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Crypto Layer — Canonical Transaction Serializer
 * =================================================
 *
 * Produces a deterministic, canonical string representation of a
 * {@link Transaction} so that hashing is stable:
 *
 * - Keys are sorted at every level of the structure.
 * - Strings are double-quoted; numbers and booleans are emitted plainly.
 * - Maps and lists are serialized recursively.
 *
 * CRITICAL: identical transactions must always serialize to identical
 * strings, otherwise tamper detection breaks.
 */
public final class TransactionSerializer {

    private TransactionSerializer() {
        // Static utility class — no instances.
    }

    /**
     * Serialize a transaction into its canonical string form.
     *
     * @param transaction the transaction to serialize
     * @return canonical string representation
     */
    public static String serialize(Transaction transaction) {
        Map<String, Object> map = new TreeMap<>();
        map.put("product_id", transaction.getProductId());
        map.put("sender", transaction.getSender());
        map.put("receiver", transaction.getReceiver());
        map.put("location", transaction.getLocation());
        map.put("timestamp", transaction.getTimestamp());
        map.put("metadata", transaction.getMetadata());

        StringBuilder sb = new StringBuilder();
        serializeMap(map, sb);
        return sb.toString();
    }

    /**
     * Convenience method: serialize and hash a transaction in one step.
     *
     * @param transaction the transaction to hash
     * @param hasher      the hash function to use
     * @return cryptographic fingerprint of the transaction
     */
    public static String hashOf(Transaction transaction, HashFunction hasher) {
        return hasher.hash(serialize(transaction));
    }

    /**
     * Recursively serialize a map with sorted keys.
     */
    private static void serializeMap(Map<String, Object> map, StringBuilder sb) {
        sb.append("{");
        Iterator<Map.Entry<String, Object>> iterator = new TreeMap<>(map).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            sb.append("\"").append(entry.getKey()).append("\":");
            appendValue(entry.getValue(), sb);
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
    }

    /**
     * Recursively serialize a list.
     */
    private static void serializeList(List<Object> list, StringBuilder sb) {
        sb.append("[");
        Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            appendValue(iterator.next(), sb);
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
    }

    /**
     * Append a single value, dispatching on its runtime type.
     */
    private static void appendValue(Object value, StringBuilder sb) {
        if (value instanceof Map) {
            serializeMap(castToMap(value), sb);
        } else if (value instanceof List) {
            serializeList(castToList(value), sb);
        } else if (value instanceof String) {
            sb.append("\"").append(value).append("\"");
        } else {
            sb.append(value);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToList(Object value) {
        return (List<Object>) value;
    }
}
