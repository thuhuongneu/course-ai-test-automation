package com.anhtester.book.drivers;

import com.anhtester.book.config.AppConfig;
import io.appium.java_client.android.AndroidDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Quản lý AndroidDriver theo thread.
 *
 * <p>Parallel luôn bật (mỗi device một thread) nên driver KHÔNG được là biến static dùng chung:
 * {@link ThreadLocal} cho mỗi thread một driver riêng, và phải {@code remove()} khi đóng — không thì
 * thread chạy test sau sẽ nhận lại driver đã chết.
 */
public final class AppiumDriverFactory {

    private static final Logger LOG = LogManager.getLogger(AppiumDriverFactory.class);
    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();

    private AppiumDriverFactory() {
    }

    public static void initDriver(String deviceKey) {
        URL serverUrl = appiumServerUrl();
        LOG.info("Mở phiên Appium cho device '{}' tại {}", deviceKey, serverUrl);
        DRIVER.set(new AndroidDriver(serverUrl, CapabilitiesManager.load(deviceKey)));
    }

    /** Driver của thread hiện tại — {@code null} nếu chưa mở phiên hoặc mở phiên thất bại. */
    public static AndroidDriver getDriver() {
        return DRIVER.get();
    }

    public static void quitDriver() {
        AndroidDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (RuntimeException e) {
            // Phiên đã chết phía server (Appium restart, hết newCommandTimeout) — không để teardown hỏng làm skip các test sau
            LOG.warn("Không đóng được phiên Appium: {}", e.getMessage());
        } finally {
            DRIVER.remove();
        }
    }

    private static URL appiumServerUrl() {
        String url = AppConfig.appiumServerUrl();
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("APPIUM_SERVER_URL không hợp lệ: " + url, e);
        }
    }
}
