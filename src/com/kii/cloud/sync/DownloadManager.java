package com.kii.cloud.sync;

import java.io.BufferedOutputStream;
import java.io.Closeable;
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
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.kii.demo.sync.activities.ProgressListActivity;
import com.kii.demo.sync.utils.NotificationUtil;
import com.kii.sync.KiiFile;
import com.kii.sync.SyncMsg;

public class DownloadManager {

    final static String TAG = "DownloadManager";

    public long downloadTotalSize = 0;
    public long downloadCurrentSize = 0;
    public AtomicBoolean downloadPorgress = new AtomicBoolean(false);
    public String destPath = null;
    private ArrayList<Pair<KiiFile, String>> dnList = new ArrayList<Pair<KiiFile, String>>();

    public double getDownloadProgress() {
        if (destPath == null)
            return -1;

        double percentage;
        if (downloadTotalSize == 0)
            percentage = 0;
        else
            percentage = ((double) (downloadCurrentSize) / (double) downloadTotalSize) * 100;
        return percentage;

    }

    public synchronized int add(KiiFile file, String dest) {
        Log.d(TAG, "add: "+dest);
        if (file != null && file.hasServerRecord()) {
            for (Pair<KiiFile, String> dn : dnList) {
                if (dn.first.getId() == file.getId())
                    return SyncMsg.OK;
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
    public int download(KiiFile kiFile, String dest) {
        try {
            downloadTotalSize = kiFile.getSize();
            downloadCurrentSize = 0;
            if (TextUtils.isEmpty(dest)) {
                destPath = KiiSyncClient.getInstance().getDownloadFolder() + "/"
                        + kiFile.getTitle();
            } else {
                destPath = dest;
            }
            downloadKiiFile(new File(destPath), kiFile, false);
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
    public void downloadKiiFile(File destFile, KiiFile srcFile,
            boolean overwrite) throws IOException {

        boolean result = false;
        Context context = null;

        if (KiiSyncClient.getInstance() != null) {
            context = KiiSyncClient.getInstance().mContext;
        }

        if (context != null) {
            Intent intent = new Intent();
            intent.setAction(ACTION_DOWNLOAD_START);
            intent.putExtra(DOWNLOAD_DEST_PATH, destFile.getAbsolutePath());
            context.sendBroadcast(intent);

            Intent progressIntent = new Intent(context.getApplicationContext(),
                    ProgressListActivity.class);
            NotificationUtil.showDownloadProgressNotification(
                    context.getApplicationContext(), progressIntent,
                    destFile.getAbsolutePath());

        }

        String remotePath = srcFile.getRemotePath();
        if (remotePath == null) {
            Log.e(TAG, "remotePath is empty");
            throw new IllegalArgumentException("remotePath is empty");
        }

        Log.d(TAG, "URL:" + remotePath);

        if (destFile.exists()) {
            if (!overwrite) {
                throw new IOException("File already exist:"
                        + destFile.getAbsolutePath());
            }
        }

        File destFolder = destFile.getParentFile();
        if (destFolder == null) {
            throw new IOException("Cannot create folder for file: " + destFile);
        }
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }
        
        // create a temp file for download the file
        File tempDest = new File(destFile.getAbsoluteFile()+"."+Long.toString(System.currentTimeMillis()));

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
        InputStream input = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            input = entity.getContent();
            fos = new FileOutputStream(tempDest);
            bos = new BufferedOutputStream(fos);
            int len = -1;
            byte[] buffer = new byte[1024];
            while ((len = input.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
                downloadCurrentSize += len;
            }
            result = true;
            if (destFile.exists()) {
                destFile.delete();
            }
            tempDest.renameTo(destFile);
            tempDest.setLastModified(srcFile.lastModified());
        } finally {
            close(bos);
            close(fos);
            close(input);
            if (context != null) {
                Intent intent = new Intent();
                intent.setAction(ACTION_DOWNLOAD_END);
                intent.putExtra(DOWNLOAD_DEST_PATH, destFile.getAbsolutePath());
                intent.putExtra(DOWNLOAD_RESULT, result);
                context.sendBroadcast(intent);
                NotificationUtil.cancelDownloadProgressNotification(context);
                if (tempDest.exists()) {
                	tempDest.delete();
                }
            }
        }
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    public KiiFile[] getDownloadList() {
        List<KiiFile>files = new ArrayList<KiiFile>();
        for(Pair<KiiFile, String>p:dnList) {
            if(p.first!=null) {
                files.add(p.first);
            }
        }
        return files.toArray(new KiiFile[files.size()]);
    }

}
