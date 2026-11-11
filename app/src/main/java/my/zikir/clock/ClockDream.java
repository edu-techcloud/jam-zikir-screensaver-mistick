package my.zikir.clock;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.util.Locale;

public class ClockDream extends DreamService {
    static volatile ClockDream instance;
    private FrameLayout root;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        final Context app = getApplicationContext();
        Dbg.log(app, "dream onCreate");
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Dbg.log(app, "CRASH: " + e);
                if (prev != null) {
                    prev.uncaughtException(t, e);
                }
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dbg.log(this, "dream onAttachedToWindow");
        setInteractive(false);
        setFullscreen(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        Shell.ready = false;
        root = Shell.build(this, false, true);
        setContentView(root);
        Dbg.log(this, "dream paparan dibina");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!Shell.ready && root != null) {
                    Dbg.log(ClockDream.this, "halaman tak aktif dalam 6s -> buka app (sandaran)");
                    try {
                        Intent i = new Intent(ClockDream.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra("fromDream", true);
                        startActivity(i);
                    } catch (Throwable t) {
                        Dbg.log(ClockDream.this, "sandaran gagal: " + t);
                    }
                }
            }
        }, 6000);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        Dbg.log(this, "dream onDreamingStarted");
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        Dbg.log(this, "dream onDreamingStopped");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dbg.log(this, "dream onDetachedFromWindow");
        handler.removeCallbacksAndMessages(null);
        if (root != null) {
            if (root.getChildCount() > 0 && root.getChildAt(0) instanceof WebView) {
                WebView w = (WebView) root.getChildAt(0);
                root.removeAllViews();
                w.destroy();
            }
            root = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }

    /** Data sistem sebenar Mi Stick untuk panel Sistem. */
    static class Bridge {
        private final Context c;
        Bridge(Context c) { this.c = c; }

        @JavascriptInterface
        public void ready() {
            Shell.ready = true;
            Dbg.log(c, "halaman aktif (rAF OK)");
        }

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
