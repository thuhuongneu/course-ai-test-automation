package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import com.jim.pos.core.AxNode;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Man hinh Checkout va cac hop thoai phu phat sinh trong luc hoan tat don hang: canh bao gia 0,
 * xac nhan Guest/Member, va cac truong bat buoc cua cua hang demo nay (Nationality Group,
 * Salesperson) - da inspect truc tiep tren build STAG 26.09.14.
 */
public class CheckoutScreen {

  private static final String TITLE = "CHECKOUT";
  private static final String TITLE_SUCCESS = "CHECKOUT SUCCESS";
  private static final String BTN_COMPLETE = "COMPLETE";
  private static final String BTN_YES_CONTINUE = "Yes, Continue";
  private static final String BTN_GUEST = "Guest";
  private static final String NATIONALITY_REQUIRED_MARK = "Nationality group is required.";
  private static final String SALESPERSON_SEARCH_LABEL = "Search on Salesperson";
  private static final String ROLE_RADIOBUTTON = "RADIOBUTTON";
  private static final String ROLE_STATICTEXT = "STATICTEXT";
  private static final String PAYMENT_CASH_PREFIX = "Cash";
  private static final String BTN_CONFIRM = "Confirm";
  private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration OUTCOME_TIMEOUT = Duration.ofSeconds(3);
  private static final long POLL_MILLIS = 200;
  private static final int MAX_COMPLETE_ATTEMPTS = 8;

  private final AxDriver driver;
  private final UpdatePromptDialog updatePrompt;

  public CheckoutScreen(AxDriver driver) {
    this.driver = driver;
    this.updatePrompt = new UpdatePromptDialog(driver);
  }

  /** Dong canh bao "Zero Price Warning" neu co (san pham demo gia 0 se kich hoat canh bao nay). */
  @Step("Xử lý cảnh báo Zero Price nếu có")
  public void confirmZeroPriceWarningIfPresent() {
    if (driver.exists(BTN_YES_CONTINUE)) {
      driver.clickByName(BTN_YES_CONTINUE);
    }
  }

  /** Chon "Guest" o hop thoai xac nhan checkout preference (bo qua thu thap diem CRM). */
  @Step("Chọn Guest ở bước xác nhận checkout")
  public void selectGuestCustomer() {
    updatePrompt.runResilient(() -> driver.clickByName(BTN_GUEST));
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
   * Chon phuong thuc thanh toan Cash: bam vao o Cash (dang la STATICTEXT khi chua chon) de mo hop
   * thoai nhap so tien - hop thoai tu dien san dung bang tong tien con lai - roi bam Confirm.
   */
  @Step("Chọn phương thức thanh toán Cash")
  public void selectCashPayment() {
    updatePrompt.declineIfPresent();
    List<AxNode> texts = driver.findByRole(ROLE_STATICTEXT);
    for (int i = 0; i < texts.size(); i++) {
      if (texts.get(i).name != null && texts.get(i).name.startsWith(PAYMENT_CASH_PREFIX)) {
        driver.clickByRole(ROLE_STATICTEXT, i);
        break;
      }
    }
    updatePrompt.runResilient(() -> driver.clickByName(BTN_CONFIRM));
  }

  /**
   * Bam COMPLETE va tu dong dien cac truong bat buoc phat sinh (Nationality Group, Salesperson)
   * khi app yeu cau, cho toi khi thay man hinh CHECKOUT SUCCESS hoac het so lan thu.
   *
   * @param nationality ten nhom quoc tich se chon neu bi yeu cau, vi du "LOCAL"
   * @param salespersonPrefix tien to ten salesperson se chon neu bi yeu cau, vi du "0001"
   * @return true neu checkout thanh cong (thay man hinh CHECKOUT SUCCESS)
   */
  @Step("Hoàn tất Checkout (tự điền các trường bắt buộc phát sinh)")
  public boolean completeCheckout(String nationality, String salespersonPrefix) {
    for (int attempt = 1; attempt <= MAX_COMPLETE_ATTEMPTS; attempt++) {
      updatePrompt.declineIfPresent();
      // Hop thoai cap nhat co the chen dung vao khe giua lan declineIfPresent() o tren va luc
      // click thuc su ben duoi - bat loi do de thu lai vong sau thay vi lam gay ca ham.
      try {
        driver.clickByName(BTN_COMPLETE);
      } catch (AxDriver.AxException e) {
        continue;
      }
      waitForAnyOutcome();

      if (driver.exists(TITLE_SUCCESS)) {
        return true;
      }
      if (driver.exists(NATIONALITY_REQUIRED_MARK)) {
        driver.clickByName(nationality);
        waitUntilGone(NATIONALITY_REQUIRED_MARK);
        continue;
      }
      if (driver.exists(SALESPERSON_SEARCH_LABEL)) {
        selectSalespersonByPrefix(salespersonPrefix);
        waitUntilGone(SALESPERSON_SEARCH_LABEL);
        continue;
      }
    }
    return driver.exists(TITLE_SUCCESS);
  }

  /** Doi toi khi mot trong cac ket qua co the co cua COMPLETE xuat hien, hoac het thoi gian. */
  private void waitForAnyOutcome() {
    long deadline = System.currentTimeMillis() + OUTCOME_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      if (driver.exists(TITLE_SUCCESS) || driver.exists(NATIONALITY_REQUIRED_MARK)
          || driver.exists(SALESPERSON_SEARCH_LABEL)) {
        return;
      }
      sleepQuiet(POLL_MILLIS);
    }
  }

  /** Doi toi khi {@code marker} bien mat khoi man hinh, hoac het thoi gian. */
  private void waitUntilGone(String marker) {
    long deadline = System.currentTimeMillis() + OUTCOME_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline && driver.exists(marker)) {
      sleepQuiet(POLL_MILLIS);
    }
  }

  private void selectSalespersonByPrefix(String prefix) {
    for (AxNode n : driver.findByRole(ROLE_RADIOBUTTON)) {
      if (n.name != null && n.name.startsWith(prefix)) {
        driver.clickByName(n.name);
        return;
      }
    }
    throw new IllegalStateException("Khong tim thay salesperson bat dau bang '" + prefix + "'");
  }

  public boolean isCheckoutSuccess() {
    return driver.exists(TITLE_SUCCESS);
  }

  private static final Pattern RECEIPT_CODE_PATTERN = Pattern.compile("Rec\\.\\s*#(\\S+)");

  /** Doc ma don hang (vi du "THF01000002936") tu dong "1 Items Rec. #..." o man hinh CHECKOUT SUCCESS. */
  @Step("Đọc mã đơn hàng vừa tạo")
  public String readOrderCode() {
    for (AxNode n : driver.findByRole(ROLE_STATICTEXT)) {
      if (n.name == null) {
        continue;
      }
      Matcher m = RECEIPT_CODE_PATTERN.matcher(n.name);
      if (m.find()) {
        return m.group(1);
      }
    }
    throw new IllegalStateException("Khong doc duoc ma don hang tu man hinh CHECKOUT SUCCESS");
  }

  private static void sleepQuiet(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
