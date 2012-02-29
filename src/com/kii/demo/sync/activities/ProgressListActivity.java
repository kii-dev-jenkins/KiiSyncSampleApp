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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.R;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;

public class ProgressListActivity extends ExpandableListActivity implements
        View.OnClickListener {
    KiiFileExpandableListAdapter mAdapter = null;
    NewEventListener mNewEventListener = null;
    private static final int MENU_ITEM_CANCEL = 2;
    
    private static final int OPTION_MENU_SETTING = 0;
    private View mHeaderView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        // set the view when it is empty
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emptyView = inflator.inflate(R.layout.uploads_empty_view, null);
        ((ViewGroup) this.getExpandableListView().getParent())
                .addView(emptyView);
        getExpandableListView().setEmptyView(emptyView);

        mNewEventListener = new NewEventListener(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mHeaderView == null) {
            mHeaderView = inflater.inflate(R.layout.header_view, null);
        }
        Button b = (Button) mHeaderView.findViewById(R.id.button_left);
        b.setText(getString(R.string.pause));
        b = (Button) mHeaderView.findViewById(R.id.button_right);
        b.setText(getString(R.string.resume));
        setHeaderText();
        getExpandableListView().addHeaderView(mHeaderView);
        connect();
        registerForContextMenu(getExpandableListView());
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if (mNewEventListener != null) {
            mNewEventListener.unregister();
        }
    }

    Receiver mReceiver = null;

    protected void connect() {
        mReceiver = new Receiver();
        registerReceiver(mReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_END));
        registerReceiver(mReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_START));
        adpterSetup();
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
            client = KiiSyncClient.getInstance(context);
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
                    Intent apiIntent = new Intent(context
                            .getApplicationContext(), StartActivity.class);
                    apiIntent.setAction(StartActivity.ACTION_ENTER_PASSWORD);
                    context.startActivity(apiIntent);
                } else if (msg.sync_result == SyncMsg.ERROR_PFS_BUSY) {
                    return;
                }
            }
            handler.sendEmptyMessageDelayed(PROGRESS_END, 500);
            setHeaderText();
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

        }

    }

    static final String TAG = "ProgressListActivity";

    protected void adpterSetup() {
        Log.d(TAG, "adapterSetup");
        if (mAdapter == null) {
            Log.d(TAG, "new Adapter");
            KiiSyncClient client = KiiSyncClient.getInstance(this);
            mAdapter = new KiiFileExpandableListAdapter(this, client,
                    KiiFileExpandableListAdapter.TYPE_PROGRESS, this);
            setListAdapter(mAdapter);
            mNewEventListener.register();
            Log.d(TAG, "progress is "+client.getProgress());
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
        KiiSyncClient kiiClient = KiiSyncClient.getInstance(this);
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
                    //finish();
                    return;
            }

        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView
                .getPackedPositionType(info.packedPosition);
        int group = ExpandableListView
                .getPackedPositionGroup(info.packedPosition);
        int child = ExpandableListView
                .getPackedPositionChild(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            KiiFile kFile = (KiiFile) mAdapter.getChild((int) group,
                    (int) child);
            if (kFile != null && kFile.isFile()) {
                menu.setHeaderTitle(kFile.getTitle());
                KiiSyncClient kiiClient = KiiSyncClient.getInstance(this);
                if (kiiClient == null) {
                    UiUtils.showToast(this, "Not ready.");
                    return;
                }
                int status = kiiClient.getStatus(kFile);
                switch (status) {
                    case KiiFile.STATUS_PREPARE_TO_SYNC:
                    case KiiFile.STATUS_UPLOADING_BODY:
                    case KiiFile.STATUS_DOWNLOADING_BODY:
                    case KiiFile.STATUS_SYNC_IN_QUEUE:
                        menu.add(MENU_ITEM_CANCEL, 0, 0,
                                getString(R.string.cancel));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
                .getMenuInfo();
        int type = ExpandableListView
                .getPackedPositionType(info.packedPosition);
        KiiSyncClient client = KiiSyncClient.getInstance(this);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPos = ExpandableListView
                    .getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView
                    .getPackedPositionChild(info.packedPosition);
            final KiiFile kFile = (KiiFile) mAdapter.getChild((int) groupPos,
                    (int) childPos);
            if (kFile != null && kFile.isFile()) {
                switch (item.getGroupId()) {
                    case MENU_ITEM_CANCEL:
                        client.cancel(kFile);
                        break;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_complex_more_button:
                View row = (View) v.getTag();
                getExpandableListView().showContextMenuForChild(row);
                break;
        }
    }

    public void handleButtonLeft(View v) {
        KiiSyncClient kiiClient = KiiSyncClient.getInstance(this);
        if (kiiClient != null) {
            kiiClient.suspend();
        }
    }

    public void handleButtonRight(View v) {
        Utils.startSync(this, BackupService.ACTION_REFRESH);
    }

    private void setHeaderText() {
        if (mHeaderView != null) {
            TextView tv = (TextView) mHeaderView.findViewById(R.id.header_text);
            tv.setText(UiUtils.getLastSyncTime(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OPTION_MENU_SETTING, 0, getString(R.string.settings));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case OPTION_MENU_SETTING:
                Intent intent = new Intent(this, StartActivity.class);
                intent.setAction(Intent.ACTION_CONFIGURATION_CHANGED);
                startActivity(intent);
                break;
        }
        return true;
    }
    
    
}
