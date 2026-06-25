package com.eventhub.tests.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** Structured SLF4J logging for suite-level events and per-test outcomes. */
public class TestListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        log.info("══════════════════════════════════════════════════════");
        log.info("  Suite: {}  |  Thread count: {}",
                context.getName(),
                context.getCurrentXmlTest().getSuite().getThreadCount());
        log.info("══════════════════════════════════════════════════════");
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("──────────────────────────────────────────────────────");
        log.info("  PASSED: {}  FAILED: {}  SKIPPED: {}",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        log.info("──────────────────────────────────────────────────────");
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("[THREAD {}] ▶ {}", Thread.currentThread().getName(), result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("[THREAD {}] ✔ {} ({}ms)",
                Thread.currentThread().getName(), result.getName(),
                result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("[THREAD {}] ✘ {} — {}",
                Thread.currentThread().getName(), result.getName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "unknown");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("[THREAD {}] ⊘ {} (SKIPPED)", Thread.currentThread().getName(), result.getName());
    }
}
