package com.example.appanh;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/** One persisted theme for every Activity, including Activities in the back stack. */
public final class ThemeManager {
    private ThemeManager() {}
    public static boolean isDark(Context context) {
        return context.getSharedPreferences("settings",0).getBoolean("dark",false);
    }
    public static void initialize(Context context) {
        AppCompatDelegate.setDefaultNightMode(isDark(context)
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
    public static void setDark(Context context,boolean dark) {
        context.getSharedPreferences("settings",0).edit().putBoolean("dark",dark).apply();
        initialize(context);
    }
}
