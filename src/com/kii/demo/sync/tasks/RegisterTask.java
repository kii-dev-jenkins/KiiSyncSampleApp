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

package com.kii.demo.sync.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.kii.cloud.sync.KiiSyncClient;
import com.kii.demo.sync.ui.StartActivity;
import com.kii.sync.SyncMsg;

public class RegisterTask extends AsyncTask<String, Void, Integer> {

    private Context mContext;

    public RegisterTask(Context context) {
        mContext = context;
    }

    static final String TAG = "RegisterTask";

    ProgressDialog dialog = null;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(mContext);
        dialog.setMessage(TAG);
        dialog.setCancelable(false);
        dialog.setButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new Thread(new Runnable() {
                    public void run() {
                        KiiSyncClient kiiClient = KiiSyncClient
                                .getInstance(mContext);
                        if (kiiClient != null) {
                            kiiClient.suspend();
                        }
                    }
                }).start();
            }
        });
        dialog.show();
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Integer doInBackground(String... params) {
        int result = SyncMsg.ERROR_SETUP;
        try {
            KiiSyncClient syncClient = KiiSyncClient.getInstance(mContext);
            if (params.length < 5) {
                return result;
            }
            String email = params[0];
            String password = params[1];
            String country = params[2];
            String nickName = params[3];
            String mobile = params[4];
            result = syncClient.register(email, password, country, nickName,
                    mobile);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return result;
    }

    /**
     * {@inheritdoc}
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Integer result) {
        if (dialog != null)
            dialog.dismiss();

        switch (result) {
            case SyncMsg.OK:
                String msg = "Registration is successful.";
                StartActivity.showAlertDialog(mContext, TAG, msg);
                break;
            case SyncMsg.ERROR_ALREADY_KII_USER:
                StartActivity.showAlertDialog(mContext, TAG,
                        "Email address/phone number is already registered.");
                break;
            case SyncMsg.ERROR_SETUP:
            default:
                StartActivity.showAlertDialog(mContext, TAG, result);
                break;
        }
    }
}
