package com.kii.demo.sync.tasks;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.activities.StartActivity;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.SyncMsg;

public class LoginTask extends AsyncTask<String, Void, Integer> {

    static final String TAG = "LoginTask";

    ProgressDialog dialog = null;

    private Context mContext;

    private List<String> services;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(mContext);
        dialog.setMessage(TAG);
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * 
     */
    public LoginTask(Context context) {
        mContext = context;
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Integer doInBackground(String... params) {
        int result = SyncMsg.ERROR_SETUP;
        if (params.length != 2) {
            return result;
        }

        String email = params[0];
        String password = params[1];
        try {
            KiiSyncClient syncClient = KiiSyncClient.getInstance(mContext);
            // login
            result = syncClient.login(email, password);
            if (result != SyncMsg.OK) {
                return result;
            } else {
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return result;
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Integer result) {
        if (dialog != null)
            dialog.dismiss();
        switch (result) {
            case SyncMsg.OK:
                String msg = "Login OK.";
                if (services != null && services.size() > 2) {
                    msg = msg + "\nSync:" + services.get(0)
                            + "\nExistingUsage:" + services.get(1)
                            + "\nMaxUsage:" + services.get(2);
                }
                StartActivity.showAlertDialog(mContext, TAG, msg);
                Utils.startSync(mContext, BackupService.ACTION_SYNC_AFTER_LOGIN);
                break;
            case SyncMsg.ERROR_SETUP:
            default:
                StartActivity.showAlertDialog(mContext, TAG, result);
                break;
        }
    }

}