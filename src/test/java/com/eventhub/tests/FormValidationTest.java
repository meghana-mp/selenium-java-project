package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.EventDetailPage;
import com.eventhub.pages.EventsPage;
import com.eventhub.pages.RegisterPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.tests.dataproviders.TestDataProvider;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC_FORM_001, TC_FORM_002 — Form Validation
 *
 * Covers:
 *   - RegisterPage: real locators id="register-email", id="register-password",
 *     input[placeholder='Repeat your password'], id="register-btn"
 *   - EventDetailPage: button-based ticket quantity (span#ticket-count) with
 *     +/- buttons; verifies minimum boundary (qty cannot go below 1)
 *   - JSON DataProvider: invalidRegistrationData, boundaryQuantities
 */
@Epic("Form Validations")
@Feature("Input Constraints")
public class FormValidationTest extends BaseTest {

    /*
     * TC_FORM_001 — User Registration Inline Validation
     * Scenario: Submitting the registration form with invalid/empty fields must
     *           show inline validation errors without navigating away.
     * Steps:
     *   1. Navigate to /register.
     *   2. Enter invalid email (e.g. "test@com"), short password ("abc"),
     *      leave confirm-password blank.
     *   3. Click the Register button (id="register-btn").
     *   4. Assert inline errors appear (span[data-testid='field-error'] or toast).
     *   5. Assert the page URL still contains "register" (no navigation occurred).
     */
    @Test(
        groups        = { AppConstants.REGRESSION },
        dataProvider  = "invalidRegistrationData",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_FORM_001 — Registration form shows inline errors for invalid input"
    )
    @Story("TC_FORM_001")
    @Description("Validates real-time inline error messaging on the registration form.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_FORM_001_registrationInlineValidation(String email, String password, String phone) {
        long start = System.currentTimeMillis();

        Allure.step("Navigate to /register");
        navigateTo(AppConstants.PATH_REGISTER);
        RegisterPage registerPage = new RegisterPage(driver);

        Allure.step("Fill invalid data — email: " + email + ", password: " + password);
        registerPage.fillForm(null, null, email, password, phone);

        Allure.step("Submit the form to trigger validation");
        registerPage.clickRegister();

        Allure.step("Assert inline validation errors are visible");
        boolean hasErrors = registerPage.hasValidationErrors() || registerPage.isToastVisible();
        assertTrue(hasErrors,
                "Expected validation errors for email='" + email + "' password='" + password + "'");

        TestUtils.logResult("TC_FORM_001", true, System.currentTimeMillis() - start,
                "Email: " + email + " | Password: " + password + " | Errors: " + hasErrors);
    }

    /*
     * TC_FORM_002 — Ticket Quantity Boundary Values
     * Scenario: The ticket quantity on the event detail page is controlled by
     *           +/- buttons (not a free-text input). The quantity must never go
     *           below 1 (minimum boundary).
     * Steps:
     *   1. Login and open the first available event (/events → first card).
     *   2. Note the current quantity from span#ticket-count (starts at 1).
     *   3. Click the minus (−) button.
     *   4. Assert the displayed quantity is still 1 (minimum enforced)
     *      OR the minus button is disabled when qty == 1.
     *   5. Log the result of the boundary scenario.
     */
    @Test(
        groups        = { AppConstants.REGRESSION },
        dataProvider  = "boundaryQuantities",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_FORM_002 — Ticket quantity cannot go below 1 (boundary enforcement)"
    )
    @Story("TC_FORM_002")
    @Description("Validates that the − button enforces qty ≥ 1 on the event detail booking form.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_FORM_002_ticketQuantityBoundaryValues(int qty, String scenario) {
        long start = System.currentTimeMillis();

        Allure.step("Login and open first available event");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        Allure.step("Read initial quantity from span#ticket-count");
        int initial = detailPage.getDisplayedQuantity();
        Allure.step("Initial qty: " + initial + " | Scenario: " + scenario);

        Allure.step("Attempt to decrement below minimum");
        detailPage.clickMinusButton();

        Allure.step("Assert quantity did not drop below 1 OR minus button is disabled");
        int afterDecrement = detailPage.getDisplayedQuantity();
        boolean atMinimum   = afterDecrement >= 1;
        boolean btnDisabled = detailPage.isMinusButtonDisabled();

        assertTrue(atMinimum,
                "Scenario '" + scenario + "': qty after decrement=" + afterDecrement
                        + " — must remain ≥ 1");

        TestUtils.logResult("TC_FORM_002", true, System.currentTimeMillis() - start,
                "Scenario: " + scenario + " | Before: " + initial
                        + " | After: " + afterDecrement + " | BtnDisabled: " + btnDisabled);
    }
}
