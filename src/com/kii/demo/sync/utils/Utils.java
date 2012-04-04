/*************************************************************************
 
 Copyright 2012 Kii Corporation
 http://kii.com
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 
 *************************************************************************/

package com.kii.demo.sync.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.text.TextUtils;
import android.util.Log;

import com.kii.cloud.sync.BackupService;
import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.R;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;
import com.kii.sync.utils.FileUtils;
import com.kii.sync.utils.ImageUtils;

public class Utils {

    static final String TAG = "Utils";

    /**
     * Convert the status code to human reading language
     * 
     * @param kFile
     * @param context
     * @return String
     */
    static public String getStatus(KiiFile kFile, Context context) {
        if (kFile == null) {
            return "NULL";
        }
        if (kFile.isDirectory()) {
            return "folder";
        }
        switch (KiiSyncClient.getInstance(context).getStatus(kFile)) {
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
                return "?("
                        + KiiSyncClient.getInstance(context).getStatus(kFile)
                        + ")";
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
     * @param destPath
     * @param mimeType
     * @return
     */
    public static String generateThumbnail(Context context, String pathImage,
            String destPath, String mimeType) {
        try {
            File dest = new File(destPath);
            if (!dest.getParentFile().exists()) {
                if (dest.getParentFile().mkdirs() == false) {
                    Log.e(TAG, "Create folder failed:" + dest.getAbsolutePath());
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
            Log.e(TAG, "generateThumbnailForImage Exception:" + ex.getMessage());
            return null;
        }
        return null;
    }

    public static void startSync(Context context, String command) {
        Intent service = new Intent(context.getApplicationContext(),
                BackupService.class);
        if (!TextUtils.isEmpty(command)) {
            service.setAction(command);
        }
        context.startService(service);
    }

    public static Drawable getThumbnailDrawableByFilename(String filename,
            Context context) {
        Drawable ret = null;
        Cursor c = Images.Media.query(context.getContentResolver(),
                Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { BaseColumns._ID }, MediaColumns.DATA + "=?",
                new String[] { filename }, null);
        try {
            if ((c != null) && c.moveToFirst()) {
                long id = c.getLong(0);
                Bitmap b = Images.Thumbnails.getThumbnail(
                        context.getContentResolver(), id,
                        Images.Thumbnails.MICRO_KIND,
                        new BitmapFactory.Options());
                if (b != null) {
                    ret = new BitmapDrawable(b);
                }
            } else {
                // create the thumbnail by myself
                MimeInfo mime = MimeUtil.getInfoByFileName(filename);
                if (mime != null && mime.isType("image")) {
                    Bitmap b = ImageUtils.createImageThumbnail2(context,
                            filename, Images.Thumbnails.MICRO_KIND);
                    ret = new BitmapDrawable(b);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return ret;
    }

    public static boolean isKiiFileInTrash(KiiFile file) {
        if (file == null) {
            return false;
        }

        String category = file.getCategory();
        if (category == null) {
            return false;
        }

        if (category.contentEquals(KiiSyncClient.CATEGORY_TRASH)) {
            return true;
        }
        return false;
    }

    /**
     * check if the local file is the same as given KiiFile
     * 
     * @param file
     * @return true if the same otherwise false
     */
    public static boolean bodySameAsLocal(KiiFile file) {
        String localPath = file.getResourceUrl();
        if (TextUtils.isEmpty(localPath)) {
            return false;
        }
        File localFile = new File(localPath);
        if (!(localFile.exists() && localFile.isFile())) {
            return false;
        }
        long fileUpdated = localFile.lastModified();
        long lastUpdated = file.getUpdateTime();
        if (lastUpdated == -1) {
            return false;
        }
        if (fileUpdated != lastUpdated) {
            return true;
        }
        long size = localFile.length();
        if (size != file.getSizeOnDB()) {
            return true;
        }
        return false;
    }

    /**
     * Get the KiiFile path which can download the file
     * 
     * @param file
     * @return
     */
    public static String getKiiFilePath(KiiFile file) {
        if (file.isFile()) {
            String path = file.getLocalPath();
            if (path == null) {
                return file.getAvailableURL().toString();
            } else {
                return path;
            }
        } else {
            return file.getBucketName();
        }
    }

    public static String getKiiFileDownloadPath(KiiFile file) {
        String root = file.getVirtualRoot();
        String path = file.getVirtualPath();
        String dest = null;
        if (root.contentEquals(KiiFile.ANDROID_EXT)) {
            dest = Environment.getExternalStorageDirectory() + "/" + path;
        } else if (root.contentEquals(KiiFile.ANDROID_ROOT)) {
            dest = Environment.getRootDirectory() + "/" + path;
        } else {
            dest = Environment.getDownloadCacheDirectory() + "/" + path;
        }
        return dest;
    }

    public static Drawable getThumbnailByResize(String filename) {
        File fThumbnail = new File(filename);
        try {
            if (fThumbnail.exists() && fThumbnail.isFile()) {
                Bitmap bitmap = BitmapFactory.decodeFile(filename);
                if (bitmap.getHeight() > 120) {
                    // resize the bitmap if too big, save memory
                    bitmap = Bitmap.createScaledBitmap(bitmap,
                            (bitmap.getWidth() * 120) / bitmap.getHeight(),
                            120, false);
                }
                return new BitmapDrawable(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
