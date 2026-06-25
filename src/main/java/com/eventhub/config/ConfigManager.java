package com.eventhub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thread-safe singleton that loads config.properties once and exposes typed accessors.
 * Environment variables override any property: app.base.url → APP_BASE_URL.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;
    private final Properties properties = new Properties();

    private ConfigManager() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            properties.load(is);
            log.debug("config.properties loaded successfully");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the property value. Environment variables take precedence.
     * Key dots are converted to underscores and uppercased for env lookup:
     * "app.base.url" → checks env var "APP_BASE_URL" first.
     */
    public String getProperty(String key) {
        String envKey = key.replace(".", "_").toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Property not found: " + key);
        }
        return value;
    }

    public String getProperty(String key, String defaultValue) {
        try {
            return getProperty(key);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
