/*
 * Copyright 2011 Anders Kalør Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.kii.demo.sync.activities;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.R;
import com.kii.demo.sync.utils.Utils;

public class FilePickerActivity extends ListActivity implements
        View.OnClickListener {

    /**
     * The file path
     */
    public final static String EXTRA_FILE_PATH = "file_path";
    public final static String EXTRA_NEW_FOLDER_NAME = "new_name";

    /**
     * Sets whether hidden files should be visible in the list or not
     */
    public final static String EXTRA_SHOW_HIDDEN_FILES = "show_hidden_files";

    /**
     * The allowed file extensions in an ArrayList of Strings
     */
    public final static String EXTRA_ACCEPTED_FILE_EXTENSIONS = "accepted_file_extensions";

    /**
     * The initial directory which will be used if no directory has been sent
     * with the intent
     */
    private final static String DEFAULT_INITIAL_DIRECTORY = "/mnt/sdcard/";

    private final static int MENU_RENAME = 1;
    private final static int MENU_UPLOAD_FILE = 2;
    private final static int MENU_UPLOAD_FOLDER = 3;
    private final static int MENU_MOVE_TO_TRASH = 4;
    private static final String TAG = "FilePickerActivity";

    protected File mDirectory;
    protected ArrayList<File> mFiles;
    protected FilePickerListAdapter mAdapter;
    protected boolean mShowHiddenFiles = false;
    protected String[] acceptedFileExtensions;
    View mHeaderView;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the view to be shown if the list is empty
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emptyView = inflator
                .inflate(R.layout.file_picker_empty_view, null);
        ((ViewGroup) getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);
        mHeaderView = inflator.inflate(R.layout.home_header_view, null);
        getListView().addHeaderView(mHeaderView);
        // Set initial directory
        mDirectory = new File(DEFAULT_INITIAL_DIRECTORY);

        // Initialize the ArrayList
        mFiles = new ArrayList<File>();

        // Set the ListAdapter
        mAdapter = new FilePickerListAdapter(this, mFiles);
        setListAdapter(mAdapter);
        registerForContextMenu(getListView());

        // Initialize the extensions array to allow any file extensions
        acceptedFileExtensions = new String[] {};

        // Get intent extras
        if (getIntent().hasExtra(EXTRA_FILE_PATH)) {
            mDirectory = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));
        }
        if (getIntent().hasExtra(EXTRA_SHOW_HIDDEN_FILES)) {
            mShowHiddenFiles = getIntent().getBooleanExtra(
                    EXTRA_SHOW_HIDDEN_FILES, false);
        }
        if (getIntent().hasExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS)) {
            ArrayList<String> collection = getIntent().getStringArrayListExtra(
                    EXTRA_ACCEPTED_FILE_EXTENSIONS);
            acceptedFileExtensions = (String[]) collection
                    .toArray(new String[collection.size()]);
        }
        mContext = this;

    }

    @Override
    protected void onResume() {
        refreshFilesList();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Updates the list view to the current directory
     */
    protected void refreshFilesList() {
        // Clear the files ArrayList
        mFiles.clear();

        // Set the extension file filter
        ExtensionFilenameFilter filter = new ExtensionFilenameFilter(
                acceptedFileExtensions);

        // Get the files in the directory
        File[] files = mDirectory.listFiles(filter);
        if (files != null && files.length > 0) {
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

        Button b = (Button) mHeaderView.findViewById(R.id.header_up_button);
        if (isAtSdHome()) {
            // at SD card home, disable Up button
            b.setEnabled(false);
        } else {
            b.setEnabled(true);
        }
        TextView tv = (TextView) mHeaderView.findViewById(R.id.header_text);
        tv.setText(getString(R.string.header_text_path) + mDirectory.getPath());
    }

    private boolean isAtSdHome() {
        return mDirectory.compareTo(Environment.getExternalStorageDirectory()
                .getAbsoluteFile()) == 0;
    }

    @Override
    public void onBackPressed() {
        if (mDirectory.getParentFile() != null) {
            if (!isAtSdHome()) {
                // Go to parent directory
                mDirectory = mDirectory.getParentFile();
                refreshFilesList();
            }
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        File newFile = (File) l.getItemAtPosition(position);
        if (newFile.isFile()) {
            // Set result
            // Intent extra = new Intent();
            // extra.putExtra(EXTRA_FILE_PATH, newFile.getAbsolutePath());
            // setResult(RESULT_OK, extra);
            // // Finish the activity
            // finish();
        } else {
            mDirectory = newFile;
            // Update the files list
            refreshFilesList();
        }

        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        File selectedFile = (File) getListView().getItemAtPosition(
                info.position);
        menu.setHeaderTitle(selectedFile.getName());
        if (selectedFile.exists()) {
            Log.v(TAG, "Selected File :" + selectedFile.getAbsolutePath());
            if (selectedFile.isDirectory()) {
                menu.add(0, MENU_RENAME, 0, "Rename");
                menu.add(0, MENU_UPLOAD_FOLDER, 0,
                        getString(R.string.menu_item_upload_folder));
            } else {
                menu.add(0, MENU_UPLOAD_FILE, 0,
                        getString(R.string.menu_item_upload_file));
                menu.add(0, MENU_MOVE_TO_TRASH, 0,
                        getString(R.string.menu_item_move_to_trash));

            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        File selectedFile = (File) getListView().getItemAtPosition(
                info.position);
        final String filePath = selectedFile.getAbsolutePath();
        final KiiSyncClient client = KiiSyncClient.getInstance(mContext);
        if (client == null) {
            Log.d(TAG, "get KiiRefClient failed, return!");
            return true;
        }

        switch (item.getItemId()) {
            case MENU_RENAME:
                final AlertDialog.Builder alert = new AlertDialog.Builder(this);
                final EditText input = new EditText(this);
                alert.setView(input);
                alert.setTitle("Enter new folder name");
                alert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                String newName = input.getText().toString()
                                        .trim();
                                client.renameFolder(filePath, newName);
                                Utils.startSync(getApplicationContext(),
                                        BackupService.ACTION_REFRESH);

                            }
                        });

                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                dialog.cancel();
                            }
                        });
                alert.show();
                break;
            case MENU_UPLOAD_FILE:
                client.upload(filePath);
                Utils.startSync(getApplicationContext(),
                        BackupService.ACTION_REFRESH);
                break;
            case MENU_UPLOAD_FOLDER:
                if (selectedFile.isDirectory()) {
                    File file = new File(filePath);
                    File[] files = file.listFiles();
                    for (int ct = 0; ct < files.length; ct++) {
                        if (files[ct].isFile()) {
                            client.upload(files[ct].getAbsolutePath());
                        }
                    }

                    Utils.startSync(getApplicationContext(),
                            BackupService.ACTION_REFRESH);
                }
                break;
            case MENU_MOVE_TO_TRASH:
                client.moveToTrash(filePath, null);
                Utils.startSync(getApplicationContext(),
                        BackupService.ACTION_REFRESH);
                break;
            default:
                break;
        }

        return true;
    }

    private class FilePickerListAdapter extends ArrayAdapter<File> {

        private List<File> mObjects;

        public FilePickerListAdapter(Context context, List<File> objects) {
            super(context, android.R.id.text1, objects);
            mObjects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            File file = mObjects.get(position);
            Drawable icon = null;
            if(ICON_CACHE.containsKey(file.getAbsolutePath())) {
                icon = ICON_CACHE.get(file.getAbsolutePath());
            } else {
                icon = Utils.getThumbnailDrawableByFilename(file.getAbsolutePath(), FilePickerActivity.this);
                ICON_CACHE.put(file.getAbsolutePath(), icon);
            }
            if (convertView == null) {
                return new KiiListItemView(FilePickerActivity.this,
                        file, KiiSyncClient.getInstance(mContext),
                        icon,
                        FilePickerActivity.this);
            } else {
                KiiListItemView v = (KiiListItemView) convertView;
                v.refreshWithNewFile(file, icon);
                return v;
            }
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
            if (mExtensions != null && mExtensions.length > 0) {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_complex_more_button:
                View row = (View) v.getTag();
                getListView().showContextMenuForChild(row);
                break;
        }
    }

    public void handleHome(View v) {
        mDirectory = Environment.getExternalStorageDirectory()
                .getAbsoluteFile();
        refreshFilesList();
        return;
    }

    public void handleUp(View v) {
        if (!isAtSdHome()) {
            // Go to parent directory
            mDirectory = mDirectory.getParentFile();
            refreshFilesList();
            return;
        }
    }

}
