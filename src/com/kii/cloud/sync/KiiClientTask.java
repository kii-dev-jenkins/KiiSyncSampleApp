package com.kii.cloud.sync;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.kii.demo.sync.activities.KiiFilePickerActivity.NewEventListener;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiFile;
import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;

public class KiiClientTask extends AsyncTask<Object, Void, Integer> {

    // quick refresh (sync header only)
    public static final int SYNC_REFRESH = 1;
    // backup a file
    public static final int SYNC_UPLOAD = 2;
    // resume upload
    public static final int SYNC_FULL_REFRESH = 3;
    public static final int SYNC_CONNECT = 4;
    // move a unsynced file to trash
    public static final int SYNC_MOVE_TRASH_FILE = 5;
    // move a sync file to trash
    public static final int SYNC_MOVE_TRASH_KIIFILE = 6;
    // move trash file to backup
    public static final int SYNC_RESTORE_TRASH_KIIFILE = 7;
    public static final int SYNC_DELETE_KIIFILE = 8;
    public static final int SYNC_DOWNLOAD = 9;
    public static final int SYNC_UPLOAD_FOLDER = 10;
    // update file change
    public static final int SYNC_UPDATE_KIIFILE_CHANGE = 12;

    // Cancel a file which is upload in progress
    public static final int SYNC_CANCEL_KIIFILE = 13;

    // Delete both the local copy and backup copy
    public static final int SYNC_DELETE_LOCAL = 14;

    // rename folder
    public static final int SYNC_RENAME_FOLDER = 15;

    Context context;
    String taskName;
    int taskId;
    KiiNewEventListener mListener;

    public KiiClientTask(Context context, String taskName, int task,
            KiiNewEventListener listener) {
        this.taskName = taskName;
        this.taskId = task;
        this.context = context;
        mListener = listener;
    }

    void showToast(String title, int errorCode) {
        showToast(title, Utils.getErrorMsg(errorCode, context));
    }

    void showToast(String title, CharSequence msg) {
        showToast(title + ":" + msg);
    }

    void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Integer doInBackground(Object... params) {

        if (taskId == SYNC_CONNECT) {
            KiiSyncClient syncClient;
            try {
                syncClient = KiiSyncClient.getInstance(context);
                if (syncClient != null) {
                    return SyncMsg.OK;
                } else
                    return SyncMsg.ERROR_SETUP;
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return SyncMsg.ERROR_SETUP;
            }
        }

        KiiSyncClient kiiClient = KiiSyncClient.getInstance();
        if (kiiClient == null) {
            return SyncMsg.ERROR_SERVER_TEMP_ERROR;
        }

        switch (taskId) {
            case SYNC_CANCEL_KIIFILE:
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof KiiFile) {
                    return kiiClient.cancel((KiiFile) params[0]);
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;
            case SYNC_DELETE_KIIFILE:
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof KiiFile) {
                    return kiiClient.delete((KiiFile) params[0], false);
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;
            case SYNC_DELETE_LOCAL:
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof KiiFile) {
                    return kiiClient.delete((KiiFile) params[0], true);
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;
            case SYNC_DOWNLOAD:
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }

                if (params[0] instanceof KiiFile) {
                    KiiFile file = (KiiFile)params[0];
                    if (params.length == 2 && params[1] instanceof String) {
                        return kiiClient.download(file,
                                (String) params[1]);
                    } else {
                        return kiiClient.download(file, Utils.getKiiFileDest(file));
                    }
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;

            case SYNC_MOVE_TRASH_FILE: {
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof String) {
                    String filePath = (String) params[0];
                    return kiiClient.moveToTrash(filePath, null);
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;
            }
            case SYNC_MOVE_TRASH_KIIFILE:
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof KiiFile) {
                    return kiiClient.moveKiiFileToTrash((KiiFile) params[0]);
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;
            case SYNC_RESTORE_TRASH_KIIFILE:
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof KiiFile) {
                    return kiiClient.restoreFromTrash((KiiFile) params[0]);
                }
                return SyncMsg.ERROR_FILE_NOT_FOUND;
            case SYNC_REFRESH:
                return kiiClient.refreshQuick();
            case SYNC_FULL_REFRESH:
                return kiiClient.refresh();
            case SYNC_UPLOAD_FOLDER: {
                if (params.length == 0) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof String) {
                    String filePath = (String) params[0];

                    File file = new File(filePath);
                    File[] files = file.listFiles();
                    ArrayList<String> upload = new ArrayList<String>();
                    for (int ct = 0; ct < files.length; ct++) {
                        if (files[ct].isFile()) {
                            upload.add(files[ct].getAbsolutePath());
                        }
                    }
                    if (upload.size() > 0) {
                        return kiiClient.upload((String[]) upload
                                .toArray(new String[upload.size()]));
                    }
                } else {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
            }
            case SYNC_UPLOAD: {
                if (params.length < 1) {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
                if (params[0] instanceof String) {
                    String filePath = (String) params[0];
                    return kiiClient.upload(filePath);
                } else {
                    return SyncMsg.ERROR_FILE_NOT_FOUND;
                }
            }
            case SYNC_UPDATE_KIIFILE_CHANGE:
                if (params[0] instanceof ArrayList<?>) {
                    ArrayList<KiiFile> scanChange = (ArrayList<KiiFile>) params[0];
                    return kiiClient.updateBody(scanChange);
                } else {
                    return SyncMsg.ERROR_INVALID_INPUT;
                }
            case SYNC_RENAME_FOLDER:
                if (params == null || params.length != 2) {
                    return SyncMsg.ERROR_INVALID_INPUT;
                } else if ((params[0] instanceof String)
                        && (params[1] instanceof String)) {
                    String oldPath = (String) params[0];
                    File oldFile = new File(oldPath);
                    if (!oldFile.exists() || !oldFile.isDirectory())
                        return SyncMsg.ERROR_FILE_NOT_FOUND;
                    File baseFile = new File(oldPath.substring(0,
                            oldPath.lastIndexOf('/')));

                    String newName = (String) params[1];
                    File newFile = new File(baseFile, newName);
                    if (oldFile.renameTo(newFile)) {
                        return kiiClient.renameFolder(
                                oldFile.getAbsolutePath(), newName);
                    } else {
                        return SyncMsg.ERROR_FILE_NOT_FOUND;
                    }
                } else {
                    return SyncMsg.ERROR_INVALID_INPUT;
                }
            default:
                return SyncMsg.ERROR_INVALID_INPUT;
        }
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Integer result) {
        switch (taskId) {
            case SYNC_CONNECT:
                if (mListener != null && mListener instanceof NewEventListener) {
                    ((NewEventListener) mListener).onConnectComplete();
                }
                break;
            case SYNC_RENAME_FOLDER:
                if (result == SyncMsg.OK) {
                    // do a quick refresh
                    KiiClientTask updateTask = new KiiClientTask(context,
                            "Refreshing", SYNC_REFRESH, null);
                    updateTask.execute();
                }
                break;
            default:
                break;
        }
        switch (result) {
    		case SyncMsg.ERROR_INTERRUPTED:
    		case SyncMsg.ERROR_PFS_BUSY:
            case SyncMsg.PFS_SYNCRESULT_FORCE_STOP:
            case SyncMsg.OK:
                break;
            case SyncMsg.ERROR_SETUP:
            default:
                showToast(taskName, result);
                break;
        }
    }
}
