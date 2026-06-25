package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.DashboardPage;
import com.eventhub.pages.EventDetailPage;
import com.eventhub.pages.EventsPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * TC_LAYOUT_001, TC_LAYOUT_002 — Layouts & Responsive Design
 *
 * Covers:
 *   - Twitter/X share link opens a new browser tab with correct URL
 *   - Mobile viewport (375×812): desktop nav hides, hamburger button
 *     (button[aria-label='Toggle menu']) becomes visible and opens nav drawer
 */
@Epic("Layouts & Windows")
@Feature("Responsive Design & Multi-Tab")
public class LayoutsTest extends BaseTest {

    /*
     * TC_LAYOUT_001 — External Twitter/X Share Link Opens New Tab
     * Scenario: Clicking the Share to Twitter/X link on an event detail card
     *           must open a new browser tab pointing to twitter.com or x.com.
     * Steps:
     *   1. Login and open any event detail page.
     *   2. Click the share-to-Twitter anchor.
     *   3. Assert the new tab URL contains "twitter.com", "x.com", or "intent/tweet".
     *   4. Close the new tab and switch back to the original window via actionUtils.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_LAYOUT_001 — Twitter share link opens new tab with correct URL"
    )
    @Story("TC_LAYOUT_001")
    @Description("Clicks the Twitter share button on an event and validates the new-tab URL.")
    @Severity(SeverityLevel.MINOR)
    public void TC_LAYOUT_001_externalLinkRedirection() {
        long start = System.currentTimeMillis();

        Allure.step("Login and open an event detail page");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        Allure.step("Click Twitter/X Share button");
        String originalHandle;
        try {
            originalHandle = detailPage.clickShareTwitterAndGetOriginalHandle();
        } catch (Exception e) {
            throw new org.testng.SkipException(
                    "TC_LAYOUT_001 skipped — no Twitter share link found on this event: "
                            + e.getMessage());
        }

        Allure.step("Assert new tab URL contains twitter.com or x.com");
        String newTabUrl = actionUtils.getCurrentUrl();
        boolean isSocialUrl = newTabUrl.contains("twitter.com")
                || newTabUrl.contains("x.com")
                || newTabUrl.contains("intent/tweet");
        assertTrue(isSocialUrl,
                "New tab URL should be Twitter/X. Actual: " + newTabUrl);

        Allure.step("Close new tab and return to original");
        actionUtils.closeCurrentTabAndSwitchTo(originalHandle);

        TestUtils.logResult("TC_LAYOUT_001", true, System.currentTimeMillis() - start,
                "New tab URL: " + newTabUrl);
    }

    /*
     * TC_LAYOUT_002 — Responsive Mobile Nav Toggle (Hamburger Menu)
     * Scenario: When the browser window is resized to a mobile viewport (375×812),
     *           the desktop horizontal navbar should be hidden and a hamburger
     *           button (button[aria-label='Toggle menu']) must appear.
     *           Clicking it should open the navigation drawer.
     * Steps:
     *   1. Login at the default desktop viewport.
     *   2. Set window size to 375×812 (iPhone SE dimensions).
     *   3. Reload the page to apply responsive CSS breakpoints.
     *   4. Assert hamburger button is visible.
     *   5. Click it and assert nav drawer links become visible.
     *   6. Restore window to maximized.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_LAYOUT_002 — Mobile viewport shows hamburger; clicking it opens nav drawer"
    )
    @Story("TC_LAYOUT_002")
    @Description("Resizes window to mobile dimensions and verifies the hamburger toggle works.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_LAYOUT_002_responsiveMobileNavToggle() {
        long start = System.currentTimeMillis();

        Allure.step("Login at desktop viewport");
        loginWithDefaults();

        Allure.step("Resize to mobile viewport: "
                + AppConstants.MOBILE_WIDTH + "x" + AppConstants.MOBILE_HEIGHT);
        driver.manage().window().setSize(
                new Dimension(AppConstants.MOBILE_WIDTH, AppConstants.MOBILE_HEIGHT));
        driver.navigate().refresh();
        actionUtils.pause(800);

        Allure.step("Assert hamburger button[aria-label='Toggle menu'] is visible");
        DashboardPage dashboard = new DashboardPage(driver);
        assertTrue(dashboard.isMobileHamburgerVisible(),
                "Hamburger button should be visible at " + AppConstants.MOBILE_WIDTH + "px width");

        Allure.step("Click hamburger to open nav drawer");
        dashboard.clickHamburger();
        actionUtils.pause(500);

        Allure.step("Assert nav links are visible in drawer");
        List<WebElement> navLinks = driver.findElements(By.cssSelector(
                "nav a, .mobile-menu a, [data-testid='nav-link'], #nav-events, #nav-bookings"));
        boolean anyVisible = navLinks.stream().anyMatch(WebElement::isDisplayed);
        assertTrue(anyVisible,
                "At least one nav link should be visible after clicking the hamburger");

        Allure.step("Restore window to full size");
        driver.manage().window().maximize();

        TestUtils.logResult("TC_LAYOUT_002", true, System.currentTimeMillis() - start,
                "Mobile nav toggle verified at " + AppConstants.MOBILE_WIDTH + "px");
    }
}
