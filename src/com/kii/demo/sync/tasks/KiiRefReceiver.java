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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kii.cloud.sync.BackupService;
import com.kii.demo.sync.utils.Utils;

public class KiiRefReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.startSync(context, BackupService.ACTION_DATA_CONNECTION_CHANGED);
    }

}
