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

package com.kii.demo.sync.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.cloud.sync.SyncNewEventListener;
import com.kii.demo.sync.R;
import com.kii.demo.sync.ui.ProgressListActivity;
import com.kii.demo.sync.ui.SettingsActivity;
import com.kii.demo.sync.ui.view.ActionItem;
import com.kii.demo.sync.ui.view.KiiFileExpandableListAdapter;
import com.kii.demo.sync.ui.view.QuickAction;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class KiiFileFragment extends Fragment {
    private static View mView;
    private NewEventListener mNewEventListener = null;
    private ExpandableListView mList = null;
    KiiFileExpandableListAdapter mAdapter;

    final static int MENU_RESTORE_TRASH = 201;
    final static int MENU_MOVE_TRASH = 202;
    final static int MENU_DELETE = 203;
    final static int MENU_DELETE_LOCAL = 204;
    final static int MENU_DOWNLOAD = 205;
    final static int MENU_CANCEL = 206;

    private QuickAction mQuickAction;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.expandable_list_with_header,
                container, false);
        Button b = (Button) mView.findViewById(R.id.button_left);
        b.setText(getString(R.string.button_refresh));
        b.setOnClickListener(mClickListener);
        b = (Button) mView.findViewById(R.id.button_right);
        b.setText(getString(R.string.header_upload));
        b.setOnClickListener(mClickListener);
        refreshUI(getActivity());
        mList = (ExpandableListView) mView.findViewById(android.R.id.list);
        setHasOptionsMenu(true);
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mNewEventListener = new NewEventListener(getActivity());
        mAdapter = new KiiFileExpandableListAdapter(getActivity(),
                KiiSyncClient.getInstance(getActivity()),
                KiiFileExpandableListAdapter.TYPE_DATA, mClickListener);
        mList.setAdapter(mAdapter);
        mList.setOnChildClickListener(mChildListener);
        mNewEventListener.register();
        registerReceiver();
    }

    private Receiver receiver;

    private void registerReceiver() {
        receiver = new Receiver();
        getActivity().registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_END));
        getActivity().registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_START));
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

    @Override
    public void onDestroy() {
        if (mNewEventListener != null) {
            mNewEventListener.unregister();
        }
        getActivity().unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(getActivity(),
                        SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
            handler.sendEmptyMessageDelayed(PROGRESS_END, 500);
            if (msg != null) {
                switch (msg.sync_result) {
                    case SyncMsg.OK:
                        refreshUI(getActivity());
                        break;
                }
            }
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

    public final static int PROGRESS_START = 1;
    public final static int PROGRESS_END = 2;
    public final static int PROGRESS_UPDATE = 3;

    private static void refreshUI(Context context) {
        // refresh the header text;
        TextView tv = (TextView) mView.findViewById(R.id.header_text);
        tv.setText(UiUtils.getLastSyncTime(context));
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_left:
                    syncRefresh();
                    break;
                case R.id.button_right:
                    Intent i = new Intent(getActivity(),
                            ProgressListActivity.class);
                    getActivity().startActivity(i);
                    break;
                case R.id.list_complex_more_button:
                    View row = (View) v.getTag();
                    final KiiFile file = (KiiFile) row.getTag();
                    mQuickAction = new QuickAction(getActivity());
                    int status = file.getStatus();
                    String category = file.getCategory();
                    setActions(status, category);
                    if (mQuickAction.getActionItem(0) != null) {
                        mQuickAction
                                .setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                                    @Override
                                    public void onItemClick(QuickAction source,
                                            int pos, int actionId) {
                                        handleKiiFileAction(file, actionId);
                                    }
                                });
                        mQuickAction.show(v);
                    }
                    break;
            }
        }
    };

    private void setActions(int status, String category) {
        switch (status) {
            case KiiFile.STATUS_PREPARE_TO_SYNC:
            case KiiFile.STATUS_UPLOADING_BODY:
                mQuickAction.addActionItem(new ActionItem(MENU_CANCEL,
                        getString(R.string.cancel_upload)));
                break;
            case KiiFile.STATUS_BODY_OUTDATED:
            case KiiFile.STATUS_NO_BODY:
            case KiiFile.STATUS_SYNCED:
            case KiiFile.STATUS_REQUEST_BODY:
            case KiiFile.STATUS_DOWNLOADING_BODY:
            case KiiFile.STATUS_UNKNOWN:
                if (TextUtils.isEmpty(category)
                        || !KiiSyncClient.CATEGORY_TRASH
                                .equalsIgnoreCase(category)) {
                    mQuickAction.addActionItem(new ActionItem(MENU_MOVE_TRASH,
                            getString(R.string.move_to_trash)));
                    if (status == KiiFile.STATUS_NO_BODY) {
                        mQuickAction.addActionItem(new ActionItem(
                                MENU_DOWNLOAD, getString(R.string.download)));
                    } else {
                        mQuickAction.addActionItem(new ActionItem(
                                MENU_DELETE_LOCAL,
                                getString(R.string.delete_local_backup)));
                    }
                } else {
                    mQuickAction.addActionItem(new ActionItem(
                            MENU_RESTORE_TRASH,
                            getString(R.string.restore_from_trash)));
                }
                mQuickAction.addActionItem(new ActionItem(MENU_DELETE,
                        getString(R.string.delete_backup_copy)));
        }
    }

    /**
     * get new records from server if there are any
     */
    private void syncRefresh() {
        Utils.startSync(getActivity(), BackupService.ACTION_REFRESH_QUICK);
    }

    private void handleKiiFileAction(final KiiFile file, int actionId) {
        final KiiSyncClient client = KiiSyncClient.getInstance(getActivity());
        switch (actionId) {
            case MENU_RESTORE_TRASH:
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        client.restoreFromTrash(file);
                    }
                };
                new Thread(r).start();
                break;
            case MENU_MOVE_TRASH:
                int ret = client.moveKiiFileToTrash(file);
                if (ret == SyncMsg.ERROR_RECORD_NOT_FOUND) {
                    UiUtils.showToast(getActivity(),
                            getString(R.string.error_cannot_trash_server_file));
                }
                break;
            case MENU_DELETE:
                client.delete(file, false);
                break;
            case MENU_DELETE_LOCAL:
                client.delete(file, true);
                break;
            case MENU_CANCEL:
                client.cancel(file);
                break;
            case MENU_DOWNLOAD:
                Toast.makeText(getActivity(),
                        "Download at:" + Utils.getKiiFileDownloadPath(file),
                        Toast.LENGTH_SHORT).show();
                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        client.download(file,
                                Utils.getKiiFileDownloadPath(file));
                    }
                };
                new Thread(r1).start();
                break;

        }
        Utils.startSync(getActivity(), BackupService.ACTION_REFRESH);
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

    private OnChildClickListener mChildListener = new OnChildClickListener() {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v,
                int groupPosition, int childPosition, long id) {
            KiiFile kFile = (KiiFile) mAdapter.getChild(groupPosition,
                    childPosition);
            if (kFile.isFile()) {

                String category = kFile.getCategory();

                // check if the file is trash category, if yes prompt for
                // restore
                if (!TextUtils.isEmpty(category)
                        && KiiSyncClient.CATEGORY_TRASH
                                .equalsIgnoreCase(category)) {
                    UiUtils.showToast(getActivity(),
                            "Please restore before view!");
                    return true;
                }

                Intent intent = null;
                MimeInfo mime = MimeUtil.getInfoByKiiFile(kFile);

                if (KiiSyncClient.getInstance(getActivity()).getStatus(kFile) != KiiFile.STATUS_NO_BODY) {
                    intent = UiUtils.getLaunchFileIntent(kFile.getLocalPath(),
                            mime);
                }
                if ((intent == null) && (kFile.getAvailableURL() != null)) {
                    if (mime != null) {
                        intent = UiUtils.getLaunchURLIntent(
                                kFile.getAvailableURL(), mime.getMimeType());
                    }
                }

                if (intent == null) {
                    UiUtils.showToast(getActivity(),
                            "Failed to launch the file - " + kFile.getTitle());
                } else {
                    try {
                        startActivity(intent);
                    } catch (Exception ex) {
                        UiUtils.showToast(
                                getActivity(),
                                "Encounter error when launch file ("
                                        + kFile.getTitle() + "). Error("
                                        + ex.getMessage() + ")");
                    }
                }
            } else {
                UiUtils.showToast(getActivity(),
                        "Failed to launch the viewer for " + kFile.getTitle());
            }
            return true;
        }

    };
}