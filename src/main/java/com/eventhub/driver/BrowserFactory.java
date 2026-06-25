package com.eventhub.driver;

import com.eventhub.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;

/**
 * Thread-safe WebDriver factory using Double-Checked Locking Singleton and ThreadLocal.
 * Each test thread gets its own isolated WebDriver instance — safe for parallel execution.
 */
public class BrowserFactory {

    private static final Logger log = LoggerFactory.getLogger(BrowserFactory.class);
    private static volatile BrowserFactory instance;
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private BrowserFactory() {}

    public static BrowserFactory getInstance() {
        if (instance == null) {
            synchronized (BrowserFactory.class) {
                if (instance == null) {
                    instance = new BrowserFactory();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a browser instance for the calling thread and stores it in ThreadLocal.
     * When SELENIUM_REMOTE_URL is set (Docker), connects to Selenium Grid via RemoteWebDriver.
     * When unset (local dev), spawns a local ChromeDriver/FirefoxDriver/EdgeDriver.
     */
    public WebDriver initDriver(String browser) {
        int implicitWait = Integer.parseInt(
                ConfigManager.getInstance().getProperty("implicit.wait.seconds", "10"));

        String remoteUrl = ConfigManager.getInstance().getProperty("selenium.remote.url", "");

        WebDriver driver = remoteUrl.isBlank()
                ? initLocalDriver(browser)
                : initRemoteDriver(remoteUrl);

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driverHolder.set(driver);
        log.info("Browser '{}' started on thread [{}]", browser, Thread.currentThread().getName());
        return driver;
    }

    private WebDriver initLocalDriver(String browser) {
        return switch (browser.trim().toLowerCase()) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                yield new FirefoxDriver(new FirefoxOptions());
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                yield new EdgeDriver(new EdgeOptions());
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver(buildChromeOptions());
            }
        };
    }

    private WebDriver initRemoteDriver(String remoteUrl) {
        URL gridUrl;
        try {
            gridUrl = new URL(remoteUrl);
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException("Invalid SELENIUM_REMOTE_URL: " + remoteUrl, e);
        }
        // The Selenium Grid health check can pass while the node is still initialising.
        // Retry up to 3 times with a 3 s back-off before giving up.
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebDriver driver = new RemoteWebDriver(gridUrl, buildChromeOptions());
                log.info("RemoteWebDriver connected to {} (attempt {})", remoteUrl, attempt);
                return driver;
            } catch (Exception e) {
                last = e;
                log.warn("RemoteWebDriver attempt {}/3 failed — retrying in 3 s: {}", attempt, e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new RuntimeException("Failed to connect to Selenium Grid at " + remoteUrl + " after 3 attempts", last);
    }

    private ChromeOptions buildChromeOptions() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--remote-allow-origins=*");
        opts.addArguments("--no-sandbox");            // required when running as root in Docker
        opts.addArguments("--disable-dev-shm-usage"); // prevents /dev/shm space crashes in containers
        opts.addArguments("--disable-gpu");           // no GPU in CI/container environments
        opts.addArguments("--disable-extensions");
        // Disable password manager via args only — setExperimentalOption("prefs", ...) can
        // cause Chromium to crash on startup inside seleniarm/standalone-chromium containers.
        opts.addArguments("--disable-features=PasswordManager,PasswordManagerOnboarding");
        opts.addArguments("--disable-save-password-bubble");
        opts.addArguments("--password-store=basic");
        return opts;
    }

    /** Returns the WebDriver for the current thread. */
    public WebDriver getDriver() {
        return driverHolder.get();
    }

    /** Quits the browser and cleans up the ThreadLocal to prevent memory leaks. */
    public void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            driver.quit();
            driverHolder.remove();
            log.info("Browser closed on thread [{}]", Thread.currentThread().getName());
        }
    }
}
