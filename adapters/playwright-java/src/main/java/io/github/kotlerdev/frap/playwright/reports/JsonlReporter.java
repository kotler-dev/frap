package io.github.kotlerdev.frap.playwright.reports;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.kotlerdev.frap.core.context.ContextEvent;
import io.github.kotlerdev.frap.core.events.HealingEvent;

/**
 * Utility for writing JSONL (newline-delimited JSON) event files.
 */
public class JsonlReporter {
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Converts an object to JSON string (single line).
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Converts a ContextEvent to JSON string.
     */
    public static String toJson(ContextEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize context event", e);
        }
    }

    /**
     * Converts a HealingEvent to JSON string.
     */
    public static String toJson(HealingEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize healing event", e);
        }
    }
}
