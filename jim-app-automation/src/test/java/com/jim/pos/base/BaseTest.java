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
import java.nio.file.Paths;
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
  /** Thoi gian cho app dung xong giao dien sau khi tien trinh da chay - xem waitUntilUiReady(). */
  private static final Duration UI_READY_TIMEOUT = Duration.ofSeconds(60);
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

    ensureCorrectBuildRunning();
    driver.focus();
    goToLoginScreen();
  }

  /**
   * Dam bao app dang chay ĐÚNG ban build khai trong {@code JIM_APP_PATH}.
   *
   * <p>Trước đây chỉ kiểm tra "app có đang chạy không" (theo tên tiến trình) - nếu một bản
   * build KHÁC đã mở sẵn trên máy (từ phiên trước, hoặc do người dùng tự mở tay), code sẽ
   * tưởng app đã sẵn sàng và bỏ qua {@code JIM_APP_PATH} hoàn toàn, dùng nhầm bản đang mở thay
   * vì bản khai trong {@code .env}. So sánh đường dẫn {@code .exe} thật để phát hiện đúng
   * trường hợp này.
   */
  private void ensureCorrectBuildRunning() {
    Path expected = ConfigReader.appPath().toAbsolutePath().normalize();
    String runningRaw = driver.runningExePath();

    if (runningRaw == null) {
      launchApp();
      return;
    }

    Path running = Paths.get(runningRaw).toAbsolutePath().normalize();
    if (running.equals(expected)) {
      return;
    }

    Allure.addAttachment("canh_bao_sai_ban_build", "text/plain",
        "App dang chay ban '" + running + "' nhung .env khai '" + expected
            + "' - dang dong ban cu va mo dung ban trong .env.");
    restartApp();
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown(ITestResult result) {
    attachScreenshot(result);
  }

  /** So lan toi da thu logout khi hop thoai cap nhat cu chen vao lam mot lan logout khong toi dich. */
  private static final int LOGOUT_ATTEMPTS = 3;

  /**
   * Dua app ve man hinh Login du dang o trang thai nao: dang co hop thoai cap nhat, dang dang nhap,
   * hay dang ket o mot man hinh khac (ke ca da di sau hon Select Register, vd da chon mot quay cu
   * the).
   *
   * <p><b>QUAN TRONG:</b> {@code restartApp()} (tat tien trinh roi mo lai) KHONG dua duoc app ve
   * man hinh Login - da kiem chung tren app that: app tu nho phien dang nhap cuc bo, nen khoi dong
   * lai chi dua no ve LAI man Select Register, khong ve Login. Vi vay logout() la con duong DUY
   * NHAT ve Login, va phai thu lai NHIEU LAN (khong chi mot lan): hop thoai cap nhat co the chen
   * vao dung lan thu, lam logout khong toi dich du ban than logout khong loi.
   *
   * <p>{@code restartApp()} chi dung khi khong nhan dien duoc man hinh dang o (khong phai Login,
   * khong co nut Logout) - de dua app ve mot trang thai da biet (Select Register) truoc khi bat
   * dau vong lap logout.
   */
  private void goToLoginScreen() {
    updatePrompt.declineIfPresent();
    if (loginScreen.isDisplayed()) {
      stabilizeLoginScreen();
      return;
    }
    if (!selectRegisterScreen.hasLogoutButton()) {
      // Man hinh khong xac dinh duoc (vd da di sau hon Select Register) - dua ve trang thai
      // da biet truoc khi thu logout.
      restartApp();
      updatePrompt.declineIfPresent();
    }
    for (int attempt = 1; attempt <= LOGOUT_ATTEMPTS; attempt++) {
      if (loginScreen.isDisplayed()) {
        stabilizeLoginScreen();
        return;
      }
      if (!selectRegisterScreen.hasLogoutButton()) {
        break;
      }
      selectRegisterScreen.logout();
      updatePrompt.declineIfPresent();
      if (loginScreen.waitUntilDisplayed()) {
        stabilizeLoginScreen();
        return;
      }
    }
    throw new IllegalStateException(
        "Khong dua duoc app ve man hinh Login sau " + LOGOUT_ATTEMPTS
            + " lan thu logout. Cay element hien tai:\n" + driver.dump());
  }

  private static final Duration STABILIZE_WINDOW = Duration.ofSeconds(3);
  private static final long STABILIZE_POLL_MILLIS = 300;

  /**
   * Man hinh Login vua mount xong co the tu bat hop thoai cap nhat NGAY SAU DO - da do dac thuc
   * te: xuat hien trong vong 1-2s dau, khong lap lai neu bi dong som.
   *
   * <p>Neu khong don no o day, thao tac dau tien cua test (enterUsername("")/enterPassword(""))
   * co the "thanh cong GIA": o nhap VON DI da trong san ngay luc vua mount, nen kiem chung do dai
   * == 0 se khop MA KHONG PHAI do fill() thuc su xoa dung - trong khi thao tac click/backspace
   * that su bi dialog che mat, khong toi dich. Day la loi AM THAM nguy hiem hon exception: khong
   * co gi de bat, test cu the ma khang dinh sai vi ly do sai.
   *
   * <p>Vi vay chu dong "vet het" khung thoi gian dialog co the bat len truoc khi tra quyen cho
   * test, thay vi chi don dialog o thoi diem kiem tra (mot lan) roi tin la an toan.
   */
  private void stabilizeLoginScreen() {
    long deadline = System.currentTimeMillis() + STABILIZE_WINDOW.toMillis();
    while (System.currentTimeMillis() < deadline) {
      updatePrompt.declineIfPresent();
      try {
        Thread.sleep(STABILIZE_POLL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Bi ngat khi doi man hinh Login on dinh", e);
      }
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
        waitUntilUiReady();
        return;
      }
      pollWait();
    }
    throw new IllegalStateException(
        "App khong khoi dong kip trong " + APP_START_TIMEOUT.getSeconds() + "s");
  }

  /**
   * Cho den khi app dung xong giao dien, KHONG chi den khi tien trinh song.
   *
   * <p>Tien trinh bao "alive" gan nhu ngay lap tuc, nhung Flutter con mat vai giay nua moi dung
   * xong man hinh dau tien - trong khoang do cay semantics RONG. Neu tra quyen dieu khien ngay
   * luc do, moi thao tac tiep theo deu that bai tren mot cay rong ("khong tim thay element"),
   * va {@link #goToLoginScreen()} se dot het so lan thu logout chi trong vai giay roi bao loi.
   *
   * <p>Het thoi gian cho thi KHONG nem loi: de buoc sau tu bao loi kem cay element thuc te, huu
   * ich hon la mot loi timeout chung chung o day.
   */
  private void waitUntilUiReady() {
    long deadline = System.currentTimeMillis() + UI_READY_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      updatePrompt.declineIfPresent();
      if (loginScreen.isDisplayed() || selectRegisterScreen.hasLogoutButton()) {
        return;
      }
      pollWait();
    }
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

  /** So lan toi da thu chup lai anh khi hop thoai cap nhat dinh vao dung luc chup. */
  private static final int SCREENSHOT_ATTEMPTS = 3;

  /**
   * Dinh kem anh chup man hinh vao Allure o cuoi MOI test, ca PASS lan FAIL.
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
      file = captureCleanScreenshot();
      boolean passed = result.getStatus() == ITestResult.SUCCESS;
      String name = passed ? "trang_thai_cuoi_cua_test" : "trang_thai_khi_that_bai";
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

  /**
   * Chup anh man hinh, dam bao khong dinh phai hop thoai "Do you want to update".
   *
   * <p>Chi don hop thoai MOT LAN truoc khi chup la chua du: {@code Ax.Screenshot} phia driver can
   * ~250ms de dua cua so len foreground truoc khi chup pixel, va hop thoai co the bat len dung vao
   * khoang tre do. Nen chup xong phai KIEM TRA LAI - con dialog thi coi nhu anh chua sach, don roi
   * chup lai, toi da {@link #SCREENSHOT_ATTEMPTS} lan.
   */
  private Path captureCleanScreenshot() throws IOException {
    Path file = Files.createTempFile("jim-pos-", ".png");
    for (int attempt = 1; attempt <= SCREENSHOT_ATTEMPTS; attempt++) {
      updatePrompt.declineIfPresent();
      driver.screenshot(file);
      if (!updatePrompt.isPresent()) {
        return file;
      }
    }
    Allure.addAttachment("ghi_chu_moi_truong", "text/plain",
        "Hộp thoại 'Do you want to update' cứ tái xuất hiện đúng lúc chụp ảnh, đã thử lại "
            + SCREENSHOT_ATTEMPTS + " lần - ảnh đính kèm có thể vẫn dính hộp thoại này.");
    return file;
  }
}
