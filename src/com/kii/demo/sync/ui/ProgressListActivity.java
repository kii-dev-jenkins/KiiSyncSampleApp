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

package com.kii.demo.sync.ui;

import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.cloud.sync.SyncNewEventListener;
import com.kii.demo.sync.R;
import com.kii.demo.sync.ui.view.KiiFileExpandableListAdapter;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class ProgressListActivity extends ExpandableListActivity implements
        View.OnClickListener {
    KiiFileExpandableListAdapter mAdapter = null;
    NewEventListener mNewEventListener = null;
    private static final int MENU_ITEM_CANCEL = 2;

    private static final int OPTION_MENU_SETTING = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        mNewEventListener = new NewEventListener(this);
        mNewEventListener.register();
        setContentView(R.layout.expandable_list_with_header);
        Button b = (Button) findViewById(R.id.button_left);
        b.setText(getString(R.string.pause));
        b = (Button) findViewById(R.id.button_right);
        b.setText(getString(R.string.resume));
        setHeaderText();
        connect();
        registerForContextMenu(getExpandableListView());
    }

    @Override
    protected void onPause() {
        handler.removeMessages(PROGRESS_AUTO);
        handler.removeMessages(PROGRESS_END);
        super.onPause();
    }

    @Override
    protected void onResume() {
        handler.sendEmptyMessageDelayed(PROGRESS_AUTO, 500);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
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

    public class NewEventListener extends SyncNewEventListener {
        public NewEventListener(Context context) {
            super(context);
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
                    Intent intent = new Intent(mContext, StartActivity.class);
                    intent.setAction(StartActivity.ACTION_ENTER_PASSWORD);
                    mContext.startActivity(intent);
                } else if (msg.sync_result == SyncMsg.ERROR_PFS_BUSY) {
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

        @Override
        public void onDownloadComplete(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
        }

    }

    static final String TAG = "ProgressListActivity";

    protected void adpterSetup() {
        if (mAdapter == null) {
            KiiSyncClient client = KiiSyncClient.getInstance(this);
            mAdapter = new KiiFileExpandableListAdapter(this, client,
                    KiiFileExpandableListAdapter.TYPE_PROGRESS, this);
            setListAdapter(mAdapter);
            handler.sendEmptyMessageDelayed(PROGRESS_AUTO, 500);
            updateProgress();
        }
    }

    public final static int PROGRESS_START = 1;
    public final static int PROGRESS_END = 3;
    public final static int PROGRESS_AUTO = 4;
    public final static int PROGRESS_UPDATE = 6;

    private int updateProgress() {
        KiiSyncClient kiiClient = KiiSyncClient.getInstance(this);
        if (kiiClient != null) {
            int progress = kiiClient.getOverallProgress();
            if (progress > 0) {
                setProgress(progress);
                mAdapter.notifyDataSetChanged();
                return progress;
            }
        }
        return 0;
    }

    private int mProgress = 0;
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROGRESS_AUTO:
                    mProgress = updateProgress();
                    if ((mProgress > 0) || (mAdapter.getGroupCount() > 0)) {
                        setProgressBarIndeterminateVisibility(true);
                        setProgressBarVisibility(true);
                        handler.sendEmptyMessageDelayed(PROGRESS_AUTO, 5000);
                        setHeaderText();
                    } else {
                        setProgressBarIndeterminateVisibility(false);
                        setProgressBarVisibility(false);
                    }
                    break;
                case PROGRESS_START:
                    handler.removeMessages(PROGRESS_AUTO);
                    handler.removeMessages(PROGRESS_END);
                    handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 500);
                    setProgressBarIndeterminateVisibility(true);
                    setProgressBarVisibility(true);
                    if ((msg.obj != null) && (msg.obj instanceof String)) {
                        setTitle((String) msg.obj);
                    }
                case PROGRESS_UPDATE:
                    handler.removeMessages(PROGRESS_AUTO);
                    handler.sendEmptyMessageDelayed(PROGRESS_UPDATE, 5000);
                    mProgress = updateProgress();
                    setHeaderText();
                    setProgressBarIndeterminateVisibility(true);
                    setProgressBarVisibility(true);
                    mAdapter.notifyDataSetChanged();
                    break;
                case PROGRESS_END:
                default:
                    handler.removeMessages(PROGRESS_AUTO);
                    handler.removeMessages(PROGRESS_UPDATE);
                    handler.removeMessages(PROGRESS_END);
                    setProgressBarIndeterminateVisibility(false);
                    setProgressBarVisibility(false);
                    setTitle(R.string.app_name);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    mProgress = updateProgress();
                    setHeaderText();
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
            KiiFile kFile = (KiiFile) mAdapter.getChild(group, child);
            if ((kFile != null) && kFile.isFile()) {
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
            final KiiFile kFile = (KiiFile) mAdapter.getChild(groupPos,
                    childPos);
            if ((kFile != null) && kFile.isFile()) {
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
        Runnable r = new Runnable() {
            public void run() {
                KiiSyncClient kiiClient = KiiSyncClient
                        .getInstance(ProgressListActivity.this);
                if (kiiClient != null) {
                    kiiClient.suspend();
                }
            }
        };
        new Thread(r).start();
    }

    public void handleButtonRight(View v) {
        Utils.startSync(this, BackupService.ACTION_REFRESH);
    }

    private void setHeaderText() {
        final String lastSyncTime = UiUtils.getLastSyncTime(this);
        ProgressListActivity.this.getExpandableListView().post(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.header_text);
                if ((mProgress > 0) && (mProgress < 100)) {
                    tv.setText("Progress: " + mProgress + "%");
                } else {
                    tv.setText(lastSyncTime);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OPTION_MENU_SETTING, 0, getString(R.string.settings));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPTION_MENU_SETTING:
                Intent intent = new Intent(this, StartActivity.class);
                intent.setAction(Intent.ACTION_CONFIGURATION_CHANGED);
                startActivity(intent);
                break;
        }
        return true;
    }

}
