package my.zikir.clock;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

/** Buka Jam Zikir sebagai app biasa (untuk ujian, dan sebagai sandaran screensaver). */
public class MainActivity extends Activity {
    private boolean fromDream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fromDream = getIntent().getBooleanExtra("fromDream", false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Dbg.log(this, "MainActivity dibuka" + (fromDream ? " (dari screensaver)" : ""));
        setContentView(Shell.build(this, !fromDream, fromDream));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (fromDream) {
            ClockDream d = ClockDream.instance;
            if (d != null) {
                try {
                    d.finish();
                } catch (Throwable t) {
                    // abaikan
                }
            }
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
