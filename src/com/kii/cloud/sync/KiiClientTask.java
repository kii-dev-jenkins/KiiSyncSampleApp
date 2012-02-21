package com.kii.cloud.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.kii.demo.sync.activities.KiiFilePickerActivity.NewEventListener;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;

public class KiiClientTask extends AsyncTask<Object, Void, Integer> {
    public static final int SYNC_CONNECT = 4;
    Context context;
    String taskName;
    int taskId;
    KiiNewEventListener mListener;

    public KiiClientTask(Context context, String taskName, int task,
            KiiNewEventListener listener) {
        this.taskName = taskName;
        this.taskId = task;
        this.context = context;
        mListener = listener;
    }

    void showToast(String title, int errorCode) {
        showToast(title, Utils.getErrorMsg(errorCode, context));
    }

    void showToast(String title, CharSequence msg) {
        showToast(title + ":" + msg);
    }

    void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Integer doInBackground(Object... params) {

        if (taskId == SYNC_CONNECT) {
            KiiSyncClient syncClient;
            try {
                syncClient = KiiSyncClient.getInstance(context);
                if (syncClient != null) {
                    return SyncMsg.OK;
                } else
                    return SyncMsg.ERROR_SETUP;
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return SyncMsg.ERROR_SETUP;
            }
        }

        KiiSyncClient kiiClient = KiiSyncClient.getInstance();
        if (kiiClient == null) {
            return SyncMsg.ERROR_SERVER_TEMP_ERROR;
        }
        return SyncMsg.OK;

    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Integer result) {
        switch (taskId) {
            case SYNC_CONNECT:
                if (mListener != null && mListener instanceof NewEventListener) {
                    ((NewEventListener) mListener).onConnectComplete();
                }
                break;
            default:
                break;
        }
        switch (result) {
    		case SyncMsg.ERROR_INTERRUPTED:
    		case SyncMsg.ERROR_PFS_BUSY:
            case SyncMsg.PFS_SYNCRESULT_FORCE_STOP:
            case SyncMsg.OK:
                break;
            case SyncMsg.ERROR_SETUP:
            default:
                showToast(taskName, result);
                break;
        }
    }
}
