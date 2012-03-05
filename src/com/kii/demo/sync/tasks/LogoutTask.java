package com.kii.demo.sync.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.ui.StartActivity;
import com.kii.sync.SyncMsg;

public class LogoutTask extends AsyncTask<String, Void, Integer> {

    static final String TAG = "LogoutTask";

    ProgressDialog dialog = null;

    private Context mContext;

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
    public LogoutTask(Context context) {
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

        try {
            KiiSyncClient syncClient = KiiSyncClient.getInstance(mContext);
            return syncClient.logout();
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
        StartActivity.showAlertDialog(mContext, TAG, result);
    }

}