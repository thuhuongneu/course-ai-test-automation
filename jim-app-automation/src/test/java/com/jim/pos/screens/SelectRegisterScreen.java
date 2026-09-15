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
   * <p>Dung thoi gian cho ngan cho cac ca kiem thu am, de khong phai doi het thoi gian mac dinh
   * chi de chung minh man hinh KHONG xuat hien.
   */
  public boolean appearsWithin(Duration timeout) {
    return updatePrompt.waitForNameResilient(TITLE, timeout);
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
    updatePrompt.runResilient(() -> driver.clickByName(BTN_LOGOUT));
  }
}
