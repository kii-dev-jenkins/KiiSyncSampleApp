/*************************************************************************
 
 Copyright 2012 Kii Corporation
 http://kii.com
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 
 *************************************************************************/

package com.kii.cloud.sync;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.kii.demo.sync.ui.ProgressListActivity;
import com.kii.demo.sync.ui.StartActivity;
import com.kii.demo.sync.utils.NotificationUtil;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class KiiFileListener extends SyncNewEventListener {

    final static String TAG = "KiiFileStatusCache";

    KiiSyncClient client = null;
    long id = 0;

    HashMap<String, Integer> cacheKiiFileStatus = new HashMap<String, Integer>();

    public KiiFileListener(Context context) {
        super(context);
        updateCache(true);
    }

    AtomicBoolean isBusy = new AtomicBoolean(false);
    AtomicInteger cacheUpdateState = new AtomicInteger();

    public void updateCache(boolean clean) {
        if (cacheUpdateState.get() < 3) {
            if (clean) {
                cacheUpdateState.set(3);
            } else {
                cacheUpdateState.set(2);
            }
        }

        if (!isBusy.getAndSet(true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    executeUpdateCache();
                }
            }).start();
        }
    }

    synchronized private void executeUpdateCache() {

        int state = cacheUpdateState.getAndSet(0);

        do {

            isBusy.set(true);

            HashMap<String, Integer> tempKiiFileStatus = null;

            if (state == 3) {
                tempKiiFileStatus = new HashMap<String, Integer>();
            } else {
                tempKiiFileStatus = cacheKiiFileStatus;
            }
            
            //workaround: avoid NPE crash
            if(client == null) {
                client = KiiSyncClient.getInstance(mContext);
            }

            KiiFile[] backupFiles = client.getBackupFiles();
            if ((backupFiles != null) && (backupFiles.length > 0)) {
                for (int ct = 0; ct < backupFiles.length; ct++) {
                    tempKiiFileStatus.put(backupFiles[ct].getResourceUrl(),
                            backupFiles[ct].getStatus());
                }
            }

            if (state == 3) {
                cacheKiiFileStatus.clear();
                cacheKiiFileStatus = tempKiiFileStatus;
            }

            isBusy.set(false);

            state = cacheUpdateState.getAndSet(0);

        } while (state != 0);

    }

    public int getKiiFileStatus(String path) {
        if (cacheKiiFileStatus.containsKey(path)) {
            return cacheKiiFileStatus.get(path);
        } else {
            return 0;
        }
    }

    @Override
    public void onNewSyncDeleteEvent(Uri[] arg0) {
        updateCache(true);
    }

    @Override
    public void onNewSyncInsertEvent(Uri[] arg0) {
        updateCache(false);

    }

    @Override
    public void onNewSyncUpdateEvent(Uri[] arg0) {
        updateCache(false);
    }

    @Override
    public void onSyncComplete(SyncMsg msg) {
        NotificationUtil.cancelSyncProgressNotification(mContext);
        updateCache(false);
        if (msg != null) {
            if (msg.sync_result == SyncMsg.ERROR_AUTHENTICAION_ERROR) {
                Intent intent = new Intent(mContext, StartActivity.class);
                intent.setAction(StartActivity.ACTION_ENTER_PASSWORD);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else if (msg.sync_result == SyncMsg.PFS_SYNCRESULT_USER_EXPIRED) {
                Intent intent = new Intent(mContext, StartActivity.class);
                intent.setAction(StartActivity.ACTION_LOGOUT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        }
    }

    @Override
    public void onSyncStart(String syncMode) {
        Intent progressIntent = new Intent(mContext.getApplicationContext(),
                ProgressListActivity.class);
        NotificationUtil.showSyncProgressNotification(
                mContext.getApplicationContext(), progressIntent);
    }

    @Override
    public void onQuotaExceeded(Uri arg0) {
    }

    @Override
    public void onLocalChangeSyncedEvent(Uri[] uris) {
        updateCache(false);
    }

    @Override
    public void onDownloadComplete(Uri[] arg0) {
        updateCache(false);
    }

}
