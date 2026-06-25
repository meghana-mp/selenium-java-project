package com.eventhub.tests.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test up to MAX_RETRIES times with exponential backoff.
 *
 * Applied automatically to every smoke-tagged test by RetryListener.
 * Each instance is per-test-method (TestNG creates a new instance per test).
 *
 * Retry flow:
 *   Attempt 1 fails → wait 1 s  → retry 1
 *   Retry 1   fails → wait 2 s  → retry 2
 *   Retry 2   fails → mark FAILED (exhausted)
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);

    private static final int     MAX_RETRIES = 2;
    private static final long[]  BACKOFF_MS  = { 1000, 2000 };

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRIES) {
            String cause = result.getThrowable() != null
                    ? result.getThrowable().getClass().getSimpleName() + ": " + result.getThrowable().getMessage()
                    : "unknown";

            log.warn("[RETRY {}/{}] '{}' — {}",
                    retryCount + 1, MAX_RETRIES, result.getName(), cause);

            pause(BACKOFF_MS[retryCount]);
            retryCount++;
            return true;
        }

        log.error("[RETRY EXHAUSTED] '{}' failed after {} attempts — marking as FAILED",
                result.getName(), MAX_RETRIES);
        return false;
    }

    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
