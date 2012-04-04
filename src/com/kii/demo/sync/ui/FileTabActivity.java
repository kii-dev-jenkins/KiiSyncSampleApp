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

package com.kii.demo.sync.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.kii.demo.sync.R;

public class FileTabActivity extends TabActivity {
    TabHost mTabHost;
    public static final int TAB_INDEX_DEVICE = 0;
    public static final int TAB_INDEX_CLOUD = 1;
    public static final int TAB_INDEX_PROGRESS = 2;
    public static final String TAB_INDEX_EXTRA = "com.kii.demo.sync.activities.kiifileviewer.tabindex";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.tab_layout);
        mTabHost = getTabHost();
        setupTabs();
        final Intent intent = getIntent();
        setCurrentTab(intent);
    }

    private void setCurrentTab(Intent intent) {
        if (intent.hasExtra(TAB_INDEX_EXTRA)) {
            int extra = intent.getIntExtra(TAB_INDEX_EXTRA, TAB_INDEX_DEVICE);
            mTabHost.setCurrentTab(extra);
        }
    }

    private void setupTabs() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("device")
                .setIndicator(getString(R.string.tab_device))
                .setContent(intent));
        intent = new Intent(this, KiiFilePickerActivity.class);
        mTabHost.addTab(mTabHost.newTabSpec("cloud")
                .setIndicator(getString(R.string.tab_cloud)).setContent(intent));
    }

    @Override
    public void onNewIntent(Intent intent) {
        setCurrentTab(intent);
    }
}
