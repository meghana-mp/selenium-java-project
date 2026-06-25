package com.eventhub.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Stateless helper methods used across all test classes. No instantiation. */
public final class TestUtils {

    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);
    private TestUtils() {}

    /** Returns a unique name by appending an epoch timestamp. */
    public static String uniqueName(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    /** Returns a UUID without hyphens — useful as a unique identifier in test data. */
    public static String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Returns a date offset by {@code daysFromToday} in yyyy-MM-dd format.
     * Positive = future, negative = past.
     */
    public static String dateOffset(int daysFromToday) {
        return LocalDate.now()
                .plusDays(daysFromToday)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /** Calculates the expected discounted total: base × (1 - discountPercent / 100). */
    public static double calculateDiscountedTotal(double base, int discountPercent) {
        return Math.round(base * (1.0 - discountPercent / 100.0) * 100.0) / 100.0;
    }

    /** Structured log line for test results — matches pattern in sibling project. */
    public static void logResult(String testId, boolean passed, long durationMs, String details) {
        String status = passed ? "PASS" : "FAIL";
        log.info("[{}] {} | {}ms | {}", status, testId, durationMs, details);
    }

    public static void logStep(String testId, String step) {
        log.info("[STEP] {} → {}", testId, step);
    }
}
