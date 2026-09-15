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
  private static final Duration ACTION_TIMEOUT = Duration.ofSeconds(20);
  private static final long POLL_MILLIS = 400;

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
   * <p>Co khoang ho giua {@link #isPresent()} va luc bam "No": hop thoai co the tu dong bien mat
   * (hoac da bi dong boi mot lan goi truoc do chua kip cap nhat trang thai) dung luc nay -
   * {@code clickByName} se nem loi "khong tim thay" du muc tieu (khong con hop thoai) thuc ra da
   * dat duoc. Gap loi do thi kiem tra lai: neu hop thoai da bien mat thi coi la thanh cong, chi
   * nem loi that khi no van con hien.
   *
   * @return true neu vua dong (hoac phat hien da tu dong) mot hop thoai, false neu khong co gi de dong
   */
  @Step("Từ chối hộp thoại cập nhật nếu đang hiện")
  public boolean declineIfPresent() {
    if (!isPresent()) {
      return false;
    }
    try {
      driver.clickByName(BTN_DECLINE);
    } catch (AxDriver.AxException e) {
      if (isPresent()) {
        throw e;
      }
      return true;
    }
    driver.waitUntilGone(TITLE, DISMISS_TIMEOUT);
    return true;
  }

  /**
   * Thuc hien mot thao tac tren man hinh, chiu duoc viec hop thoai cap nhat chen vao dung khe giua
   * luc kiem tra va luc thao tac.
   *
   * <p>Hop thoai co the tu bat len vao <b>bat ky luc nao</b>, tren <b>bat ky man hinh nao</b>
   * (Login, Select Register...), va da quan sat duoc no co the bat len LIEN TIEP NHIEU LAN gan
   * nhau (vd. ngay sau khi mot man hinh moi vua mount) - thu lai dung MOT lan la chua du, dialog
   * van con co the chan ca lan thu lai. Vi vay lap lai theo kieu co timeout tong ({@code
   * ACTION_TIMEOUT}) thay vi thu dung mot so lan co dinh: moi lan action that bai vi dialog thi
   * don dialog roi thu lai, cho den khi thanh cong hoac het thoi gian. Dung chung cho moi Screen
   * class thay vi moi noi tu viet lai logic nay.
   */
  public void runResilient(Runnable action) {
    long deadline = System.currentTimeMillis() + ACTION_TIMEOUT.toMillis();
    AxDriver.AxException lastFailure = null;
    while (System.currentTimeMillis() < deadline) {
      declineIfPresent();
      try {
        action.run();
        return;
      } catch (AxDriver.AxException e) {
        lastFailure = e;
        sleepBetweenPolls();
      }
    }
    throw lastFailure != null ? lastFailure
        : new IllegalStateException("Thao tac khong thanh cong trong " + ACTION_TIMEOUT.getSeconds() + "s");
  }

  /**
   * Cho den khi element ten {@code name} xuat hien, TU DONG don hop thoai cap nhat neu no chen
   * vao giua luc cho.
   *
   * <p>Khac voi {@link AxDriver#waitForName}: cai do cho "mu" hoan toan ben phia driver, khong
   * biet gi ve hop thoai cap nhat. Neu hop thoai bat len dung trong luc dang cho, toan bo man
   * hinh phia sau - ke ca element dang cho - bien mat khoi cay, va phep cho se treo het timeout
   * ma khong bao gio thay duoc, du element that ra van se xuat hien ngay khi hop thoai bi dong.
   * Ham nay tu poll tu phia Java, moi vong deu thu don hop thoai truoc khi kiem tra - dung chung
   * cho moi Screen class thay vi moi noi tu viet lai vong lap nay.
   */
  public boolean waitForNameResilient(String name, Duration timeout) {
    long deadline = System.currentTimeMillis() + timeout.toMillis();
    while (System.currentTimeMillis() < deadline) {
      declineIfPresent();
      if (driver.exists(name)) {
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
      throw new IllegalStateException("Bi ngat khi cho element xuat hien", e);
    }
  }
}
