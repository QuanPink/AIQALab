package com.framework.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates random test data to avoid collisions in parallel runs.
 */
public class RandomDataUtils {

    private RandomDataUtils() {}

    public static String uniqueEmail(String prefix) {
        return prefix + "." + System.currentTimeMillis() + "@testframework.com";
    }

    public static String uniqueEmail() {
        return uniqueEmail("user");
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String randomName(String prefix) {
        return prefix + "_" + ThreadLocalRandom.current().nextInt(10_000, 99_999);
    }

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static String randomPhone() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(0, 100_000_000));
    }
}
