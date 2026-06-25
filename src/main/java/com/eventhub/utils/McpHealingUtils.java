package com.eventhub.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic self-healing via Claude API (MCP-style).
 *
 * When every pre-written locator in waitForVisibleWithMcpHealing fails, this
 * utility sends the current page source to Claude and asks it to derive a
 * working CSS selector or XPath for the described element.
 *
 * Requirements:
 *   Set env var ANTHROPIC_API_KEY before running tests.
 *   Without it the method returns null and the caller re-throws the original exception.
 *
 * Design decisions:
 *   - Uses claude-haiku (fast, cheap) — locator finding is a simple structured task.
 *   - Strips <head> from page source to keep the prompt focused on visible DOM.
 *   - Caches healed locators by description so the same element is not re-queried
 *     across multiple test methods in the same JVM run.
 *   - No new Maven dependencies — uses java.net.http.HttpClient (Java 11+)
 *     and Jackson which is already on the classpath.
 */
public class McpHealingUtils {

    private static final Logger log = LoggerFactory.getLogger(McpHealingUtils.class);

    private static final String API_URL        = "https://api.anthropic.com/v1/messages";
    private static final String MODEL          = "claude-haiku-4-5-20251001";
    private static final int    MAX_BODY_CHARS = 6000;
    private static final int    MAX_TOKENS     = 120;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Avoids duplicate API calls when multiple tests look for the same element description.
    private static final ConcurrentHashMap<String, String> LOCATOR_CACHE = new ConcurrentHashMap<>();

    private McpHealingUtils() {}

    /**
     * Asks Claude to find a CSS selector or XPath for {@code elementDescription} using
     * the current page DOM. Returns null (without throwing) when:
     *   - ANTHROPIC_API_KEY is not set
     *   - The API call fails for any reason
     *   - Claude returns an unparseable response
     */
    public static By healLocator(WebDriver driver, String elementDescription) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[MCP Healing] ANTHROPIC_API_KEY not set — skipping dynamic heal for '{}'",
                    elementDescription);
            return null;
        }

        // Return cached result from a previous heal in this JVM run
        String cached = LOCATOR_CACHE.get(elementDescription);
        if (cached != null) {
            log.info("[MCP Healing] Cache hit for '{}' → {}", elementDescription, cached);
            return parseLocator(cached);
        }

        String body = extractBody(driver.getPageSource());
        String prompt = buildPrompt(elementDescription, body);

        try {
            String raw = callClaude(apiKey, prompt);
            if (raw != null && !raw.isBlank()) {
                LOCATOR_CACHE.put(elementDescription, raw.trim());
                log.warn("[MCP Healing] Claude returned '{}' for element '{}'",
                        raw.trim(), elementDescription);
                return parseLocator(raw.trim());
            }
        } catch (Exception e) {
            log.error("[MCP Healing] API call failed for '{}' — {}: {}",
                    elementDescription, e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private static String buildPrompt(String description, String bodyHtml) {
        return "You are a Selenium automation expert.\n"
                + "Element to find: \"" + description + "\"\n\n"
                + "Page HTML (body only):\n" + bodyHtml + "\n\n"
                + "Reply with ONLY one line — no explanation, no markdown:\n"
                + "CSS: <css-selector>   OR   XPATH: <xpath-expression>\n"
                + "Pick the most stable locator (prefer id, data-testid, then structural CSS).";
    }

    private static String callClaude(String apiKey, String prompt) throws Exception {
        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);

        ArrayNode messages = MAPPER.createArrayNode();
        messages.add(message);

        ObjectNode requestBody = MAPPER.createObjectNode();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.set("messages", messages);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[MCP Healing] Claude API {} — {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode root    = MAPPER.readTree(response.body());
        JsonNode content = root.path("content");
        if (content.isArray() && content.size() > 0) {
            return content.get(0).path("text").asText(null);
        }
        return null;
    }

    private static By parseLocator(String line) {
        if (line == null || line.isBlank()) return null;
        String s = line.trim();
        if (s.startsWith("CSS:")) {
            String selector = s.substring(4).trim();
            log.debug("[MCP Healing] Parsed CSS selector: {}", selector);
            return By.cssSelector(selector);
        }
        if (s.startsWith("XPATH:")) {
            String xpath = s.substring(6).trim();
            log.debug("[MCP Healing] Parsed XPath: {}", xpath);
            return By.xpath(xpath);
        }
        log.warn("[MCP Healing] Response not in expected format: '{}'", s);
        return null;
    }

    /** Strips <head> so the prompt focuses on visible DOM; truncates to MAX_BODY_CHARS. */
    private static String extractBody(String pageSource) {
        if (pageSource == null) return "";
        int bodyStart = pageSource.indexOf("<body");
        String body = bodyStart >= 0 ? pageSource.substring(bodyStart) : pageSource;
        return body.length() <= MAX_BODY_CHARS ? body : body.substring(0, MAX_BODY_CHARS);
    }
}
