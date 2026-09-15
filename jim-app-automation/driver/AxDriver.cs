// =============================================================================
//  AxDriver - driver MSAA cho app ConnectPOS (Flutter Windows desktop)
// -----------------------------------------------------------------------------
//  VI SAO KHONG DUNG APPIUM / WinAppDriver:
//  App la Flutter -> ve toan bo UI len canvas. Da do thuc te tren build
//  "Jim - STAG - 26.09.14 - 09.43":
//    - UIA (ControlView / RawView / FindAll Descendants) : CHI thay 1 node FLUTTERVIEW
//    - MSAA / IAccessible                                : thay DAY DU cay element
//  WinAppDriver (nen tang cua appium-windows-driver) chay tren UIA -> khong dung duoc.
//  Driver nay noi thang IAccessible, dong vai tro tuong duong WinAppDriver.
//
//  GIAO THUC (stdin/stdout, cac truong phan cach bang TAB):
//    dump                                  -> NODE... @END
//    findRole   <role>                     -> NODE... @END   (sap xep tren->duoi)
//    clickName  <name>                     -> @END
//    clickRole  <role> <index>             -> @END
//    fill       <role> <index> <text>      -> @END
//    readValue  <role> <index>             -> VALUE<TAB>... @END
//    exists     <name>                     -> @END ok|fail
//    waitFor    <name> <timeoutMs>         -> @END ok|fail
//    waitGone   <name> <timeoutMs>         -> @END ok|fail
//    foreground                            -> @END
//    screenshot <path>                     -> @END
//    quit                                  -> thoat
//  Moi phan hoi ket thuc bang dong: @END <TAB> ok|fail <TAB> <thong diep>
// =============================================================================
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Globalization;
using System.Runtime.InteropServices;
using System.Text;
using Accessibility;

public class AxNode {
    public string Name = "", Value = "";
    public int Role, Depth, L, T, W, H;
    public string RoleName { get { return Ax.RoleToString(Role); } }
}

public static class Ax {
    // ------------------------------ P/Invoke ------------------------------
    [DllImport("oleacc.dll")]
    static extern int AccessibleObjectFromWindow(IntPtr hwnd, uint id, ref Guid iid,
        [In, Out, MarshalAs(UnmanagedType.IUnknown)] ref object ppv);
    [DllImport("oleacc.dll")]
    static extern int AccessibleChildren(IAccessible c, int start, int count,
        [Out, MarshalAs(UnmanagedType.LPArray, ArraySubType = UnmanagedType.Struct)] object[] rg, out int got);

    [DllImport("user32.dll")] static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] static extern void mouse_event(uint f, uint x, uint y, uint d, IntPtr e);
    [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] static extern bool ShowWindow(IntPtr h, int c);
    [DllImport("user32.dll")] static extern bool IsIconic(IntPtr h);
    [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] static extern bool AttachThreadInput(uint a, uint b, bool attach);
    [DllImport("kernel32.dll")] static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] static extern bool BringWindowToTop(IntPtr h);
    [DllImport("user32.dll")] static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint f);
    [DllImport("user32.dll")] static extern void SwitchToThisWindow(IntPtr h, bool altTab);
    [DllImport("user32.dll")] static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
    [DllImport("user32.dll")] static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll", EntryPoint = "SystemParametersInfo")]
    static extern bool SpiSet(uint action, uint param, IntPtr pv, uint winIni);
    [DllImport("user32.dll", EntryPoint = "SystemParametersInfo")]
    static extern bool SpiGet(uint action, uint param, ref bool pv, uint winIni);

    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }

    // ------------------------------ SendInput ------------------------------
    [StructLayout(LayoutKind.Sequential)]
    struct KEYBDINPUT { public ushort wVk, wScan; public uint dwFlags, time; public IntPtr dwExtraInfo; }
    // Chi dung SendInput cho ban phim; chuot dung mouse_event nen union khong can nhanh MOUSEINPUT.
    // Van phai la union co kich thuoc dung cua INPUT nen giu nguyen layout Explicit.
    [StructLayout(LayoutKind.Explicit, Size = 32)]
    struct INPUTUNION { [FieldOffset(0)] public KEYBDINPUT ki; }
    [StructLayout(LayoutKind.Sequential)]
    struct INPUT { public uint type; public INPUTUNION u; }
    [DllImport("user32.dll", SetLastError = true)]
    static extern uint SendInput(uint n, INPUT[] inputs, int size);

    const uint INPUT_KEYBOARD = 1, KEYEVENTF_KEYUP = 0x0002, KEYEVENTF_UNICODE = 0x0004;
    const ushort VK_BACK = 0x08, VK_END = 0x23;

    /// Do sau toi da khi duyet cay semantics. Cay sau hon muc nay thi element se bien mat
    /// am tham - de o mot cho de con sua khi app doi bo cuc.
    public const int MAX_DEPTH = 18;
    const int MIN_BACKSPACES = 10, BACKSPACE_MARGIN = 5;
    const uint SPI_SETSCREENREADER = 0x0047, SPI_GETSCREENREADER = 0x0046, SPIF_SENDCHANGE = 0x0002;
    static readonly Guid IID_IAccessible = new Guid("618736e0-3c3d-11cf-810c-00aa00389b71");
    const uint OBJID_CLIENT = 0xFFFFFFFC;

    public static string RoleToString(int r) {
        switch (r) {
            case 1: return "TITLEBAR";
            case 2: return "MENUBAR";
            case 3: return "SCROLLBAR";
            case 9: return "WINDOW";
            case 10: return "CLIENT";
            case 16: return "PANE";
            case 20: return "GROUPING";
            case 30: return "LINK";
            case 33: return "LIST";
            case 34: return "LISTITEM";
            case 40: return "PUSHBUTTON";
            case 41: return "STATICTEXT";
            case 42: return "TEXT";
            case 43: return "BUTTON";
            case 44: return "CHECKBUTTON";
            case 45: return "RADIOBUTTON";
            case 46: return "COMBOBOX";
            default: return "ROLE" + r;
        }
    }

    // --- Co screen-reader: Flutter chi sinh semantics khi phat hien tro ly man hinh.
    //     Co nay volatile (khong ghi registry, mat khi logoff). Luu gia tri cu de tra lai.
    static bool _srOriginal;
    static bool _srTouched;

    public static bool ScreenReaderEnabled {
        get { bool v = false; SpiGet(SPI_GETSCREENREADER, 0, ref v, 0); return v; }
    }

    public static void EnableScreenReader() {
        if (!_srTouched) { _srOriginal = ScreenReaderEnabled; _srTouched = true; }
        SpiSet(SPI_SETSCREENREADER, 1, IntPtr.Zero, SPIF_SENDCHANGE);
    }

    public static void RestoreScreenReader() {
        if (!_srTouched) return;
        SpiSet(SPI_SETSCREENREADER, _srOriginal ? 1u : 0u, IntPtr.Zero, SPIF_SENDCHANGE);
        _srTouched = false;
    }

    // ------------------------------ Cay element ------------------------------
    public static IntPtr FindWindow(string processName) {
        foreach (var p in System.Diagnostics.Process.GetProcessesByName(processName))
            if (p.MainWindowHandle != IntPtr.Zero) return p.MainWindowHandle;
        return IntPtr.Zero;
    }

    /// Duong dan file .exe THAT SU cua tien trinh dang chay, hay null neu khong chay.
    ///
    /// FindWindow chi biet TEN tien trinh (vd "ConnectPOS"), khong biet no chay tu ban build
    /// nao. Neu mot ban build cu da mo san tu truoc, driver se tuong "app da chay" va bo qua
    /// JIM_APP_PATH trong .env - du .env da tro sang ban khac. Ham nay cho phep ben goi so
    /// sanh duong dan that de quyet dinh co can dong/mo lai app hay khong.
    public static string RunningExePath(string processName) {
        foreach (var p in System.Diagnostics.Process.GetProcessesByName(processName)) {
            if (p.MainWindowHandle == IntPtr.Zero) continue;
            try { return p.MainModule.FileName; } catch { return null; }
        }
        return null;
    }

    static IAccessible RootOf(IntPtr hwnd) {
        object o = null;
        Guid iid = IID_IAccessible;
        int hr = AccessibleObjectFromWindow(hwnd, OBJID_CLIENT, ref iid, ref o);
        if (hr != 0 || o == null) throw new Exception("AccessibleObjectFromWindow hr=0x" + hr.ToString("X8"));
        return (IAccessible)o;
    }

    static List<IAccessible> Children(IAccessible a) {
        var res = new List<IAccessible>();
        int cnt;
        try { cnt = a.accChildCount; } catch { return res; }
        if (cnt <= 0) return res;
        var arr = new object[cnt];
        int got;
        if (AccessibleChildren(a, 0, cnt, arr, out got) != 0) return res;
        for (int i = 0; i < got; i++) {
            var ia = arr[i] as IAccessible;
            if (ia != null) res.Add(ia);
        }
        return res;
    }

    public static List<AxNode> Dump(IntPtr hwnd, int maxDepth) {
        var res = new List<AxNode>();
        Rec(RootOf(hwnd), 1, res, maxDepth);
        return res;
    }

    static void Rec(IAccessible n, int d, List<AxNode> res, int maxDepth) {
        if (d > maxDepth) return;
        foreach (var c in Children(n)) {
            var node = new AxNode();
            node.Depth = d;
            try { node.Name = c.get_accName(0) ?? ""; } catch { }
            try { node.Value = c.get_accValue(0) ?? ""; } catch { }
            try { node.Role = Convert.ToInt32(c.get_accRole(0)); } catch { }
            try { c.accLocation(out node.L, out node.T, out node.W, out node.H, 0); } catch { }
            res.Add(node);
            Rec(c, d + 1, res, maxDepth);
        }
    }

    /// Cac node cung role, sap xep theo vi tri tren man hinh (tren->duoi, trai->phai).
    /// CAN THIET: accName cua o input chinh la placeholder -> BIEN MAT khi o co noi dung,
    /// nen khong the dung ten lam locator cho o nhap.
    public static List<AxNode> ByRole(IntPtr hwnd, string role) {
        var res = new List<AxNode>();
        foreach (var n in Dump(hwnd, MAX_DEPTH))
            if (n.RoleName == role && n.W > 0 && n.H > 0) res.Add(n);
        res.Sort(delegate (AxNode a, AxNode b) {
            return a.T != b.T ? a.T.CompareTo(b.T) : a.L.CompareTo(b.L);
        });
        return res;
    }

    public static AxNode ByName(IntPtr hwnd, string name) {
        foreach (var n in Dump(hwnd, MAX_DEPTH))
            if (string.Equals(n.Name, name, StringComparison.OrdinalIgnoreCase) && n.W > 0) return n;
        return null;
    }

    // ------------------------------ Foreground ------------------------------
    static bool TryForegroundAlt(IntPtr hwnd) {
        if (IsIconic(hwnd)) { ShowWindow(hwnd, 9); System.Threading.Thread.Sleep(400); }
        keybd_event(0x12, 0, 0, IntPtr.Zero);                  // ALT down: nha khoa foreground
        System.Threading.Thread.Sleep(40);
        SwitchToThisWindow(hwnd, true);
        BringWindowToTop(hwnd);
        SetForegroundWindow(hwnd);
        System.Threading.Thread.Sleep(40);
        keybd_event(0x12, 0, KEYEVENTF_KEYUP, IntPtr.Zero);    // ALT up
        System.Threading.Thread.Sleep(400);
        return GetForegroundWindow() == hwnd;
    }

    static bool TryForegroundAttach(IntPtr hwnd) {
        uint pid;
        uint cur = GetCurrentThreadId();
        uint fgTid = GetWindowThreadProcessId(GetForegroundWindow(), out pid);
        uint tgTid = GetWindowThreadProcessId(hwnd, out pid);
        AttachThreadInput(cur, fgTid, true);
        AttachThreadInput(cur, tgTid, true);
        SetWindowPos(hwnd, new IntPtr(-1), 0, 0, 0, 0, 0x0001 | 0x0002 | 0x0040);
        SetWindowPos(hwnd, new IntPtr(-2), 0, 0, 0, 0, 0x0001 | 0x0002 | 0x0040);
        BringWindowToTop(hwnd);
        SetForegroundWindow(hwnd);
        AttachThreadInput(cur, tgTid, false);
        AttachThreadInput(cur, fgTid, false);
        System.Threading.Thread.Sleep(350);
        return GetForegroundWindow() == hwnd;
    }

    /// CHOT AN TOAN: khong dua duoc app len foreground thi NEM LOI, tuyet doi khong
    /// go phim - tranh mat khau roi nham sang cua so khac dang o tren.
    public static void EnsureForeground(IntPtr hwnd) {
        for (int i = 0; i < 5; i++) {
            if (GetForegroundWindow() == hwnd) return;
            if (IsSessionLocked())
                throw new Exception("MAY DANG KHOA MAN HINH. Automation desktop dieu khien chuot/ban "
                    + "phim that nen khong the chay khi may khoa. Hay mo khoa may, tat che do tu khoa "
                    + "man hinh trong thoi gian chay suite, roi chay lai.");
            if (TryForegroundAlt(hwnd)) return;
            if (TryForegroundAttach(hwnd)) return;
            System.Threading.Thread.Sleep(500);
        }
        throw new Exception("Khong dua duoc app len foreground (dang o '" + ForegroundName()
            + "') - dung lai de tranh go nham cua so khac");
    }

    /// Nhan dien man hinh khoa de bao loi cho ro, thay vi de nguoi chay tu doan tu thong bao
    /// "khong dua duoc app len foreground".
    ///
    /// KHONG dua vao su ton tai cua tien trinh LockApp/LogonUI: tren Windows 11, LockApp.exe VAN
    /// nam trong danh sach tien trinh (o trang thai treo) sau khi da mo khoa - kiem tra kieu do se
    /// bao nham la dang khoa.
    ///
    /// Cach dung: khi phien bi khoa, input desktop la secure desktop cua Winlogon nen
    /// OpenInputDesktop that bai. Do la dau hieu tin cay.
    static bool IsSessionLocked() {
        IntPtr desk = OpenInputDesktop(0, false, DESKTOP_SWITCHDESKTOP);
        if (desk == IntPtr.Zero) return true;
        CloseDesktop(desk);
        return false;
    }

    [DllImport("user32.dll", SetLastError = true)]
    static extern IntPtr OpenInputDesktop(uint flags, bool inherit, uint desiredAccess);
    [DllImport("user32.dll", SetLastError = true)]
    static extern bool CloseDesktop(IntPtr desktop);
    const uint DESKTOP_SWITCHDESKTOP = 0x0100;

    static string ForegroundName() {
        uint pid;
        GetWindowThreadProcessId(GetForegroundWindow(), out pid);
        try { return System.Diagnostics.Process.GetProcessById((int)pid).ProcessName; } catch { return "?"; }
    }

    /// Ghi lai cua so foreground NGAY TRUOC khi go phim.
    ///
    /// Khong co dong log nay thi mot lan mat focus chi de lai trieu chung "thieu 1 ky tu" - khong
    /// cach nao biet ky tu do roi sang dau, va rat de quy oan cho driver. Voi CI (khong ai ngoi
    /// nhin man hinh) day la bang chung duy nhat phan biet "driver go hong" voi "cua so khac chen
    /// len". Log ra stdout theo dung dinh dang giao thuc de ben Java nhan ra va giu lai.
    public static void LogForeground(IntPtr hwnd) {
        IntPtr fg = GetForegroundWindow();
        Console.WriteLine("FG\t" + hwnd.ToInt64() + "\t" + fg.ToInt64() + "\t" + ForegroundName());
        Console.Out.Flush();
    }

    // ------------------------------ Tuong tac ------------------------------
    public static void Click(IntPtr hwnd, AxNode n) {
        EnsureForeground(hwnd);
        int cx = n.L + n.W / 2, cy = n.T + n.H / 2;   // toa do LAY TU ELEMENT, khong hardcode
        SetCursorPos(cx, cy);
        System.Threading.Thread.Sleep(120);
        mouse_event(0x0002, 0, 0, 0, IntPtr.Zero);    // LEFTDOWN
        mouse_event(0x0004, 0, 0, 0, IntPtr.Zero);    // LEFTUP
        System.Threading.Thread.Sleep(250);
    }

    static void SendChar(char c) {
        var i = new INPUT[2];
        i[0].type = INPUT_KEYBOARD; i[0].u.ki.wScan = c; i[0].u.ki.dwFlags = KEYEVENTF_UNICODE;
        i[1].type = INPUT_KEYBOARD; i[1].u.ki.wScan = c; i[1].u.ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP;
        SendInput(2, i, Marshal.SizeOf(typeof(INPUT)));
    }

    static void SendVk(ushort vk) {
        var i = new INPUT[2];
        i[0].type = INPUT_KEYBOARD; i[0].u.ki.wVk = vk;
        i[1].type = INPUT_KEYBOARD; i[1].u.ki.wVk = vk; i[1].u.ki.dwFlags = KEYEVENTF_KEYUP;
        SendInput(2, i, Marshal.SizeOf(typeof(INPUT)));
    }

    /// Xoa sach roi go text.
    ///
    /// Dung SendInput chu KHONG dung SendKeys - da kiem chung tren app nay:
    ///   - o input Flutter KHONG nhan Ctrl+A  -> phai xoa bang End + Backspace
    ///   - SendKeys.SendWait go bi thieu / nhan doi ky tu
    ///
    /// App co the mat foreground giua chung (cua so khac chen len, hop thoai dong...).
    /// Khi do PHAI dung go ngay - khong duoc de ky tu roi sang cua so khac - roi thu lai
    /// ca thao tac tu dau. Thu lai an toan vi moi lan deu xoa sach o truoc khi go.
    /// Go text vao o thu <index> cua <role>, sau do DOC LAI de kiem chung.
    ///
    /// Kiem chung bang DO DAI chu khong bang noi dung: o mat khau phoi ra chuoi dau cham tron
    /// (mot dau cho mot ky tu) chu khong phoi mat khau that - so sanh do dai dung duoc cho ca
    /// o thuong lan o mat khau, va khong lam lo mat khau ra log.
    ///
    /// Khong kiem chung thi mot lan xoa/go hut se troi qua am tham: o van con noi dung cu va
    /// test se kiem thu nham mot kich ban khac han.
    public static string FillVerified(IntPtr hwnd, string role, int index, string text,
            out int attempts) {
        const int maxAttempts = 3;
        string lastProblem = "";
        attempts = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            attempts = attempt;
            try {
                // THU TU BAT BUOC: dua app len foreground TRUOC roi moi doc toa do.
                // Lam nguoc lai thi toa do co the da cu: viec dua cua so len truoc co the
                // doi kich thuoc/vi tri cua so, va cu click se roi lech ra ngoai element.
                EnsureForeground(hwnd);
                // NodeAt nam TRONG try: o co the tam thoi bien mat khoi cay (vd. app vua bat mot
                // hop thoai che len) - do la trang thai tam, dang thu lai chu khong phai loi that.
                FillOnce(hwnd, NodeAt(hwnd, role, index), text);
            } catch (ForegroundLostException ex) {
                lastProblem = ex.Message;
                System.Threading.Thread.Sleep(500);
                continue;
            } catch (NodeMissingException ex) {
                lastProblem = ex.Message;
                System.Threading.Thread.Sleep(600);
                continue;
            }
            AxNode after = NodeAt(hwnd, role, index);
            string actual = after == null ? null : (after.Value ?? "");
            if (actual != null && actual.Length == text.Length) return actual;
            lastProblem = "o co " + (actual == null ? "(mat node)" : actual.Length.ToString())
                + " ky tu, mong doi " + text.Length;
            System.Threading.Thread.Sleep(300);
        }
        throw new Exception("Go text that bai sau " + maxAttempts + " lan thu (" + lastProblem + ")");
    }

    public static AxNode NodeAt(IntPtr hwnd, string role, int index) {
        var l = ByRole(hwnd, role);
        if (index < 0 || index >= l.Count)
            throw new NodeMissingException("Khong co node role=" + role + " index=" + index
                + " (chi tim thay " + l.Count + ")");
        return l[index];
    }

    class ForegroundLostException : Exception {
        public ForegroundLostException(string m) : base(m) { }
    }

    /// Node khong co trong cay o thoi diem nay. Co the la trang thai TAM (app vua bat mot hop
    /// thoai che len, hoac man hinh dang render lai) nen dang thu lai, khac voi loi that su.
    public class NodeMissingException : Exception {
        public NodeMissingException(string m) : base(m) { }
    }

    static void FillOnce(IntPtr hwnd, AxNode n, string text) {
        Click(hwnd, n);
        if (GetForegroundWindow() != hwnd)
            throw new ForegroundLostException("App mat foreground truoc khi go - cua so dang o tren "
                + "la '" + ForegroundName() + "'");
        SendVk(VK_END);
        System.Threading.Thread.Sleep(80);
        // So lan Backspace tinh theo do dai dang co (cong du phong), thay vi mot con so co dinh:
        // noi dung dai hon con so do se khong duoc xoa het va lan go sau bi noi vao duoi.
        int toDelete = Math.Max((n.Value ?? "").Length, MIN_BACKSPACES) + BACKSPACE_MARGIN;
        for (int i = 0; i < toDelete; i++) { SendVk(VK_BACK); System.Threading.Thread.Sleep(15); }
        System.Threading.Thread.Sleep(150);
        LogForeground(hwnd);
        foreach (char c in text) {
            if (GetForegroundWindow() != hwnd)
                throw new ForegroundLostException("App mat foreground giua chung khi dang go - cua so "
                    + "dang o tren la '" + ForegroundName() + "'");
            SendChar(c);
            System.Threading.Thread.Sleep(35);
        }
        System.Threading.Thread.Sleep(300);
    }

    public static bool WaitFor(IntPtr hwnd, string name, int timeoutMs) {
        var sw = System.Diagnostics.Stopwatch.StartNew();
        while (sw.ElapsedMilliseconds < timeoutMs) {
            if (ByName(hwnd, name) != null) return true;
            System.Threading.Thread.Sleep(400);
        }
        return false;
    }

    public static bool WaitGone(IntPtr hwnd, string name, int timeoutMs) {
        var sw = System.Diagnostics.Stopwatch.StartNew();
        while (sw.ElapsedMilliseconds < timeoutMs) {
            if (ByName(hwnd, name) == null) return true;
            System.Threading.Thread.Sleep(400);
        }
        return false;
    }

    public static void Screenshot(IntPtr hwnd, string path) {
        EnsureForeground(hwnd);
        System.Threading.Thread.Sleep(250);
        RECT r;
        GetWindowRect(hwnd, out r);
        int w = Math.Max(1, r.Right - r.Left), h = Math.Max(1, r.Bottom - r.Top);
        using (var bmp = new Bitmap(w, h))
        using (var g = Graphics.FromImage(bmp)) {
            g.CopyFromScreen(r.Left, r.Top, 0, 0, new Size(w, h));
            bmp.Save(path, ImageFormat.Png);
        }
    }
}

public static class Program {
    const string PROC = "ConnectPOS";

    static string Esc(string s) {
        if (s == null) return "";
        return s.Replace("\\", "\\\\").Replace("\t", "\\t").Replace("\r", "").Replace("\n", "\\n");
    }

    /// Giai ma chuoi da escape. PHAI quet mot luot tu trai sang phai, KHONG duoc dung chuoi
    /// Replace noi tiep: Esc nhan doi dau '\' truoc, nen "C:\temp" di qua chuoi Replace se
    /// bien thanh "C:\<TAB>emp".
    static string Unesc(string s) {
        if (s == null) return "";
        var sb = new StringBuilder(s.Length);
        for (int i = 0; i < s.Length; i++) {
            char c = s[i];
            if (c == '\\' && i + 1 < s.Length) {
                char n = s[++i];
                sb.Append(n == 'n' ? '\n' : n == 't' ? '\t' : n);
            } else {
                sb.Append(c);
            }
        }
        return sb.ToString();
    }

    static void Node(AxNode n) {
        Console.WriteLine("NODE\t" + n.Depth + "\t" + n.RoleName + "\t" + Esc(n.Name) + "\t"
            + Esc(n.Value) + "\t" + n.L + "\t" + n.T + "\t" + n.W + "\t" + n.H);
    }

    static void End(bool ok, string msg) {
        Console.WriteLine("@END\t" + (ok ? "ok" : "fail") + "\t" + Esc(msg));
        Console.Out.Flush();
    }

    static IntPtr Hwnd() {
        IntPtr cur = Ax.FindWindow(PROC);
        if (cur == IntPtr.Zero)
            throw new Exception("Khong thay cua so app '" + PROC + "' - app da chay chua?");
        return cur;
    }

    static AxNode RoleAt(IntPtr h, string role, int idx) {
        return Ax.NodeAt(h, role, idx);
    }

    public static int Main(string[] args) {
        Console.OutputEncoding = Encoding.UTF8;
        Ax.EnableScreenReader();                  // bat semantics cua Flutter
        Console.WriteLine("@READY\tscreenReader=" + Ax.ScreenReaderEnabled);
        Console.Out.Flush();

        string line;
        while ((line = Console.ReadLine()) != null) {
            var p = line.Split('\t');
            string cmd = p[0].Trim();
            try {
                switch (cmd) {
                    case "quit":
                        Ax.RestoreScreenReader();
                        End(true, "bye");
                        return 0;

                    case "screenReaderOn":
                        Ax.EnableScreenReader();
                        End(true, "on=" + Ax.ScreenReaderEnabled);
                        break;

                    case "screenReaderRestore":
                        Ax.RestoreScreenReader();
                        End(true, "restored");
                        break;

                    case "alive":
                        End(Ax.FindWindow(PROC) != IntPtr.Zero, "app window");
                        break;

                    case "runningExePath": {
                        string exePath = Ax.RunningExePath(PROC);
                        if (exePath == null) { End(false, "app khong chay"); break; }
                        Console.WriteLine("PATH\t" + Esc(exePath));
                        End(true, "running");
                        break;
                    }

                    case "dump":
                        foreach (var n in Ax.Dump(Hwnd(), Ax.MAX_DEPTH)) Node(n);
                        End(true, "dumped");
                        break;

                    case "findRole":
                        foreach (var n in Ax.ByRole(Hwnd(), p[1])) Node(n);
                        End(true, "found");
                        break;

                    case "foreground":
                        Ax.EnsureForeground(Hwnd());
                        End(true, "foreground");
                        break;

                    // Luon EnsureForeground TRUOC khi doc toa do element: dua cua so len truoc
                    // co the lam no doi kich thuoc, doc toa do truoc thi click se roi lech.
                    case "clickName": {
                        IntPtr h = Hwnd();
                        Ax.EnsureForeground(h);
                        AxNode n = Ax.ByName(h, Unesc(p[1]));
                        if (n == null) { End(false, "Khong thay element ten '" + p[1] + "'"); break; }
                        Ax.Click(h, n);
                        End(true, "clicked");
                        break;
                    }

                    case "clickRole": {
                        IntPtr h = Hwnd();
                        Ax.EnsureForeground(h);
                        Ax.Click(h, RoleAt(h, p[1], int.Parse(p[2], CultureInfo.InvariantCulture)));
                        End(true, "clicked");
                        break;
                    }

                    // Bao luon SO LAN THU, ke ca khi thanh cong: mot lan go phai lam lai (bi cua so
                    // khac chen len, o tam bien mat khoi cay) van cho ket qua dung nen se troi qua
                    // am tham - trong khi do chinh la dau hieu som cua moi truong khong on dinh.
                    case "fill": {
                        int attempts;
                        string got = Ax.FillVerified(Hwnd(), p[1],
                            int.Parse(p[2], CultureInfo.InvariantCulture), Unesc(p[3]), out attempts);
                        End(true, "filled sau " + attempts + " lan thu, doc lai " + got.Length + " ky tu");
                        break;
                    }

                    case "readValue": {
                        AxNode n = RoleAt(Hwnd(), p[1], int.Parse(p[2], CultureInfo.InvariantCulture));
                        Console.WriteLine("VALUE\t" + Esc(n.Value));
                        End(true, "read");
                        break;
                    }

                    case "exists":
                        End(Ax.ByName(Hwnd(), Unesc(p[1])) != null, "exists");
                        break;

                    case "waitFor":
                        End(Ax.WaitFor(Hwnd(), Unesc(p[1]), int.Parse(p[2], CultureInfo.InvariantCulture)),
                            "waitFor " + p[1]);
                        break;

                    case "waitGone":
                        End(Ax.WaitGone(Hwnd(), Unesc(p[1]), int.Parse(p[2], CultureInfo.InvariantCulture)),
                            "waitGone " + p[1]);
                        break;

                    case "screenshot":
                        Ax.Screenshot(Hwnd(), Unesc(p[1]));
                        End(true, "saved");
                        break;

                    default:
                        End(false, "Lenh khong ho tro: " + cmd);
                        break;
                }
            } catch (Exception ex) {
                End(false, ex.Message);
            }
        }
        Ax.RestoreScreenReader();
        return 0;
    }
}
