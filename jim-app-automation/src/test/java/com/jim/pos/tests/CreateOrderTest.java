package com.jim.pos.tests;

import com.jim.pos.base.BaseTest;
import com.jim.pos.config.ConfigReader;
import com.jim.pos.core.TcId;
import com.jim.pos.screens.CheckoutScreen;
import com.jim.pos.screens.OrderDetailScreen;
import com.jim.pos.screens.ReceiptListScreen;
import com.jim.pos.screens.TerminalScreen;
import com.jim.pos.screens.UpdatePromptDialog;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test tao don hang moi trong terminal THF01 cua app JIM POS, thanh toan bang Cash, roi tra cuu
 * lai don hang vua tao trong Receipt List va xem chi tiet.
 *
 * <p>Flow da xac nhan truc tiep tren build "Jim - STAG - 26.09.14 - 09.43": chon terminal THF01 o
 * SelectRegisterScreen se vao thang man hinh ban hang (khong co man hinh "New Order" rieng) -&gt;
 * go SKU vao o "Search or scan product" roi Enter se TU DONG them san pham vao gio hang (mo phong
 * may quet ma vach) -&gt; Pay -&gt; Guest -&gt; Checkout: chon Cash (tu dien du so tien) -&gt; COMPLETE,
 * dien Nationality Group va Salesperson khi app yeu cau (truong bat buoc cua cua hang demo nay)
 * -&gt; CHECKOUT SUCCESS -&gt; mo Receipt List tu menu dieu huong -&gt; tim theo ma don hang vua tao,
 * bam Enter de loc danh sach -&gt; bam vao dong ket qua de mo man hinh chi tiet don hang.
 */
@Feature("Tạo đơn hàng")
public class CreateOrderTest extends BaseTest {

  private static final String TERMINAL_NAME = "THF01";
  private static final String PRODUCT_SKU = "8852676068172";
  private static final String PRODUCT_NAME = "WL HEAVY EMB SH 1722A BLK";
  private static final String NATIONALITY_GROUP = "LOCAL";
  private static final String SALESPERSON_PREFIX = "0001";

  private static final Duration STABILIZE_WINDOW = Duration.ofSeconds(3);
  private static final long STABILIZE_POLL_MILLIS = 300;
  private static final int MAX_STEP_ATTEMPTS = 4;

  private TerminalScreen terminalScreen;
  private CheckoutScreen checkoutScreen;
  private ReceiptListScreen receiptListScreen;
  private OrderDetailScreen orderDetailScreen;
  private UpdatePromptDialog orderUpdatePrompt;

  /** Khoi tao them cac Screen object rieng cho test tao order, chay sau BaseTest.setUp(). */
  @BeforeMethod(dependsOnMethods = "setUp")
  public void setUpOrderScreens() {
    terminalScreen = new TerminalScreen(driver);
    checkoutScreen = new CheckoutScreen(driver);
    receiptListScreen = new ReceiptListScreen(driver);
    orderDetailScreen = new OrderDetailScreen(driver);
    orderUpdatePrompt = new UpdatePromptDialog(driver);
  }

  /**
   * "Vet het" khung thoi gian hop thoai cap nhat co the tu bat len ngay sau khi mot man hinh moi
   * vua mount - cung ky thuat voi BaseTest.stabilizeLoginScreen(), ap dung truoc cac buoc thao tac
   * dau tien tren mot man hinh moi de tranh click bi dialog chen vao giua chung.
   */
  private void stabilize() {
    long deadline = System.currentTimeMillis() + STABILIZE_WINDOW.toMillis();
    while (System.currentTimeMillis() < deadline) {
      orderUpdatePrompt.declineIfPresent();
      try {
        Thread.sleep(STABILIZE_POLL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Bi ngat khi cho man hinh on dinh", e);
      }
    }
  }

  /**
   * Thu lai toi da {@link #MAX_STEP_ATTEMPTS} lan mot buoc thao tac + kiem chung ket qua: hop
   * thoai cap nhat co the chen vao dung khe giua luc thao tac (lam thao tac nem loi, hoac "thanh
   * cong" nhung khong co tac dung tren UI that vi bi dialog che) - thu lai ca hai truong hop nay
   * thay vi de mot lan chen dialog lam gay ca buoc.
   */
  private boolean retryStep(Runnable action, BooleanSupplier verify) {
    for (int attempt = 1; attempt <= MAX_STEP_ATTEMPTS; attempt++) {
      try {
        action.run();
      } catch (RuntimeException e) {
        continue;
      }
      if (verify.getAsBoolean()) {
        return true;
      }
    }
    return false;
  }

  @TcId("JIM_ORDER_TC_001")
  @Story("Tạo đơn hàng mới")
  @Test(description = "Tạo đơn hàng mới trong terminal THF01, thanh toán Cash, tra cứu lại trong "
      + "Receipt List thành công", groups = {"order", "smoke"})
  @Description("Đăng nhập → chọn terminal THF01 → tìm sản phẩm theo SKU và thêm vào giỏ → Pay → "
      + "chọn Cash → hoàn tất Checkout → xác nhận CHECKOUT SUCCESS → mở Receipt List → tìm lại "
      + "đúng đơn hàng vừa tạo → xem chi tiết đơn hàng, xác nhận trạng thái Completed.")
  @Severity(SeverityLevel.BLOCKER)
  public void createOrder_payWithCash_thenFoundInReceiptList() {
    // Arrange: đăng nhập bằng tài khoản hợp lệ lấy từ .env
    Allure.step("Arrange: đăng nhập để vào SelectRegisterScreen", () -> {
      Assert.assertTrue(loginScreen.isDisplayed(), "Phải đang ở màn hình Login trước khi đăng nhập");
      loginScreen.login(ConfigReader.username(), ConfigReader.password());
      Assert.assertTrue(selectRegisterScreen.waitUntilDisplayed(),
          "Đăng nhập thành công phải chuyển sang màn hình SelectRegisterScreen");
    });

    // Act: chọn terminal THF01 - vào thẳng màn hình bán hàng
    Allure.step("Act: chọn terminal " + TERMINAL_NAME, () -> {
      // Man hinh SelectRegisterScreen vua mount xong co the tu bat hop thoai cap nhat NGAY SAU DO
      // (cung hien tuong da ghi nhan o man hinh Login - xem BaseTest.stabilizeLoginScreen).
      stabilize();
      boolean displayed = retryStep(
          () -> selectRegisterScreen.selectTerminal(TERMINAL_NAME),
          terminalScreen::waitUntilDisplayed);
      Assert.assertTrue(displayed,
          "Màn hình bán hàng của terminal " + TERMINAL_NAME + " phải hiển thị sau khi chọn");
    });

    // Act: tìm sản phẩm theo SKU (mô phỏng máy quét mã vạch) - tự động thêm vào giỏ.
    // KHONG retry buoc nay: moi lan quet lai la mot lan them hang nua vao gio, khong phai mot
    // lan thu lai vo hai. Quet MOT lan roi cho gio cap nhat.
    Allure.step("Act: tìm sản phẩm theo SKU " + PRODUCT_SKU + " và thêm vào giỏ", () -> {
      stabilize();
      terminalScreen.searchAndAddProduct(PRODUCT_SKU);
      Assert.assertTrue(terminalScreen.waitUntilCartNotEmpty(),
          "Giỏ hàng phải có hàng sau khi quét SKU " + PRODUCT_SKU);
      Assert.assertTrue(terminalScreen.cartContains(PRODUCT_SKU),
          "Giỏ hàng phải chứa đúng sản phẩm vừa quét (SKU " + PRODUCT_SKU + ")");
    });

    // Act: bấm Pay, xác nhận Guest để chuyển sang Checkout
    Allure.step("Act: bấm Pay để chuyển sang Checkout", () -> {
      boolean checkoutShown = retryStep(() -> {
        terminalScreen.clickPay();
        checkoutScreen.confirmZeroPriceWarningIfPresent();
        checkoutScreen.selectGuestCustomer();
      }, checkoutScreen::waitUntilDisplayed);
      Assert.assertTrue(checkoutShown, "Màn hình CHECKOUT phải hiển thị");
    });

    // Act: chọn phương thức thanh toán Cash
    Allure.step("Act: chọn phương thức thanh toán Cash", checkoutScreen::selectCashPayment);

    // Act: hoàn tất thanh toán (tự điền Nationality Group / Salesperson nếu app yêu cầu)
    Allure.step("Act: hoàn tất Checkout", () -> {
      boolean success = checkoutScreen.completeCheckout(NATIONALITY_GROUP, SALESPERSON_PREFIX);
      Assert.assertTrue(success, "Checkout phải hoàn tất thành công");
    });

    // Assert: đơn hàng được tạo thành công
    Allure.step("Assert: màn hình CHECKOUT SUCCESS phải hiển thị - đơn hàng đã tạo thành công",
        () -> Assert.assertTrue(checkoutScreen.isCheckoutSuccess(),
            "Đơn hàng phải được tạo thành công, xác nhận qua màn hình CHECKOUT SUCCESS"));

    // Act: đọc mã đơn hàng vừa tạo để tra cứu lại
    String orderCode = checkoutScreen.readOrderCode();
    Allure.addAttachment("ma_don_hang_vua_tao", "text/plain", orderCode);

    // Act: quay lại màn hình bán hàng, mở Receipt List
    Allure.step("Act: quay lại màn hình bán hàng và mở Receipt List", () -> {
      boolean receiptListShown = retryStep(
          terminalScreen::openReceiptList, receiptListScreen::waitUntilDisplayed);
      Assert.assertTrue(receiptListShown, "Màn hình Receipt List phải hiển thị");
    });

    // Act: tìm order theo mã, bấm Enter, rồi click vào kết quả để xem chi tiết
    Allure.step("Act: tìm order theo mã " + orderCode, () -> {
      receiptListScreen.searchOrder(orderCode);
      boolean detailShown = retryStep(
          receiptListScreen::clickFirstResult, orderDetailScreen::waitUntilDisplayed);
      Assert.assertTrue(detailShown,
          "Click vào kết quả tìm kiếm phải mở màn hình chi tiết đơn hàng");
    });

    // Assert: chi tiết đơn hàng đúng với đơn vừa tạo
    Allure.step("Assert: chi tiết đơn hàng khớp với đơn vừa tạo và ở trạng thái Completed", () -> {
      Assert.assertTrue(orderDetailScreen.hasOrderCode(orderCode),
          "Chi tiết đơn hàng phải hiển thị đúng mã " + orderCode);
      Assert.assertTrue(orderDetailScreen.hasProduct(PRODUCT_NAME),
          "Chi tiết đơn hàng phải hiển thị đúng sản phẩm " + PRODUCT_NAME);
      Assert.assertTrue(orderDetailScreen.isStatusCompleted(),
          "Đơn hàng vừa thanh toán phải ở trạng thái Completed");
    });
  }
}
