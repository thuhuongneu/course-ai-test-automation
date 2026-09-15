package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import io.qameta.allure.Step;
import java.time.Duration;

/**
 * Man hinh danh sach hoa don (Receipts), mo tu menu dieu huong cua TerminalScreen.
 *
 * <p>Da inspect truc tiep tren build STAG 26.09.14: go ma vao o "Search Order" roi Enter se loc
 * danh sach xuong con dung cac hoa don khop. Danh sach ket qua render dang bang (DataTable) va
 * KHONG lo hang nao ra accessibility tree - Flutter khong populate semantics cho children cua
 * bang nay - nen khong the click theo ten/role nhu binh thuong. Voi dung MOT ket qua (ma tim la
 * ma hoa don chinh xac), hang do luon nam co dinh ngay duoi dong tieu de cot, nen click theo toa
 * do tuyet doi ({@link AxDriver#clickAt}) la cach duy nhat kha thi - da do dac truc tiep tren UI
 * that.
 */
public class ReceiptListScreen {

  private static final String TITLE = "Receipts";
  private static final String ROLE_TEXT = "TEXT";
  private static final int SEARCH_BOX_INDEX = 0;
  // Toa do hang ket qua dau tien trong bang, do truc tiep tren build STAG 26.09.14 - on dinh vi
  // day la vi tri co dinh ngay duoi dong tieu de cot khi chi co dung mot ket qua.
  private static final int FIRST_RESULT_X = 200;
  private static final int FIRST_RESULT_Y = 235;
  // Danh sach ket qua khong lo ra accessibility tree (xem Javadoc lop) nen khong the cho truc
  // tiep "chi con 1 dong" - dung tin hieu gian tiep: nut phan trang "Next" bien mat khi danh sach
  // da loc xuong vua/it hon mot trang.
  private static final String PAGINATION_NEXT = "Next";
  private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration FILTER_TIMEOUT = Duration.ofSeconds(5);
  private static final long POLL_MILLIS = 200;

  private final AxDriver driver;
  private final UpdatePromptDialog updatePrompt;

  public ReceiptListScreen(AxDriver driver) {
    this.driver = driver;
    this.updatePrompt = new UpdatePromptDialog(driver);
  }

  public boolean isDisplayed() {
    return driver.exists(TITLE);
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

  /**
   * Go ma don hang vao o Search Order roi Enter de loc danh sach, cho toi khi loc xong (nut phan
   * trang "Next" bien mat) - bam vao toa do co dinh ({@link #clickFirstResult}) qua som, khi danh
   * sach cu chua kip loc, se trung nham dong khac.
   */
  @Step("Tìm order theo mã: {orderCode}")
  public void searchOrder(String orderCode) {
    updatePrompt.declineIfPresent();
    driver.fill(ROLE_TEXT, SEARCH_BOX_INDEX, orderCode);
    driver.pressEnter();
    waitUntilFiltered();
  }

  private void waitUntilFiltered() {
    long deadline = System.currentTimeMillis() + FILTER_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline && driver.exists(PAGINATION_NEXT)) {
      sleepQuiet(POLL_MILLIS);
    }
  }

  /** Bam vao hang ket qua dau tien de mo man hinh chi tiet don hang. */
  @Step("Click vào kết quả đầu tiên để xem chi tiết đơn hàng")
  public void clickFirstResult() {
    updatePrompt.declineIfPresent();
    driver.clickAt(FIRST_RESULT_X, FIRST_RESULT_Y);
  }

  private static void sleepQuiet(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
