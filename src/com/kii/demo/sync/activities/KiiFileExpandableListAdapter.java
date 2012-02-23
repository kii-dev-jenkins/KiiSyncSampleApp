package com.kii.demo.sync.activities;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kii.cloud.sync.DownloadManager;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.R;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
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

                KiiFile[] root = kiiClient.getBackupFolders();
                if (root != null && root.length > 0) {
                    for (int ct = 0; ct < root.length; ct++) {
                        itemsList.add(new KiiFileList(root[ct]));
                    }
                }
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

        View view = convertView;

        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.list_complex,
                    parent, false);
        }
        KiiFile file = (KiiFile) getChild(groupPosition, childPosition);
        return getKiiFileView(file, view);
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
            setSyncStatus(view, 0);
            setOneLineText(new SpannableString(file.getBucketName()), view);
            setIcon(R.drawable.icon_format_folder, view);
        } else {
            String category = file.getCategory();
            int status;

            // if the file is not belong to TRASH
            // it will use the cache as much
            if (!TextUtils.isEmpty(category)
                    && KiiSyncClient.CATEGORY_TRASH.equalsIgnoreCase(category)) {
                status = kiiClient.getStatus(file);
            } else {
                status = kiiClient.getStatusFromCache(file);
            }
            setSyncStatus(view, status);
            String title;
            String caption;
            String subCaption;

            MimeInfo mime = MimeUtil.getInfoByKiiFile(file);

            title = file.getTitle();

            // if the file is uploading, show the progress
            if (mType == TYPE_PROGRESS
                    && (status == KiiFile.STATUS_SYNC_IN_QUEUE
                            || status == KiiFile.STATUS_UPLOADING_BODY || status == KiiFile.STATUS_PREPARE_TO_SYNC)) {
                int progress = file.getUploadProgress();
                if (progress < 0)
                    progress = 0;
                caption = Integer.toString(progress) + " %";
            } else {
                // set the creation date
                caption = (String) DateUtils.formatSameDayTime(
                        file.getUpdateTime(), System.currentTimeMillis(),
                        DateFormat.SHORT, DateFormat.SHORT);
            }

            // caption = file.getMimeType();

            // set the size
            subCaption = getAsString(file.getSizeOnDB()) + "bytes";

            setTwoLinesText(new SpannableString(title), new SpannableString(
                    caption), subCaption, R.drawable.icon_format_text, view);

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
                setIcon(icon, view);
            } else {
                if (mime != null) {
                    setIcon(mime.getIconID(), view);
                } else {
                    setIcon(R.drawable.icon_format_unsupport, view);
                }
            }
            ImageView ib = (ImageView) view.findViewById(R.id.menu_button);
            ib.setTag(view);
            if (!(mOnClickListener == null)) {
                ib.setOnClickListener(mOnClickListener);
            } else {
                ib.setVisibility(View.GONE);
            }
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

        KiiFileList group = (KiiFileList) getGroup(groupPosition);

        if (group.getParent() == null) {
            // disable the sync status
            String title = group.getTitle();
            setSyncStatus(view, 0);
            setOneLineText(new SpannableString(title), view);

            if (title.startsWith("Backup")) {
                setIcon(R.drawable.icon_kiisync, view);
            } else if (title.startsWith("Error")) {
                setIcon(R.drawable.icon_format_error, view);
            } else if (title.startsWith("Trash")) {
                setIcon(R.drawable.icon_format_trashcan, view);
            } else if (title.startsWith("Progress")) {
                setIcon(R.drawable.icon_format_progress, view);
            } else {
                setIcon(R.drawable.icon_format_folder, view);
            }
            view.setTag(null);
        } else {

            KiiFile file = group.getParent();
            if (file.isDirectory()) {
                // disable the sync status
                setSyncStatus(view, 0);
                setOneLineText(new SpannableString(group.getTitle()), view);
                setIcon(R.drawable.icon_others, view);
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

    /**
     * Default is 2 lines text, change to single line text
     * 
     * @param text
     * @param curView
     * @return
     */
    public View setOneLineText(String text, boolean alighCenter, View curView) {
        TextView textView = (TextView) curView
                .findViewById(R.id.list_complex_1line_title);
        textView.setText(text);
        if (alighCenter) {
            textView.setGravity(Gravity.CENTER);
        }
        curView.findViewById(R.id.list_complex_1line_text).setVisibility(
                View.VISIBLE);
        curView.findViewById(R.id.list_complex_2lines_text).setVisibility(
                View.GONE);
        return curView;
    }

    public View setOneLineText(SpannableString title, View curView) {
        TextView textView = (TextView) curView
                .findViewById(R.id.list_complex_1line_title);
        textView.setText(title);
        curView.findViewById(R.id.list_complex_1line_text).setVisibility(
                View.VISIBLE);
        curView.findViewById(R.id.list_complex_2lines_text).setVisibility(
                View.GONE);
        return curView;
    }

    /**
     * Set 2 lines text, title and caption
     * 
     * @param title
     * @param caption
     * @param curView
     * @return
     */
    public View setTwoLinesText(SpannableString title, SpannableString caption,
            View curView) {

        TextView titleView = (TextView) curView
                .findViewById(R.id.list_complex_title);
        TextView captionView = (TextView) curView
                .findViewById(R.id.list_complex_caption);
        titleView.setText(title);

        if (caption != null)
            captionView.setText(caption);
        else
            captionView.setText("");

        captionView
                .setPadding(captionView.getPaddingLeft(),
                        captionView.getPaddingTop(), 50,
                        captionView.getPaddingBottom());

        curView.findViewById(R.id.list_complex_1line_text).setVisibility(
                View.GONE);
        curView.findViewById(R.id.list_complex_2lines_text).setVisibility(
                View.VISIBLE);
        curView.findViewById(R.id.list_complex_sub_caption).setVisibility(
                View.GONE);
        return curView;
    }

    public View setTwoLinesText(SpannableString title, SpannableString caption,
            String subCaption, int iconId, View curView) {
        TextView titleView = (TextView) curView
                .findViewById(R.id.list_complex_title);
        TextView captionView = (TextView) curView
                .findViewById(R.id.list_complex_caption);
        titleView.setText(title);
        if (caption != null)
            captionView.setText(caption);
        else
            captionView.setText("");

        // text on the bottom right
        TextView subCaptionView = (TextView) curView
                .findViewById(R.id.list_complex_sub_caption);

        if (subCaption != null) {
            subCaptionView.setText(subCaption);
        } else {
            subCaptionView.setText("");
        }

        curView.findViewById(R.id.list_complex_1line_text).setVisibility(
                View.GONE);
        curView.findViewById(R.id.list_complex_2lines_text).setVisibility(
                View.VISIBLE);
        curView.findViewById(R.id.list_complex_sub_caption).setVisibility(
                View.VISIBLE);
        return curView;
    }

    /**
     * Set the icon
     * 
     * @param iconID
     *            is -1, the imageView will be gone.
     * @param curView
     * @return curView
     */
    public View setIcon(int iconID, View curView) {
        ImageView imageView = (ImageView) curView
                .findViewById(R.id.list_complex_icon);
        if (iconID < 0) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setImageResource(iconID);
        }

        return curView;
    }

    /**
     * Set the icon
     * 
     * @param icon
     *            if null, ignore
     * @param curView
     * @return curView
     */
    public View setIcon(Drawable icon, View curView) {
        ImageView imageView = (ImageView) curView
                .findViewById(R.id.list_complex_icon);

        if (icon != null) {
            imageView.setImageDrawable(icon);
            curView.findViewById(R.id.list_complex_icon_main).setVisibility(
                    View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
            curView.findViewById(R.id.list_complex_icon_main).setVisibility(
                    View.GONE);
        }

        return curView;
    }

    private void setSyncStatus(View view, int status) {
        ImageView statusIcon = (ImageView) view
                .findViewById(R.id.list_sync_status_icon);
        statusIcon.setVisibility(View.GONE);

        switch (status) {
            case 0:
                // disable
                return;
            case KiiFile.STATUS_NO_BODY:
                // "Server Only";
                statusIcon.setImageResource(R.drawable.sync_cloud);
                statusIcon.setVisibility(View.VISIBLE);
                break;
            case KiiFile.STATUS_SYNCED:
            case KiiFile.STATUS_REQUEST_BODY:
            case KiiFile.STATUS_DOWNLOADING_BODY:
                statusIcon.setImageResource(R.drawable.sync_sync);
                statusIcon.setVisibility(View.VISIBLE);
                break;
            case KiiFile.STATUS_PREPARE_TO_SYNC:
            case KiiFile.STATUS_SYNC_IN_QUEUE:
            case KiiFile.STATUS_UPLOADING_BODY:
                statusIcon.setImageResource(R.drawable.syncing);
                statusIcon.setVisibility(View.VISIBLE);
                break;
            case KiiFile.STATUS_BODY_OUTDATED:
                statusIcon.setImageResource(R.drawable.sync_outdated);
                statusIcon.setVisibility(View.VISIBLE);
                break;
            case KiiFile.STATUS_DELETE_REQUEST:
            case KiiFile.STATUS_DELETE_REQUEST_INCLUDEBODY:
            case KiiFile.STATUS_SERVER_DELETE_REQUEST:
                statusIcon.setImageResource(R.drawable.sync_trashcan);
                statusIcon.setVisibility(View.VISIBLE);
                break;
            case KiiFile.STATUS_UNKNOWN:
            default:
                statusIcon.setImageResource(R.drawable.syncing_error);
                statusIcon.setVisibility(View.VISIBLE);
                break;
        }
    }

    private static final String[] Q = new String[] { "", "K", "M", "G", "T",
            "P", "E" };

    public String getAsString(long bytes) {
        for (int i = 6; i > 0; i--) {
            double step = Math.pow(1024, i);
            if (bytes > step)
                return String.format("%3.1f %s", bytes / step, Q[i]);
        }
        return Long.toString(bytes);
    }

}
