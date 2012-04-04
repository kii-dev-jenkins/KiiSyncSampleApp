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
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.cloud.sync.SyncNewEventListener;
import com.kii.demo.sync.R;
import com.kii.demo.sync.ui.view.KiiFileExpandableListAdapter;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class KiiFilePickerActivity extends ExpandableListActivity implements
        View.OnClickListener {

    public static final String TAG = "KiiFilePickerActivity";
    // message for the handler
    public final static int PROGRESS_START = 1;
    public final static int PROGRESS_END = 3;
    public final static int PROGRESS_UPDATE = 6;

    final static int MENU_RESTORE_TRASH = 1;
    final static int MENU_MOVE_TRASH = 2;
    final static int MENU_DELETE = 3;
    final static int MENU_DELETE_LOCAL = 4;
    final static int MENU_DOWNLOAD = 5;
    final static int MENU_CANCEL = 6;

    NewEventListener mNewEventListener = null;

    KiiFileExpandableListAdapter mAdapter;
    private static Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mNewEventListener = new NewEventListener(this);
        setContentView(R.layout.expandable_list_with_header);
        connect();
        registerForContextMenu(getExpandableListView());
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROGRESS_START:
                    handler.removeMessages(PROGRESS_END);
                case PROGRESS_UPDATE:
                    mAdapter.notifyDataSetChanged();
                    break;
                case PROGRESS_END:
                default:
                    handler.removeMessages(PROGRESS_END);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
            }

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        // unregister the listener
        if (mNewEventListener != null) {
            mNewEventListener.unregister();
        }
        super.onDestroy();
    }

    /**
     * get new records from server if there are any
     */
    private void syncRefresh() {
        Utils.startSync(this, BackupService.ACTION_REFRESH_QUICK);
    }

    /**
     * Login
     */
    private void connect() {

        Receiver receiver = new Receiver();
        // register ACTION_INIT_COMPLETE which will be send by KiiClient
        // registerReceiver(receiver, new
        // IntentFilter(KiiRefClient.ACTION_INIT_COMPLETE));
        registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_END));
        registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_START));
        adpaterSetup();
    }

    private void adpaterSetup() {
        Button b = (Button) findViewById(R.id.button_left);
        b.setText(getString(R.string.button_refresh));
        b = (Button) findViewById(R.id.button_right);
        b.setText(getString(R.string.header_upload));
        setLastSyncTime();
        mAdapter = new KiiFileExpandableListAdapter(this,
                KiiSyncClient.getInstance(mContext),
                KiiFileExpandableListAdapter.TYPE_DATA, this);
        setListAdapter(mAdapter);
        mNewEventListener.register();
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        KiiFile kFile = (KiiFile) mAdapter.getChild(groupPosition,
                childPosition);
        Log.d(TAG, "onChildClick: kFile is " + kFile.getTitle());
        if (kFile.isFile()) {

            String category = kFile.getCategory();

            // check if the file is trash category, if yes prompt for
            // restore
            if (!TextUtils.isEmpty(category)
                    && KiiSyncClient.CATEGORY_TRASH.equalsIgnoreCase(category)) {
                UiUtils.showToast(mContext, "Please restore before view!");
                return true;
            }

            Intent intent = null;
            MimeInfo mime = MimeUtil.getInfoByKiiFile(kFile);

            if (KiiSyncClient.getInstance(mContext).getStatus(kFile) != KiiFile.STATUS_NO_BODY) {
                intent = UiUtils
                        .getLaunchFileIntent(kFile.getLocalPath(), mime);
            }
            if ((intent == null) && (kFile.getAvailableURL() != null)) {
                if (mime != null) {
                    intent = UiUtils.getLaunchURLIntent(
                            kFile.getAvailableURL(), mime.getMimeType());
                }
            }

            if (intent == null) {
                UiUtils.showToast(mContext, "Failed to launch the file - "
                        + kFile.getTitle());
            } else {
                try {
                    startActivity(intent);
                } catch (Exception ex) {
                    UiUtils.showToast(
                            mContext,
                            "Encounter error when launch file ("
                                    + kFile.getTitle() + "). Error("
                                    + ex.getMessage() + ")");
                }
            }
        } else {
            UiUtils.showToast(mContext, "Failed to launch the viewer for "
                    + kFile.getTitle());
        }
        return super.onChildClick(parent, v, groupPosition, childPosition, id);
    }

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
        // Only create a context menu for child items
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            KiiFile kFile = (KiiFile) mAdapter.getChild(group, child);
            if ((kFile != null) && kFile.isFile()) {
                menu.setHeaderTitle(kFile.getTitle());

                KiiSyncClient kiiClient = KiiSyncClient.getInstance(mContext);

                if (kiiClient == null) {
                    UiUtils.showToast(mContext, "Not ready.");
                    return;
                }

                int status = kiiClient.getStatus(kFile);

                switch (status) {
                    case KiiFile.STATUS_DELETE_REQUEST:
                    case KiiFile.STATUS_DELETE_REQUEST_INCLUDEBODY:
                    case KiiFile.STATUS_SERVER_DELETE_REQUEST:
                        UiUtils.showToast(mContext,
                                "No option for deleted file.");
                        break;
                    case KiiFile.STATUS_PREPARE_TO_SYNC:
                    case KiiFile.STATUS_UPLOADING_BODY:
                        menu.add(MENU_CANCEL, 0, 0, "Cancel Upload");
                        break;
                    // TODO: can implement the option to update the backup copy
                    // or restore the previous version
                    case KiiFile.STATUS_BODY_OUTDATED:
                    case KiiFile.STATUS_NO_BODY:
                    case KiiFile.STATUS_SYNCED:
                    case KiiFile.STATUS_REQUEST_BODY:
                    case KiiFile.STATUS_DOWNLOADING_BODY:
                    case KiiFile.STATUS_UNKNOWN:
                    default:
                        String category = kFile.getCategory();
                        if (TextUtils.isEmpty(category)
                                || !KiiSyncClient.CATEGORY_TRASH
                                        .equalsIgnoreCase(category)) {
                            menu.add(MENU_MOVE_TRASH, 0, 0, "Move To Trash");
                            if (status == KiiFile.STATUS_NO_BODY) {
                                menu.add(MENU_DOWNLOAD, 0, 0, "Download");
                            } else {
                                menu.add(MENU_DELETE_LOCAL, 0, 0,
                                        "Delete Local & Backup");
                            }
                        } else {
                            menu.add(MENU_RESTORE_TRASH, 0, 0,
                                    "Restore From Trash");
                        }
                        menu.add(MENU_DELETE, 0, 0, "Delete Backup Copy");
                        break;
                }
            } else {
                UiUtils.showToast(mContext, "No menu for directory.");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
                .getMenuInfo();

        String title = "Menu";

        int type = ExpandableListView
                .getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

            int groupPos = ExpandableListView
                    .getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView
                    .getPackedPositionChild(info.packedPosition);

            final KiiFile kFile = (KiiFile) mAdapter.getChild(groupPos,
                    childPos);
            if ((kFile != null) && kFile.isFile()) {
                final KiiSyncClient client = KiiSyncClient
                        .getInstance(mContext);
                if (client == null) {
                    Log.d(TAG, "get KiiRefClient failed, return!");
                    return true;
                }
                switch (item.getGroupId()) {
                    case MENU_RESTORE_TRASH:
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                client.restoreFromTrash(kFile);
                            }
                        };
                        new Thread(r).start();
                        break;
                    case MENU_MOVE_TRASH:
                        int ret = client.moveKiiFileToTrash(kFile);
                        if (ret == SyncMsg.ERROR_RECORD_NOT_FOUND) {
                            UiUtils.showToast(
                                    this,
                                    getString(R.string.error_cannot_trash_server_file));
                            return true;
                        }
                        break;
                    case MENU_DELETE:
                        client.delete(kFile, false);
                        break;
                    case MENU_DELETE_LOCAL:
                        client.delete(kFile, true);
                        break;
                    case MENU_CANCEL:
                        client.cancel(kFile);
                        break;
                    case MENU_DOWNLOAD:
                        Toast.makeText(
                                this,
                                "Download at:"
                                        + Utils.getKiiFileDownloadPath(kFile),
                                Toast.LENGTH_SHORT).show();
                        Runnable r1 = new Runnable() {
                            @Override
                            public void run() {
                                client.download(kFile, Utils.getKiiFileDownloadPath(kFile));
                            }
                        };
                        new Thread(r1).start();
                        break;
                }

                Utils.startSync(getApplicationContext(),
                        BackupService.ACTION_REFRESH);
            }

            return true;
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            int groupPos = ExpandableListView
                    .getPackedPositionGroup(info.packedPosition);
            Toast.makeText(this, title + ": Group " + groupPos + " clicked",
                    Toast.LENGTH_SHORT).show();

            return true;
        }

        return false;
    }

    class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()
                    .equals(DownloadManager.ACTION_DOWNLOAD_START)) {
                handler.sendEmptyMessageDelayed(
                        KiiFilePickerActivity.PROGRESS_START, 500);
            } else if (intent.getAction().equals(
                    DownloadManager.ACTION_DOWNLOAD_END)) {
                handler.sendEmptyMessage(KiiFilePickerActivity.PROGRESS_END);
            }
        }
    }

    public class NewEventListener extends SyncNewEventListener {

        final static String TAG = "NewEventListener";

        KiiSyncClient client = null;
        long id = 0;

        public NewEventListener(Context context) {
            super(context);
        }

        @Override
        public void onNewSyncDeleteEvent(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_UPDATE, 500);
        }

        @Override
        public void onNewSyncInsertEvent(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_UPDATE, 500);
        }

        @Override
        public void onNewSyncUpdateEvent(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_UPDATE, 500);
        }

        @Override
        public void onSyncComplete(SyncMsg msg) {
            handler.sendEmptyMessageDelayed(KiiFilePickerActivity.PROGRESS_END,
                    500);
            if (msg != null) {
                switch (msg.sync_result) {
                    case SyncMsg.OK:
                        setLastSyncTime();
                        break;
                }
            }
        }

        @Override
        public void onSyncStart(String syncMode) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_START, 500);
        }

        @Override
        public void onQuotaExceeded(Uri arg0) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_UPDATE, 500);

        }

        @Override
        public void onLocalChangeSyncedEvent(Uri[] uris) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_UPDATE, 500);
        }

        @Override
        public void onDownloadComplete(Uri[] arg0) {
            handler.sendEmptyMessageDelayed(
                    KiiFilePickerActivity.PROGRESS_UPDATE, 500);
        }

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
        syncRefresh();
    }

    public void handleButtonRight(View v) {
        Intent i = new Intent(this, ProgressListActivity.class);
        this.startActivity(i);

    }

    private void setLastSyncTime() {
        TextView tv = (TextView) findViewById(R.id.header_text);
        tv.setText(UiUtils.getLastSyncTime(this));
    }

}
