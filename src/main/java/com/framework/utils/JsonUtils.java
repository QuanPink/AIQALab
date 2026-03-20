package com.framework.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Utility for reading JSON test-data files from the classpath.
 */
public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {}

    /** Deserialises a classpath JSON file into the given type. */
    public static <T> T fromFile(String classpathPath, Class<T> clazz) {
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new RuntimeException("Test data file not found on classpath: " + classpathPath);
            }
            return MAPPER.readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from " + classpathPath, e);
        }
    }

    /** Deserialises a classpath JSON array file into a {@code List<T>}. */
    public static <T> List<T> fromFileAsList(String classpathPath, Class<T> elementType) {
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new RuntimeException("Test data file not found on classpath: " + classpathPath);
            }
            return MAPPER.readValue(is,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON list from " + classpathPath, e);
        }
    }

    /** Serialises an object to a JSON string. */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialise object to JSON", e);
        }
    }

    /** Deserialises a JSON string into the given type. */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialise JSON string", e);
        }
    }
}
