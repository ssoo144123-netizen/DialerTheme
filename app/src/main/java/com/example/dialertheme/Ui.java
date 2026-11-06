package com.example.dialertheme;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class Ui {

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rect(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * AppTheme.scale(c));
        tv.setTextColor(color);
        return tv;
    }

    public static TextView pill(Context c, String s, int bg, int fg) {
        TextView tv = text(c, s, 15, fg);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        tv.setBackground(rect(c, bg, 24));
        return tv;
    }

    public static void styleWindow(Activity a, AppTheme t) {
        Window w = a.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(t.bg));
        w.setStatusBarColor(t.bg);
        w.setNavigationBarColor(t.bg);
        int flags = 0;
        if (t.isLight()) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }
}
