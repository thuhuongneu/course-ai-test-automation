package com.connectpos.msi.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Đọc cấu hình (base URL, tài khoản test) từ file .env ở thư mục gốc project.
 * Không hardcode giá trị thật trong code — chỉ có default fallback để chạy được ngay khi chưa có .env.
 */
public class ConfigReader {

  private static final Dotenv dotenv = Dotenv.configure()
      .ignoreIfMissing()
      .load();

  private ConfigReader() {
  }

  public static String getBaseUrl() {
    return getValue("BASE_URL", "https://msi.dev.connectpos.com");
  }

  public static String getValidUsername() {
    return getValue("MSI_USERNAME", "msicore");
  }

  public static String getValidPassword() {
    return getValue("MSI_PASSWORD", "msicore123");
  }

  private static String getValue(String key, String defaultValue) {
    String value = dotenv.get(key);
    if (value == null || value.isBlank()) {
      value = System.getenv(key);
    }
    return (value == null || value.isBlank()) ? defaultValue : value;
  }
}
