package com.hy300.hub;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;

/**
 * A clean, dark-Material "About device" screen that replaces MagcubicOS's harsh blue one.
 * Read-only info only; no OTA/update rows. Styled to match the Hub tiles (Material surface,
 * rounded cards). Device strings come from system properties and standard Android APIs.
 */
public class AboutActivity extends Activity {

    private static final int BG      = Color.parseColor("#0A0E14");
    private static final int CARD    = Color.parseColor("#1B2230");
    private static final int LABEL   = Color.parseColor("#9AA4B2");
    private static final int VALUE   = Color.WHITE;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(28);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Về thiết bị");
        title.setTextColor(VALUE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setPadding(dp(4), 0, 0, dp(16));
        root.addView(title);

        root.addView(row("Mẫu thiết bị",  prop("persist.sys.modelName", Build.MODEL)));
        root.addView(row("Phiên bản UI",  prop("ro.build.display.id", "-")));
        root.addView(row("Phiên bản Android", Build.VERSION.RELEASE));
        root.addView(row("Độ phân giải",  resolution()));
        root.addView(row("Bộ nhớ RAM",    ram()));
        root.addView(row("Bộ nhớ lưu trữ", storage()));
        root.addView(row("MAC không dây", wlanMac()));
        root.addView(row("Số sê-ri",      prop("ro.serialno", "-")));

        scroll.addView(root);
        setContentView(scroll);
    }

    private ViewGroup row(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(LABEL);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        card.addView(l, lp);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(VALUE);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        v.setGravity(Gravity.END);
        card.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);
        return card;
    }

    private String resolution() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        return m.widthPixels + " x " + m.heightPixels;
    }

    private String ram() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return gb(mi.totalMem);
    }

    private String storage() {
        try {
            StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
            long total = sf.getBlockCountLong() * sf.getBlockSizeLong();
            long avail = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
            return gb(total) + " / trống " + gb(avail);
        } catch (Exception e) { return "-"; }
    }

    private String wlanMac() {
        try (BufferedReader r = new BufferedReader(new FileReader("/sys/class/net/wlan0/address"))) {
            String s = r.readLine();
            return s != null ? s.trim() : "-";
        } catch (Exception e) { return "-"; }
    }

    private static String gb(long bytes) {
        double g = bytes / 1024.0 / 1024.0 / 1024.0;
        if (g >= 10) return Math.round(g) + " GB";
        return String.format(java.util.Locale.US, "%.2f GB", g);
    }

    private static String prop(String key, String def) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getMethod("get", String.class, String.class);
            String v = (String) m.invoke(null, key, def);
            return (v == null || v.isEmpty()) ? def : v;
        } catch (Exception e) { return def; }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
