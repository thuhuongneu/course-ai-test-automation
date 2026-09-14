package com.jim.pos.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Doc cau hinh tu file {@code .env} o goc project.
 *
 * <p>Credentials va duong dan build KHONG duoc hardcode trong code hay ghi vao tai lieu -
 * {@code .env} da nam trong {@code .gitignore}.
 */
public final class ConfigReader {

  private static final Dotenv DOTENV = Dotenv.configure()
      .directory(projectRoot().toString())
      .ignoreIfMissing()
      .load();

  private ConfigReader() {
  }

  /** Goc project - noi chua pom.xml, .env va thu muc driver/. */
  public static Path projectRoot() {
    return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
  }

  public static Path appPath() {
    return Paths.get(require("JIM_APP_PATH"));
  }

  public static String username() {
    return require("JIM_USERNAME");
  }

  public static String password() {
    return require("JIM_PASSWORD");
  }

  private static String require(String key) {
    String value = DOTENV.get(key);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalStateException(
          "Thieu '" + key + "' trong .env - hay sao chep .env.example thanh .env va dien gia tri");
    }
    return value.trim();
  }
}
