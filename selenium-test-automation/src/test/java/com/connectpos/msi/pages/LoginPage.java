package com.connectpos.msi.pages;

import com.connectpos.msi.config.ConfigReader;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object cho màn Login của MSI (Multi Source Inventory).
 * Locator được verify trực tiếp trên DOM thật (không đoán):
 * - username/password có id ổn định.
 * - nút Login/checkbox không có id/data-testid (MUI sinh class hash) -> dùng cssSelector theo attribute.
 * - error message không có id -> dùng xpath theo đúng text hiển thị thật.
 */
public class LoginPage {

  private static final By USERNAME_INPUT = By.id("user-name");
  private static final By PASSWORD_INPUT = By.id("password");
  private static final By TOGGLE_PASSWORD_VISIBILITY = By.cssSelector("button[aria-label='toggle password visibility']");
  private static final By REMEMBER_ME_CHECKBOX = By.cssSelector("form input[type='checkbox']");
  private static final By LOGIN_BUTTON = By.cssSelector("form button[type='submit']");
  private static final By ERROR_MESSAGE = By.xpath("//div[text()='Username or password is incorrect. Please try again!']");

  private final WebDriver driver;
  private final WebDriverWait wait;
  private final WebDriverWait pageLoadWait;

  public LoginPage(WebDriver driver) {
    this.driver = driver;
    // 30s vì tuỳ máy/mạng, request tới MSI (server dev nội bộ) có thể chậm hơn đáng kể so với môi
    // trường build — kể cả sau khi trang đã load, request đăng nhập/hiển thị lỗi vẫn cần margin rộng.
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    // Riêng lần mở trang: profile Chrome mới tinh (không cache), MSI lại là SPA nặng (~190 request
    // tài nguyên tĩnh) nên lần load đầu tiên chậm hơn hẳn các thao tác sau đó -> cần wait dài hơn.
    this.pageLoadWait = new WebDriverWait(driver, Duration.ofSeconds(60));
  }

  /** Mở trang login và chờ form hiển thị xong. */
  public void open() {
    driver.get(ConfigReader.getBaseUrl() + "/login");
    pageLoadWait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_INPUT));
  }

  public void enterUsername(String username) {
    WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_INPUT));
    input.clear();
    input.sendKeys(username);
  }

  public void enterPassword(String password) {
    WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));
    input.clear();
    input.sendKeys(password);
  }

  public void togglePasswordVisibility() {
    wait.until(ExpectedConditions.elementToBeClickable(TOGGLE_PASSWORD_VISIBILITY)).click();
  }

  public void toggleRememberMe() {
    wait.until(ExpectedConditions.elementToBeClickable(REMEMBER_ME_CHECKBOX)).click();
  }

  public void clickLogin() {
    wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON)).click();
  }

  /** Điền username + password rồi submit — dùng cho Happy Path lẫn Negative Path. */
  public void login(String username, String password) {
    enterUsername(username);
    enterPassword(password);
    clickLogin();
  }

  /** Chờ redirect sang màn Product List sau khi đăng nhập thành công. */
  public void waitForRedirectToProductList() {
    wait.until(ExpectedConditions.urlContains("/product/list"));
  }

  public boolean isErrorMessageDisplayed() {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE)).isDisplayed();
  }

  public boolean isStillOnLoginPage() {
    wait.until(ExpectedConditions.urlContains("/login"));
    return driver.findElement(USERNAME_INPUT).isDisplayed();
  }
}
