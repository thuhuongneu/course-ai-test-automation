package com.jim.pos.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client dieu khien app ConnectPOS desktop - dong vai tro giong WebDriver/Appium driver.
 *
 * <p><b>Vi sao khong dung Appium:</b> app la Flutter nen ve UI len canvas. Do thuc te tren build
 * STAG 26.09.14: UI Automation (UIA) chi nhin thay duy nhat mot node {@code FLUTTERVIEW}, trong khi
 * MSAA/IAccessible thay day du cay element. WinAppDriver - nen tang cua {@code appium-windows-driver}
 * - chay tren UIA, nen khong dieu khien duoc app nay. Lop nay giao tiep voi mot tien trinh driver
 * viet bang C# noi thang IAccessible.
 *
 * <p>Driver duoc bien dich bang {@code csc.exe} co san trong Windows nen <b>khong can cai dat gi
 * them</b>.
 */
public class AxDriver implements AutoCloseable {

  private static final Path CSC = Paths.get(
      System.getenv().getOrDefault("SystemRoot", "C:\\Windows"),
      "Microsoft.NET", "Framework64", "v4.0.30319", "csc.exe");

  private final Process process;
  private final BufferedWriter stdin;
  private final BufferedReader stdout;

  /** Ket qua mot lenh: trang thai + cac node tra ve (neu co). */
  private static final class Reply {
    boolean ok;
    String message = "";
    final List<AxNode> nodes = new ArrayList<>();
    String value;
  }

  public AxDriver(Path projectRoot) {
    Path exe = buildDriverIfNeeded(projectRoot);
    try {
      ProcessBuilder pb = new ProcessBuilder(exe.toString());
      pb.redirectErrorStream(true);
      this.process = pb.start();
      this.stdin = new BufferedWriter(
          new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      this.stdout = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

      String ready = stdout.readLine();
      if (ready == null || !ready.startsWith("@READY")) {
        throw new IllegalStateException("Driver khong khoi dong duoc, nhan duoc: " + ready);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Khong chay duoc driver: " + exe, e);
    }
  }

  /** Bien dich AxDriver.cs khi chua co .exe hoac khi source moi hon ban da bien dich. */
  private static Path buildDriverIfNeeded(Path projectRoot) {
    Path source = projectRoot.resolve("driver").resolve("AxDriver.cs");
    Path exe = projectRoot.resolve("target").resolve("driver").resolve("AxDriver.exe");
    try {
      boolean stale = !Files.exists(exe)
          || Files.getLastModifiedTime(source).toMillis() > Files.getLastModifiedTime(exe).toMillis();
      if (!stale) {
        return exe;
      }
      if (!Files.exists(CSC)) {
        throw new IllegalStateException("Khong tim thay csc.exe tai " + CSC
            + " - can .NET Framework 4.x (co san tren moi ban Windows).");
      }
      Files.createDirectories(exe.getParent());
      Process p = new ProcessBuilder(CSC.toString(), "-nologo", "-optimize+", "-target:exe",
          "-out:" + exe, "-r:System.dll", "-r:System.Drawing.dll", "-r:Accessibility.dll",
          source.toString())
          .redirectErrorStream(true)
          .start();
      String output;
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
        output = reader.lines().reduce("", (a, b) -> a + System.lineSeparator() + b);
      }
      if (p.waitFor() != 0) {
        throw new IllegalStateException("Bien dich driver that bai:" + output);
      }
      return exe;
    } catch (IOException e) {
      throw new UncheckedIOException("Loi khi bien dich driver", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Bi ngat khi bien dich driver", e);
    }
  }

  // ------------------------------ Giao thuc ------------------------------

  private synchronized Reply send(String... parts) {
    try {
      stdin.write(String.join("\t", parts));
      stdin.newLine();
      stdin.flush();

      Reply reply = new Reply();
      List<String> unexpected = new ArrayList<>();
      String line;
      while ((line = stdout.readLine()) != null) {
        if (line.startsWith("@END\t")) {
          String[] f = line.split("\t", -1);
          reply.ok = "ok".equals(f[1]);
          reply.message = f.length > 2 ? unescape(f[2]) : "";
          return reply;
        }
        if (line.startsWith("NODE\t")) {
          String[] f = line.split("\t", -1);
          reply.nodes.add(new AxNode(
              Integer.parseInt(f[1]), f[2], unescape(f[3]), unescape(f[4]),
              Integer.parseInt(f[5]), Integer.parseInt(f[6]),
              Integer.parseInt(f[7]), Integer.parseInt(f[8])));
        } else if (line.startsWith("VALUE\t")) {
          reply.value = unescape(line.split("\t", -1)[1]);
        } else {
          // stderr cua driver duoc gop vao stdout: giu lai de bao loi cho ro khi driver chet,
          // thay vi bo im lang roi chi con mot cau "loi giao tiep" khong the debug duoc
          unexpected.add(line);
        }
      }
      throw new IllegalStateException("Driver dong ket noi giua chung (con song="
          + process.isAlive() + ")"
          + (unexpected.isEmpty() ? "" : System.lineSeparator() + String.join(System.lineSeparator(),
              unexpected)));
    } catch (IOException e) {
      throw new UncheckedIOException("Loi giao tiep voi driver", e);
    }
  }

  private Reply require(String... parts) {
    Reply r = send(parts);
    if (!r.ok) {
      throw new AxException(r.message);
    }
    return r;
  }

  private static String escape(String s) {
    return s == null ? ""
        : s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "");
  }

  /**
   * Giai ma chuoi da escape. PHAI quet mot luot tu trai sang phai, KHONG duoc dung chuoi
   * {@code replace} noi tiep: {@code escape} nhan doi dau {@code \} truoc, nen mot chuoi nhu
   * {@code C:\temp} di qua day se bien thanh {@code C:\<TAB>emp} neu giai ma bang replace.
   */
  private static String unescape(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' && i + 1 < s.length()) {
        char next = s.charAt(++i);
        out.append(next == 'n' ? '\n' : next == 't' ? '\t' : next);
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  // ------------------------------ API ------------------------------

  /** App co dang chay khong. */
  public boolean isAppRunning() {
    return send("alive").ok;
  }

  /** Toan bo cay element hien tai - dung khi debug. */
  public List<AxNode> dump() {
    return require("dump").nodes;
  }

  /**
   * Cac element cung role, sap xep theo vi tri tren man hinh (tren xuong duoi, trai sang phai).
   * Day la cach dinh vi o nhap lieu, vi placeholder ({@code name}) bien mat khi o co noi dung.
   */
  public List<AxNode> findByRole(String role) {
    return require("findRole", role).nodes;
  }

  /** Bao hieu app da chet, phat ra tu phia driver. */
  private static final String APP_GONE = "Khong thay cua so app";

  /**
   * Element dau tien co ten dung bang {@code name} (khong phan biet hoa thuong).
   *
   * <p>Phan biet "khong co element" voi "app da chet": neu app crash, moi phep kiem tra se tra ve
   * false va mot assertion kieu {@code assertFalse(...)} se PASS vi ly do sai hoan toan.
   */
  public boolean exists(String name) {
    Reply r = send("exists", escape(name));
    failIfAppGone(r);
    return r.ok;
  }

  private static void failIfAppGone(Reply r) {
    if (!r.ok && r.message != null && r.message.contains(APP_GONE)) {
      throw new AxException(r.message);
    }
  }

  /** Dua app len foreground; nem loi neu khong the - de khong go phim nham cua so khac. */
  public void focus() {
    require("foreground");
  }

  public void clickByName(String name) {
    require("clickName", escape(name));
  }

  /** Xoa sach o nhap roi go {@code text} vao do. */
  public void fill(String role, int index, String text) {
    require("fill", role, String.valueOf(index), escape(text));
  }

  /**
   * Gia tri hien tai cua o nhap.
   *
   * <p>O mat khau phoi ra chuoi dau cham tron - <b>mot dau cho mot ky tu</b> - chu khong phoi noi
   * dung that, nen doc duoc <b>do dai</b> ma khong lam lo mat khau ra log hay report.
   */
  public String readValue(String role, int index) {
    Reply r = require("readValue", role, String.valueOf(index));
    return r.value == null ? "" : r.value;
  }

  /** Cho den khi element ten {@code name} xuat hien. Tra ve false neu het thoi gian. */
  public boolean waitForName(String name, Duration timeout) {
    Reply r = send("waitFor", escape(name), String.valueOf(timeout.toMillis()));
    failIfAppGone(r);
    return r.ok;
  }

  /** Cho den khi element ten {@code name} bien mat. Tra ve false neu het thoi gian. */
  public boolean waitUntilGone(String name, Duration timeout) {
    Reply r = send("waitGone", escape(name), String.valueOf(timeout.toMillis()));
    failIfAppGone(r);
    return r.ok;
  }

  public void screenshot(Path target) {
    require("screenshot", escape(target.toAbsolutePath().toString()));
  }

  @Override
  public void close() {
    try {
      send("quit");
    } catch (RuntimeException ignored) {
      // driver co the da chet - khong de loi don dep che mat loi test that su
    } finally {
      closeQuietly(stdin);
      closeQuietly(stdout);
      process.destroy();
      try {
        process.waitFor(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void closeQuietly(Closeable c) {
    try {
      c.close();
    } catch (IOException ignored) {
      // dang don dep - khong co gi de lam them
    }
  }

  /** Loi phat sinh tu driver (khong tim thay element, mat foreground...). */
  public static class AxException extends RuntimeException {
    public AxException(String message) {
      super(message);
    }
  }
}
