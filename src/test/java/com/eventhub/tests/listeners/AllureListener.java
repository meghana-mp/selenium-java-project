package com.eventhub.tests.listeners;

import com.eventhub.driver.BrowserFactory;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures a screenshot + browser state on every test failure or skip.
 *
 * Allure attachment  → visible in the Allure HTML report under the failing test
 * Disk copy          → target/screenshots/<TestClass>_<method>_<timestamp>.png
 *                      useful when Allure isn't generated yet
 */
public class AllureListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(AllureListener.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path SCREENSHOT_DIR = Paths.get("target", "screenshots");

    @Override
    public void onTestFailure(ITestResult result) {
        captureEvidence(result, "FAIL");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        captureEvidence(result, "SKIP");
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("▶ START  {}.{}", result.getTestClass().getName(), result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("✔ PASS   {}.{} ({}ms)",
                result.getTestClass().getName(), result.getName(),
                result.getEndMillis() - result.getStartMillis());
    }

    // ── Evidence capture ────────────────────────────────────────────────────

    private void captureEvidence(ITestResult result, String reason) {
        WebDriver driver = BrowserFactory.getInstance().getDriver();
        if (driver == null) {
            log.warn("[{}] No active driver — screenshot skipped for {}", reason, result.getName());
            return;
        }

        byte[] png = takeScreenshot(driver);
        if (png != null) {
            attachToAllure(png, result, reason);
            saveToDisk(png, result, reason);
        }

        attachBrowserState(driver, result);
        attachFailureMessage(result);
    }

    private byte[] takeScreenshot(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.warn("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }

    private void attachToAllure(byte[] png, ITestResult result, String reason) {
        String label = "Screenshot [" + reason + "] — " + result.getName();
        Allure.addAttachment(label, "image/png", new ByteArrayInputStream(png), ".png");
        log.info("[{}] Screenshot attached to Allure for: {}", reason, result.getName());
    }

    private void saveToDisk(byte[] png, ITestResult result, String reason) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            String filename = sanitize(result.getTestClass().getRealClass().getSimpleName())
                    + "_" + sanitize(result.getName())
                    + "_" + reason
                    + "_" + LocalDateTime.now().format(TIMESTAMP)
                    + ".png";
            Path dest = SCREENSHOT_DIR.resolve(filename);
            Files.write(dest, png);
            log.info("[{}] Screenshot saved: {}", reason, dest.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not save screenshot to disk: {}", e.getMessage());
        }
    }

    private void attachBrowserState(WebDriver driver, ITestResult result) {
        if (!hasActiveAllureTest()) return;
        try {
            String url   = driver.getCurrentUrl();
            String title = driver.getTitle();
            String body  = "Test   : " + result.getName() + "\n"
                         + "URL    : " + url + "\n"
                         + "Title  : " + title + "\n"
                         + "Thread : " + Thread.currentThread().getName();
            Allure.addAttachment("Browser State", "text/plain",
                    new ByteArrayInputStream(body.getBytes()), ".txt");
        } catch (Exception ignored) {}
    }

    private void attachFailureMessage(ITestResult result) {
        if (!hasActiveAllureTest()) return;
        Throwable t = result.getThrowable();
        if (t == null) return;
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            Allure.addAttachment("Failure Stacktrace", "text/plain",
                    new ByteArrayInputStream(sw.toString().getBytes()), ".txt");
        } catch (Exception ignored) {}
    }

    private boolean hasActiveAllureTest() {
        try {
            return Allure.getLifecycle().getCurrentTestCase().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
