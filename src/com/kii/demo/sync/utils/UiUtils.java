package com.kii.demo.sync.utils;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.demo.sync.R;
import com.kii.demo.sync.ui.view.KiiFileExpandableListAdapter;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;
import com.kii.sync.SyncPref;

public class UiUtils {

    public static void setSyncStatus(ImageView statusIcon, int status) {
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

    public static Intent getLaunchFileIntent(String path, MimeInfo mime) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        if (mime == null) {
            return null;
        }
        Intent commIntent = null;
        Uri fileUri = Uri.fromFile(new File(path));
        commIntent = new Intent(Intent.ACTION_VIEW);
        commIntent.setDataAndType(fileUri, mime.getMimeType());
        return commIntent;
    }

    public static Intent getLaunchURLIntent(URL url, String mimeType) {
        Intent commIntent = null;
        commIntent = new Intent(Intent.ACTION_VIEW);
        if (mimeType.startsWith("video")) {
            commIntent.setDataAndType(Uri.parse(url.toString()), "video/*");
        } else if (mimeType.startsWith("audio")) {
            commIntent.setDataAndType(Uri.parse(url.toString()), mimeType);
        } else {
            commIntent.setData(Uri.parse(url.toString()));
        }
        return commIntent;
    }

    public static String getLastSyncTime(Context context) {
        long backupTime = 0;

        try {
            //sometimes it won't be inited before call this API
            //need to give more SyncPref some lazy init method;
            SyncPref.init(context);
            backupTime = SyncPref.getLastSuccessfulSyncTime();
        } catch (Exception ex) {
        }

        if (backupTime > 0) {
            return String.format("Last successful sync is %s",
                    (String) DateUtils.getRelativeTimeSpanString(backupTime,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                                    | DateUtils.FORMAT_ABBREV_ALL));
        } else {
            return context.getString(R.string.none);
        }
    }

    public static View setIcon(int iconID, View curView) {
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
     * Default is 2 lines text, change to single line text
     * 
     * @param text
     * @param curView
     * @return
     */
    public static View setOneLineText(String text, boolean alighCenter,
            View curView) {
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

    public static View setOneLineText(SpannableString title, View curView) {
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
    public static View setTwoLinesText(SpannableString title,
            SpannableString caption, View curView) {

        TextView titleView = (TextView) curView
                .findViewById(R.id.list_complex_title);
        TextView captionView = (TextView) curView
                .findViewById(R.id.list_complex_caption);
        titleView.setText(title);

        if (caption != null) {
            captionView.setText(caption);
        } else {
            captionView.setText("");
        }

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

    public static View setTwoLinesText(SpannableString title,
            SpannableString caption, String subCaption, int iconId, View curView) {
        TextView titleView = (TextView) curView
                .findViewById(R.id.list_complex_title);
        TextView captionView = (TextView) curView
                .findViewById(R.id.list_complex_caption);
        titleView.setText(title);
        if (caption != null) {
            captionView.setText(caption);
        } else {
            captionView.setText("");
        }

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
     * @param icon
     *            if null, ignore
     * @param curView
     * @return curView
     */
    public static View setIcon(Drawable icon, View curView) {
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

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static String getKiiFileCaption(KiiFile file, int status, int type) {
        String caption;
        if ((type == KiiFileExpandableListAdapter.TYPE_PROGRESS)
                && ((status == KiiFile.STATUS_SYNC_IN_QUEUE)
                        || (status == KiiFile.STATUS_UPLOADING_BODY) || (status == KiiFile.STATUS_PREPARE_TO_SYNC))) {
            int progress = file.getUploadProgress();
            if (progress < 0) {
                progress = 0;
            }
            caption = Integer.toString(progress) + " %";
        } else {
            // set the creation date
            caption = (String) DateUtils.formatSameDayTime(
                    file.getUpdateTime(), System.currentTimeMillis(),
                    DateFormat.SHORT, DateFormat.SHORT);
        }
        return caption;
    }

    /**
     * Convert the error code to error message
     * 
     * @param code
     * @param context
     * @return
     */
    public static String getErrorMsg(int code, Context context) {
        switch (code) {
    
            case SyncMsg.OK:
                return "Successful";
    
            case SyncMsg.PFS_SYNCRESULT_FORCE_STOP:
            case SyncMsg.PFS_SYNCRESULT_REQUEST_FORCE_STOP:
                return context.getString(R.string.msg_ERROR_FORCE_STOP);
    
            case SyncMsg.PFS_SYNCRESULT_RUNNING:
            case SyncMsg.PFS_SYNCRESULT_BUSY:
                return context.getString(R.string.msg_ERROR_BUSY);
    
            case SyncMsg.ERROR_ALREADY_KII_USER:
                return context.getString(R.string.msg_ERROR_ALREADY_KII_USER);
    
            case SyncMsg.ERROR_GET_ACCOUNTS:
                return context.getString(R.string.msg_ERROR_GET_ACCOUNTS);
    
            case SyncMsg.ERROR_AUTHENTICAION_ERROR:
                return context
                        .getString(R.string.msg_ERROR_AUTHENTICAION_ERROR);
            case SyncMsg.ERROR_NO_ACCOUNT:
                return context.getString(R.string.msg_ERROR_NO_ACCOUNT);
            case SyncMsg.ERROR_DIFFERENT_USERNAME:
                return context.getString(R.string.msg_ERROR_DIFFERENT_USERNAME);
            case SyncMsg.ERROR_NON_VERIFIED_USERNAME:
                return context
                        .getString(R.string.msg_ERROR_NON_VERIFIED_USERNAME);
            case SyncMsg.ERROR_INVALID_INPUT:
                return context.getString(R.string.msg_ERROR_INVALID_INPUT);
            case SyncMsg.ERROR_NO_CONNECTION:
                return context.getString(R.string.msg_ERROR_NO_CONNECTION);
            case SyncMsg.ERROR_NO_HOST:
                return context.getString(R.string.msg_ERROR_NO_HOST);
            case SyncMsg.ERROR_TIMEOUT:
                return context.getString(R.string.msg_ERROR_TIMEOUT);
            case SyncMsg.ERROR_IO:
                return context.getString(R.string.msg_ERROR_IO);
            case SyncMsg.ERROR_PROTOCOL:
                return context.getString(R.string.msg_ERROR_PROTOCOL);
            case SyncMsg.ERROR_SERVER_HARD_ERROR:
                return context.getString(R.string.msg_ERROR_SERVER_HARD_ERROR);
            case SyncMsg.ERROR_SERVER_TEMP_ERROR:
                return context.getString(R.string.msg_ERROR_SERVER_TEMP_ERROR);
            case SyncMsg.ERROR_GET_SITE_ERROR:
                return context.getString(R.string.msg_ERROR_GET_SITE_ERROR);
            case SyncMsg.ERROR_JSON:
                return context.getString(R.string.msg_ERROR_JSON);
            case SyncMsg.ERROR_FILE_NULL:
                return context.getString(R.string.msg_ERROR_UPLOAD_FILES);
            default:
                return String.format(
                        context.getString(R.string.msg_ERROR_OTHERS), code);
        }
    }

}
