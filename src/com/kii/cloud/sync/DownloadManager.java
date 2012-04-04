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

package com.kii.cloud.sync;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.util.Pair;

import com.kii.demo.sync.ui.ProgressListActivity;
import com.kii.demo.sync.utils.NotificationUtil;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class DownloadManager {

    final static String TAG = "DownloadManager";

    private long downloadTotalSize = 0;
    private long downloadCurrentSize = 0;
    public AtomicBoolean downloadPorgress = new AtomicBoolean(false);
    public String destPath = null;
    private ArrayList<Pair<KiiFile, String>> dnList = new ArrayList<Pair<KiiFile, String>>();
    private Context mContext;

    DownloadManager(Context context) {
        mContext = context;
    }

    public double getDownloadProgress() {
        if (destPath == null) {
            return 0;
        }
        double percentage;
        if (downloadTotalSize == 0) {
            percentage = 0;
        } else {
            percentage = ((double) (downloadCurrentSize) / (double) downloadTotalSize) * 100;
        }
        return percentage;

    }

    public synchronized int add(KiiFile file, String dest) {
        if ((file != null) && file.hasServerRecord()) {
            for (Pair<KiiFile, String> dn : dnList) {
                if (dn.first.getId() == file.getId()) {
                    return SyncMsg.OK;
                }
            }
            synchronized (this) {
                dnList.add(new Pair<KiiFile, String>(file, dest));
            }
            return SyncMsg.OK;
        }
        return SyncMsg.ERROR_FILE_NOT_FOUND;
    }

    public int resume() {
        if (!downloadPorgress.getAndSet(true)) {
            while (dnList.size() != 0) {
                Pair<KiiFile, String> dn = dnList.get(0);
                int ret = download(dn.first, dn.second);
                if (ret != SyncMsg.OK) {
                    downloadPorgress.set(false);
                    return ret;
                }
                delete(dn.first);
            }
            downloadPorgress.set(false);
            return SyncMsg.OK;
        }
        return SyncMsg.ERROR_PFS_BUSY;
    }

    public void delete(KiiFile file) {
        synchronized (this) {
            for (int ct = 0; ct < dnList.size(); ct++) {
                if (dnList.get(ct).first.getId() == file.getId()) {
                    dnList.remove(ct);
                }
            }
        }
    }

    /**
     * Download a given KiiFile to a specific folder
     */
    private int download(KiiFile kiFile, String dest) {
        try {
            downloadTotalSize = kiFile.getSizeOnDB();
            downloadCurrentSize = 0;
            destPath = dest;
            downloadKiiFile(new File(destPath), kiFile, true);
            destPath = null;
            return SyncMsg.OK;
        } catch (Exception ex) {
            Log.e(TAG, "Exception download" + ex.getMessage());
            return SyncMsg.ERROR_NO_CONNECTION;
        } finally {
        }
    }

    public static final String ACTION_DOWNLOAD_START = "com.kii.sync.download.start";
    public static final String ACTION_DOWNLOAD_END = "com.kii.sync.download.end";
    public static final String DOWNLOAD_DEST_PATH = "dest_path";
    public static final String DOWNLOAD_RESULT = "result";

    /**
     * Download KiiFile.
     * 
     * @param destFile
     *            - specifiy where the downloaded file is saved.
     * @param srcFile
     *            - KiiFile to be downloaded
     * @param overwrite
     *            - specifiy if overwrite the destination file.
     * @throws IOException
     */
    private void downloadKiiFile(File destFile, KiiFile srcFile,
            boolean overwrite) throws IOException {
        boolean result = true;
        InputStream input = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        File tempDest = null;
        try {
            // check for valid URL
            String remotePath = srcFile.getRemotePath();
            if (remotePath == null) {
                Log.e(TAG, "remotePath is empty");
                throw new IllegalArgumentException("HTTP download URL is empty");
            }
            Log.d(TAG, "downloadKiiFile, remotePath is " + remotePath);

            // check if the destinated file exist
            // if yes, check if overwrite permitted
            if (destFile.exists()) {
                if (!overwrite) {
                    throw new IllegalArgumentException("File already exist:"
                            + destFile.getAbsolutePath());
                }
            }

            // check if the destinated folder exist
            // if not, create the folder
            File destFolder = destFile.getParentFile();
            if (destFolder == null) {
                throw new IllegalArgumentException(
                        "Cannot create folder for file: " + destFile);
            }
            if (!destFolder.exists()) {
                destFolder.mkdirs();
            }

            // send notification that download in progress
            if (mContext != null) {
                Intent intent = new Intent();
                intent.setAction(ACTION_DOWNLOAD_START);
                intent.putExtra(DOWNLOAD_DEST_PATH, destFile.getAbsolutePath());
                mContext.sendBroadcast(intent);
                Intent progressIntent = new Intent(
                        mContext.getApplicationContext(),
                        ProgressListActivity.class);
                NotificationUtil.showDownloadProgressNotification(
                        mContext.getApplicationContext(), progressIntent,
                        destFile.getAbsolutePath());
            }

            // create a temp file for download the file
            tempDest = new File(destFile.getAbsoluteFile() + "."
                    + Long.toString(System.currentTimeMillis()));
            HttpGet httpGet = new HttpGet(remotePath);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != 200) {
                throw new IOException("Code: " + statusLine.getStatusCode()
                        + "; Reason:" + statusLine.getReasonPhrase());
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new IOException("Cannot read file content.");
            }

            input = entity.getContent();
            fos = new FileOutputStream(tempDest);
            bos = new BufferedOutputStream(fos);
            int len = -1;
            byte[] buffer = new byte[1024];
            // download the file by batch
            while ((len = input.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
                downloadCurrentSize += len;
            }

            // delete the existing if it exist
            if (destFile.exists()) {
                destFile.delete();
            }
            if (tempDest.exists()) {
                Log.d(TAG, "Download file: s=" + tempDest.length() + "; d="
                        + tempDest.lastModified());
                // rename the download file to it original file
                if (!tempDest.renameTo(destFile)) {
                    throw new IllegalArgumentException("Failed to rename:"
                            + tempDest.getAbsolutePath());
                }
                // TODO: the download is success, update the kiifile status;
                // after rename, create a new file handler
                tempDest = new File(destFile.getAbsolutePath());
                // check if the file exists
                if (tempDest.exists()) {
                    if (tempDest.setLastModified(srcFile.getUpdateTime()) == false) {
                        // on some Galaxy phones, it will fail, we simply ignore
                        // this error and print an error log
                        Log.e(TAG,
                                "Failed to restore:"
                                        + tempDest.getAbsolutePath());
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Failed to restore, file not exist after rename:"
                                    + tempDest.getAbsolutePath());
                }
            } else {
                throw new IllegalArgumentException(
                        "Failed to restore, file not exist after dnload:"
                                + tempDest.getAbsolutePath());
            }

        } catch (IllegalArgumentException ex) {
            if (mContext != null) {
                Intent intent = new Intent();
                intent.setAction(ACTION_DOWNLOAD_START);
                intent.putExtra(DOWNLOAD_DEST_PATH, destFile.getAbsolutePath());
                mContext.sendBroadcast(intent);

                Intent progressIntent = new Intent(
                        mContext.getApplicationContext(),
                        ProgressListActivity.class);
                NotificationUtil.showDownloadProgressNotification(
                        mContext.getApplicationContext(), progressIntent,
                        ex.getMessage());
                result = false;
            }
            throw new IOException("IllegalArgumentException:" + ex.getMessage());

        } finally {
            Utils.closeSilently(bos);
            Utils.closeSilently(fos);
            Utils.closeSilently(input);
            if (mContext != null) {
                Intent intent = new Intent();
                intent.setAction(ACTION_DOWNLOAD_END);
                intent.putExtra(DOWNLOAD_DEST_PATH, destFile.getAbsolutePath());
                intent.putExtra(DOWNLOAD_RESULT, result);
                mContext.sendBroadcast(intent);
                // cancel the notification if no error
                if (result) {
                    NotificationUtil
                            .cancelDownloadProgressNotification(mContext);
                } else {
                    // delete the temp file if error exist
                    if ((tempDest != null) && tempDest.exists()) {
                        tempDest.delete();
                    }
                }
                KiiSyncClient.getInstance(mContext).notifyKiiFileLocalChange();
                // force the system to run a media scan
                MediaScannerConnection
                        .scanFile(mContext,
                                new String[] { destFile.getAbsolutePath() },
                                null, null);
            }
        }
    }

    public KiiFile[] getDownloadList() {
        List<KiiFile> files = new ArrayList<KiiFile>();
        for (Pair<KiiFile, String> p : dnList) {
            if (p.first != null) {
                files.add(p.first);
            }
        }
        return files.toArray(new KiiFile[files.size()]);
    }

}
