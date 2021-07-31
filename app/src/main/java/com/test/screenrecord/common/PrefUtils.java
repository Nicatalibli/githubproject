package com.test.screenrecord.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefUtils {
    public static final String VALUE_RESOLUTION = "720";
    public static final String VALUE_FRAMES = "30";
    public static final String VALUE_BITRATE = "7130317";
    public static final String VALUE_ORIENTATION = "auto";
    public static final String VALUE_AUDIO = "1";
    public static final String VALUE_NAME_FORMAT = "yyyyMMdd_hhmmss";
    public static final String VALUE_NAME_PREFIX = "recording";
    public static final String VALUE_LANGUAGE = "vi";
    public static final String VALUE_TIMER = "3";
    public static final boolean VALUE_USE_FLOAT = true;
    public static final boolean VALUE_TOUCHES = false;
    public static final boolean VALUE_CAMERA = false;
    public static final boolean VALUE_ENABLE_TARGET_APP = false;
    public static final boolean VALUE_SAVING_GIF = true;
    public static final boolean VALUE_SHAKE = false;
    public static final boolean VALUE_VIBRATE = true;

    public static void saveStringValue(Context context, String key, String value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void savePosValue(Context context, String key, int value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void saveBooleanValue(Context context, String key, boolean value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static String readStringValue(Context context, String key, String def) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, def);
    }

    public static int readPosValue(Context context, String key, int def) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(key, def);
    }

    public static boolean readBooleanValue(Context context, String key, boolean def) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, def);
    }

    public static boolean firstOpen(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstOpen = preferences.getBoolean("firstopen", true);
        if (isFirstOpen) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("firstopen", false);
            editor.commit();
        }
        return isFirstOpen;
    }
}
