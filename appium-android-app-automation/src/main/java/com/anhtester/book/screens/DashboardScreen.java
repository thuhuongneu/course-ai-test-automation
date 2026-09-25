package com.anhtester.book.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Màn hình đầu tiên khi mở app (Dashboard).
 *
 * <p>App hybrid: giao diện nằm trong một WebView không bật debug, nên chỉ có context NATIVE_APP.
 * Locator lấy từ cây accessibility mà WebView đưa ra, không dùng được CSS.
 */
public class DashboardScreen extends BaseScreen {

    private final By bookManagementSignInCard = AppiumBy.accessibilityId("Book management sign in");

    public DashboardScreen(AndroidDriver driver) {
        super(driver);
    }

    @Step("Mở màn Sign in qua thẻ 'Book management sign in'")
    public LoginScreen openSignIn() {
        tap(bookManagementSignInCard);
        return new LoginScreen(driver);
    }
}
