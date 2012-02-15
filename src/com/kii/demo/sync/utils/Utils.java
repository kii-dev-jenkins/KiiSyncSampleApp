package com.kii.demo.sync.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.text.TextUtils;
import android.util.Log;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

import com.kii.sync.utils.FileUtils;
import com.kii.sync.utils.ImageUtils;

import com.kii.demo.sync.R;

public class Utils {
	
	static final String TAG =  "Utils";

    /**
     * Convert the status code to human reading language
     * 
     * @param kFile
     * @return String
     */
    static public String getStatus(KiiFile kFile) {
        if (kFile == null)
            return "NULL";
        if (kFile.isDirectory())
            return "folder";
        switch (KiiSyncClient.getInstance().getStatus(kFile)) {
            case KiiFile.STATUS_UNKNOWN:
                return "UNKNOWN";
            case KiiFile.STATUS_SYNCED:
                return "SYNCED";
            case KiiFile.STATUS_DELETE_REQUEST:
                return "DELETE_REQUEST";
            case KiiFile.STATUS_NO_BODY:
                return "Server Only";
            case KiiFile.STATUS_REQUEST_BODY:
                return "REQUEST_BODY";
            case KiiFile.STATUS_DOWNLOADING_BODY:
                return "DOWNLOADING_BODY";
            case KiiFile.STATUS_UPLOADING_BODY:
                return "UPLOADING_BODY";
            case KiiFile.STATUS_SYNC_IN_QUEUE:
                return "SYNC_IN_QUEUE";
            case KiiFile.STATUS_SERVER_DELETE_REQUEST:
                return "SERVER_DELETE_REQUEST";
            case KiiFile.STATUS_PREPARE_TO_SYNC:
                return "PREPARE_TO_SYNC";
            case KiiFile.STATUS_BODY_OUTDATED:
                return "OUT DATED";

            default:
                return "?(" + KiiSyncClient.getInstance().getStatus(kFile) + ")";
        }
    }

    /**
     * Convert the error code to error message
     * 
     * @param code
     * @param context
     * @return
     */
    static public String getErrorMsg(int code, Context context) {
        switch (code) {

            case SyncMsg.OK:
                return "Successful";
            case SyncMsg.ERROR_RECORD_NOT_FOUND:
                return context.getString(R.string.msg_ERROR_RECORD_NOT_FOUND);
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
    
    public static String moveFile(File dest, File src) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel srcChannel = null;
        FileChannel destChannel = null;
        try {
            fis = new FileInputStream(src);
            srcChannel = fis.getChannel();
            File destFolder = dest.getParentFile();
            if (destFolder != null) {
                destFolder.mkdirs();
            }
            fos = new FileOutputStream(dest);
            destChannel = fos.getChannel();

            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
            dest.setLastModified(src.lastModified());
            return dest.getAbsolutePath();
        } catch (IOException e) {
            Log.w("FileUtils", "copyFile", e);
            return null;
        } finally {
            closeSilently(srcChannel);
            closeSilently(destChannel);
            closeSilently(fis);
            closeSilently(fos);
        }
    }
    
    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable t) {

            }
        }
    }
    
    /**
     * Generate the thumbnail with the given unique key and store in the temp
     * folder
     * 
     * @param pathImage
     * @param uniqueKey
     * @return
     */
    public static String generateThumbnail(Context context, String pathImage, String destPath,
            String mimeType) {

        try {

            File dest = new File(destPath);
            if (!dest.getParentFile().exists()) {
                if (dest.getParentFile().mkdirs() == false) {
                    Log
                            .e(TAG, "Create folder failed:"
                                    + dest.getAbsolutePath());
                    return null;
                }
            }
            Bitmap b = null;

            if (mimeType.startsWith("image")) {
                b = ImageUtils.createImageThumbnail2(context, pathImage,
                        Images.Thumbnails.MINI_KIND);
            } else if (mimeType.startsWith("video")) {
                b = ThumbnailUtils.createVideoThumbnail(pathImage,
                        Video.Thumbnails.MICRO_KIND);
            }

            if (b != null) {
                OutputStream fos = null;
                try {
                    fos = new FileOutputStream(dest);
                    b.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    return dest.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        FileUtils.closeSilently(fos);
                    }
                }
            }
        } catch (Exception ex) {
            Log
                    .e(TAG, "generateThumbnailForImage Exception:"
                            + ex.getMessage());
            return null;
        }
        return null;
    }
    
    public static String getKiiFileDest(KiiFile file) {
        String title = file.getTitle();
        String dest = KiiSyncClient.getInstance().getDownloadFolder() + "/"
                + title;
        File f = new File(dest);
        if (f.exists()) {
            int sufpos = title.lastIndexOf(".");
            String time = String.valueOf(System.currentTimeMillis());
            if (sufpos < 0) {
                title = title + "-" + time;
            } else {
                title = title.substring(0, sufpos) + "-" + time
                        + title.substring(sufpos);
            }
            dest = KiiSyncClient.getInstance().getDownloadFolder() + "/" + title;
        }
        return dest;
    }

    public static void startBackupService(Context context, String command) {
        Intent service = new Intent(context, BackupService.class);
        if (!TextUtils.isEmpty(command)) {
            service.setAction(command);
        }
        context.startService(service);
    }
}
