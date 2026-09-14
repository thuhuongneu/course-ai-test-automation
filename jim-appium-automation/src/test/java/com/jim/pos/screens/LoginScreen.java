package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import io.qameta.allure.Step;
import java.time.Duration;

/**
 * Man hinh dang nhap cua app JIM POS.
 *
 * <p><b>Chien luoc locator</b> - da kiem chung truc tiep tren build STAG 26.09.14:
 *
 * <ul>
 *   <li>Nut LOGIN va cac thong bao validation dinh vi bang <b>ten</b>: ten cua chung co dinh.
 *   <li>O Username / Password <b>KHONG</b> dinh vi bang ten duoc. {@code accName} cua o nhap chinh
 *       la placeholder ("Username" / "Password") va <b>bien mat ngay khi o co noi dung hoac duoc
 *       focus</b>. Thay vao do dinh vi theo role {@code TEXT} + thu tu tren man hinh: o tren cung
 *       la Username, o duoi la Password.
 * </ul>
 */
public class LoginScreen {

  private static final String ROLE_INPUT = "TEXT";
  private static final int IDX_USERNAME = 0;
  private static final int IDX_PASSWORD = 1;

  private static final String BTN_LOGIN = "LOGIN";
  private static final String MSG_USERNAME_REQUIRED = "Username is required!";
  private static final String MSG_PASSWORD_REQUIRED = "Password is required";
  private static final String MSG_FILL_BOTH = "Please fill in your username and password to log in";

  private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(10);
  private static final long POLL_MILLIS = 400;

  private final AxDriver driver;
  private final UpdatePromptDialog updatePrompt;

  public LoginScreen(AxDriver driver) {
    this.driver = driver;
    this.updatePrompt = new UpdatePromptDialog(driver);
  }

  /**
   * App co the tu bat hop thoai "Do you want to update" vao <b>bat ky luc nao</b>. Khi no hien,
   * toan bo man hinh Login bien mat khoi cay semantics - moi phep tim element se tra ve rong va
   * test fail oan. Vi vay phai don no truoc moi thao tac.
   */
  private void dismissInterruptions() {
    updatePrompt.declineIfPresent();
  }

  /**
   * Thuc hien mot thao tac tren man hinh, chiu duoc viec hop thoai cap nhat chen vao dung khe giua
   * luc kiem tra va luc thao tac.
   *
   * <p>Chi don hop thoai mot lan truoc khi thao tac la chua du: no co the vua bat len ngay sau phep
   * kiem tra, lam element bien mat khoi cay va thao tac bao "khong tim thay". Gap loi do thi don
   * hop thoai lan nua roi lam lai - lan hai that bai moi la loi that.
   */
  private void withInterruptionRetry(Runnable action) {
    dismissInterruptions();
    try {
      action.run();
    } catch (AxDriver.AxException first) {
      dismissInterruptions();
      action.run();
    }
  }

  /** Man hinh Login da hien thi day du (co nut LOGIN va du 2 o nhap) chua. */
  public boolean isDisplayed() {
    dismissInterruptions();
    return driver.exists(BTN_LOGIN) && driver.findByRole(ROLE_INPUT).size() >= 2;
  }

  /**
   * Cho man hinh Login hien ra.
   *
   * <p>Ca hai dieu kien (nut LOGIN + du 2 o nhap) deu nam TRONG vong cho. De dieu kien thu hai ra
   * ngoai thi khi Flutter render dan - nut LOGIN xuat hien truoc hai o nhap vai tram mili giay -
   * ham se tra ve false ngay lap tuc du chi cho them chut nua la du.
   */
  public boolean waitUntilDisplayed() {
    long deadline = System.currentTimeMillis() + LOAD_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      if (isDisplayed()) {
        return true;
      }
      sleepBetweenPolls();
    }
    return false;
  }

  private void sleepBetweenPolls() {
    try {
      Thread.sleep(POLL_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Bi ngat khi cho man hinh Login", e);
    }
  }

  @Step("Nhập username")
  public LoginScreen enterUsername(String username) {
    withInterruptionRetry(() -> driver.fill(ROLE_INPUT, IDX_USERNAME, username));
    return this;
  }

  @Step("Nhập password")
  public LoginScreen enterPassword(String password) {
    withInterruptionRetry(() -> driver.fill(ROLE_INPUT, IDX_PASSWORD, password));
    return this;
  }

  @Step("Bấm nút LOGIN")
  public void clickLogin() {
    withInterruptionRetry(() -> driver.clickByName(BTN_LOGIN));
  }

  @Step("Đăng nhập")
  public void login(String username, String password) {
    enterUsername(username);
    enterPassword(password);
    clickLogin();
  }

  /** Gia tri dang hien trong o Username - dung de tu kiem chung da go dung chua. */
  public String readUsername() {
    dismissInterruptions();
    return driver.readValue(ROLE_INPUT, IDX_USERNAME);
  }

  /**
   * So ky tu dang co trong o Password.
   *
   * <p>O mat khau phoi ra chuoi dau cham tron (mot dau cho mot ky tu) chu khong phoi noi dung that,
   * nen kiem chung duoc <b>do dai</b> ma khong lam lo mat khau ra log hay report.
   */
  public int passwordLength() {
    dismissInterruptions();
    return driver.readValue(ROLE_INPUT, IDX_PASSWORD).length();
  }

  /** Van con dang o man hinh Login (dung cho cac ca kiem thu am). */
  public boolean isStillOnLoginScreen() {
    dismissInterruptions();
    return driver.exists(BTN_LOGIN);
  }

  public boolean isUsernameRequiredMessageShown() {
    dismissInterruptions();
    return driver.waitForName(MSG_USERNAME_REQUIRED, MESSAGE_TIMEOUT);
  }

  public boolean isPasswordRequiredMessageShown() {
    dismissInterruptions();
    return driver.waitForName(MSG_PASSWORD_REQUIRED, MESSAGE_TIMEOUT);
  }

  public boolean isFillBothFieldsMessageShown() {
    dismissInterruptions();
    return driver.waitForName(MSG_FILL_BOTH, MESSAGE_TIMEOUT);
  }
}
