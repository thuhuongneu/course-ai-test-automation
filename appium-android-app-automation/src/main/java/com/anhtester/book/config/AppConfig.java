package com.anhtester.book.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Cấu hình tập trung của framework.
 *
 * <p>Thứ tự ưu tiên khi đọc một khoá: {@code -Dkhoá} (system property) → biến môi trường → file
 * {@code .env} ở gốc project → giá trị mặc định. Máy local điền tài khoản vào {@code .env}, CI truyền
 * qua biến môi trường (GitHub Secrets).
 */
public final class AppConfig {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private AppConfig() {
    }

    public static String appiumServerUrl() {
        return get("APPIUM_SERVER_URL", "http://127.0.0.1:4723");
    }

    public static long waitTimeoutSeconds() {
        return Long.parseLong(get("WAIT_TIMEOUT_SECONDS", "15"));
    }

    public static String testUsername() {
        return required("TEST_USERNAME");
    }

    public static String testPassword() {
        return required("TEST_PASSWORD");
    }

    private static String required(String key) {
        String value = get(key, "");
        if (value.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình " + key
                    + " — điền vào file .env (mẫu: .env.example) hoặc truyền qua biến môi trường.");
        }
        return value;
    }

    private static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if (value == null) {
            value = DOTENV.get(key);
        }
        return value != null ? value : defaultValue;
    }
}
