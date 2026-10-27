package com.hy300.hub;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    // {iconResName, label, kind, target}
    // kind "cmp": "pkg/cls" (direct Intent, falls back to su for non-exported)
    // kind "pkg": launch package (AirScreen falls back to stock AirPlay)
    // kind "act": a settings action
    static final String[][] ITEMS = {
        {"ic_settings",    "Cài đặt",        "cmp", "com.htc.magcubicos/com.htc.magcubicos.activity.MainSettingActivity"},
        {"ic_projector",   "Máy chiếu",           "cmp", "com.htc.magcubicos/com.htc.magcubicos.activity.ProjectActivity"},
        {"ic_scan",        "Căn camera",               "cmp", "com.hysd.vafocus/com.hysd.vafocus.TchjzActivity"},
        {"ic_frame",       "Chỉnh 4 góc",         "cmp", "com.hysd.vafocus/com.hysd.vafocus.TchhdActivity"},
        {"ic_aperture",    "Lấy nét",             "cmp", "com.hysd.vafocus/com.hysd.vafocus.VajzActivity"},
        {"ic_wifi",        "WiFi",                          "cmp", "com.htc.magcubicos/com.htc.magcubicos.activity.WifiActivity"},
        {"ic_bluetooth",   "Bluetooth",                     "cmp", "com.htc.magcubicos/com.htc.magcubicos.activity.BluetoothActivity"},
        {"ic_airplay",     "AirScreen",                     "pkg", "com.ses.screenmirror"},
        {"ic_screenshare", "Chiếu màn hình", "pkg", "com.htc.samescreen"},
        {"ic_cast",        "DLNA",                          "cmp", "com.mphotool.dlna/com.mphotool.dlna.MainActivity"},
        {"ic_code",        "Nhà phát triển", "cmp", "com.htc.magcubicos/com.htc.magcubicos.activity.DeveloperModeActivity"},
        {"ic_info",        "Thông tin",                "cmp", "com.htc.magcubicos/com.htc.magcubicos.activity.AboutActivity"},
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0A0E14"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(28);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Máy chiếu & Cài đặt");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setPadding(dp(8), 0, 0, dp(16));
        root.addView(title);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        root.addView(grid);

        View first = null;
        for (final String[] it : ITEMS) {
            View tile = makeTile(it);
            if (first == null) first = tile;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(150);
            lp.height = dp(112);
            lp.setMargins(dp(8), dp(8), dp(8), dp(8));
            grid.addView(tile, lp);
        }

        scroll.addView(root);
        setContentView(scroll);
        if (first != null) first.requestFocus();
    }

    private View makeTile(final String[] it) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(false);
        tile.setBackground(tileBg());

        ImageView icon = new ImageView(this);
        int resId = getResources().getIdentifier(it[0], "drawable", getPackageName());
        if (resId != 0) icon.setImageResource(resId);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(40), dp(40));

        TextView label = new TextView(this);
        label.setText(it[1]);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);

        tile.addView(icon, ip);
        tile.addView(label, p);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launch(it); }
        });
        return tile;
    }

    private Drawable tileBg() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rect(Color.parseColor("#2A82E4"), dp(2), Color.WHITE));
        s.addState(new int[]{android.R.attr.state_pressed}, rect(Color.parseColor("#2A82E4"), 0, 0));
        s.addState(new int[]{}, rect(Color.parseColor("#1B2230"), 0, 0));
        return s;
    }

    private GradientDrawable rect(int fill, int strokeW, int strokeC) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(14));
        if (strokeW > 0) g.setStroke(strokeW, strokeC);
        return g;
    }

    private void launch(String[] it) {
        String kind = it[2], target = it[3], label = it[1];
        if ("pkg".equals(kind)) {
            Intent i = getPackageManager().getLaunchIntentForPackage(target);
            if (i == null) {
                if ("com.ses.screenmirror".equals(target)) {
                    startCmp("com.mphotool.airplay", "com.mphotool.airplay.MainActivity", label);
                } else {
                    toast("Chưa cài: " + label);
                }
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception e) { toast("Không mở: " + label); }
            return;
        }
        if ("act".equals(kind)) {
            try { startActivity(new Intent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (Exception e) { toast("Không mở: " + label); }
            return;
        }
        String[] pc = target.split("/");
        startCmp(pc[0], pc[1], label);
    }

    private void startCmp(String pkg, String cls, final String label) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName(pkg, cls));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (SecurityException se) {
            suStart(pkg, cls, label);
        } catch (Exception e) {
            suStart(pkg, cls, label);
        }
    }

    // Non-exported vendor activities (ProjectActivity/zoom, DeveloperMode, About): launch as root.
    private void suStart(final String pkg, final String cls, final String label) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "am start -n " + pkg + "/" + cls});
                    int rc = p.waitFor();
                    if (rc != 0) toastUi("Cần cấp quyền root (Magisk) cho app này: " + label);
                } catch (Exception e) {
                    toastUi("Không mở (cần root): " + label);
                }
            }
        }).start();
    }

    private void toastUi(final String s) {
        runOnUiThread(new Runnable() { @Override public void run() { toast(s); } });
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
