package com.kii.demo.sync.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.ui.StartActivity;
import com.kii.sync.SyncMsg;

public class ChangePwdTask extends AsyncTask<String, Void, Integer> {

    static final String TAG = "ChangePwdTask";

    ProgressDialog dialog = null;

    private Context mContext;

    String newPassword = null;

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
    public ChangePwdTask(Context context) {
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

        String oldPassword = params[0];
        newPassword = params[1];
        try {
            KiiSyncClient syncClient = KiiSyncClient.getInstance(mContext);
            return syncClient.changePassword(oldPassword, newPassword);
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
                String msg = "Change Password is successful. Please login with new password:"
                        + newPassword;
                StartActivity.showAlertDialog(mContext, TAG, msg);
                break;
            case SyncMsg.ERROR_SETUP:
            default:
                StartActivity.showAlertDialog(mContext, TAG, result);
                break;
        }
    }

}