package my.zikir.clock;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.util.Locale;

public class ClockDream extends DreamService {
    private WebView web;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        web = new WebView(this);
        web.setBackgroundColor(0xFF050A18);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        web.addJavascriptInterface(new Bridge(getApplicationContext()), "ZK");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (web != null) { web.destroy(); web = null; }
    }

    /** Data sistem sebenar Mi Stick untuk panel Sistem. */
    static class Bridge {
        private final Context c;
        Bridge(Context c) { this.c = c; }

        @JavascriptInterface
        public String stats() {
            long up = SystemClock.elapsedRealtime();
            long tot = 1, av = 0, st = 1, sf = 0;
            String net = "NONE", ip = "";
            int rssi = 0, speed = 0;
            try {
                ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                tot = mi.totalMem; av = mi.availMem;
            } catch (Exception e) { }
            try {
                StatFs s = new StatFs(Environment.getDataDirectory().getPath());
                st = s.getTotalBytes(); sf = s.getAvailableBytes();
            } catch (Exception e) { }
            try {
                ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) {
                    net = ni.getTypeName();
                    if ("WIFI".equalsIgnoreCase(net)) {
                        WifiManager wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wi = wm.getConnectionInfo();
                        if (wi != null) {
                            rssi = wi.getRssi(); speed = wi.getLinkSpeed();
                            int i = wi.getIpAddress();
                            ip = (i & 255) + "." + ((i >> 8) & 255) + "." + ((i >> 16) & 255) + "." + ((i >> 24) & 255);
                        }
                    }
                }
            } catch (Exception e) { }
            return String.format(Locale.US,
                "{\"up\":%d,\"ramTotal\":%d,\"ramAvail\":%d,\"stTotal\":%d,\"stFree\":%d,\"net\":\"%s\",\"rssi\":%d,\"speed\":%d,\"ip\":\"%s\"}",
                up, tot, av, st, sf, net, rssi, speed, ip);
        }
    }
}
