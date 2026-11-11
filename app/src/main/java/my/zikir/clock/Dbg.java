package my.zikir.clock;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Log ringkas disimpan dalam app supaya boleh dibaca selepas screensaver dicuba. */
final class Dbg {
    static synchronized void log(Context c, String msg) {
        try {
            SharedPreferences p = c.getApplicationContext().getSharedPreferences("dbg", Context.MODE_PRIVATE);
            String old = p.getString("log", "");
            String[] parts = old.length() == 0 ? new String[0] : old.split("\n");
            StringBuilder sb = new StringBuilder();
            int start = Math.max(0, parts.length - 11);
            for (int i = start; i < parts.length; i++) {
                sb.append(parts[i]).append('\n');
            }
            sb.append(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())).append(' ').append(msg);
            p.edit().putString("log", sb.toString()).commit();
        } catch (Throwable t) {
            // abaikan
        }
    }

    static String read(Context c) {
        try {
            return c.getApplicationContext().getSharedPreferences("dbg", Context.MODE_PRIVATE)
                    .getString("log", "(log kosong)");
        } catch (Throwable t) {
            return "(log gagal dibaca)";
        }
    }
}
