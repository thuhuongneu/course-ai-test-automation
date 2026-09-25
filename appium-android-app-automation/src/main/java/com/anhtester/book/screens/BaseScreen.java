package com.anhtester.book.screens;

import com.anhtester.book.config.AppConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Lớp nền cho mọi Screen Object — gom các thao tác dùng chung. Mọi phép chờ đều là smart wait
 * ({@link WebDriverWait}), không có {@code Thread.sleep}.
 */
public abstract class BaseScreen {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected BaseScreen(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(AppConfig.waitTimeoutSeconds()));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void tap(By locator) {
        log.info("Chạm vào {}", locator);
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /** Không ghi giá trị ra log — ô nhập có thể là mật khẩu. */
    protected void type(By locator, String value) {
        log.info("Nhập dữ liệu vào {}", locator);
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(value);
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText();
    }

    /** Chờ element hiện ra trong thời gian chờ mặc định; hết giờ thì trả {@code false}, không ném lỗi. */
    protected boolean isVisible(By locator) {
        try {
            waitForVisible(locator);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Chờ element biến mất khỏi màn hình; hết giờ mà vẫn còn thì trả {@code false}. */
    protected boolean isGone(By locator) {
        try {
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Cuộn tới element theo text — thay cho việc chạm mù vào vùng chưa hiển thị. */
    protected WebElement scrollToText(String text) {
        return driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().text(\"" + text + "\"))"));
    }
}
