package com.kii.demo.sync.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import com.kii.cloud.sync.BackupService;
import com.kii.demo.sync.utils.BackupPref;
import com.kii.demo.sync.utils.Utils;
import com.kii.demo.sync.R;

public class Settings extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BackupPref.init(this);
        addPreferencesFromResource(R.xml.sync_pref);
        SharedPreferences pref = getSharedPreferences(
                BackupPref.SHARED_PREFERENCES_NAME, 0);
        pref.registerOnSharedPreferenceChangeListener(this);
        handleUserIntention();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.contentEquals(BackupPref.PREF_USER_INTENTION)) {
            Utils.startBackupService(this, BackupService.ACTION_TIMER_CHANGED);
        } else if ((key.contentEquals(BackupPref.PREF_SYNC_MODE) || (key
                .contentEquals(BackupPref.PREF_SYNC_WIFI_ONLY)))) {
            handleUserIntention();
            if (BackupPref.getSyncMode() == BackupPref.MODE_AUTO) {
                Utils.startBackupService(this, BackupService.ACTION_AUTO_SYNC_MODE);
            }
        }
    }

    private void handleUserIntention() {
        ListPreference userIntention = (ListPreference) findPreference(BackupPref.PREF_USER_INTENTION);
        if ((BackupPref.getSyncMode() == BackupPref.MODE_USER_INTENTION)) {
            userIntention.setEnabled(true);
        } else {
            userIntention.setEnabled(false);
        }
    }

}
