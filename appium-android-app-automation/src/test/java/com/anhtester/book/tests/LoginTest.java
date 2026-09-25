package com.anhtester.book.tests;

import com.anhtester.book.annotations.TcId;
import com.anhtester.book.base.BaseTest;
import com.anhtester.book.screens.DashboardScreen;
import com.anhtester.book.screens.LoginScreen;
import com.anhtester.book.utils.TestDataGenerator;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test chức năng Đăng nhập của app Book (hybrid WebView).
 *
 * <p>Hành vi đã kiểm chứng trên emulator-5554: đăng nhập bằng email chưa đăng ký thì app hiện toast
 * "User not found." khoảng 3 giây và giữ nguyên màn Sign in. Toast không có trong cây accessibility nên
 * test chỉ khẳng định phần quan sát được — không bịa ra bước kiểm nội dung thông báo.
 */
@Feature("Đăng nhập")
public class LoginTest extends BaseTest {

    @TcId("BOOK_LOGIN_TC_002")
    @Story("Đăng nhập sai thông tin")
    @Test(description = "Đăng nhập thất bại với email chưa đăng ký — ở lại màn Sign in",
            groups = {"login", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Nhập email chưa đăng ký (sinh tự động theo timestamp) và mật khẩu bất kỳ rồi bấm "
            + "'Login account'; app phải từ chối đăng nhập và giữ nguyên màn Sign in.")
    public void testLoginWithUnregisteredEmail() {
        String email = TestDataGenerator.email("loginUnregistered");
        String password = TestDataGenerator.code("pw");
        DashboardScreen dashboardScreen = new DashboardScreen(driver());

        LoginScreen loginScreen = Allure.step("Arrange: Mở màn Sign in từ Dashboard", () -> {
            LoginScreen screen = dashboardScreen.openSignIn();
            Assert.assertTrue(screen.isDisplayed(),
                    "Màn Sign in phải hiển thị sau khi bấm thẻ 'Book management sign in'");
            return screen;
        });

        Allure.step("Act: Đăng nhập bằng email chưa đăng ký", () -> loginScreen.login(email, password));

        Allure.step("Assert: App từ chối đăng nhập và giữ nguyên màn Sign in", () ->
                Assert.assertTrue(loginScreen.staysOnSignIn(),
                        "Email chưa đăng ký thì không được vào hệ thống — phải ở lại màn Sign in"));
    }
}
