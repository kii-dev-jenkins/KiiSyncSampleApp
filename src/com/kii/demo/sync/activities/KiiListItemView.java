package com.kii.demo.sync.activities;

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
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = mInflater.inflate(R.layout.list_complex, this, false);
        this.client = client;
        this.mainIcon = mainIcon;
        mOnClickListener = listener;
        type = TYPE_FILE;
        getDataFromFile(file);
        bindView();
        v.setTag(file);
        addView(v, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
    }

    public KiiListItemView(Context context, KiiFile kfile,
            KiiSyncClient client, Drawable mainIcon,
            View.OnClickListener listener) {
        super(context);
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = mInflater.inflate(R.layout.list_complex, this, false);
        this.client = client;
        this.mainIcon = mainIcon;
        mOnClickListener = listener;
        type = TYPE_KII_FILE;
        getDataFromKiiFile(kfile);
        bindView();
        v.setTag(kfile);
        addView(v, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
    }
    
//    public KiiListItemView(Context context, String title) {
//        super(context);
//        mContext = context;
//        mInflater = (LayoutInflater) mContext
//                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        v = mInflater.inflate(R.layout.list_complex, this, false);
//        refreshViewWithData(title);
//        v.setTag(null);
//        addView(v, new LayoutParams(LayoutParams.FILL_PARENT,
//                LayoutParams.WRAP_CONTENT));
//    }
//
//    public void refreshViewWithData(String title) {
//        ImageView statusIcon = (ImageView) v
//                .findViewById(R.id.list_sync_status_icon);
//        UiUtils.setSyncStatus(statusIcon, 0);
//        int drawableId = R.drawable.icon_format_folder;
//        if (!TextUtils.isEmpty(title)) {
//            if (title.startsWith("Backup")) {
//                drawableId = R.drawable.icon_kiisync;
//            } else if (title.startsWith("Error")) {
//                drawableId = R.drawable.icon_format_error;
//            } else if (title.startsWith("Trash")) {
//                drawableId = R.drawable.icon_format_trashcan;
//            } else if (title.startsWith("Progress")) {
//                drawableId = R.drawable.icon_format_progress;
//            }
//            UiUtils.setOneLineText(new SpannableString(title), v);
//        }
//        this.mainIcon = mContext.getResources().getDrawable(drawableId);
//        UiUtils.setIcon(mainIcon, v);
//        type = TYPE_GROUP;
//    }

    public void refreshWithNewFile(File file, Drawable mainIcon) {
        this.mainIcon = mainIcon;
        type = TYPE_FILE;
        getDataFromFile(file);
        bindView();
        v.setTag(file);
    }

    public void refreshWithNewKiiFile(KiiFile file, Drawable mainIcon) {
        if(file==null)
            return;
        this.mainIcon = mainIcon;
        type = TYPE_FILE;
        getDataFromKiiFile(file);
        bindView();
        v.setTag(file);
    }

    private void getDataFromFile(File file) {
        filename = file.getName();
        displaytime = file.lastModified();
        filesize = file.length();
        isDirectory = file.isDirectory();
        syncstatus = client.getStatusFromCache(file.getAbsolutePath());
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
        // if the file is not belong to TRASH
        // it will use the cache as much
        if (!TextUtils.isEmpty(category)
                && KiiSyncClient.CATEGORY_TRASH.equalsIgnoreCase(category)) {
            syncstatus = client.getStatus(kfile);
        } else {
            syncstatus = client.getStatusFromCache(kfile);
        }
        mime = MimeUtil.getInfoByKiiFile(kfile);
    }
    
    private void bindView() {
        ImageView ib = (ImageView) v
                .findViewById(R.id.list_complex_more_button);
        if (type == TYPE_FILE) {
            ib.setVisibility(View.VISIBLE);
            View padding_view = v.findViewById(R.id.padding_view);
            padding_view.setVisibility(View.GONE);
        }
        ib.setTag(v);
        if (!(mOnClickListener == null)) {
            ib.setOnClickListener(mOnClickListener);
        } else {
            ib.setVisibility(View.GONE);
        }
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
            String caption = (String) DateUtils.formatSameDayTime(displaytime,
                    System.currentTimeMillis(), DateFormat.SHORT,
                    DateFormat.SHORT);
            String subCaption = Formatter.formatFileSize(mContext, filesize);
            UiUtils.setTwoLinesText(new SpannableString(filename),
                    new SpannableString(caption), subCaption,
                    R.drawable.icon_format_text, v);
            ib.setVisibility(View.VISIBLE);
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

    }
}
