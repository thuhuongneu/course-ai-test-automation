package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import io.qameta.allure.Step;
import java.time.Duration;

/**
 * Man hinh chon quay (Select Register) - man hinh dau tien sau khi dang nhap thanh cong.
 */
public class SelectRegisterScreen {

  private static final String TITLE = "Select Register";
  private static final String BTN_LOGOUT = "Logout";
  private static final Duration LOGIN_TIMEOUT = Duration.ofSeconds(60);
  private static final long POLL_MILLIS = 500;

  private final AxDriver driver;
  private final UpdatePromptDialog updatePrompt;

  public SelectRegisterScreen(AxDriver driver) {
    this.driver = driver;
    this.updatePrompt = new UpdatePromptDialog(driver);
  }

  public boolean isDisplayed() {
    return driver.exists(TITLE);
  }

  /** Cho man hinh Select Register xuat hien sau khi bam LOGIN (thoi gian cho mac dinh). */
  public boolean waitUntilDisplayed() {
    return appearsWithin(LOGIN_TIMEOUT);
  }

  /**
   * Man hinh Select Register co xuat hien trong khoang {@code timeout} khong.
   *
   * <p>Vong lap tu xu ly hop thoai cap nhat: app co the bat no len ngay sau khi dang nhap, va luc
   * do man hinh phia sau bien mat khoi cay semantics - khong xu ly thi phep cho se timeout oan.
   *
   * <p>Dung thoi gian cho ngan cho cac ca kiem thu am, de khong phai doi het thoi gian mac dinh
   * chi de chung minh man hinh KHONG xuat hien.
   */
  public boolean appearsWithin(Duration timeout) {
    long deadline = System.currentTimeMillis() + timeout.toMillis();
    while (System.currentTimeMillis() < deadline) {
      if (isDisplayed()) {
        return true;
      }
      if (!updatePrompt.declineIfPresent()) {
        sleepBetweenPolls();
      }
    }
    return false;
  }

  /**
   * Con nut Logout tuc la phien dang nhap dang hoat dong.
   *
   * <p>Don hop thoai cap nhat truoc khi doc: khi no hien, man hinh phia sau bien mat khoi cay
   * semantics nen se tuong nham la khong co nut Logout.
   */
  public boolean hasLogoutButton() {
    updatePrompt.declineIfPresent();
    return driver.exists(BTN_LOGOUT);
  }

  @Step("Đăng xuất")
  public void logout() {
    driver.clickByName(BTN_LOGOUT);
  }

  /**
   * Nhip poll giua hai lan doc cay semantics. Khong phai hard sleep kieu "cho cho chac": vong lap
   * thoat ngay khi dieu kien dung, day chi la khoang nghi de khong quay lien tuc mot API COM.
   */
  private void sleepBetweenPolls() {
    try {
      Thread.sleep(POLL_MILLIS);
    } catch (InterruptedException e) {
      // Nuot roi chay tiep se thanh busy-loop: co interrupt dang bat lam Thread.sleep nem ngay,
      // vong lap se nen lien tuc API COM dump cay cho het thoi gian cho.
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Bi ngat khi cho man hinh Select Register", e);
    }
  }
}
