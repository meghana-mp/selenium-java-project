package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.DashboardPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * TC_AUTH_002 — Session Expiration & Auto-Redirect
 *
 * Covers:
 *   - JavascriptExecutor: clears localStorage + sessionStorage
 *   - Protected-route guard: SPA redirects to /login when token is absent
 *   - DashboardPage.clearSession() deletes all cookies + JS storage
 */
@Epic("Authentication")
@Feature("Session Management")
public class SessionTest extends BaseTest {

    /*
     * TC_AUTH_002 — Session Expiration & Auto-Redirect to Login
     * Scenario: After a successful login, programmatically invalidate the session
     *           and navigate to a protected route — the app must redirect to /login.
     * Steps:
     *   1. Login with credentials from config.properties.
     *   2. Assert we are on the dashboard (precondition).
     *   3. Call DashboardPage.clearSession() — deletes all cookies + clears
     *      localStorage and sessionStorage via JavascriptExecutor.
     *   4. Navigate directly to /bookings (protected route).
     *   5. Assert the URL now contains "login" OR a session-expired toast is visible.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_AUTH_002 — Session expiration triggers auto-redirect to login"
    )
    @Story("TC_AUTH_002")
    @Description("Clears session programmatically and verifies the app redirects to /login.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_AUTH_002_sessionExpirationAutoRedirect() {
        long start = System.currentTimeMillis();

        Allure.step("Login with valid credentials");
        loginWithDefaults();
        DashboardPage dashboard = new DashboardPage(driver);

        Allure.step("Assert login succeeded (precondition)");
        assertTrue(dashboard.isOnDashboard(), "Login must succeed before session test");

        Allure.step("Programmatically clear all cookies and Web Storage");
        dashboard.clearSession();

        Allure.step("Navigate to protected route /bookings without a session");
        navigateTo(AppConstants.PATH_MY_BOOKINGS);

        // SPA auth guard is async — wait for URL to leave /bookings before asserting
        try {
            waitUtils.getWait().until(d ->
                    d.getCurrentUrl().contains("login")
                    || dashboard.isSessionExpiredMessageVisible());
        } catch (Exception ignored) {}

        Allure.step("Assert redirected to /login or session-expired message visible");
        boolean redirected = dashboard.isRedirectedToLogin();
        boolean sessionMsg = dashboard.isSessionExpiredMessageVisible();

        assertTrue(redirected || sessionMsg,
                "Expected redirect to /login or session-expired toast. URL: "
                        + actionUtils.getCurrentUrl());

        TestUtils.logResult("TC_AUTH_002", true, System.currentTimeMillis() - start,
                "Redirected: " + redirected + " | Toast: " + sessionMsg);
    }
}
