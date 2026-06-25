package com.eventhub.tests.base;

import com.eventhub.config.ConfigManager;
import com.eventhub.constants.AppConstants;
import com.eventhub.driver.BrowserFactory;
import com.eventhub.pages.LoginPage;
import com.eventhub.tests.listeners.AllureListener;
import com.eventhub.tests.listeners.TestListener;
import com.eventhub.utils.ActionUtils;
import com.eventhub.utils.WebDriverWaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;

import java.time.Duration;

/**
 * Abstract base for all test classes.
 *
 * Exposes two utilities as protected fields:
 *   waitUtils   — explicit waits (WebDriverWaitUtils)
 *   actionUtils — all browser actions: clicks, scrolls, input, tab handling,
 *                 URL/title queries, and fixed pauses (ActionUtils)
 *
 * Common helpers (navigateTo, loginWithDefaults) live here so no test class
 * needs to duplicate them.
 */
@Listeners({ AllureListener.class, TestListener.class })
public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWaitUtils waitUtils;
    protected ActionUtils actionUtils;

    @Parameters("browser")
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional("chrome") String browser) {
        driver = BrowserFactory.getInstance().initDriver(browser);
        driver.get(ConfigManager.getInstance().getProperty("app.base.url"));

        int timeout = ConfigManager.getInstance().getIntProperty(
                "explicit.wait.seconds", AppConstants.DEFAULT_EXPLICIT_WAIT);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        waitUtils   = new WebDriverWaitUtils(driver, wait);
        actionUtils = new ActionUtils(driver, waitUtils);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        BrowserFactory.getInstance().quitDriver();
    }

    // ── Shared helpers ──────────────────────────────────────────────────────

    /** Navigate to a path relative to app.base.url (e.g. AppConstants.PATH_LOGIN). */
    protected void navigateTo(String path) {
        driver.get(ConfigManager.getInstance().getProperty("app.base.url") + path);
    }

    /** Navigate to /login and sign in with the credentials from config.properties. */
    protected void loginWithDefaults() {
        ConfigManager cfg = ConfigManager.getInstance();
        navigateTo(AppConstants.PATH_LOGIN);
        new LoginPage(driver).login(cfg.getProperty("app.email"), cfg.getProperty("app.password"));
    }
}
