package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.DashboardPage;
import com.eventhub.pages.LoginPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.tests.dataproviders.TestDataProvider;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC_AUTH_001 — Authentication: Successful Login & Session Persistence
 *
 * Covers:
 *   - Page Object Model: LoginPage, DashboardPage
 *   - JSON DataProvider: validLoginData from auth-data.json
 *   - Allure annotations: @Epic, @Feature, @Story, @Severity
 *   - Session state verification via cookies
 */
@Epic("Authentication")
@Feature("Login")
public class LoginTest extends BaseTest {

    /*
     * TC_AUTH_001 — Successful Login & Session Persistence
     * Scenario: Verify that a user with valid credentials can log in and that
     *           the session is persisted (cookies present, navbar shows user email).
     * Steps:
     *   1. Navigate to /login.
     *   2. Enter valid email and password from auth-data.json via DataProvider.
     *   3. Click the Login button (id="login-btn").
     *   4. Assert URL is no longer on /login (landed on dashboard).
     *   5. Assert the navbar shows the logged-in user's email (id="user-email-display").
     *   6. Assert at least one session cookie is present in the browser.
     */
    // Retry-eligible (smoke group): RetryListener wires RetryAnalyzer automatically.
    // On failure retries up to 2 times — 1 s then 2 s back-off — before marking FAILED.
    // Static self-healing: DashboardPage.getDisplayedUserEmail() calls waitForVisibleWithFallback
    // with 3 locators — By.id("user-email-display") → [data-testid='user-email'] → nav span[class*='email'].
    // Falls through silently; no API call needed.
    @Test(
        groups        = { AppConstants.SMOKE, AppConstants.REGRESSION },
        dataProvider  = "validLoginData",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_AUTH_001 — Successful login and session persistence"
    )
    @Story("TC_AUTH_001")
    @Description("Validates successful login and verifies session state (cookies + navbar email).")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_AUTH_001_successfulLoginAndSessionPersistence(String email, String password) {
        long start = System.currentTimeMillis();

        Allure.step("Navigate to login page");
        navigateTo(AppConstants.PATH_LOGIN);
        LoginPage loginPage = new LoginPage(driver);

        Allure.step("Enter valid credentials and click Login");
        DashboardPage dashboard = loginPage.login(email, password);

        Allure.step("Assert URL changed away from /login (landed on dashboard)");
        assertTrue(dashboard.isOnDashboard(),
                "Expected to land on dashboard. URL: " + actionUtils.getCurrentUrl());

        Allure.step("Assert user email is visible in navbar");
        String displayedEmail = dashboard.getDisplayedUserEmail();
        assertFalse(displayedEmail.isBlank(),
                "Navbar email display (id=user-email-display) should not be blank after login");

        Allure.step("Assert session token is stored (localStorage-based auth)");
        assertTrue(dashboard.hasActiveSession(),
                "Auth token should be present in localStorage after successful login");

        TestUtils.logResult("TC_AUTH_001", true, System.currentTimeMillis() - start,
                "User: " + email + " | Email in navbar: " + displayedEmail);
    }
}
