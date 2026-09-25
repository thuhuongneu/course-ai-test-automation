package com.anhtester.book.drivers;

import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Đọc capabilities của từng device từ {@code capabilities/devices.json}.
 *
 * <p>Mỗi device một khối riêng (udid, systemPort riêng). {@code testng.xml} chọn khối nào chạy trên
 * thread nào qua tham số {@code device}.
 */
public final class CapabilitiesManager {

    private static final Path DEVICES_FILE = Path.of("capabilities", "devices.json");

    private CapabilitiesManager() {
    }

    public static DesiredCapabilities load(String deviceKey) {
        Map<String, Object> devices = readDevicesFile();
        Object device = devices.get(deviceKey);
        if (!(device instanceof Map)) {
            throw new IllegalArgumentException("Không có device '" + deviceKey + "' trong "
                    + DEVICES_FILE + ". Các device đang khai: " + devices.keySet());
        }
        DesiredCapabilities caps = new DesiredCapabilities();
        ((Map<?, ?>) device).forEach((name, value) -> caps.setCapability(String.valueOf(name), value));
        return caps;
    }

    private static Map<String, Object> readDevicesFile() {
        try (Reader reader = Files.newBufferedReader(DEVICES_FILE, StandardCharsets.UTF_8)) {
            return new Json().toType(reader, Json.MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được " + DEVICES_FILE.toAbsolutePath(), e);
        }
    }
}
