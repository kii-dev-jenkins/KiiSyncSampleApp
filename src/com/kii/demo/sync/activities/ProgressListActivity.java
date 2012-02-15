package com.kii.demo.sync.activities;

import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;

import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiClientTask;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;
import com.kii.demo.sync.R;

public class ProgressListActivity extends ExpandableListActivity {
    KiiFileExpandableListAdapter mAdapter = null;
    NewEventListener mNewEventListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        mNewEventListener = new NewEventListener(this);
        connect();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        unregisterReceiver(mReceiver);
        if (mNewEventListener != null) {
            mNewEventListener.unregister();
        }
    }

    Receiver mReceiver = new Receiver();

    protected void connect() {
        registerReceiver(mReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_END));
        registerReceiver(mReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_START));
        if (KiiSyncClient.getInstance() == null) {
            KiiClientTask task = new KiiClientTask(getApplicationContext(),
                    "Connect", KiiClientTask.SYNC_CONNECT, mNewEventListener);
            task.execute();
        } else {
            adpterSetup();
        }
    }

    class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()
                    .equals(DownloadManager.ACTION_DOWNLOAD_START)) {
                handler.sendEmptyMessageDelayed(PROGRESS_START, 500);
            } else if (intent.getAction().equals(
                    DownloadManager.ACTION_DOWNLOAD_END)) {
                handler.sendEmptyMessage(PROGRESS_END);
            }
        }
    }

    public class NewEventListener implements KiiNewEventListener {

        final static String TAG = "NewEventListener";

        KiiSyncClient client = null;
        long id = 0;
        Context context = null;

        public NewEventListener(Context context) {
            this.context = context;
            id = System.currentTimeMillis();
        }

        public boolean register() {
            client = KiiSyncClient.getInstance();
            if (client == null) {
                throw new NullPointerException();
            }
            return client.registerNewEventListener(id, this);
        }

        public void unregister() {
            if (id != 0) {
                client.unregisterNewEventListener(id);
            }
        }

        @Override
        public void onNewSyncDeleteEvent(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
        }

        @Override
        public void onNewSyncInsertEvent(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
        }

        @Override
        public void onNewSyncUpdateEvent(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
        }

        @Override
        public void onSyncComplete(SyncMsg msg) {
            if (msg != null) {
                if (msg.sync_result == SyncMsg.ERROR_AUTHENTICAION_ERROR) {
                    Intent apiIntent = new Intent(
                            context.getApplicationContext(), StartActivity.class);
                    apiIntent.setAction(StartActivity.ACTION_ENTER_PASSWORD);
                    context.startActivity(apiIntent);
                } else if (msg.sync_result == SyncMsg.ERROR_PFS_BUSY) {
                    return;
                }
            }
            handler.sendEmptyMessageDelayed(PROGRESS_END, 500);
        }

        @Override
        public void onSyncStart(String syncMode) {
            handler.sendEmptyMessageDelayed(PROGRESS_START, 500);
        }

        @Override
        public void onQuotaExceeded(Uri arg0) {
            handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);

        }

        @Override
        public void onLocalChangeSyncedEvent(Uri[] uris) {
            handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
        }

        public void onConnectComplete() {
            adpterSetup();
        }

    }

    static final String TAG = "ProgressListActivity";

    protected void adpterSetup() {
        Log.d(TAG, "adapterSetup");
        if (mAdapter == null) {
            Log.d(TAG, "new Adapter");
            KiiSyncClient client = KiiSyncClient.getInstance();
            mAdapter = new KiiFileExpandableListAdapter(this, client,
                    KiiFileExpandableListAdapter.TYPE_PROGRESS);
            setListAdapter(mAdapter);
            mNewEventListener.register();
            if (client.getDownloadManager().getDownloadList().length > 0
                    || client.getListInProgress().length > 0) {
                handler.sendEmptyMessageDelayed(PROGRESS_START, 500);
                handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
            }
        }
    }

    public final static int PROGRESS_START = 1;
    public final static int PROGRESS_CHECK = 2;
    public final static int PROGRESS_END = 3;
    public final static int PROGRESS_AUTO = 4;
    public final static int SETUP_ADPTOR = 5;
    public final static int PROGRESS_UPDATE = 6;

    private boolean updateProgress() {
        KiiSyncClient kiiClient = KiiSyncClient.getInstance();
        if (kiiClient != null) {
            int progress = kiiClient.getProgress();
            if (progress > 0) {
                setProgress(progress * 100);
                mAdapter.notifyDataSetChanged();
                return true;
            }

            DownloadManager downManager = kiiClient.downManager;
            if (downManager != null && downManager.getDownloadProgress() >= 0) {
                setProgress((int) (downManager.getDownloadProgress() * 100));
                return true;
            }
        }
        return false;
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SETUP_ADPTOR:
                    adpterSetup();
                    break;
                case PROGRESS_AUTO:
                    if (updateProgress()) {
                        setProgressBarIndeterminateVisibility(true);
                        setProgressBarVisibility(true);
                        handler.sendEmptyMessageDelayed(PROGRESS_AUTO, 500);
                    } else {
                        setProgressBarIndeterminateVisibility(false);
                        setProgressBarVisibility(false);
                    }
                    break;
                case PROGRESS_START:
                    handler.removeMessages(PROGRESS_AUTO);
                    handler.removeMessages(PROGRESS_CHECK);
                    handler.removeMessages(PROGRESS_END);
                    setProgressBarIndeterminateVisibility(true);
                    setProgressBarVisibility(true);
                    if (msg.obj != null && msg.obj instanceof String) {
                        setTitle((String) msg.obj);
                    }
                case PROGRESS_CHECK:
                    updateProgress();
                    msg.what = PROGRESS_CHECK;
                    Message newMsg = new Message();
                    newMsg.copyFrom(msg);
                    handler.sendMessageDelayed(newMsg, 500);
                    break;

                case PROGRESS_UPDATE:
                    mAdapter.notifyDataSetChanged();
                    break;
                case PROGRESS_END:
                default:
                    handler.removeMessages(PROGRESS_AUTO);
                    handler.removeMessages(PROGRESS_CHECK);
                    handler.removeMessages(PROGRESS_END);

                    setProgressBarIndeterminateVisibility(false);
                    setProgressBarVisibility(false);
                    setTitle(R.string.app_name);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
            }

        }
    };

}
