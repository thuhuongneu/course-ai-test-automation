package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import io.qameta.allure.Step;
import java.time.Duration;

/**
 * Hop thoai "Do you want to update" - app tu bat len khi phat hien co ban moi.
 *
 * <p>Phai xu ly rieng vi khi hop thoai nay hien, <b>toan bo man hinh phia sau bien mat khoi cay
 * semantics</b> (da kiem chung: luc dialog bat thi khong con tim thay "Select Register"). Neu khong
 * tu choi no, moi phep cho element se tuong nham la man hinh chua load xong.
 *
 * <p>Luon chon "No" de test chay dung tren ban build dang kiem thu.
 */
public class UpdatePromptDialog {

  private static final String TITLE = "Do you want to update";
  private static final String BTN_DECLINE = "No";
  private static final Duration DISMISS_TIMEOUT = Duration.ofSeconds(10);

  private final AxDriver driver;

  public UpdatePromptDialog(AxDriver driver) {
    this.driver = driver;
  }

  public boolean isPresent() {
    return driver.exists(TITLE);
  }

  /**
   * Tu choi cap nhat neu hop thoai dang hien.
   *
   * @return true neu vua dong mot hop thoai, false neu khong co gi de dong
   */
  @Step("Từ chối hộp thoại cập nhật nếu đang hiện")
  public boolean declineIfPresent() {
    if (!isPresent()) {
      return false;
    }
    driver.clickByName(BTN_DECLINE);
    driver.waitUntilGone(TITLE, DISMISS_TIMEOUT);
    return true;
  }
}
