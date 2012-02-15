package com.kii.cloud.sync;

import java.util.HashMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.kii.demo.sync.utils.BackupPref;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;
import com.kii.sync.SyncPref;

public class BackupService extends Service {

    public static final String TAG = "BackgroundService";

    private KiiSyncClient mSyncClient = null;
    private AlarmManager mAlarmManager = null;
    private PendingIntent mPi = null;

    public static final String ACTION_DATA_CONNECTION_CHANGED = "data_connection_changed";
    public static final String ACTION_TIMER_CHANGED = "timer_changed";
    public static final String ACTION_TIMER_TIMEOUT = "timer_timeout";
    public static final String ACTION_AUTO_SYNC_MODE = "auto_sync_mode";
    public static final String ACTION_SYNC_AFTER_LOGIN = "sync_after_login";

    private static final int DATA_CONNECTION_CHANGED = 0;
    private static final int TIMER_CHANGED = 1;
    private static final int TIMER_TIMEOUT = 2;
    private static final int AUTO_SYNC_MODE = 3;
    private static final int SYNC_AFTER_LOGIN = 4;

    private static final int REQ_CODE_TIMER = 0;

    private static final HashMap<String, Integer> ACTION_MAP = new HashMap<String, Integer>();
    static {
        ACTION_MAP.put(ACTION_DATA_CONNECTION_CHANGED, new Integer(
                DATA_CONNECTION_CHANGED));
        ACTION_MAP.put(ACTION_TIMER_CHANGED, TIMER_CHANGED);
        ACTION_MAP.put(ACTION_TIMER_TIMEOUT, TIMER_TIMEOUT);
        ACTION_MAP.put(ACTION_AUTO_SYNC_MODE, AUTO_SYNC_MODE);
        ACTION_MAP.put(ACTION_SYNC_AFTER_LOGIN, SYNC_AFTER_LOGIN);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        new InitTask().execute();
    }

    private class InitTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            int ret = SyncMsg.OK;
            Context context = BackupService.this;
            BackupPref.init(context);
            try {
                mSyncClient = KiiSyncClient.getInstance(context);
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            mAlarmManager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            Intent service = new Intent(context, BackupService.class);
            service.setAction(ACTION_TIMER_TIMEOUT);
            mPi = PendingIntent.getService(context, REQ_CODE_TIMER, service,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            handler.sendEmptyMessageDelayed(AUTO_SYNC_MODE, 5000);
            cancelAlarm();
            setAlarm();
            return ret;
        }
    }

    private class SyncTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            return mSyncClient.refresh();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mSyncClient != null) {
            handleCommand(intent);
        }
        return START_NOT_STICKY;
    }

    private void handleCommand(Intent intent) {
        if (mSyncClient == null)
            return;
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        int i = ACTION_MAP.get(action);
        switch (i) {
            case TIMER_CHANGED:
                cancelAlarm();
                setAlarm();
                break;
            case TIMER_TIMEOUT:
                if (hasPendingSync() && dataConnectionMatches()) {
                    startSync();
                }
                break;
            case DATA_CONNECTION_CHANGED:
            case AUTO_SYNC_MODE:
                if (shouldStartSync()) {
                    startSync();
                } else if (shouldStopSync()) {
                    stopSync();
                }
                break;
            case SYNC_AFTER_LOGIN:
                startSync();
                break;
        }
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_SYNC_MODE:
                    if (shouldStartSync()) {
                        startSync();
                    }
                    break;
            }
        }
    };

    private void startSync() {
        if (SyncPref.isLoggedIn()) {
            if (mSyncClient.getProgress() == SyncMsg.SYNC_NOT_RUNNING) {
                new SyncTask().execute();
            }
        }
    }

    private void stopSync() {
        if (SyncPref.isLoggedIn()) {
            mSyncClient.suspend();
            handler.removeMessages(AUTO_SYNC_MODE);
        }
    }

    private boolean shouldStartSync() {
        if (hasPendingSync() && isAutoSync() && dataConnectionMatches()) {
            return true;
        }
        return false;
    }

    private boolean shouldStopSync() {
        if (!hasPendingSync()) {
            return false;
        } else if (!isAutoSync()) {
            return true;
        } else if (!dataConnectionMatches()) {
            return true;
        }
        return false;
    }

    private boolean hasPendingSync() {
        KiiFile[] list = mSyncClient.getListInProgress();
        return (list != null) && (list.length > 0);
    }

    private boolean isAutoSync() {
        return (BackupPref.getSyncMode() == BackupPref.MODE_AUTO);
    }

    private boolean dataConnectionMatches() {
        boolean wifiOnly = BackupPref.getSyncWifiOnly();
        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null)
            return false;
        int type = ni.getType();
        return !wifiOnly
                || (wifiOnly && ((type == ConnectivityManager.TYPE_WIFI) || (type == ConnectivityManager.TYPE_WIMAX)));
    }

    private void setAlarm() {
        int time = BackupPref.getUserIntentionTime();
        if (time > 0) {
            long interval = time * 60 * 60; 
            mAlarmManager.setRepeating(AlarmManager.RTC,
                    System.currentTimeMillis() + interval, interval, mPi);
        }
    }

    private void cancelAlarm() {
        mAlarmManager.cancel(mPi);
    }

}
