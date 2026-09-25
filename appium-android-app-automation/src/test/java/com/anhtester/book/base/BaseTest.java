package com.anhtester.book.base;

import com.anhtester.book.drivers.AppiumDriverFactory;
import com.anhtester.book.utils.ScreenshotUtil;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Lớp nền cho mọi test: mỗi test mở một phiên Appium riêng trên device do {@code testng.xml} chỉ định
 * và đóng phiên ở teardown — test không phụ thuộc nhau, chạy song song theo device được.
 */
public abstract class BaseTest {

    /** @Optional để chạy lẻ 1 class (IDE, -Dtest=...) vẫn có device mặc định. */
    @Parameters("device")
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional("android_emulator_5554") String device) {
        AppiumDriverFactory.initDriver(device);
    }

    /** Đính ảnh trạng thái cuối cho MỌI test — cả PASS lẫn FAIL — rồi đóng phiên. */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        AndroidDriver driver = AppiumDriverFactory.getDriver();
        if (driver == null) {
            return;
        }
        try {
            String name = result.isSuccess() ? "trang_thai_cuoi_cua_test" : "trang_thai_khi_that_bai";
            ScreenshotUtil.captureAndAttach(driver, name);
        } catch (RuntimeException e) {
            // Không chụp được ảnh thì ghi chú lại — không để lỗi chụp ảnh che mất lỗi thật của test
            Allure.addAttachment("khong_chup_duoc_man_hinh", "text/plain", String.valueOf(e.getMessage()));
        } finally {
            AppiumDriverFactory.quitDriver();
        }
    }

    protected AndroidDriver driver() {
        return AppiumDriverFactory.getDriver();
    }
}
