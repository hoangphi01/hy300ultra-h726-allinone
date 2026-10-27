import android.content.Context;
import android.net.wifi.WifiManager;
import java.lang.reflect.Method;

/**
 * Holds a Wi-Fi lock system-wide so the AIC8800 chip does not enter power save during
 * casting/streaming (a common cause of intermittent AirPlay disconnects on this box, since
 * PhairPlay itself holds only a multicast lock). Runs via app_process from a privileged
 * context (root); gets a system Context through ActivityThread, acquires the lock, and blocks
 * forever on a Looper to keep the process (and the lock) alive.
 *
 * arg: lock mode. 4 = WIFI_MODE_FULL_LOW_LATENCY (best for streaming, pair with
 * `cmd wifi force-low-latency-mode enabled`), 3 = WIFI_MODE_FULL_HIGH_PERF. Default 4.
 */
public class HoldWifiLock {
    public static void main(String[] args) throws Exception {
        android.os.Looper.prepareMainLooper();
        Class<?> at = Class.forName("android.app.ActivityThread");
        Method systemMain = at.getMethod("systemMain");
        Object thread = systemMain.invoke(null);
        Method getSysCtx = at.getMethod("getSystemContext");
        Context ctx = (Context) getSysCtx.invoke(thread);
        if (ctx == null) { System.out.println("FAIL: null system context"); return; }
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wm == null) { System.out.println("FAIL: null WifiManager"); return; }
        int mode = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        WifiManager.WifiLock lock = wm.createWifiLock(mode, "hy300-wifilock");
        lock.setReferenceCounted(false);
        lock.acquire();
        System.out.println("WifiLock acquired mode=" + mode + " held=" + lock.isHeld());
        android.os.Looper.loop();
    }
}
