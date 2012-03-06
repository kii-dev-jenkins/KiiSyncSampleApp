package com.kii.demo.sync.ui.view;

import java.io.File;
import java.text.DateFormat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.R;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
import com.kii.demo.sync.utils.UiUtils;
import com.kii.sync.KiiFile;

public class KiiListItemView extends LinearLayout {
    private Context mContext;
    private LayoutInflater mInflater;
    private String filename = null;
    private long displaytime = -1;
    private long filesize = -1;
    private boolean isDirectory = false;
    private int syncstatus = -1;
    private KiiSyncClient client;
    private View v;
    private Drawable mainIcon;
    private MimeInfo mime;
    private View.OnClickListener mOnClickListener = null;
    private int type = -1;
    private static final int TYPE_FILE = 1;
    private static final int TYPE_KII_FILE = 2;
    private static final int TYPE_GROUP = 3;

    public KiiListItemView(Context context, File file, KiiSyncClient client,
            Drawable mainIcon, View.OnClickListener listener) {
        super(context);
        init(context, client, mainIcon, listener);
        getFileView(file);
        addView(v, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public KiiListItemView(Context context, KiiFile file, KiiSyncClient client,
            Drawable mainIcon, View.OnClickListener listener) {
        super(context);
        init(context, client, mainIcon, listener);
        getKiiFileView(file);
        addView(v, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void init(Context context, KiiSyncClient client, Drawable mainIcon,
            View.OnClickListener listener) {
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = mInflater.inflate(R.layout.list_complex, this, false);
        this.client = client;
        this.mainIcon = mainIcon;
        mOnClickListener = listener;
    }

    public KiiListItemView(Context context, KiiFileList group) {
        super(context);
        init(context, null, null, null);
        getGroupView(group);
        addView(v, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public static final String TAG = "KiiListItemView";

    public void refreshWithNewFile(File file, Drawable mainIcon) {
        this.mainIcon = mainIcon;
        getFileView(file);
    }

    public void refreshWithNewKiiFile(KiiFile file, Drawable mainIcon) {
        this.mainIcon = mainIcon;
        getKiiFileView(file);
    }

    public void refreshWithNewGroup(KiiFileList group) {
        getGroupView(group);
    }

    private void getFileView(File file) {
        type = TYPE_FILE;
        getDataFromFile(file);
        bindView();
        v.setTag(file);
    }

    private void getGroupView(KiiFileList group) {
        type = TYPE_GROUP;
        getDataFromGroup(group);
        bindView();
        v.setTag(null);
    }

    private void getKiiFileView(KiiFile file) {
        type = TYPE_KII_FILE;
        getDataFromKiiFile(file);
        bindView();
        v.setTag(file);
    }

    private void getDataFromFile(File file) {
        filename = file.getName();
        displaytime = file.lastModified();
        filesize = file.length();
        isDirectory = file.isDirectory();
        syncstatus = client.getStatus(file.getAbsolutePath());
        mime = MimeUtil.getInfoByFileName(filename);
    }

    private void getDataFromKiiFile(KiiFile kfile) {
        if (kfile.isDirectory()) {
            filename = kfile.getBucketName();
        } else {
            filename = kfile.getTitle();
        }
        displaytime = kfile.getUpdateTime();
        filesize = kfile.getSizeOnDB();
        isDirectory = kfile.isDirectory();
        String category = kfile.getCategory();
        // if the file is not in TRASH
        // it will use the cache as much
        if (!TextUtils.isEmpty(category)
                && KiiSyncClient.CATEGORY_TRASH.equalsIgnoreCase(category)) {
            syncstatus = client.getStatus(kfile);
        } else {
            syncstatus = client.getStatus(kfile);
        }
        mime = MimeUtil.getInfoByKiiFile(kfile);
    }

    private void getDataFromGroup(KiiFileList group) {
        filename = group.getTitle();
        if (filename.startsWith("Backup")) {
            mainIcon = mContext.getResources().getDrawable(
                    R.drawable.icon_kiisync);
        } else if (filename.startsWith("Error")) {
            mainIcon = mContext.getResources().getDrawable(
                    R.drawable.icon_format_error);
        } else if (filename.startsWith("Trash")) {
            mainIcon = mContext.getResources().getDrawable(
                    R.drawable.icon_format_trashcan);
        } else if (filename.startsWith("Progress")) {
            mainIcon = mContext.getResources().getDrawable(
                    R.drawable.icon_format_progress);
        } else {
            mainIcon = mContext.getResources().getDrawable(
                    R.drawable.icon_format_folder);
        }
    }

    private void bindView() {
        ImageView iv = (ImageView) v
                .findViewById(R.id.list_complex_more_button);
        if (type == TYPE_FILE) {
            iv.setVisibility(View.VISIBLE);
            View padding_view = v.findViewById(R.id.padding_view);
            padding_view.setVisibility(View.GONE);
        }
        iv.setTag(v);
        if (mOnClickListener != null) {
            iv.setOnClickListener(mOnClickListener);
        } else {
            iv.setVisibility(View.GONE);
        }
        iv.setFocusable(false);
        iv.setFocusableInTouchMode(false);
        if (type != TYPE_GROUP) {
            if (isDirectory) {
                ImageView statusIcon = (ImageView) v
                        .findViewById(R.id.list_sync_status_icon);
                UiUtils.setSyncStatus(statusIcon, 0);
                UiUtils.setIcon(R.drawable.icon_format_folder, v);
                UiUtils.setOneLineText(new SpannableString(filename), v);
            } else {
                ImageView statusIcon = (ImageView) v
                        .findViewById(R.id.list_sync_status_icon);
                UiUtils.setSyncStatus(statusIcon, syncstatus);
                String caption = (String) DateUtils.formatSameDayTime(
                        displaytime, System.currentTimeMillis(),
                        DateFormat.SHORT, DateFormat.SHORT);
                String subCaption = Formatter
                        .formatFileSize(mContext, filesize);
                UiUtils.setTwoLinesText(new SpannableString(filename),
                        new SpannableString(caption), subCaption,
                        R.drawable.icon_format_text, v);
                iv.setVisibility(View.VISIBLE);
                if (mainIcon != null) {
                    UiUtils.setIcon(mainIcon, v);
                } else {
                    if (mime != null) {
                        UiUtils.setIcon(mime.getIconID(), v);
                    } else {
                        UiUtils.setIcon(R.drawable.icon_format_unsupport, v);
                    }
                }
            }
        } else {
            ImageView statusIcon = (ImageView) v
                    .findViewById(R.id.list_sync_status_icon);
            UiUtils.setSyncStatus(statusIcon, 0);
            UiUtils.setOneLineText(new SpannableString(filename), v);
            UiUtils.setIcon(mainIcon, v);
        }
    }

    public void setCaption(String caption, String subCaption) {
        UiUtils.setTwoLinesText(new SpannableString(filename),
                new SpannableString(caption), subCaption,
                R.drawable.icon_format_text, v);
    }
}
