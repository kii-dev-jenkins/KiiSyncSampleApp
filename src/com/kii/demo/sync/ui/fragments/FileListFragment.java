package com.kii.demo.sync.ui.fragments;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.cloud.sync.SyncNewEventListener;
import com.kii.demo.sync.R;
import com.kii.demo.sync.ui.view.ActionItem;
import com.kii.demo.sync.ui.view.KiiListItemView;
import com.kii.demo.sync.ui.view.QuickAction;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class FileListFragment extends ListFragment {
    protected File mDirectory;
    protected ArrayList<File> mFiles;
    protected FilePickerListAdapter mAdapter;
    protected boolean mShowHiddenFiles = false;
    protected String[] acceptedFileExtensions;
    private final static String DEFAULT_INITIAL_DIRECTORY = "/mnt/sdcard/";
    private View mView;
    private NewEventListener mListener = null;

    private final static int OPTIONS_MENU_SCAN_CHANGE = 0;
    private final static int OPTIONS_MENU_DOWNLOAD_ALL = 1;
    QuickAction mQuickAction = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.list_with_header, container, false);
        Button b = (Button) mView.findViewById(R.id.button_left);
        b.setText(getString(R.string.header_btn_home));
        b.setOnClickListener(mClickListener);
        b = (Button) mView.findViewById(R.id.button_right);
        b.setText(getString(R.string.header_btn_up));
        b.setOnClickListener(mClickListener);
        setHasOptionsMenu(true);
        return mView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        File newFile = (File) l.getItemAtPosition(position);
        if (newFile.isFile()) {
            Intent intent = null;
            MimeInfo mime = MimeUtil.getInfoByFileName(newFile
                    .getAbsolutePath());

            intent = UiUtils.getLaunchFileIntent(newFile.getAbsolutePath(),
                    mime);
            if (intent == null) {
                UiUtils.showToast(getActivity(), "Failed to launch the file - "
                        + newFile.getName());
            } else {
                try {
                    startActivity(intent);
                } catch (Exception ex) {
                    UiUtils.showToast(
                            getActivity(),
                            "Encounter error when launch file ("
                                    + newFile.getName() + "). Error("
                                    + ex.getMessage() + ")");
                }
            }
        } else {
            mDirectory = newFile;
            // Update the files list
            refreshFilesList();
        }

        super.onListItemClick(l, v, position, id);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDirectory = new File(DEFAULT_INITIAL_DIRECTORY);
        mFiles = new ArrayList<File>();
        mAdapter = new FilePickerListAdapter(getActivity(), mFiles);
        setListAdapter(mAdapter);
        mListener = new NewEventListener(getActivity());
        mListener.register();
    }

    private void uploadFile(final File file) {
        KiiSyncClient client = KiiSyncClient.getInstance(getActivity());
        client.upload(file.getAbsolutePath());
        Utils.startSync(getActivity(), BackupService.ACTION_REFRESH);
    }

    private void uploadFolder(final File file) {
        KiiSyncClient client = KiiSyncClient.getInstance(getActivity());
        File[] files = file.listFiles();
        for (int ct = 0; ct < files.length; ct++) {
            if (files[ct].isFile()) {
                client.upload(files[ct].getAbsolutePath());
            }
        }
        Utils.startSync(getActivity(), BackupService.ACTION_REFRESH);
    }

    private void moveToTrash(File file) {
        KiiSyncClient client = KiiSyncClient.getInstance(getActivity());
        client.moveToTrash(file.getAbsolutePath(), null);
        Utils.startSync(getActivity(), BackupService.ACTION_REFRESH);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, OPTIONS_MENU_SCAN_CHANGE, 0, R.string.scan_change);
        menu.add(0, OPTIONS_MENU_DOWNLOAD_ALL, 1, R.string.download_all);
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPTIONS_MENU_SCAN_CHANGE:
                new ScanTask().execute();
                break;
            case OPTIONS_MENU_DOWNLOAD_ALL:
                Utils.startSync(getActivity(),
                        BackupService.ACTION_REFRESH_QUICK);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        refreshFilesList();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (mListener != null) {
            mListener.unregister();
        }
        super.onDestroy();
    }

    private class FilePickerListAdapter extends BaseAdapter {

        private List<File> mObjects;

        public FilePickerListAdapter(Context context, List<File> objects) {
            super();
            mObjects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            File file = mObjects.get(position);
            Drawable icon = null;
            if (ICON_CACHE.containsKey(file.getAbsolutePath())) {
                icon = ICON_CACHE.get(file.getAbsolutePath());
            } else {
                icon = Utils.getThumbnailDrawableByFilename(
                        file.getAbsolutePath(), getActivity());
                ICON_CACHE.put(file.getAbsolutePath(), icon);
            }
            KiiListItemView v;
            if (convertView == null) {
                v = new KiiListItemView(getActivity(), file,
                        KiiSyncClient.getInstance(getActivity()), icon,
                        mClickListener);
                return v;
            } else {
                v = (KiiListItemView) convertView;
                v.refreshWithNewFile(file, icon);
                return v;
            }
        }

        @Override
        public int getCount() {
            return mObjects.size();
        }

        @Override
        public Object getItem(int position) {
            return mObjects.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }

    private static HashMap<String, Drawable> ICON_CACHE = new HashMap<String, Drawable>();

    private class FileComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            if (f1 == f2) {
                return 0;
            }
            if (f1.isDirectory() && f2.isFile()) {
                // Show directories above files
                return -1;
            }
            if (f1.isFile() && f2.isDirectory()) {
                // Show files below directories
                return 1;
            }
            // Sort the directories alphabetically
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    private class ExtensionFilenameFilter implements FilenameFilter {
        private String[] mExtensions;

        public ExtensionFilenameFilter(String[] extensions) {
            super();
            mExtensions = extensions;
        }

        @Override
        public boolean accept(File dir, String filename) {
            if (new File(dir, filename).isDirectory()) {
                // Accept all directory names
                return true;
            }
            if ((mExtensions != null) && (mExtensions.length > 0)) {
                for (int i = 0; i < mExtensions.length; i++) {
                    if (filename.endsWith(mExtensions[i])) {
                        // The filename ends with the extension
                        return true;
                    }
                }
                // The filename did not match any of the extensions
                return false;
            }
            // No extensions has been set. Accept all file extensions.
            return true;
        }
    }

    private void refreshFilesList() {
        // Clear the files ArrayList
        mFiles.clear();

        // Set the extension file filter
        ExtensionFilenameFilter filter = new ExtensionFilenameFilter(
                acceptedFileExtensions);

        // Get the files in the directory
        File[] files = mDirectory.listFiles(filter);
        if ((files != null) && (files.length > 0)) {
            for (File f : files) {
                if (f.isHidden() && !mShowHiddenFiles) {
                    // Don't add the file
                    continue;
                }
                // Add the file the ArrayAdapter
                mFiles.add(f);
            }

            Collections.sort(mFiles, new FileComparator());
        }
        mAdapter.notifyDataSetChanged();
        Button b = (Button) mView.findViewById(R.id.button_right);
        if (isAtSdHome()) {
            // at SD card home, disable Up button
            b.setEnabled(false);
        } else {
            b.setEnabled(true);
        }
        TextView tv = (TextView) mView.findViewById(R.id.header_text);
        tv.setText(getString(R.string.header_text_path) + mDirectory.getPath());
    }

    private boolean isAtSdHome() {
        return mDirectory.compareTo(Environment.getExternalStorageDirectory()
                .getAbsoluteFile()) == 0;
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_left:
                    mDirectory = Environment.getExternalStorageDirectory()
                            .getAbsoluteFile();
                    refreshFilesList();
                    break;
                case R.id.button_right:
                    mDirectory = mDirectory.getParentFile();
                    refreshFilesList();
                    break;
                case R.id.list_complex_more_button:
                    View row = (View) v.getTag();
                    final File f = (File) row.getTag();
                    mQuickAction = new QuickAction(getActivity());
                    if (f.isFile()) {
                        mQuickAction.addActionItem(new ActionItem(
                                ACTION_ITEM_UPLOAD,
                                getString(R.string.menu_item_upload_file)));
                        mQuickAction.addActionItem(new ActionItem(
                                ACTION_ITEM_MOVE_TO_TRASH,
                                getString(R.string.menu_item_move_to_trash)));
                    } else if (f.isDirectory()) {
                        mQuickAction.addActionItem(new ActionItem(
                                ACTION_ITEM_UPLOAD_FOLDER,
                                getString(R.string.menu_item_upload_folder)));
                    }
                    mQuickAction
                            .setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                                @Override
                                public void onItemClick(QuickAction source,
                                        int pos, int actionId) {
                                    switch (actionId) {
                                        case ACTION_ITEM_UPLOAD:
                                            uploadFile(f);
                                            break;
                                        case ACTION_ITEM_MOVE_TO_TRASH:
                                            moveToTrash(f);
                                            break;
                                        case ACTION_ITEM_UPLOAD_FOLDER:
                                            uploadFolder(f);
                                            break;
                                    }
                                }
                            });
                    if (mQuickAction.getActionItem(0) != null) {
                        mQuickAction.show(v);
                    }
                    break;
            }
        }
    };

    private static final int ACTION_ITEM_MOVE_TO_TRASH = 0;
    private static final int ACTION_ITEM_UPLOAD = 1;
    private static final int ACTION_ITEM_UPLOAD_FOLDER = 2;

    private class NewEventListener extends SyncNewEventListener {

        private NewEventListener(Context context) {
            super(context);
        }

        @Override
        public void onNewSyncDeleteEvent(Uri[] arg0) {
        }

        @Override
        public void onNewSyncInsertEvent(Uri[] arg0) {
        }

        @Override
        public void onNewSyncUpdateEvent(Uri[] arg0) {
        }

        @Override
        public void onSyncComplete(SyncMsg msg) {
            if (needDownload) {
                new DownloadAllTask().execute();
                needDownload = false;
            }
            getListView().post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onSyncStart(String syncMode) {
        }

        @Override
        public void onQuotaExceeded(Uri arg0) {
        }

        @Override
        public void onLocalChangeSyncedEvent(Uri[] uris) {
        }

        @Override
        public void onDownloadComplete(Uri[] arg0) {
        }
    }

    private boolean needDownload = false;

    public class DownloadAllTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            KiiSyncClient client = KiiSyncClient.getInstance(getActivity());
            if (client != null) {
                KiiFile[] files = client.getBackupFiles();
                if (files != null) {
                    for (KiiFile file : files) {
                        int status = client.getStatus(file);
                        if (!Utils.isKiiFileInTrash(file)
                                && ((status == KiiFile.STATUS_BODY_OUTDATED) || (status == KiiFile.STATUS_NO_BODY))) {
                            client.download(file,
                                    Utils.getKiiFileDownloadPath(file));
                        }
                    }
                }
            }
            return null;
        }

    }

    ProgressDialog scanDialog = null;

    public class ScanTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPostExecute(Void result) {
            if (scanDialog != null) {
                scanDialog.dismiss();
                scanDialog = null;
            }
            if (!scanChange.isEmpty()) {
                // TODO: let fragment activity to show dialog;
                getActivity().showDialog(DIALOG_UPDATE);
            } else {
                UiUtils.showToast(getActivity(), "No update is found.");
            }
        }

        @Override
        protected void onPreExecute() {
            if (scanDialog == null) {
                scanDialog = ProgressDialog.show(getActivity(), "",
                        "Scanning for update. Please wait...", true);
            } else {
                if (scanTotalCount > 0) {
                    scanDialog.setMessage(String.format("Scan %d out of %d",
                            scanCurCount, scanTotalCount));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            scanFileChange();
            return null;
        }

    }

    static ArrayList<KiiFile> scanChange = null;
    int scanTotalCount = -1;
    int scanCurCount = 0;

    private void scanFileChange() {
        scanChange = new ArrayList<KiiFile>();
        scanTotalCount = -1;
        scanCurCount = 0;
        KiiSyncClient kiiClient = KiiSyncClient.getInstance(getActivity());
        KiiFile[] files = kiiClient.getBackupFiles();
        scanTotalCount = files.length;
        scanCurCount = 0;
        for (; scanCurCount < files.length; scanCurCount++) {
            if (files[scanCurCount].isFile()) {
                if (Utils.bodySameAsLocal(files[scanCurCount])) {
                    scanChange.add(files[scanCurCount]);
                }
            }
        }
    }

    public static List<KiiFile> getLocalChanges() {
        return scanChange;
    }

    public static final int DIALOG_UPDATE = 101;

}
