package my.zikir.clock;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Bina paparan WebView + baris status diagnosis (digunakan oleh screensaver dan app biasa). */
final class Shell {
    private static final int RED = 0xFFFF6B5E;

    static FrameLayout build(final Context ctx) {
        final FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(0xFF050A18);

        final TextView status = new TextView(ctx);
        status.setTextColor(0xFFB6C3DD);
        status.setTextSize(20);
        status.setBackgroundColor(0x99000000);
        status.setPadding(24, 12, 24, 12);
        status.setText("Jam Zikir: memulakan WebView...");
        final Handler handler = new Handler(Looper.getMainLooper());

        try {
            WebView web = new WebView(ctx);
            web.setBackgroundColor(0xFF050A18);
            WebSettings s = web.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setAllowFileAccess(true);
            s.setAllowUniversalAccessFromFileURLs(true);

            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView v, String url) {
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
                    fail(status, "Ralat muat: " + desc + " (" + url + ")");
                }
            });

            web.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage m) {
                    if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        fail(status, "JS: " + m.message() + " (baris " + m.lineNumber() + ")");
                    }
                    return true;
                }
            });

            web.addJavascriptInterface(new ClockDream.Bridge(ctx.getApplicationContext()), "ZK");
            root.addView(web, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            web.loadUrl("file:///android_asset/index.html");
        } catch (Throwable t) {
            fail(status, "WebView gagal dimulakan: " + t);
        }

        root.addView(status, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START));
        return root;
    }

    private static void fail(TextView status, String msg) {
        status.setTextColor(RED);
        status.setVisibility(View.VISIBLE);
        status.setText(msg);
    }
}
