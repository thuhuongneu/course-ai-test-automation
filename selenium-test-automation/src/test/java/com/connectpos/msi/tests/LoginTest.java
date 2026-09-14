package com.connectpos.msi.tests;

import com.connectpos.msi.base.BaseTest;
import com.connectpos.msi.config.ConfigReader;
import com.connectpos.msi.pages.LoginPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cho chức năng Login của MSI.
 * Nguồn requirement: Happy Path / Exception Path đã phân tích và verify trực tiếp trên UI thật
 * (redirect -> /product/list khi thành công; message lỗi chung khi sai/thiếu credential,
 * không giới hạn số lần đăng nhập sai).
 */
public class LoginTest extends BaseTest {

  private LoginPage loginPage;

  @BeforeMethod(dependsOnMethods = "setUp")
  public void openLoginPage() {
    loginPage = new LoginPage(driver);
    loginPage.open();
  }

  /** Gắn TC ID + Tags vào Allure report (không có @Tag annotation trong allure-testng, phải set qua label). */
  private void allureMeta(String testId, String... tags) {
    Allure.label("testId", testId);
    for (String tag : tags) {
      Allure.label("tag", tag);
    }
  }

  @Test(description = "Đăng nhập thành công với tài khoản hợp lệ - chuyển tới màn Product List",
      groups = {"login", "smoke"})
  @Description("Đăng nhập MSI với username/password hợp lệ, xác nhận hệ thống chuyển hướng đúng sang màn Product List")
  @Severity(SeverityLevel.BLOCKER)
  public void login_validCredentials_redirectToProductList() {
    allureMeta("MSI_LOGIN_TC_001", "login", "smoke");

    // Arrange: tài khoản test hợp lệ lấy từ .env, không hardcode trong code
    String username = ConfigReader.getValidUsername();
    String password = ConfigReader.getValidPassword();
    Allure.step("Arrange: chuẩn bị tài khoản hợp lệ (" + username + ") từ .env");

    // Act: đăng nhập với credential hợp lệ
    Allure.step("Act: đăng nhập với username/password hợp lệ", () -> loginPage.login(username, password));

    // Assert: phải chuyển hướng vào màn Product List
    Allure.step("Assert: phải chuyển hướng vào màn Product List", () -> {
      loginPage.waitForRedirectToProductList();
      Assert.assertTrue(driver.getCurrentUrl().contains("/product/list"),
          "Đăng nhập thành công phải chuyển hướng tới màn Product List");
    });
  }

  @Test(description = "Đăng nhập với password sai - hiển thị thông báo lỗi chung",
      groups = {"login", "negative"})
  @Description("Username đúng nhưng password sai; hệ thống phải từ chối đăng nhập và hiển thị thông báo lỗi chung, không phân biệt sai username hay password")
  @Severity(SeverityLevel.CRITICAL)
  public void login_invalidPassword_showError() {
    allureMeta("MSI_LOGIN_TC_002", "login", "negative");

    // Arrange: username đúng nhưng password sai
    String username = ConfigReader.getValidUsername();
    String wrongPassword = "wrong-password-" + System.currentTimeMillis();
    Allure.step("Arrange: chuẩn bị username hợp lệ + password sai");

    // Act
    Allure.step("Act: đăng nhập với password sai", () -> loginPage.login(username, wrongPassword));

    // Assert: hiển thị thông báo lỗi chung, không phân biệt sai username hay password
    Allure.step("Assert: phải hiển thị thông báo lỗi chung", () ->
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Phải hiển thị thông báo 'Username or password is incorrect' khi sai password"));
  }

  @Test(description = "Submit form đăng nhập khi để trống - hiển thị lỗi, không cho qua",
      groups = {"login", "negative"})
  @Description("Submit form với username/password trống (không có validation phía client); server phải trả lỗi và giữ nguyên trên trang login")
  @Severity(SeverityLevel.NORMAL)
  public void login_emptyCredentials_showError() {
    allureMeta("MSI_LOGIN_TC_003", "login", "negative");

    // Arrange: không nhập gì vào username/password
    Allure.step("Arrange: để trống username/password");

    // Act: submit trực tiếp
    Allure.step("Act: submit form khi đang trống", loginPage::clickLogin);

    // Assert: hệ thống không chặn client-side, server trả về cùng thông báo lỗi chung
    Allure.step("Assert: hiển thị lỗi và ở lại trang login", () -> {
      Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
          "Submit với field trống phải hiển thị thông báo lỗi, không được đăng nhập");
      Assert.assertTrue(loginPage.isStillOnLoginPage(),
          "Submit với field trống phải ở lại trang login");
    });
  }
}
