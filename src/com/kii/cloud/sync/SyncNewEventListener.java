//
//
//  Copyright 2012 Kii Corporation
//  http://kii.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//

package com.kii.cloud.sync;

import android.content.Context;
import android.net.Uri;

import com.kii.sync.KiiNewEventListener;
import com.kii.sync.SyncMsg;

public abstract class SyncNewEventListener implements KiiNewEventListener {
    KiiSyncClient client = null;
    protected long id = 0;
    protected Context mContext;

    public SyncNewEventListener(Context context) {
        mContext = context;
        id = System.currentTimeMillis();
    }

    public boolean register() {
        client = KiiSyncClient.getInstance(mContext);
        if (client == null) {
            throw new NullPointerException();
        }
        return client.registerNewEventListener(id, this);
    }

    public void unregister() {
        if (id != 0) {
            client.unregisterNewEventListener(id);
        }
    }
    @Override
    public void onLocalChangeSyncedEvent(Uri[] arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewSyncDeleteEvent(Uri[] arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewSyncInsertEvent(Uri[] arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewSyncUpdateEvent(Uri[] arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onQuotaExceeded(Uri arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSyncComplete(SyncMsg arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSyncStart(String arg0) {
        // TODO Auto-generated method stub

    }

    public abstract void onDownloadComplete(Uri[] arg0);

}
