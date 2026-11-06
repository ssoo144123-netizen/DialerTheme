package com.example.dialertheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class AppTheme {
    public static final int GREEN = 0xFF2E9E4F;
    public static final int RED = 0xFFD93025;

    public final String name;
    public final int bg, surface, primary, onPrimary, text, text2;

    AppTheme(String name, int bg, int surface, int primary, int onPrimary, int text, int text2) {
        this.name = name;
        this.bg = bg;
        this.surface = surface;
        this.primary = primary;
        this.onPrimary = onPrimary;
        this.text = text;
        this.text2 = text2;
    }

    public static final AppTheme[] ALL = {
        new AppTheme("روشن",        0xFFFFFFFF, 0xFFF1F3F4, 0xFF1A73E8, 0xFFFFFFFF, 0xFF202124, 0xFF5F6368),
        new AppTheme("تیره",        0xFF121212, 0xFF1E1E1E, 0xFF8AB4F8, 0xFF202124, 0xFFE8EAED, 0xFF9AA0A6),
        new AppTheme("مشکی کامل",   0xFF000000, 0xFF151515, 0xFFBB86FC, 0xFF000000, 0xFFFFFFFF, 0xFFB0B0B0),
        new AppTheme("اقیانوس",     0xFF0B1F2E, 0xFF12304A, 0xFF26C6DA, 0xFF00222B, 0xFFE0F7FA, 0xFF80CBC4),
        new AppTheme("جنگل",        0xFF0F1F14, 0xFF1B3324, 0xFF66BB6A, 0xFF0B1F10, 0xFFE8F5E9, 0xFFA5D6A7),
        new AppTheme("صورتی",       0xFFFFF5F7, 0xFFFCE4EC, 0xFFD81B60, 0xFFFFFFFF, 0xFF3E2027, 0xFF8E5A66),
        new AppTheme("غروب",        0xFF2B1B17, 0xFF3E2723, 0xFFFF7043, 0xFF2B0F08, 0xFFFFF3E0, 0xFFBCAAA4),
        new AppTheme("بنفش",        0xFFF8F5FF, 0xFFEDE7F6, 0xFF6A1B9A, 0xFFFFFFFF, 0xFF2A1B3D, 0xFF6E5C82),
    };

    private static final float[] SCALES = {0.9f, 1f, 1.15f, 1.3f};

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("theme", Context.MODE_PRIVATE);
    }

    public static int index(Context c) {
        int i = prefs(c).getInt("theme", 0);
        return (i < 0 || i >= ALL.length) ? 0 : i;
    }

    public static AppTheme current(Context c) {
        return ALL[index(c)];
    }

    public static void setCurrent(Context c, int i) {
        prefs(c).edit().putInt("theme", i).apply();
    }

    public static int sizeIndex(Context c) {
        int i = prefs(c).getInt("size", 1);
        return (i < 0 || i >= SCALES.length) ? 1 : i;
    }

    public static void setSize(Context c, int i) {
        prefs(c).edit().putInt("size", i).apply();
    }

    public static float scale(Context c) {
        return SCALES[sizeIndex(c)];
    }

    public boolean isLight() {
        double lum = 0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg);
        return lum > 140;
    }
}
