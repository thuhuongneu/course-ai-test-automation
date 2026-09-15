package com.jim.pos.screens;

import com.jim.pos.core.AxDriver;
import com.jim.pos.core.AxNode;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Man hinh ban hang cua mot terminal POS (vao thang sau khi chon terminal o SelectRegisterScreen -
 * khong co man hinh "New Order" rieng biet).
 *
 * <p>Cau truc thuc te (da inspect truc tiep tren build STAG 26.09.14): o "Search or scan product"
 * - thiet ke cho may quet ma vach - go SKU roi Enter se tu dong them thang san pham vao gio hang
 * (khong can qua man hinh chi tiet san pham). Nut "Pay (N items) ..." o cuoi, ten thay doi theo
 * so luong/gia tien nen KHONG the click theo ten co dinh - phai loc theo tien to "Pay" roi click
 * theo vi tri (role + index).
 */
public class TerminalScreen {

  private static final String ROLE_TEXT = "TEXT";
  private static final String ROLE_BUTTON = "BUTTON";
  private static final int SEARCH_BOX_INDEX = 0;
  private static final String PAY_BUTTON_PREFIX = "Pay";
  private static final String BTN_RECEIPT_LIST = "RECEIPT LIST";
  private static final int MENU_ICON_INDEX = 0;
  private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

  private final AxDriver driver;
  private final UpdatePromptDialog updatePrompt;

  public TerminalScreen(AxDriver driver) {
    this.driver = driver;
    this.updatePrompt = new UpdatePromptDialog(driver);
  }

  /** Kiem tra da vao man hinh ban hang chua (co o tim san pham). */
  public boolean isDisplayed() {
    return driver.exists("Search or scan product");
  }

  /** Cho man hinh ban hang xuat hien sau khi chon terminal. */
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
   * Go SKU vao o "Search or scan product" roi Enter - mo phong may quet ma vach: san pham khop
   * duy nhat se duoc them thang vao gio hang, khong can vao man hinh chi tiet.
   */
  @Step("Tìm sản phẩm theo SKU {sku} và thêm vào giỏ (mô phỏng máy quét)")
  public void searchAndAddProduct(String sku) {
    updatePrompt.declineIfPresent();
    driver.fill(ROLE_TEXT, SEARCH_BOX_INDEX, sku);
    driver.pressEnter();
  }

  /** So luong item hien co trong gio hang, doc tu nut Pay ("Pay (N items) ..."). */
  public int cartItemCount() {
    String payLabel = findPayButtonLabel();
    if (payLabel == null) {
      return 0;
    }
    // Dinh dang: "Pay\n (N items)\n..." - lay so ngay truoc chu "items".
    Matcher m = Pattern.compile("(\\d+)\\s*items?").matcher(payLabel);
    return m.find() ? Integer.parseInt(m.group(1)) : 0;
  }

  /** Bam nut Pay de sang man hinh Checkout. */
  @Step("Bấm nút Pay để sang bước Checkout")
  public void clickPay() {
    updatePrompt.declineIfPresent();
    List<AxNode> buttons = driver.findByRole(ROLE_BUTTON);
    for (int i = 0; i < buttons.size(); i++) {
      if (buttons.get(i).name != null && buttons.get(i).name.startsWith(PAY_BUTTON_PREFIX)) {
        driver.clickByRole(ROLE_BUTTON, i);
        return;
      }
    }
    throw new IllegalStateException("Khong tim thay nut Pay tren man hinh ban hang");
  }

  /**
   * Mo menu dieu huong (icon hamburger goc tren trai) roi vao Receipt List.
   *
   * <p>Bo qua buoc bam icon neu menu da mo san (nut RECEIPT LIST da co tren cay) - giup ham nay
   * goi lai an toan khi bi retry o tang tren, tranh bam nham icon hamburger luc menu dang mo (luc
   * do no khong con nam o vi tri/role cu).
   */
  @Step("Mở Receipt List từ menu điều hướng")
  public void openReceiptList() {
    updatePrompt.declineIfPresent();
    if (!driver.exists(BTN_RECEIPT_LIST)) {
      driver.clickByRole(ROLE_BUTTON, MENU_ICON_INDEX);
    }
    updatePrompt.runResilient(() -> driver.clickByName(BTN_RECEIPT_LIST));
  }

  private String findPayButtonLabel() {
    for (AxNode n : driver.findByRole(ROLE_BUTTON)) {
      if (n.name != null && n.name.startsWith(PAY_BUTTON_PREFIX)) {
        return n.name;
      }
    }
    return null;
  }

  private static void sleepQuiet(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
