package com.jim.pos.base;

import com.jim.pos.config.ConfigReader;
import com.jim.pos.core.AxDriver;
import com.jim.pos.screens.LoginScreen;
import com.jim.pos.screens.SelectRegisterScreen;
import com.jim.pos.screens.UpdatePromptDialog;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Lop nen cho moi test class: quan ly vong doi app va driver, dua app ve man hinh Login truoc moi
 * test de cac test doc lap, khong phu thuoc thu tu chay.
 */
public class BaseTest {

  /** Ten tien trinh cua app - dung de kiem tra dang chay va de tat khi can khoi dong lai. */
  private static final String PROCESS_NAME = "ConnectPOS";

  private static final Duration APP_START_TIMEOUT = Duration.ofSeconds(60);
  private static final long PROCESS_POLL_MILLIS = 1000;

  /**
   * Driver song suot ca suite, KHONG tao lai theo tung test.
   *
   * <p>Ly do bat buoc: driver bat co screen-reader luc khoi dong va <b>tra co ve gia tri cu luc
   * thoat</b>. Neu moi test tao/dong driver rieng thi giua hai test co bi tat, trong khi app van
   * dang chay - ma Flutter chi doc co nay <b>luc khoi dong engine</b>. Hau qua: tu test thu hai tro
   * di, cay semantics khong con dam bao -> test do ngau nhien.
   */
  private static AxDriver sharedDriver;

  protected AxDriver driver;
  protected LoginScreen loginScreen;
  protected SelectRegisterScreen selectRegisterScreen;
  protected UpdatePromptDialog updatePrompt;

  @BeforeSuite(alwaysRun = true)
  public void startDriver() {
    sharedDriver = new AxDriver(ConfigReader.projectRoot());
    // JVM bi kill giua chung thi @AfterSuite khong chay - hook nay dam bao driver van duoc
    // dong tu te de co screen-reader duoc tra lai nhu cu.
    Runtime.getRuntime().addShutdownHook(new Thread(BaseTest::stopDriverQuietly));
  }

  @AfterSuite(alwaysRun = true)
  public void stopDriver() {
    stopDriverQuietly();
  }

  private static synchronized void stopDriverQuietly() {
    if (sharedDriver != null) {
      sharedDriver.close();
      sharedDriver = null;
    }
  }

  @BeforeMethod
  public void setUp() {
    driver = sharedDriver;
    loginScreen = new LoginScreen(driver);
    selectRegisterScreen = new SelectRegisterScreen(driver);
    updatePrompt = new UpdatePromptDialog(driver);

    if (!driver.isAppRunning()) {
      launchApp();
    }
    driver.focus();
    goToLoginScreen();
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown(ITestResult result) {
    attachScreenshot(result);
  }

  /**
   * Dua app ve man hinh Login du dang o trang thai nao: dang co hop thoai cap nhat, dang dang nhap,
   * hay dang ket o mot man hinh khac.
   */
  private void goToLoginScreen() {
    updatePrompt.declineIfPresent();
    if (loginScreen.isDisplayed()) {
      return;
    }
    if (selectRegisterScreen.hasLogoutButton()) {
      selectRegisterScreen.logout();
      updatePrompt.declineIfPresent();
      if (loginScreen.waitUntilDisplayed()) {
        return;
      }
    }
    // Khong nhan dien duoc man hinh dang o -> khoi dong lai app cho sach trang thai
    restartApp();
    updatePrompt.declineIfPresent();
    if (!loginScreen.waitUntilDisplayed()) {
      throw new IllegalStateException(
          "Khong dua duoc app ve man hinh Login. Cay element hien tai:\n" + driver.dump());
    }
  }

  private void launchApp() {
    Path exe = ConfigReader.appPath();
    if (!Files.exists(exe)) {
      throw new IllegalStateException("Khong thay file app tai: " + exe
          + " - kiem tra lai JIM_APP_PATH trong .env");
    }
    try {
      // PHAI vut bo stdout/stderr cua app: neu khong, app ghi log day buffer pipe (~64KB) ma
      // khong ai doc -> app tu treo giua chung.
      new ProcessBuilder(exe.toString())
          .directory(exe.getParent().toFile())
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
    } catch (IOException e) {
      throw new IllegalStateException("Khong khoi dong duoc app: " + exe, e);
    }
    long deadline = System.currentTimeMillis() + APP_START_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      if (driver.isAppRunning()) {
        driver.focus();
        return;
      }
      pollWait();
    }
    throw new IllegalStateException(
        "App khong khoi dong kip trong " + APP_START_TIMEOUT.getSeconds() + "s");
  }

  private void restartApp() {
    try {
      Process kill = new ProcessBuilder("taskkill", "/IM", PROCESS_NAME + ".exe", "/F")
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
      kill.waitFor();
      kill.destroy();
    } catch (IOException e) {
      throw new IllegalStateException("Khong tat duoc app dang chay", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Bi ngat khi tat app", e);
    }
    pollWait();
    launchApp();
  }

  /**
   * Nhip poll vong doi tien trinh OS. Day KHONG phai hard sleep trong luong test: moi phep cho tren
   * UI deu dung smart wait, day chi la khoang nghi giua hai lan hoi "app da chay chua" - thu khong
   * co API su kien de lang nghe.
   *
   * <p>Bi ngat thi thoat han chu khong nuot: nuot roi chay tiep se bien vong lap thanh busy-loop
   * (co interrupt dang bat lam Thread.sleep nem ngay lap tuc, khong ngu nua).
   */
  private void pollWait() {
    try {
      Thread.sleep(PROCESS_POLL_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Bi ngat khi cho app khoi dong", e);
    }
  }

  /**
   * Dinh kem anh chup man hinh vao Allure o cuoi MOI test, ca PASS lan FAIL.
   *
   * <p>App co the tu bat hop thoai "Do you want to update" sau khi dang nhap. Neu chup luc do thi
   * anh chi thay hop thoai, khong con la bang chung ve trang thai cuoi cua test - nen dong no truoc
   * khi chup, va ghi lai viec da dong de nguoi doc report biet.
   *
   * <p>Boc try-catch vi neu setUp da fail thi driver/app co the khong con dung duoc - khong duoc de
   * loi chup anh che mat loi test that su.
   */
  private void attachScreenshot(ITestResult result) {
    if (driver == null) {
      return;
    }
    Path file = null;
    try {
      if (updatePrompt.declineIfPresent()) {
        Allure.addAttachment("ghi_chu_moi_truong", "text/plain",
            "App bật hộp thoại 'Do you want to update' ở cuối test; đã chọn No trước khi chụp ảnh.");
      }
      boolean passed = result.getStatus() == ITestResult.SUCCESS;
      String name = passed ? "trang_thai_cuoi_cua_test" : "trang_thai_khi_that_bai";
      file = Files.createTempFile("jim-pos-", ".png");
      driver.screenshot(file);
      Allure.addAttachment(name, "image/png",
          new ByteArrayInputStream(Files.readAllBytes(file)), "png");
    } catch (IOException | RuntimeException e) {
      Allure.addAttachment("khong_chup_duoc_man_hinh", "text/plain", String.valueOf(e.getMessage()));
    } finally {
      if (file != null) {
        try {
          Files.deleteIfExists(file);
        } catch (IOException ignored) {
          // file tam - khong anh huong ket qua test
        }
      }
    }
  }
}
