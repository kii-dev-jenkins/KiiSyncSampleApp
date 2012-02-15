package com.kii.demo.sync.tasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kii.cloud.sync.BackupService;
import com.kii.demo.sync.utils.Utils;

public class KiiRefReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.startBackupService(context, BackupService.ACTION_DATA_CONNECTION_CHANGED);
    }

}
