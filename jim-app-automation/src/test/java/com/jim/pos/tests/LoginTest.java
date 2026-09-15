package com.jim.pos.tests;

import com.jim.pos.base.BaseTest;
import com.jim.pos.config.ConfigReader;
import com.jim.pos.core.TcId;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import java.time.Duration;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test chuc nang Dang nhap cua app JIM POS (Flutter Windows desktop).
 *
 * <p>Moi hanh vi khang dinh trong file nay deu da duoc kiem chung truc tiep tren build
 * "Jim - STAG - 26.09.14 - 09.43":
 *
 * <ul>
 *   <li>Dang nhap dung -> app chuyen sang man hinh "Select Register".
 *   <li>Sai mat khau -> app o lai man hinh Login. App <b>khong</b> hien thong bao loi nao (da doc
 *       cay semantics 5 lan lien tiep sau khi bam LOGIN, khong co node thong bao) - vi vay test chi
 *       khang dinh phan quan sat duoc, khong bia ra mot thong bao khong ton tai.
 *   <li>Bo trong -> hien "Username is required!", "Password is required" va sau khi bam LOGIN thi
 *       hien "Please fill in your username and password to log in".
 * </ul>
 */
@Feature("Đăng nhập")
public class LoginTest extends BaseTest {

  /** Thoi gian du de khang dinh man hinh Select Register KHONG xuat hien (ca kiem thu am). */
  private static final Duration NEGATIVE_WAIT = Duration.ofSeconds(15);

  @TcId("JIM_LOGIN_TC_001")
  @Story("Đăng nhập hợp lệ")
  @Test(description = "Đăng nhập thành công với tài khoản hợp lệ - chuyển sang màn Select Register",
      groups = {"login", "smoke"})
  @Description("Đăng nhập app JIM POS bằng username/password hợp lệ, xác nhận app rời màn hình "
      + "Login và chuyển sang màn hình chọn quay (Select Register) với phiên đăng nhập đang hoạt động.")
  @Severity(SeverityLevel.BLOCKER)
  public void login_validCredentials_opensSelectRegister() {
    // Arrange: tài khoản hợp lệ lấy từ .env, không hardcode trong code
    String username = ConfigReader.username();
    String password = ConfigReader.password();
    Allure.step("Arrange: app đang ở màn hình Login", () ->
        Assert.assertTrue(loginScreen.isDisplayed(),
            "Phải đang ở màn hình Login trước khi bắt đầu đăng nhập"));

    // Act: nhập thông tin, tự kiểm chứng đã nhập đúng rồi mới đăng nhập
    Allure.step("Act: nhập username và xác nhận ô đã nhận đúng giá trị", () -> {
      loginScreen.enterUsername(username);
      Assert.assertEquals(loginScreen.readUsername(), username,
          "Ô Username phải chứa đúng giá trị vừa nhập");
    });
    Allure.step("Act: nhập password và xác nhận ô đã nhận đủ số ký tự", () -> {
      loginScreen.enterPassword(password);
      Assert.assertEquals(loginScreen.passwordLength(), password.length(),
          "Ô Password phải nhận đủ số ký tự vừa nhập");
    });
    Allure.step("Act: bấm nút LOGIN", loginScreen::clickLogin);

    // Assert: app rời màn Login và vào màn chọn quay
    Allure.step("Assert: app chuyển sang màn hình Select Register", () -> {
      Assert.assertTrue(selectRegisterScreen.waitUntilDisplayed(),
          "Đăng nhập thành công phải chuyển sang màn hình Select Register");
      Assert.assertTrue(selectRegisterScreen.hasLogoutButton(),
          "Màn hình Select Register phải có nút Logout - dấu hiệu phiên đăng nhập đang hoạt động");
      Assert.assertFalse(loginScreen.isStillOnLoginScreen(),
          "Đăng nhập thành công thì màn hình Login phải biến mất");
    });
  }

  @TcId("JIM_LOGIN_TC_002")
  @Story("Đăng nhập sai thông tin")
  @Test(description = "Đăng nhập với password sai - bị từ chối, ở lại màn Login",
      groups = {"login", "negative"})
  @Description("Username đúng nhưng password sai; app phải từ chối đăng nhập và giữ nguyên màn hình "
      + "Login. Lưu ý: app KHÔNG hiển thị thông báo lỗi nào (đã kiểm chứng trên cây semantics), nên "
      + "test chỉ khẳng định việc bị từ chối, không khẳng định có thông báo.")
  @Severity(SeverityLevel.CRITICAL)
  public void login_invalidPassword_staysOnLoginScreen() {
    // Arrange: username đúng, password sai sinh động theo timestamp để truy ngược được
    String username = ConfigReader.username();
    String wrongPassword = "wrong-pass-" + System.currentTimeMillis();
    Allure.step("Arrange: chuẩn bị username hợp lệ và password sai (sinh theo timestamp)", () ->
        Assert.assertTrue(loginScreen.isDisplayed(), "Phải đang ở màn hình Login"));

    // Act
    Allure.step("Act: đăng nhập với password sai", () ->
        loginScreen.login(username, wrongPassword));

    // Assert: không được vào hệ thống
    Allure.step("Assert: app từ chối đăng nhập và ở lại màn hình Login", () -> {
      Assert.assertFalse(selectRegisterScreen.appearsWithin(NEGATIVE_WAIT),
          "Password sai thì tuyệt đối không được vào màn hình Select Register");
      Assert.assertTrue(loginScreen.isStillOnLoginScreen(),
          "Password sai thì phải ở lại màn hình Login");
    });
  }

  @TcId("JIM_LOGIN_TC_003")
  @Story("Đăng nhập sai thông tin")
  @Test(description = "Đăng nhập khi bỏ trống username và password - hiện thông báo bắt buộc nhập",
      groups = {"login", "negative"})
  @Description("Để trống cả hai ô rồi bấm LOGIN; app phải hiển thị thông báo bắt buộc nhập cho từng "
      + "ô và thông báo chung, đồng thời không cho đăng nhập.")
  @Severity(SeverityLevel.NORMAL)
  public void login_emptyCredentials_showsRequiredMessages() {
    // Arrange: xoá trắng cả hai ô nhập
    Allure.step("Arrange: xoá trắng ô Username và Password", () -> {
      loginScreen.enterUsername("");
      loginScreen.enterPassword("");
    });

    // Act
    Allure.step("Act: bấm LOGIN khi cả hai ô đang trống", loginScreen::clickLogin);

    // Assert: hiện đủ thông báo và không đăng nhập được
    Allure.step("Assert: hiện thông báo bắt buộc nhập cho từng ô", () -> {
      Assert.assertTrue(loginScreen.isUsernameRequiredMessageShown(),
          "Bỏ trống Username phải hiện thông báo 'Username is required!'");
      Assert.assertTrue(loginScreen.isPasswordRequiredMessageShown(),
          "Bỏ trống Password phải hiện thông báo 'Password is required'");
    });
    Allure.step("Assert: hiện thông báo chung và ở lại màn hình Login", () -> {
      Assert.assertTrue(loginScreen.isFillBothFieldsMessageShown(),
          "Bấm LOGIN khi trống phải hiện 'Please fill in your username and password to log in'");
      Assert.assertTrue(loginScreen.isStillOnLoginScreen(),
          "Bỏ trống thông tin thì phải ở lại màn hình Login");
    });
  }
}
