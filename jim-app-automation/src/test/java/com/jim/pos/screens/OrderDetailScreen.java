package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import com.jim.pos.core.AxNode;
import io.qameta.allure.Step;
import java.time.Duration;

/**
 * Man hinh chi tiet don hang, mo tu ReceiptListScreen khi tim kiem khop duy nhat mot ket qua.
 *
 * <p>Luu y: ma don hang ("Global ID") hien thi trong o <b>value</b> cua mot STATICTEXT co ten
 * rong (da inspect truc tiep tren build STAG 26.09.14) - khac voi cac nhan/label khac deu nam o
 * <b>name</b>. Vi vay khong dung {@link AxDriver#exists} (chi so khop theo name) de kiem tra ma
 * don hang, phai duyet {@link AxDriver#findByRole} va so khop ca hai truong.
 */
public class OrderDetailScreen {

  private static final String LABEL_STATUS = "Status";
  private static final String LABEL_GLOBAL_ID = "Global ID";
  private static final String STATUS_COMPLETED = "Completed";
  private static final String ROLE_STATICTEXT = "STATICTEXT";
  private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

  private final AxDriver driver;
  private final UpdatePromptDialog updatePrompt;

  public OrderDetailScreen(AxDriver driver) {
    this.driver = driver;
    this.updatePrompt = new UpdatePromptDialog(driver);
  }

  public boolean isDisplayed() {
    return driver.exists(LABEL_STATUS) && driver.exists(LABEL_GLOBAL_ID);
  }

  public boolean waitUntilDisplayed() {
    long deadline = System.currentTimeMillis() + WAIT_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      updatePrompt.declineIfPresent();
      if (isDisplayed()) {
        return true;
      }
      sleepQuiet(300);
    }
    return false;
  }

  /** Trang thai don hang co phai "Completed" khong. */
  @Step("Kiểm tra trạng thái đơn hàng là Completed")
  public boolean isStatusCompleted() {
    return driver.exists(STATUS_COMPLETED);
  }

  /** Ma don hang (Global ID) hien thi co dung {@code expectedOrderCode} khong. */
  @Step("Kiểm tra Global ID của đơn hàng khớp {expectedOrderCode}")
  public boolean hasOrderCode(String expectedOrderCode) {
    for (AxNode n : driver.findByRole(ROLE_STATICTEXT)) {
      if (expectedOrderCode.equals(n.value) || expectedOrderCode.equals(n.name)) {
        return true;
      }
    }
    return false;
  }

  /** San pham co ten {@code productName} co xuat hien trong chi tiet don hang khong. */
  public boolean hasProduct(String productName) {
    return driver.exists(productName);
  }

  private static void sleepQuiet(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
