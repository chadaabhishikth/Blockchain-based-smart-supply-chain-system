package com.supplychain.selftest;

/**
 * Self-Test Layer — Assertion Helper
 * ===================================
 *
 * Tiny assertion utility for the menu-driven self tests. Unlike the
 * plain Java {@code assert} keyword, these checks are always active —
 * no {@code -ea} JVM flag required.
 */
public final class Check {

    private Check() {
        // Static utility class — no instances.
    }

    /**
     * Fail with the given message unless the condition holds.
     */
    public static void that(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("✗ " + message);
        }
    }
}
