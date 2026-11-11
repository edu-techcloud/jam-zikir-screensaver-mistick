package my.zikir.clock;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Bina paparan WebView + baris status diagnosis (digunakan oleh screensaver dan app biasa). */
final class Shell {
    private static final int RED = 0xFFFF6B5E;
    static volatile boolean ready = false;
    private static int deaths = 0;

    static FrameLayout build(final Context ctx, boolean showLog, boolean lite) {
        final FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(0xFF050A18);
        final Handler handler = new Handler(Looper.getMainLooper());

        final TextView status = new TextView(ctx);
        status.setTextColor(0xFFB6C3DD);
        status.setTextSize(20);
        status.setBackgroundColor(0x99000000);
        status.setPadding(24, 12, 24, 12);
        status.setText("Jam Zikir: memulakan WebView...");

        String logText = showLog ? Dbg.read(ctx) : "";
        deaths = 0;

        try {
            makeWeb(ctx, root, status, handler, lite);
        } catch (Throwable t) {
            Dbg.log(ctx, "WebView gagal: " + t);
            fail(status, "WebView gagal dimulakan: " + t);
        }

        root.addView(status, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START));

        if (showLog) {
            final TextView lv = new TextView(ctx);
            lv.setTextColor(0xFFFFE29A);
            lv.setTextSize(16);
            lv.setBackgroundColor(0xCC000000);
            lv.setPadding(20, 10, 20, 10);
            lv.setText("LOG screensaver terakhir:\n" + logText);
            root.addView(lv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.START));
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lv.setVisibility(View.GONE);
                }
            }, 25000);
        }
        return root;
    }

    private static void makeWeb(final Context ctx, final FrameLayout root, final TextView status,
                                final Handler handler, final boolean lite) {
        WebView web = new WebView(ctx);
        web.setBackgroundColor(0xFF050A18);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                Dbg.log(ctx, "halaman dimuat" + (lite ? " (mod ringan)" : ""));
                status.setText("Halaman dimuat OK");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (status.getCurrentTextColor() != RED) {
                            status.setVisibility(View.GONE);
                        }
                    }
                }, 8000);
            }

            @Override
            public void onReceivedError(WebView v, int code, String desc, String url) {
                Dbg.log(ctx, "ralat muat: " + desc);
                fail(status, "Ralat muat: " + desc + " (" + url + ")");
            }

            @Override
            public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                deaths++;
                Dbg.log(ctx, "renderer WebView mati (crash=" + d.didCrash() + ") kali ke-" + deaths);
                fail(status, "WebView terhenti (kemungkinan memori). Cuba semula...");
                root.removeView(v);
                v.destroy();
                if (deaths <= 3) {
                    makeWeb(ctx, root, status, handler, true);
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    Dbg.log(ctx, "JS ralat: " + m.message());
                    fail(status, "JS: " + m.message() + " (baris " + m.lineNumber() + ")");
                }
                return true;
            }
        });

        web.addJavascriptInterface(new ClockDream.Bridge(ctx.getApplicationContext()), "ZK");
        root.addView(web, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        web.onResume();
        web.resumeTimers();
        web.loadUrl("file:///android_asset/index.html" + (lite ? "?lite=1" : ""));
    }

    private static void fail(TextView status, String msg) {
        status.setTextColor(RED);
        status.setVisibility(View.VISIBLE);
        status.setText(msg);
    }
}
