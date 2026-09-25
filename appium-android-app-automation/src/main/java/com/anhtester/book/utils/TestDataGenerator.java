package com.anhtester.book.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Data unique + traceable theo format {@code [prefix]_[testName]_[timestamp]_[random]} — phần random
 * chống trùng khi 2 lần gọi (hoặc 2 device chạy song song) rơi vào cùng một mili giây.
 */
public final class TestDataGenerator {

    private TestDataGenerator() {
    }

    private static String stamp() {
        return System.currentTimeMillis() + "_" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000, 0x100000));
    }

    public static String email(String testName) {
        return username(testName) + "@auto.test";
    }

    public static String username(String testName) {
        return code("auto_" + testName);
    }

    public static String code(String prefix) {
        return prefix + "_" + stamp();
    }
}
