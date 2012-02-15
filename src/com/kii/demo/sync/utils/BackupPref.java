package com.kii.demo.sync.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class BackupPref {
    public static final String SHARED_PREFERENCES_NAME = "com.kii.sync.api.samples_preferences";
    
    public static final String PREF_SYNC_WIFI_ONLY = "wifi_only";
    public static final String PREF_SYNC_MODE = "mode";
    public static final int MODE_AUTO = 0;
    public static final int MODE_MANUAL = 1;
    public static final int MODE_USER_INTENTION = 2;
    public static final String PREF_USER_INTENTION = "user_intention";
    public static final int EVERY_4_HOURS = 4;
    public static final int EVERY_12_HOURS = 12;
    public static final int EVERY_24_HOURS = 24;
    private static SharedPreferences mPref;
    private static Context mContext = null;

    public static void init(Context context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
            mPref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
        }
    }
    
    public static SharedPreferences getSharedPreferences() {
        return mPref;
    }
    
    static String getString(String key) {
        return mPref.getString(key, null);
    }

    static String getString(String key, String defaultValue) {
        return mPref.getString(key, defaultValue);
    }
    
    static int getInteger(String key, int defaultValue) {
        return mPref.getInt(key, defaultValue);
    }

    static boolean setInteger(String key, int value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    static boolean setString(String key, String value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    static boolean clear(String key) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.clear();
        return editor.commit();
    }

    static boolean setBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    static boolean getBoolean(String key) {
        return mPref.getBoolean(key, false);
    }

    static boolean getBoolean(String key, boolean defaultValue) {
        return mPref.getBoolean(key, defaultValue);
    }
    
    public static boolean setSyncWifiOnly(Boolean wifi) {
        return setBoolean(PREF_SYNC_WIFI_ONLY, wifi);
    }

    public static boolean getSyncWifiOnly() {
        return getBoolean(PREF_SYNC_WIFI_ONLY, false);
    }
    
    public static boolean setSyncMode(int mode) {
        return setInteger(PREF_SYNC_MODE, mode);
    }
    
    public static int getSyncMode() {
        return Integer.parseInt(getString(PREF_SYNC_MODE, Integer.toString(MODE_AUTO)));
    }
    
    public static boolean setUserIntentionTime(int time) {
        return setInteger(PREF_USER_INTENTION, time);
    }
    
    public static int getUserIntentionTime() {
        return Integer.parseInt(getString(PREF_USER_INTENTION, "0"));
    }
    
}
