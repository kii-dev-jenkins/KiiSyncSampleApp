/*
 * Copyright (C) 2008-2011 Kii Software Inc.
 */
package com.kii.cloud.sync;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.kii.cloud.sync.auth.CloudStorage;
import com.kii.cloud.sync.auth.Identity;
import com.kii.demo.sync.utils.MimeInfo;
import com.kii.demo.sync.utils.MimeUtil;
import com.kii.demo.sync.utils.Utils;
import com.kii.sync.KiiClient;
import com.kii.sync.KiiFile;
import com.kii.sync.KiiFileUtil;
import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;
import com.kii.sync.SyncPref;
import com.kii.sync.provider.DatabaseHelper;
import com.kii.sync.utils.HttpClientUtils;

/**
 * Extension of KiiClient to provide customize service for Application specific
 * 
 * @author HueyMeng
 */
public class KiiSyncClient {

    private static String TAG = "KiiSyncClient";
    private static KiiSyncClient mInstance = null;
    
    private KiiClient mSyncManager = null;
    private Authentication mAuthManager = null;

    private static final String URL_SUBSCRIPTION = "/subscription/sync";
    private static final String TAG_SYNC_ENABLED = "syncEnabled";
    private static final String TAG_USED_BYTE_SIZE = "usedByteSize";
    private static final String TAG_QUOTA_BYTE_SIZE = "quotaByteSize";

    /**
     * Category of TRASH.
     */
    public static final String CATEGORY_TRASH = "trash";
    /**
     * Category of NONE.
     */
    public static final String CATEGORY_NONE = "none";

    public static final String CATEGORY_ASTRO = "file";
    
    public static final String CATEGORY_BACKUP = "backup";

    Context mContext;
    static KiiFileListener mFileStatusCache;
    
    /**
     * Change the password of the user.
     * 
     * @param oldPassword
     * @param newPassword
     * @return refer to {@link com.kii.sync.SyncMsg SyncMsg}
     */
    public int changePassword(String oldPassword, String newPassword) {
        return mAuthManager.changePassword(oldPassword, newPassword);
    }
    
    /**
     * Register user if no account has been registered
     * 
     * @param email
     *            : email address for login
     * @param password
     *            : password (min length and max length)
     * @param country
     *            :
     * @param nickName
     *            :
     * @param mobile
     *            :
     * @return
     * {@link SyncMsg#OK }      Registration successful.<br>
     * {@link SyncMsg#ERROR_INVALID_INPUT}      Input is invalid.<br>
     * {@link SyncMsg#ERROR_ALREADY_KII_USER}         User  is already registered.<br>
     * {@link SyncMsg#ERROR_PROTOCOL}   Protocol error.<br>
     * {@link SyncMsg#ERROR_IO}         IO error.<br>
     * {@link SyncMsg#ERROR_JSON}       Json format is invalid.<br>
     * {@link SyncMsg#ERROR_UNKNOWN_STATUSCODE}         Unknown HTTP status code.<br>
     *
     */
    public int register(String email, String password, String country,
            String nickName, String mobile) {
        return mAuthManager.register(email, password, country, nickName, mobile);
    }
    
    /**
     * Log in using the given username and password. 
     * If the given password is different from previous one, it will login again
     * else just return OK.
     * 
     * @param username
     * @param password
     * @return
     */
    public int login(String username, String password) {
        if (SyncPref.isLoggedIn()){
        	if(SyncPref.getPassword().compareTo(password)==0){
        		return SyncMsg.OK;
        	}
        }
        return mAuthManager.login(username, password);
    }

    /**
     * Clear the local sync database and user preference Non Blocking Call
     * 
     * @return
     */
    public int logout() {
    	mAuthManager.logout();
        return mSyncManager.wipeOut();
    }
    
    /**
     * Register event listener.
     * 
     * @param key
     *            id of the listener. Application use this api have to generate.
     * @param listener
     *            Event Listener instance.
     *            {@link com.kii.sync.KiiNewEventListener KiiNewEventListener}
     * @return TRUE: success, FALSE: key has already in use.
     * 
     * {@link KiiSyncClient#unregisterNewEventListener(Long)}
     */
    public boolean registerNewEventListener(Long key,
            KiiNewEventListener listener) {
        return mSyncManager.registerNewEventListener(key, listener);
    }

    /**
     * Unregister event listener.
     * 
     * @param key
     *            used for register
     * 
     * {@link KiiSyncClient#registerNewEventListener(Long, KiiNewEventListener)}
     *             
     */
    public void unregisterNewEventListener(Long key) {
    	mSyncManager.unregisterNewEventListener(key);
    }
    
    /**
     * suspend the existing sync session.
     * @return
     * {@link SyncMsg#OK}      Successful.<br>
     * {@link SyncMsg#ERROR_SETUP}      SyncManager is not instantiated.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_REMOTEEXCEPTION}          Remote method invocation failed.<br>
     * {@link SyncMsg#ERROR_PFS_NOTFOUND}     There is not sync session to stop.<br>
     * {@link SyncMsg#ERROR_PFS_NG}     Failed by unknown reason.<br>
     * {@link SyncMsg#ERROR_PFS_INVARGS}     Failed by unknown reason.<br>
     * 
     */
    public int suspend() {
        return mSyncManager.suspend();
    }

    /**
     * Sync the local records with the server records.
     * It also upload file if there are any files in pending stage.
     * 
     * Note: This sync operation takes times especially when there are file to be uploaded. 
     * If you just want to sync the file metadata and not uploading the file to the server,
     * refer to {@link KiiSyncClient#refreshQuick()}
     * 
     * @return
     * {@link SyncMsg#PFS_SYNCRESULT_SUCCESSFULLY}  Refresh successfully.<br>
     * {@link SyncMsg#ERROR_AUTHENTICAION_ERROR}  Authentication error.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_NOT_SETUP}    SessionManager is not instantiated.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_OTHERERROR}  Unknown error.<br>
     * {@link SyncMsg#ERROR_INTERRUPTED} Sync session is interrupted.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_REMOTEEXCEPTION}  Could not connect to remote method.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_MORE} sync OK but not complete yet (more item in server).<br>
     * {@link SyncMsg#PFS_SYNCRESULT_REQUEST_FORCE_STOP} Force stop is requested.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_FORCE_STOP} Sync is forcefully stopped.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_RUNNING} Another sync session is running.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_BUSY} sync cannot proceed because the sync system is busy.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_TIMEOUT} Sync is timeout.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_USER_EXPIRED} User has expired.<br>
     * {@link SyncMsg#ERROR_SETUP} SyncManager is not instantiated.<br>
     * 
     */
    public int refresh(){
    	return mSyncManager.refresh();
    }
    
    /**
     * Only sync the file meta data (to/from server).
     * It will not upload files that are pending state.
     * To upload files, refer to {@link KiiSyncClient#refresh()}
     * 
     * @return
     * {@link SyncMsg#PFS_SYNCRESULT_SUCCESSFULLY}  Refresh successfully.<br>
     * {@link SyncMsg#ERROR_AUTHENTICAION_ERROR}  Authentication error.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_NOT_SETUP}    SessionManager is not instantiated.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_OTHERERROR}  Unknown error.<br>
     * {@link SyncMsg#ERROR_INTERRUPTED} Sync session is interrupted.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_REMOTEEXCEPTION}  Could not connect to remote method.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_MORE} sync OK but not complete yet (more item in server).<br>
     * {@link SyncMsg#PFS_SYNCRESULT_REQUEST_FORCE_STOP} Force stop is requested.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_FORCE_STOP} Sync is forcefully stopped.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_RUNNING} Another sync session is running.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_BUSY} sync cannot proceed because the sync system is busy.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_TIMEOUT} Sync is timeout.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_USER_EXPIRED} User has expired.<br>
     * {@link SyncMsg#ERROR_SETUP} SyncManager is not instantiated.<br> 
     */
    public int refreshQuick() {
        return mSyncManager.refreshQuick();
    }
    
    /**
     * Returns the sync overall progress if the sync is not running, it will
     * return -1
     * 
     * @return 
     * <b>Positive integer between 1 to 100 </b>     Percentage of progress.<br>
     * {@link SyncMsg#ERROR_SETUP}      SyncManager is not instantiated.<br>
     * {@link SyncMsg#SYNC_NOT_RUNNING}         Currently no sync session is running.<br>
     * {@link SyncMsg#PFS_SYNCRESULT_REMOTEEXCEPTION}          Remote method invocation failed.
     */
    public int getProgress() {
        return mSyncManager.getProgress();
    }
    

    /**
     * Get the path to store the download files 
     * Default directory is Environment.DIRECTORY_DOWNLOADS if not set
     * 
     * @return absolute path of the download directory
     */
    public String getDownloadFolder() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    /**
     * Get the path to temporary store the trash file.
     * Once the trash files are backup to the cloud, it will be deleted.
     * Default directory is application cache directory
     * 
     * @return absolute path of the temp trash folder
     */
    public String getTrashTempFolder() {
        if (mContext != null) {
            String cacheDir = this.mContext.getCacheDir().getAbsolutePath() + "/trash/";
            File directory = new File(cacheDir);
            if( ! directory.exists() ){
            	directory.mkdir();
            }
            return cacheDir;	
        } else {
            return null;
        }
    }

    /**
     * Get the path to store the file thumbnail if there are any
     * Deffault directory is application cache directory
     * @return absolute path of the thumbanil folder
     */
    public String getTempThumbnailFolder() {
        if (mContext != null) {
            String cacheDir = this.mContext.getCacheDir().getAbsolutePath();
            return cacheDir + "/thumbnail/";
        } else {
            return null;
        }
    }

    /**
     * @param context
     * @throws InterruptedException
     * @throws InstantiationException
     */
    private KiiSyncClient(Context context) throws InterruptedException,
            InstantiationException {
    	
    	mSyncManager = KiiClient.getInstance(context);
    	mSyncManager.initSync();
        mContext = context;
        // configure the KiiClient don't generate the thumbnail
        SyncPref.setGenerateThumbnail(false);
        // disable the auto backup
        SyncPref.setSyncAuto(false);
        // SyncPref.setAutoUpdate(SyncPref.AUTO_UPDATE_OFF);
        // SyncPref.setIdentityUrl("http://new-dev-us.kii.com/app/identity/");
        // SyncPref.setIdentityUrl("http://test-ms2.kii.com/app/identity/");
        // SyncPref.setIdentityUrl("https://product-jp.kii.com/app/identity/");
        mAuthManager = new CloudStorage(context, mSyncManager);
        //mAuthManager = new Identity(mSyncManager, "http://new-dev-us.kii.com/app/identity/");
    }
    
    public static synchronized KiiSyncClient getInstance(Context context)
            throws InstantiationException {
        if (mInstance == null) {
            try {
                mInstance = new KiiSyncClient(context);
                mFileStatusCache = new KiiFileListener(context);
                // start the backup service
                context.startService(new Intent(context, BackupService.class));
            } catch (InterruptedException e) {
                throw new InstantiationException("Cause: " + e);
            }
        }
        return mInstance;
    }

    public void refreshKiiFileStatusCache() {
        mFileStatusCache.updateCache(false);
    }

    /**
     * Get the KiiRefClient instance.
     * 
     * @return null if it is not initialized
     */
    public static KiiSyncClient getInstance() {
        return mInstance;
    }

    /**
     * Get the number of records that are pending Non Blocking Call
     * 
     * @return
     */
    public int getUnsyncCount() {

        KiiFile[] files = getListInProgress();
        if (files == null)
            return 0;
        else
            return files.length;
    }

    /**
     * Get the number of records that are already synced and from the cloud Non
     * Blocking Call
     * 
     * @return
     */
    public int getSyncCount() {
        int syncedRecords = mSyncManager.getCountKiiFile(KiiFile.STATUS_SYNCED);
        int serverRecords = mSyncManager.getCountKiiFile(KiiFile.STATUS_NO_BODY);
        return syncedRecords + serverRecords;
    }

    /**
     * Get a list of files are backup Non Blocking Call
     * 
     * @return array of KiiFile
     */
    public KiiFile[] getBackupFiles() {
        return KiiFileUtil.listKiiFiles(mContext);
    }

    /**
     * Check if the file content has changed, if yes, it will upload the file
     * too
     * 
     * @param files
     *            list of KiiFiles to be uploaded
     * @return SyncMsg
     */
    public int updateBody(List<KiiFile> files) {
        for (KiiFile file : files) {
            String path = file.getResourceUrl();
            if (delete(file, false) == SyncMsg.OK) {
                upload(path, false);
            }
        }
        mSyncManager.getSyncObserver().notifyLocalChangeSynced(null);
        return mSyncManager.refresh();
    }

    /**
     * Delete the KiiFile and call quick refresh
     * 
     * @param KiiFile
     *            the file to be deleted
     * @param deleteLocal
     *            if true, the local file will be deleted
     * @return
     */
    public int delete(KiiFile file, boolean deleteLocal) {
        int ret;
        if (deleteLocal) {
            ret = mSyncManager.delete(file);
        } else {
            ret = mSyncManager.deleteRemainOrginalFile(file);
        }
        if (ret == SyncMsg.OK) {
        	mSyncManager.getSyncObserver().notifySyncDelete(null);
            return mSyncManager.refresh();
        }
        return ret;
    }

    /**
     * Delete given list of KiiFiles and call quick refresh to update header
     * 
     * @param files
     * @param deleteLocal
     *            if true, the local file will be deleted
     * @return
     */
    public int delete(KiiFile[] files, boolean deleteLocal) {
        List<Integer> retValues;
        if (deleteLocal) {
            retValues = mSyncManager.deleteFiles(files);
        } else {
            retValues = mSyncManager.deleteFilesRemainOriginalFile(files);
        }
        if (retValues == null || retValues.size() == 0)
            return SyncMsg.ERROR_FILE_NOT_FOUND;

        // Can iterate through the list of return values
        // for(int ct=0; ct<retValues.size(); ct++){
        //	
        // }

        mSyncManager.getSyncObserver().notifySyncDelete(null);
        return mSyncManager.refresh();
    }

    /**
     * Cancel a file to be uploaded.
     * To remove a uploaded file, refer to {@link KiiSyncClient#delete(KiiFile, boolean)}
     * 
     * @return
     */
    public int cancel(KiiFile file) {
        int status = getStatus(file);
        if (status == KiiFile.STATUS_PREPARE_TO_SYNC
                || status == KiiFile.STATUS_UPLOADING_BODY) {
            int ret = mSyncManager.deleteRemainOrginalFile(file);
            if (ret == SyncMsg.OK) {
            	mSyncManager.getSyncObserver().notifySyncDelete(null);
            	mSyncManager.refresh();
            }
            return ret;
        }
        return SyncMsg.ERROR_INVALID_INPUT;
    }

    /**
     * Get the total storage usage in bytes Non Blocking Call
     * 
     * @return num of bytes
     */
    public long getStorageUsage() {
    	return mSyncManager.getServerUsedDiskSpace();
    }



    /**
     * Get a list of trashed files. Non Blocking Call
     * 
     * @return
     */
    public KiiFile[] getTrashFiles() {
        return KiiFileUtil.listByCategory(mContext, CATEGORY_TRASH);
    }

    /**
     * Get all the files that are backup by Astro
     * 
     * @return
     */
    public KiiFile[] getAstroFiles() {
        return KiiFileUtil.listByCategory(mContext, CATEGORY_ASTRO);
    }

    /**
     * Get a list of folders which the files are backup Non Blocking Call
     * 
     * @return array of KiiFile with type as folder
     */
    public KiiFile[] getBackupFolders() {
        return KiiFileUtil.listAllFolder(mContext, false);
    }

    /**
     * Upload a single file
     * 
     * @param filePath
     * @return SyncMsg
     * @see SyncMsg
     */
    public int upload(String filePath) {
        int ret = upload(filePath, false);
        if (ret == SyncMsg.OK) {
        	mSyncManager.getSyncObserver().notifyLocalChangeSynced(null);
            return mSyncManager.refresh();
        }
        return ret;

    }

    /**
     * Upload the given list of file
     * 
     * @param files
     *            list of files to be upload by full path name
     * @return SyncMsg
     * @see SyncMsg
     */
    public int upload(String[] files) {
        for (String file : files) {
            int code = upload(file, false);
            if (code != SyncMsg.OK) {
                Log
                        .e(TAG, "Failed to upload(" + file + "), error code:"
                                + code);
            }
        }
        mSyncManager.getSyncObserver().notifyLocalChangeSynced(null);
        return mSyncManager.refresh();
    }

    /**
     * Upload a file
     * 
     * @param filePath
     * @param commit
     *            true it will call sync immediately after upload
     * @return SyncMsg
     * @see SyncMsg
     */
    private int upload(String filePath, boolean commit) {

        // if a similiar file having the same unique key, it will be return
        Log.d(TAG, "upload, filePath is "+filePath);
        KiiFile file = createKiiFileByPath(filePath);

        // check if the file has already been synced
        if (file.hasServerRecord()) {
            return SyncMsg.OK;
        }
        
        String thumbnail = null;
        String mimeType = null;
        String unique = Long.toString(System.currentTimeMillis());
        
        // application generates it own mimetype if any
        MimeInfo mime = MimeUtil.getInfoByFileName(filePath);
        if (mime != null) {
            mimeType = mime.getMimeType();
        }
        
    	// application generates it own thumbnail if any 
        if (!TextUtils.isEmpty(mimeType)) {
    		thumbnail = Utils.generateThumbnail(mContext, 
    				filePath, 
    						getTempThumbnailFolder()+unique+".jpg", mimeType);
    	}
        
        // check if application has provided the mimetype
        // it will override the default
    	if (!TextUtils.isEmpty(mimeType)) {
    		file.setMimeType(mimeType);
    	}
    	// check if application has provided thumbnail
    	// it will override the default 
    	if (!TextUtils.isEmpty(thumbnail)) {
    		file.setThumbnail(thumbnail);
    	}
    	
        return mSyncManager.upload(file, commit);
    }

    /**
     * Move file to trash
     * 
     * @param originalPath
     *            the full path of the trash file
     * @param mimeType
     *            optional
     * @return SyncMsg
     */
    public int moveToTrash(String originalPath, String mimeType) {

    	File src = new File(originalPath);
    	
    	if(!src.isFile()){
    		return SyncMsg.ERROR_FILE_NOT_FOUND;
    	}
    	
        KiiFile kiFile = createKiiFileByPath(originalPath);

        // if the file already exist as KiiFile, just update the category
        if (kiFile.hasLocalRecord()) {
            kiFile.setCategory(CATEGORY_TRASH);
            return moveKiiFileToTrash(kiFile);
        }
        
        String thumbnail = null;
        String unique = Long.toString(System.currentTimeMillis());
        
        // application generates it own mimetype if any
        if(TextUtils.isEmpty(mimeType)){
	        MimeInfo mime = MimeUtil.getInfoByFileName(originalPath);
	        if (mime != null) {
	            mimeType = mime.getMimeType();
            }
        }
        
    	// application generates it own thumbnail if any 
        if (!TextUtils.isEmpty(mimeType)) {
    		thumbnail = Utils.generateThumbnail(mContext, 
    						originalPath, 
    						getTempThumbnailFolder()+unique+".jpg", mimeType);
    	}
        
    	// application specific data
        kiFile.setAppData("application specific data");
        kiFile.setCategory(CATEGORY_TRASH);        	
        // check if application has provided the mimetype
        // it will override the default
    	if (!TextUtils.isEmpty(mimeType)) {
    		kiFile.setMimeType(mimeType);
    	}
    	// check if application has provided thumbnail
    	// it will override the default 
    	if (!TextUtils.isEmpty(thumbnail)) {
    		kiFile.setThumbnail(thumbnail);
    	}
    	
//    	File dest = new File(getTrashTempFolder()+unique+"."+MimeUtil.getSuffixOfFile(originalPath));
//    	
//    	try {
//			dest.createNewFile();
//		} catch (IOException e) {
//			return SyncMsg.ERROR_IO;
//		}
		
    	int res = kiFile.copyToTmp(getTrashTempFolder()+unique+"."+MimeUtil.getSuffixOfFile(originalPath)); 
    	
    	if( res == SyncMsg.OK ){
    		// delete the original file
        	if( src.delete() ){
        		return upload(kiFile);
        	}else{
        		return SyncMsg.ERROR_ACCESS_FILE;
        	}
        	
    	}
        
    	return res;
//        
//        File dest = new File(getTrashTempFolder(), unique+"."+MimeUtil.getSuffixOfFile(originalPath));
//        File src = new File(originalPath);
//        
//        if (Utils.moveFile(dest, src) != null) {
//        	KiiFile kiTrashFile = createKiiFileByPath(dest.getAbsolutePath());
//        	kiTrashFile.setResourceUrl(originalPath);
//        	kiTrashFile.setTitle(new File(originalPath).getName());
//        	// appliction specific data
//        	kiTrashFile.setAppData("application specific data");
//        	kiTrashFile.setCategory(CATEGORY_TRASH);        	
//            // check if application has provided the mimetype
//            // it will override the default
//        	if (!TextUtils.isEmpty(mimeType)) {
//        		kiTrashFile.setMimeType(mimeType);
//        	}
//        	// check if application has provided thumbnail
//        	// it will override the default 
//        	if (!TextUtils.isEmpty(thumbnail)) {
//        		kiTrashFile.setThumbnail(thumbnail);
//        	}
//        	
//        	// delete the original file
//        	src.delete();
//        	
//        	return upload(kiTrashFile);
//        }
//
//        return SyncMsg.ERROR_ACCESS_FILE;
    }

    /**
     * Override the existing upload to add notification
     * 
     * @param file
     * @return
     */
    public int upload(KiiFile file) {
        int ret = mSyncManager.upload(file, false);
        if (ret == SyncMsg.OK) {
        	mSyncManager.getSyncObserver().notifyLocalChangeSynced(
                    new Uri[] { file.getUri() });
            return mSyncManager.refresh();
        }
        return ret;
    }

    /**
     * Move the KiiFile to trash.
     * 
     * @param kiFile
     * @return SyncMsg
     */
    public int moveKiiFileToTrash(KiiFile kiFile) {

        // don't support for server copy
        if (getStatus(kiFile) != KiiFile.STATUS_SYNCED) {
            Log.e(TAG, "Can't trash server file");
            return SyncMsg.ERROR_RECORD_NOT_FOUND;
        }

        String origPath = kiFile.getResourceUrl();

        // time which the content is moved to trash
        kiFile.setAppData(Long.toString(System.currentTimeMillis()));
        kiFile.setCategory(CATEGORY_TRASH);
        int ret = update(kiFile);
        if (origPath != null) {
            File file = new File(origPath);
            if (file.delete() == false) {
                Log.e(TAG, "Delete file has failed.");
            }
        }
        return ret;
    }

    /**
     * Return the status of an KiiFile from Cache Non Blocking Call
     * 
     * @param pathName
     * @return
     */
    public int getStatusFromCache(String pathName) {
        return getFileStatusCache().getKiiFileStatus(pathName);
    }

    public int getStatusFromCache(KiiFile file) {
        return getStatusFromCache(file.getResourceUrl());
    }
    
    private KiiFileListener getFileStatusCache() {
        return mFileStatusCache;
    }

    /**
     * Return the status of an KiiFile Non Blocking Call
     * 
     * @param pathName
     * @return
     */
    public int getStatus(String pathName) {

        KiiFile[] files = getKiiFilesByPath(pathName);

        if (files == null || files.length == 0)
            return 0;

        // only takes the first file in the list
        // TODO: when support multi devices sync, need to iterate through the
        // list and match by unique key
        KiiFile file = files[0];

        return getStatus(file);
    }

    /**
     * Return the status of an KiiFile Non Blocking Call
     * 
     * @param file
     * @return
     */
    public int getStatus(KiiFile file) {
    	
    	// read the status of the KiiFile
    	// this status is reading from the database
    	int status = file.getStatus(); 
    	
        if ( status == KiiFile.STATUS_SYNCED
                || status == KiiFile.STATUS_NO_BODY) {

            // check if the exist, if not indicate only remote copy
            if (file.isFile()) {
                File f = new File(file.getResourceUrl());
                if (!f.exists()) {
                    return KiiFile.STATUS_NO_BODY;
                }
            }

            if (bodySameAsLocal(file)) {
                return KiiFile.STATUS_BODY_OUTDATED;
            } else {
                return KiiFile.STATUS_SYNCED;
            }
        }

        return status;
    }

    /**
     * check if the local file is the same as given KiiFile
     * 
     * @param file
     * @return true if the same otherwise false
     */
    public boolean bodySameAsLocal(KiiFile file) {
        String localPath = file.getLocalPath();
        if(TextUtils.isEmpty(localPath)){
        	return false;
        }
        File localFile = new File(localPath);
        if (!(localFile.exists() && localFile.isFile())) {
            return false;
        }
        long fileUpdated = localFile.lastModified();
        Log.v(TAG, "fileUpdated: " + fileUpdated);
        long lastUpdated = file.getUpdateTime();
        if (lastUpdated == -1) {
            return false;
        }
        if (fileUpdated != lastUpdated) {
            return true;
        }
        long size = localFile.length();
        Log.v(TAG, "size: " + size);
        if (size != file.getSizeOnDB()) {
            return true;
        }
        return false;
    }


    /**
     * Get the list of KiiFile by matching the unique key Non Blocking Call
     * 
     * @param uniqueKey
     * @return
     */
    KiiFile[] getKiiFileByUniqueKey(String uniqueKey) {
        return KiiFileUtil.listBySelection(mContext,
                DatabaseHelper.FileColumns.UNIQUE_KEY + "='" + uniqueKey + "'");
    }

    /**
     * Get KiiFile by the path provided. If the KiiFile already exist, it will
     * return the existing KiiFilr Non Blocking Call
     * 
     * @param path
     * @return
     */
    KiiFile createKiiFileByPath(String filepath) {
        return KiiFileUtil.createKiiFileFromFile(filepath);
    }

    /**
     * Get the list of KiiFile by matching the path Non Blocking Call
     * 
     * @param path
     * @return
     */
    KiiFile[] getKiiFilesByPath(String path) {
        path = path.replaceAll("'", "''");
        return KiiFileUtil.listBySelection(mContext,
                DatabaseHelper.FileColumns.RESOURCE_URL + "='" + path + "'");
    }

    /**
     * Get the list of files that is pending uploading Non Blocking Call
     * 
     * @return
     */
    public KiiFile[] getListError() {
        return KiiFileUtil.listBySelection(mContext, "status<0");
    }

    /**
     * Get the list of files that are in progress Non Blocking Call
     * 
     * @return
     */
    public KiiFile[] getListInProgress() {
        return KiiFileUtil.listBySelection(mContext, "status IN ("
                + KiiFile.STATUS_SYNC_IN_QUEUE + ", "
                + KiiFile.STATUS_UPLOADING_BODY + ", "
                + KiiFile.STATUS_PREPARE_TO_SYNC + ", "
                + KiiFile.STATUS_SERVER_DELETE_REQUEST + ", "
                + KiiFile.STATUS_DELETE_REQUEST + ", "
                + KiiFile.STATUS_DELETE_REQUEST_INCLUDEBODY + ")");
    }

    /**
     * Restore the file from Trash
     * 
     * @param file
     * @return
     */
    public int restoreFromTrash(KiiFile file) {
        try {
        	// download a file 
            download(file, true);
        } catch (IOException e) {
            Log.e(TAG, "restoreFromTrash download IOException", e);
            return SyncMsg.ERROR_IO;
        }
        // application specific data
        file.setAppData("File is restore from trash");
        // reset the category as NULL to indicate non trash
        file.setCategory(CATEGORY_NONE);
        
        return update(file);
    }

    /**
     * Save the kFile to database Update to the cloud
     */
    public int update(KiiFile kFile) {
        // int res = kFile.saveToDb();
        // if (res == SyncMsg.OK) {
        int res = mSyncManager.update(kFile, false);
        if (res == SyncMsg.OK) {
        	mSyncManager.getSyncObserver().notifyLocalChangeSynced(null);
            return mSyncManager.refresh();
        }
        // }
        return res;
    }

    public DownloadManager downManager = null;

    /**
     * Download the file to dnload folder
     */
    public int download(KiiFile kiFile) {
        if (downManager == null) {
            downManager = new DownloadManager();
        }
        downManager.add(kiFile, null);
        return downManager.resume();
    }

    public int download(KiiFile file, String dest) {
        if (downManager == null) {
            downManager = new DownloadManager();
        }
        downManager.add(file, dest);
        return downManager.resume();
    }
    /**
     * Download the file to it original position
     * 
     * @param kiFile
     * @param overwrite
     * @throws IOException
     */
    public void download(KiiFile kiFile, boolean overwrite) throws IOException {
        if (downManager == null) {
            downManager = new DownloadManager();
        }

        downManager.downloadKiiFile(new File(kiFile.getResourceUrl()), kiFile,
                true);
    }
    
    public DownloadManager getDownloadManager() {
        if(downManager == null) {
            downManager = new DownloadManager();
        }
        return downManager;
    }

    /**
     * For testing only Set the subscription and quota
     * 
     * @param sync
     * @param quota
     *            in number of bytes
     * @return SyncMsg
     */
    public int setSubscription(String username, String password, boolean sync,
            long quota) {
        HttpPut request = new HttpPut(
                "https://product-jp.kii.com/integration/astro"
                        + URL_SUBSCRIPTION);
        request.addHeader("Content-Type", "application/json");
        JSONObject j = new JSONObject();
        StringEntity entry = null;

        try {
            j.put("quota", quota);
            j.put(TAG_SYNC_ENABLED, sync);

            Log.v(TAG, "User create for JsonObj: " + j.toString());
            entry = new StringEntity(j.toString(), HTTP.UTF_8);
            entry.setContentType("application/json");

            request.setEntity(entry);
            HttpResponse response = null;

            response = HttpClientUtils.execute(request, username, password);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                return SyncMsg.OK;
            } else {
                return SyncMsg.convertStatusCodeToKiiError(HttpClientUtils
                        .logHttpResponse(TAG, request, response));
            }

        } catch (JSONException e) {
            Log.e(TAG, "API setSubscription JSONException:", e);
            return SyncMsg.ERROR_JSON;
        } catch (UnsupportedEncodingException ue) {
            Log.e(TAG, "API setSubscription UnsupportedEncodingException:", ue);
            return SyncMsg.ERROR_JSON;
        } catch (ClientProtocolException cpe) {
            Log.e(TAG, "API setSubscription ClientProtocolException:", cpe);
            return SyncMsg.ERROR_IO;
        } catch (IOException ioe) {
            Log.e(TAG, "API setSubscription IOException:", ioe);
            return SyncMsg.ERROR_IO;
        }
    }

    /**
     * Returns a list of services that the account is currently link to
     * 
     * @return "syncEnabled":true,"usedByteSize":0,"quotaByteSize":1000000000
     */
    public List<String> getSubscription(String username, String password) {
        // HttpGet request = new HttpGet(SyncPref.getAppUrl() +
        // URL_SUBSCRIPTION);
        HttpGet request = new HttpGet(
                "https://product-jp.kii.com/integration/astro"
                        + URL_SUBSCRIPTION);
        request.addHeader("Content-Type", "application/json");

        HttpResponse response = null;
        try {
            response = HttpClientUtils.execute(request, username, password);
            if (response.getStatusLine().getStatusCode() != 200) {
                HttpClientUtils.logHttpResponse(TAG, request, response);
                return null;
            }

            ArrayList<String> servicesList = new ArrayList<String>();

            String res = EntityUtils.toString(response.getEntity());
            Log.d(TAG, "getListServices response is " + res);
            JSONObject jsonObj = new JSONObject(res);

            if (jsonObj.has(TAG_SYNC_ENABLED)) {
                servicesList.add(jsonObj.getString(TAG_SYNC_ENABLED));
            } else {
                servicesList.add(null);
            }

            if (jsonObj.has(TAG_USED_BYTE_SIZE)) {
                servicesList.add(jsonObj.getString(TAG_USED_BYTE_SIZE));
            } else {
                servicesList.add(null);
            }

            if (jsonObj.has(TAG_QUOTA_BYTE_SIZE)) {
                servicesList.add(jsonObj.getString(TAG_QUOTA_BYTE_SIZE));
            } else {
                servicesList.add(null);
            }
            
            

            return servicesList;

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public static boolean isFileInTrash(KiiFile file) {
        if(file == null) {
            return false;
        }
        
        String category = file.getCategory();
        if(category == null) {
            return false;
        }
        
        if(category.contentEquals(CATEGORY_TRASH)) {
            return true;
        }
        return false;
    } 

    /**
     * @param fullPathOfFolder
     *            fullpath of folder would be renamed.
     * @param newName
     *            new Folder name
     * @return TODO: define code. This method is blocking and may take long time
     *         when there are many files/folders inside. Do not call from
     *         responsive thread (eg. UI/Main thread).
     */
    public int renameFolder(String fullPathOfFolder, String newName) {
        if (fullPathOfFolder == null || newName == null) {
            return Result.INVARG;
        }
        Log.v(TAG, " going to update files in folder");
        KiiFile[] kiiFiles = KiiFileUtil.listFilesInFolder(fullPathOfFolder);
        if (kiiFiles == null || kiiFiles.length <= 0)
            return Result.NOTFOUND;

        Log.v(TAG, "Total kiiFiles found :" + kiiFiles.length);
        KiiFileUtil.rename2(kiiFiles, fullPathOfFolder, newName);
        return mSyncManager.upload(kiiFiles);
    }

    class Result {
        static final int OK = 0;
        static final int NOTFOUND = 1;
        static final int INVARG = -1;
        static final int NG = -2;
    }
    
}