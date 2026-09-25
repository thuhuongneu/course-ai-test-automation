package com.anhtester.book.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Màn Sign in.
 *
 * <p>Hai ô nhập có resource-id {@code _r_d_} / {@code _r_e_} do React tự sinh theo thứ tự render —
 * thêm/bớt một component là đổi, nên KHÔNG dùng. Thuộc tính hint cũng không ổn định — có lần WebView đưa
 * ra "Email address", có lần để rỗng (đã kiểm trên cùng emulator). Ô nhập không có content-desc, nên dùng
 * XPath tương đối bám theo NHÃN hiển thị: ô nhập nằm trong khối anh em ngay sau nhãn.
 */
public class LoginScreen extends BaseScreen {

    private final By signInHeading = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.TextView\").text(\"Sign in\")");
    private final By emailInput = AppiumBy.xpath(
            "//android.view.View[@text='Email address']/following-sibling::*//android.widget.EditText");
    private final By passwordInput = AppiumBy.xpath(
            "//android.view.View[@text='Password']/following-sibling::*//android.widget.EditText");
    private final By loginButton = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.Button\").text(\"Login account\")");

    public LoginScreen(AndroidDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return isVisible(signInHeading);
    }

    /**
     * Chờ hết thời gian chờ mặc định xem màn Sign in có biến mất không — vẫn còn thì {@code true}.
     *
     * <p>Đăng nhập bị từ chối thì app hiện toast lỗi (vd. "User not found.") nhưng toast KHÔNG nằm
     * trong cây accessibility (đã kiểm cả chế độ nhiều cửa sổ), nên chỉ khẳng định được việc ở lại
     * màn Sign in, không đọc được nội dung thông báo.
     */
    public boolean staysOnSignIn() {
        return !isGone(signInHeading);
    }

    @Step("Nhập email: {email}")
    public void enterEmail(String email) {
        type(emailInput, email);
    }

    /** Không dùng @Step: Allure ghi mọi tham số của @Step vào allure-results, mật khẩu sẽ nằm trong file. */
    public void enterPassword(String password) {
        Allure.step("Nhập mật khẩu", () -> type(passwordInput, password));
    }

    @Step("Bấm nút 'Login account'")
    public void submit() {
        tap(loginButton);
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        submit();
    }
}
