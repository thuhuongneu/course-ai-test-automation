package com.connectpos.msi.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base class dùng chung cho mọi test class: khởi tạo/đóng WebDriver, đảm bảo mỗi test độc lập.
 * Headed mode mặc định (debug); truyền -Dheadless=true để chạy headless (CI/CD).
 */
public class BaseTest {

  protected WebDriver driver;

  @BeforeMethod
  public void setUp() {
    WebDriverManager.chromedriver().setup();

    ChromeOptions options = new ChromeOptions();
    boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));
    if (headless) {
      options.addArguments("--headless=new", "--window-size=1920,1080");
    }

    driver = new ChromeDriver(options);
    if (!headless) {
      // Viewport desktop bắt buộc khi debug headed, theo rules-selenium.md
      driver.manage().window().setSize(new Dimension(1920, 1080));
    }
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    attachScreenshot();
    if (driver != null) {
      driver.quit();
    }
  }

  /**
   * Đính kèm screenshot vào Allure report ở cuối mọi test, kể cả PASS lẫn FAIL.
   * Bọc try-catch vì nếu chính @BeforeMethod đã fail (vd. timeout mở trang), driver/session có thể
   * không còn dùng được nữa -> không được để lỗi chụp ảnh che mất lỗi test thật sự.
   */
  private void attachScreenshot() {
    if (driver == null) {
      return;
    }
    try {
      byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
      Allure.addAttachment("Screenshot cuối test", "image/png",
          new ByteArrayInputStream(screenshot), "png");
    } catch (WebDriverException e) {
      // Session đã chết (crash/timeout trước đó) -> bỏ qua, không chặn tearDown
    }
  }
}
