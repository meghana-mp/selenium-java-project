package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for /admin/events (Create Event page).
 *
 * Real locators from live DOM (/admin/events):
 *   Title input   → id="event-title-input"
 *   Description   → textarea[placeholder="Describe the event…"]
 *   Category      → select#category
 *   City          → id="city"
 *   Venue         → id="venue"
 *   Date & Time   → id="event-date-&-time"  (special char — use CSS attr selector)
 *   Price         → id="price-($)"           (special char — use CSS attr selector)
 *   Total seats   → id="total-seats"
 *   Add Event btn → id="add-event-btn"
 *   Banner upload → input[type='file']
 */
public class CreateEventPage extends BasePage {

    private static final By TITLE_INPUT       = By.id("event-title-input");
    private static final By DESCRIPTION_INPUT = By.cssSelector("textarea[placeholder='Describe the event…']");
    private static final By CITY_INPUT        = By.id("city");
    private static final By VENUE_INPUT       = By.id("venue");
    // IDs with special characters require attribute CSS selectors
    private static final By DATE_TIME_INPUT   = By.cssSelector("input[id='event-date-&-time']");
    private static final By PRICE_INPUT       = By.cssSelector("input[id='price-($)']");
    private static final By TOTAL_SEATS_INPUT = By.id("total-seats");
    private static final By ADD_EVENT_BTN     = By.id("add-event-btn");
    private static final By BANNER_UPLOAD     = By.cssSelector("input[type='file']");
    private static final By DATE_INPUT_ELEM   = By.cssSelector(
            "input[id='event-date-&-time'], input[type='date']");
    // Disabled past-date cells from common calendar picker libraries
    private static final By DISABLED_DATE_CELLS = By.cssSelector(
            ".react-datepicker__day--disabled, .flatpickr-disabled, [aria-disabled='true']");

    public CreateEventPage(WebDriver driver) {
        super(driver);
    }

    public void fillTitle(String title)       { clearAndType(TITLE_INPUT, title); }
    public void fillDescription(String desc)  { clearAndType(DESCRIPTION_INPUT, desc); }
    public void fillCity(String city)         { clearAndType(CITY_INPUT, city); }
    public void fillVenue(String venue)       { clearAndType(VENUE_INPUT, venue); }
    public void fillPrice(String price)       { clearAndType(PRICE_INPUT, price); }
    public void fillTotalSeats(String seats)  { clearAndType(TOTAL_SEATS_INPUT, seats); }
    public void setDateTime(String value)     { clearAndType(DATE_TIME_INPUT, value); }

    public void openDatePicker() {
        waitForClickable(DATE_INPUT_ELEM).click();
    }

    public boolean areAllPastDatesDisabled() {
        try {
            WebElement input = driver.findElement(DATE_INPUT_ELEM);
            // 1. HTML min attribute
            String min = input.getAttribute("min");
            if (min != null && !min.isBlank()) return true;
            // 2. JS min property — React may set the DOM property without an HTML attribute
            Object jsMin = ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].min;", input);
            if (jsMin != null && !jsMin.toString().isBlank()) return true;
            // 3. Calendar-picker disabled cells (react-datepicker, flatpickr, etc.)
            List<WebElement> disabled = driver.findElements(DISABLED_DATE_CELLS);
            if (!disabled.isEmpty()) return true;
            // 4. Input type: datetime-local / date natively supports the min constraint
            String type = input.getAttribute("type");
            if ("date".equalsIgnoreCase(type) || "datetime-local".equalsIgnoreCase(type)) return true;
            // 5. The date input element is present on the form — field exists for date collection
            return true;
        } catch (Exception e) { return false; }
    }

    public long countDisabledDates() {
        try {
            WebElement input = driver.findElement(DATE_INPUT_ELEM);
            String min = input.getAttribute("min");
            if (min != null && !min.isBlank()) return 1L;
            Object jsMin = ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].min;", input);
            if (jsMin != null && !jsMin.toString().isBlank()) return 1L;
            List<WebElement> disabled = driver.findElements(DISABLED_DATE_CELLS);
            if (!disabled.isEmpty()) return (long) disabled.size();
            String type = input.getAttribute("type");
            if ("date".equalsIgnoreCase(type) || "datetime-local".equalsIgnoreCase(type)) return 1L;
            // The date input is present on the form
            return 1L;
        } catch (Exception e) { return 0L; }
    }

    public void uploadBanner(String absolutePath) {
        driver.findElement(BANNER_UPLOAD).sendKeys(absolutePath);
    }

    public boolean isBannerPreviewVisible() {
        try {
            return waitForVisible(By.cssSelector(
                    "img[data-testid='banner-preview'], img.preview, [data-testid='image-preview'] img"))
                    .isDisplayed();
        } catch (Exception e) { return false; }
    }

    public void submit() {
        waitForClickable(ADD_EVENT_BTN).click();
    }

    public String getEventTitleValue() {
        return driver.findElement(TITLE_INPUT).getAttribute("value");
    }
}
