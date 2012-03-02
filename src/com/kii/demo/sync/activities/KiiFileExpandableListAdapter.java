package com.kii.demo.sync.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.R;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.sync.KiiFile;

public class KiiFileExpandableListAdapter extends BaseExpandableListAdapter {

    // itemsList is the list of items that contain items
    // if items[] is null, it is reference to trash folder
    ArrayList<KiiFileList> itemsList = new ArrayList<KiiFileList>();

    Activity mActivity = null;
    View.OnClickListener mOnClickListener = null;

    KiiSyncClient kiiClient = null;

    private static HashMap<String, Drawable> ICON_CACHE = new HashMap<String, Drawable>();

    private int mType = -1;
    public static final int TYPE_DATA = 1;
    public static final int TYPE_PROGRESS = 2;

    /**
     * Get the KiiFile path which can download the file
     * 
     * @param file
     * @return
     */
    String getKiiFilePath(KiiFile file) {
        if (file.isFile()) {
            String path = file.getLocalPath();
            if (path == null)
                return file.getAvailableURL().toString();
            else
                return path;
        } else {
            return file.getBucketName();
        }
    }

    public KiiFileExpandableListAdapter(Activity activity,
            KiiSyncClient kiiClient, int type, OnClickListener listener) {
        if (kiiClient == null)
            throw new NullPointerException();
        kiiClient.refreshKiiFileStatusCache();
        mActivity = activity;
        mOnClickListener = listener;
        this.kiiClient = kiiClient;
        this.mType = type;
        addDataSet(itemsList);
    }

    @Override
    public void notifyDataSetChanged() {
        itemsList = new ArrayList<KiiFileList>();
        addDataSet(itemsList);
        super.notifyDataSetChanged();
    }

    protected void addDataSet(ArrayList<KiiFileList> itemsList) {
        if (mType == TYPE_PROGRESS) {
            DownloadManager downloadManager = kiiClient.downManager;
            if (downloadManager != null) {
                KiiFile[] downloadFiles = downloadManager.getDownloadList();
                if (downloadFiles != null && downloadFiles.length > 0) {
                    itemsList
                            .add(new KiiFileList("Downloading", downloadFiles));
                }
            }

            KiiFile[] uploadingFiles = kiiClient.getListInProgress();
            if (uploadingFiles != null && uploadingFiles.length > 0) {
                itemsList.add(new KiiFileList("Progress", uploadingFiles));
            }
        } else if (mType == TYPE_DATA) {
            KiiFile[] errorFiles = kiiClient.getListError();
            if (errorFiles != null && errorFiles.length > 0) {
                itemsList.add(new KiiFileList("Error", errorFiles));
            }

            KiiFile[] trashFiles = kiiClient.getTrashFiles();
            if (trashFiles != null && trashFiles.length > 0) {
                itemsList.add(new KiiFileList("Trash", trashFiles));
            } else {
                itemsList.add(new KiiFileList("Trash"));
            }

            KiiFile[] astroFiles = kiiClient.getAstroFiles();
            if (astroFiles != null && astroFiles.length > 0) {
                itemsList.add(new KiiFileList("Astro", astroFiles));
            }

            KiiFile[] backupFiles = kiiClient.getBackupFiles();
            if (backupFiles != null && backupFiles.length > 0) {
                itemsList.add(new KiiFileList("Backup", backupFiles));

                // KiiFile[] root = kiiClient.getBackupFolders();
                // if (root != null && root.length > 0) {
                // for (int ct = 0; ct < root.length; ct++) {
                // itemsList.add(new KiiFileList(root[ct]));
                // }
                // }
            } else {
                itemsList.add(new KiiFileList("Backup"));
            }
        }
    }

    public Object getChild(int groupPosition, int childPosition) {
        KiiFileList group = itemsList.get(groupPosition);
        return group.getChild(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        KiiFileList group = itemsList.get(groupPosition);
        return group.getChildrenCount();
    }

    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        KiiFile file = (KiiFile) getChild(groupPosition, childPosition);
        Drawable icon = getKiiFileMainIcon(file);
        KiiListItemView view;
        if (convertView == null) {
            view = new KiiListItemView(mActivity, file, kiiClient, icon,
                    mOnClickListener);
        } else {
            view = (KiiListItemView) convertView;
            view.refreshWithNewKiiFile(file, icon);
        }
        int status = kiiClient.getKiiFileStatus(file);
        String caption = UiUtils.getKiiFileCaption(file, status, mType);
        String subCaption = Formatter.formatFileSize(mActivity,
                file.getSizeOnDB());
        view.setCaption(caption, subCaption);
        return view;
    }

    private static Drawable getKiiFileMainIcon(KiiFile file) {
        Drawable icon = null;
        MimeInfo mime = MimeUtil.getInfoByKiiFile(file);
        String sThumbnail = null;
        if (mime != null) {
            sThumbnail = file.getThumbnail();
        }
        try {
            if (!TextUtils.isEmpty(sThumbnail)) {

                if (ICON_CACHE.containsKey(sThumbnail)) {
                    icon = ICON_CACHE.get(sThumbnail);
                } else {
                    File fThumbnail = new File(sThumbnail);
                    if (fThumbnail.exists() && fThumbnail.isFile()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(sThumbnail);

                        if (bitmap.getHeight() > 120) {
                            // resize the bitmap if too big, save memory
                            bitmap = Bitmap.createScaledBitmap(
                                    bitmap,
                                    (bitmap.getWidth() * 120)
                                            / bitmap.getHeight(), 120, false);
                        }
                        icon = new BitmapDrawable(bitmap);
                    }
                    ICON_CACHE.put(sThumbnail, icon);
                    return icon;
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Get the view for KiiFile
     * 
     * @param file
     * @param view
     * @return
     */
    View getKiiFileView(KiiFile file, View view) {
        if (file == null)
            return view;

        if (file.isDirectory()) {
            // disable the sync status
            ImageView statusIcon = (ImageView) view
                    .findViewById(R.id.list_sync_status_icon);
            UiUtils.setSyncStatus(statusIcon, 0);
            UiUtils.setOneLineText(new SpannableString(file.getBucketName()),
                    view);
            UiUtils.setIcon(R.drawable.icon_format_folder, view);
        } else {

            // if the file is not belong to TRASH
            // it will use the cache as much
            int status = kiiClient.getKiiFileStatus(file);
            ImageView statusIcon = (ImageView) view
                    .findViewById(R.id.list_sync_status_icon);
            UiUtils.setSyncStatus(statusIcon, status);
            String title;
            String caption;
            String subCaption;

            MimeInfo mime = MimeUtil.getInfoByKiiFile(file);

            title = file.getTitle();

            // if the file is uploading, show the progress
            caption = UiUtils.getKiiFileCaption(file, status, mType);

            // set the size
            subCaption = Formatter
                    .formatFileSize(mActivity, file.getSizeOnDB());
            UiUtils.setTwoLinesText(new SpannableString(title),
                    new SpannableString(caption), subCaption,
                    R.drawable.icon_format_text, view);

            String sThumbnail = null;
            Drawable icon = null;

            // only show the thumbnail for image
            if (mime != null) {
                sThumbnail = file.getThumbnail();
            }

            try {
                if (!TextUtils.isEmpty(sThumbnail)) {

                    if (ICON_CACHE.containsKey(sThumbnail)) {
                        icon = ICON_CACHE.get(sThumbnail);
                    } else {
                        File fThumbnail = new File(sThumbnail);
                        if (fThumbnail.exists() && fThumbnail.isFile()) {
                            Bitmap bitmap = BitmapFactory
                                    .decodeFile(sThumbnail);

                            if (bitmap.getHeight() > 120) {
                                // resize the bitmap if too big, save memory
                                bitmap = Bitmap.createScaledBitmap(
                                        bitmap,
                                        (bitmap.getWidth() * 120)
                                                / bitmap.getHeight(), 120,
                                        false);
                            }
                            icon = new BitmapDrawable(bitmap);
                        }
                        ICON_CACHE.put(sThumbnail, icon);
                    }
                }
            } catch (Exception ex) {
                ICON_CACHE.put(sThumbnail, icon);
            }

            if (icon != null) {
                UiUtils.setIcon(icon, view);
            } else {
                if (mime != null) {
                    UiUtils.setIcon(mime.getIconID(), view);
                } else {
                    UiUtils.setIcon(R.drawable.icon_format_unsupport, view);
                }
            }
            ImageView iv = (ImageView) view
                    .findViewById(R.id.list_complex_more_button);
            iv.setVisibility(View.VISIBLE);
            iv.setTag(view);
            if (!(mOnClickListener == null)) {
                iv.setOnClickListener(mOnClickListener);
            } else {
                iv.setVisibility(View.GONE);
            }
            iv.setFocusable(false);
            iv.setFocusableInTouchMode(false);
        }

        view.setTag(file);
        return view;
    }

    public Object getGroup(int groupPosition) {
        return itemsList.get(groupPosition);
    }

    public int getGroupCount() {
        return itemsList.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.list_complex,
                    parent, false);
        }

        // disable the view for more button
        view.findViewById(R.id.list_complex_more_button).setVisibility(
                View.GONE);

        KiiFileList group = (KiiFileList) getGroup(groupPosition);

        if (group.getParent() == null) {
            // disable the sync status
            String title = group.getTitle();
            ImageView statusIcon = (ImageView) view
                    .findViewById(R.id.list_sync_status_icon);
            UiUtils.setSyncStatus(statusIcon, 0);
            UiUtils.setOneLineText(new SpannableString(title), view);

            if (title.startsWith("Backup")) {
                UiUtils.setIcon(R.drawable.icon_kiisync, view);
            } else if (title.startsWith("Error")) {
                UiUtils.setIcon(R.drawable.icon_format_error, view);
            } else if (title.startsWith("Trash")) {
                UiUtils.setIcon(R.drawable.icon_format_trashcan, view);
            } else if (title.startsWith("Progress")) {
                UiUtils.setIcon(R.drawable.icon_format_progress, view);
            } else {
                UiUtils.setIcon(R.drawable.icon_format_folder, view);
            }
            view.setTag(null);
        } else {

            KiiFile file = group.getParent();
            if (file.isDirectory()) {
                // disable the sync status
                ImageView statusIcon = (ImageView) view
                        .findViewById(R.id.list_sync_status_icon);
                UiUtils.setSyncStatus(statusIcon, 0);
                UiUtils.setOneLineText(new SpannableString(group.getTitle()),
                        view);
                UiUtils.setIcon(R.drawable.icon_others, view);
                view.setTag(file);
            } else {
                getKiiFileView(group.getParent(), view);
            }
        }
        return view;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

}
