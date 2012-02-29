package com.kii.cloud.sync.auth;

import com.kii.cloud.sync.Authentication;
import com.kii.sync.KiiClient;
import com.kii.sync.SyncMsg;
import com.kii.sync.SyncPref;

/**
 * The Authentication is using the Kii Identity
 */
public class Identity implements Authentication{
	
	private KiiClient mSyncManager = null;
	
	String URL_IDENTITY = "/app/identity/";
	final String mBaseURL;
	
	/**
	 * Create an instance of Authentication Manager based on Identity Server
	 * @param syncManager reference to KiiClient
	 */
	public Identity(KiiClient syncManager, String baseURL){
		mSyncManager = syncManager;
		mBaseURL = baseURL;
		SyncPref.setIdentityUrl(baseURL+URL_IDENTITY);
	}
	
	@Override
    public int changePassword(String oldPassword, String newPassword) {
        return mSyncManager.changePassword(oldPassword, newPassword);
    }
    
	@Override
    public int register(String email, String password, String country,
            String nickName, String mobile) {
        return mSyncManager.register(email, password, country, nickName, mobile, null,
                null);
    }
    
	@Override
    public int login(String username, String password) {
        if (SyncPref.isLoggedIn()){
        	if(SyncPref.getPassword().compareTo(password)==0){
        		return SyncMsg.OK;
        	}
        }
        return mSyncManager.connect(username, password);
    }

	@Override
    public int logout() {
        return SyncMsg.OK;
    }
}
