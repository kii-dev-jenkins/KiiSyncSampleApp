package com.kii.cloud.sync;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.kii.demo.sync.activities.ProgressListActivity;
import com.kii.demo.sync.activities.StartActivity;
import com.kii.demo.sync.utils.NotificationUtil;
import com.kii.sync.KiiFile;
import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;

public class KiiFileListener implements KiiNewEventListener {

    final static String TAG = "KiiFileStatusCache";

    KiiSyncClient client = null;
    long id = 0;
    Context context = null;

    HashMap<String, Integer> cacheKiiFileStatus = new HashMap<String, Integer>();

    public KiiFileListener(Context context) {
        this.context = context;
        client = KiiSyncClient.getInstance();
        if (client == null) {
            throw new NullPointerException();
        }
        id = System.currentTimeMillis();
        if (client.registerNewEventListener(id, this) == false) {
            Log.e(TAG,
                    "KiiFileStatusCache registerNewEventListener returns false");
        }

        updateCache(true);
    }

    public void unregister() {
        if (id != 0) {
            client.unregisterNewEventListener(id);
        }
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

            KiiFile[] astroFiles = client.getAstroFiles();
            if (astroFiles != null && astroFiles.length > 0) {
                for (int ct = 0; ct < astroFiles.length; ct++) {
                    String path = astroFiles[ct].getResourceUrl();
                    int status = client.getStatus(astroFiles[ct]);
                    tempKiiFileStatus.put(path, status);
                }
            }

            KiiFile[] backupFiles = client.getBackupFiles();
            if (backupFiles != null && backupFiles.length > 0) {
                for (int ct = 0; ct < backupFiles.length; ct++) {
                    String path = backupFiles[ct].getResourceUrl();
                    int status = client.getStatus(backupFiles[ct]);
                    tempKiiFileStatus.put(path, status);
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
        NotificationUtil.cancelSyncProgressNotification(context);
        updateCache(false);
        if (msg != null) {
            if (msg.sync_result == SyncMsg.ERROR_AUTHENTICAION_ERROR) {
                Intent apiIntent = new Intent(context.getApplicationContext(),
                        StartActivity.class);
                apiIntent.setAction(StartActivity.ACTION_ENTER_PASSWORD);
                context.startActivity(apiIntent);
            }

            if (msg.sync_result == SyncMsg.PFS_SYNCRESULT_USER_EXPIRED) {
                Intent apiIntent = new Intent(context.getApplicationContext(),
                        StartActivity.class);
                apiIntent.setAction(StartActivity.ACTION_LOGOUT);
                context.startActivity(apiIntent);
            }
        }
    }

    @Override
    public void onSyncStart(String syncMode) {
        Intent progressIntent = new Intent(context.getApplicationContext(),
                ProgressListActivity.class);
        NotificationUtil.showSyncProgressNotification(
                context.getApplicationContext(), progressIntent);
    }

    @Override
    public void onQuotaExceeded(Uri arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLocalChangeSyncedEvent(Uri[] uris) {
        // cleanUpTrashFile(uris);
        updateCache(false);
    }

    // /**
    // * Delete the temp file which are created when uploading the trash file
    // * @param uris is an array of Uri to indicate the list of KiiFiles that
    // have updated the status
    // */
    // void cleanUpTrashFile(Uri[] uris){
    // if(uris!=null && uris.length!=0){
    // for(int ct=0; ct<uris.length; ct++){
    // // get KiiFile from given Uri
    // KiiFile kFile = KiiFileUtil.createKiiFileFromUri(context, uris[ct]);
    // if(kFile!=null){
    // // check if the file is trash and local path must not be empty
    // if(kFile.getCategory().compareTo(KiiSyncClient.CATEGORY_TRASH)==0 &&
    // !TextUtils.isEmpty(kFile.getLocalPath())){
    // // check if the file has already been synced successful
    // if( kFile.getStatus() == KiiFile.STATUS_SYNCED ||
    // kFile.getStatus() == KiiFile.STATUS_NO_BODY ){
    // // get the file path
    // File file = new File(kFile.getLocalPath());
    // // check the file exist and isFile
    // if(file!=null &&
    // file.exists() &&
    // file.isFile()){
    // file.delete();
    // Log.d(TAG,"Delete a local trash file:"+file.getAbsolutePath());
    // }
    // }
    // }
    // }
    // }
    // }
    //
    // }
}
