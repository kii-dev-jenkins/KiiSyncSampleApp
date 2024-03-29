//
//
//  Copyright 2012 Kii Corporation
//  http://kii.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//

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
import android.widget.Toast;

import com.kii.demo.sync.R;
import com.kii.demo.sync.utils.BackupPref;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class BackupService extends Service {

    public static final String TAG = "BackgroundService";

    private KiiSyncClient mSyncClient = null;
    private AlarmManager mAlarmManager = null;
    private PendingIntent mPi = null;
    private boolean syncInited = false;

    public static final String ACTION_DATA_CONNECTION_CHANGED = "data_connection_changed";
    public static final String ACTION_TIMER_CHANGED = "timer_changed";
    public static final String ACTION_TIMER_TIMEOUT = "timer_timeout";
    public static final String ACTION_AUTO_SYNC_MODE = "auto_sync_mode";
    public static final String ACTION_SYNC_AFTER_LOGIN = "sync_after_login";
    public static final String ACTION_REFRESH = "refresh";
    public static final String ACTION_REFRESH_QUICK = "refresh_quick";

    private static final int DATA_CONNECTION_CHANGED = 0;
    private static final int TIMER_CHANGED = 1;
    private static final int TIMER_TIMEOUT = 2;
    private static final int AUTO_SYNC_MODE = 3;
    private static final int SYNC_AFTER_LOGIN = 4;
    private static final int REFRESH = 5;
    private static final int REFRESH_QUICK = 6;

    private static final int REQ_CODE_TIMER = 0;

    private static final HashMap<String, Integer> ACTION_MAP = new HashMap<String, Integer>();
    static {
        ACTION_MAP.put(ACTION_DATA_CONNECTION_CHANGED, new Integer(
                DATA_CONNECTION_CHANGED));
        ACTION_MAP.put(ACTION_TIMER_CHANGED, TIMER_CHANGED);
        ACTION_MAP.put(ACTION_TIMER_TIMEOUT, TIMER_TIMEOUT);
        ACTION_MAP.put(ACTION_AUTO_SYNC_MODE, AUTO_SYNC_MODE);
        ACTION_MAP.put(ACTION_SYNC_AFTER_LOGIN, SYNC_AFTER_LOGIN);
        ACTION_MAP.put(ACTION_REFRESH, REFRESH);
        ACTION_MAP.put(ACTION_REFRESH_QUICK, REFRESH_QUICK);
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
            mSyncClient = KiiSyncClient.getInstance(context);
            initSync();
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

    private static final int SYNC_REFRESH = 0;
    private static final int SYNC_REFRESH_QUICK = 1;

    private class SyncTask extends AsyncTask<Void, Void, Integer> {
        int taskId = -1;

        public SyncTask(int taskId) {
            this.taskId = taskId;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            initSync();
            if (taskId == SYNC_REFRESH) {
                return mSyncClient.refresh();
            } else {
                return mSyncClient.refreshQuick();
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case SyncMsg.ERROR_INTERRUPTED:
                case SyncMsg.ERROR_PFS_BUSY:
                case SyncMsg.PFS_SYNCRESULT_FORCE_STOP:
                case SyncMsg.OK:
                case SyncMsg.ERROR_SETUP:
                default:
                    break;
            }
            showToast(getString(R.string.sync), result);
        }
    }

    private void showToast(String title, int errorCode) {
        showToast(title, Utils.getErrorMsg(errorCode, this));
    }

    private void showToast(String title, CharSequence msg) {
        showToast(title + ":" + msg);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mSyncClient != null) {
            handleCommand(intent);
        }
        return START_NOT_STICKY;
    }

    private void handleCommand(Intent intent) {
        if (mSyncClient == null) {
            mSyncClient = KiiSyncClient.getInstance(this);
        }
        if ((intent == null) || (intent.getAction() == null)) {
            return;
        }
        int i = ACTION_MAP.get(intent.getAction());
        switch (i) {
            case TIMER_CHANGED:
                cancelAlarm();
                setAlarm();
                break;
            case TIMER_TIMEOUT:
                if (hasPendingSync() && dataConnectionMatches()) {
                    startSync(SYNC_REFRESH);
                }
                break;
            case DATA_CONNECTION_CHANGED:
            case AUTO_SYNC_MODE:
                if (shouldAutoSync()) {
                    startSync(SYNC_REFRESH);
                } else if (shouldStopSync()) {
                    stopSync();
                }
                break;
            case SYNC_AFTER_LOGIN:
                startSync(SYNC_REFRESH);
                break;
            case REFRESH:
                startSync(SYNC_REFRESH);
                break;
            case REFRESH_QUICK:
                startSync(SYNC_REFRESH_QUICK);
                break;

        }
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_SYNC_MODE:
                    if (shouldAutoSync()) {
                        startSync(SYNC_REFRESH);
                    }
                    break;
            }
        }
    };

    private void startSync(int mode) {
        if (mSyncClient.getKiiUMInfo() != null) {
            if (!mSyncClient.isSyncRunning()) {
                new SyncTask(mode).execute();
            }
        }
    }

    private void stopSync() {
        new Thread(new Runnable() {
            public void run() {
                if (mSyncClient.getKiiUMInfo() != null) {
                    mSyncClient.suspend();
                    handler.removeMessages(AUTO_SYNC_MODE);
                }
            }
        }).start();
    }

    private boolean shouldAutoSync() {
        if (hasPendingSync() && isSetToAutoSync() && dataConnectionMatches()) {
            return true;
        }
        return false;
    }

    private boolean shouldStopSync() {
        if (!hasPendingSync()) {
            return false;
        } else if (!isSetToAutoSync()) {
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

    private boolean isSetToAutoSync() {
        return (BackupPref.getSyncMode() == BackupPref.MODE_AUTO);
    }

    private boolean dataConnectionMatches() {
        boolean wifiOnly = BackupPref.getSyncWifiOnly();
        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            return false;
        }
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

    private void initSync() {
        if (!syncInited) {
            mSyncClient.initSync();
            syncInited = true;
        }
    }

}
