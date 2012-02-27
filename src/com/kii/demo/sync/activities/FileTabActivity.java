package com.kii.demo.sync.activities;

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
                .setIndicator(getString(R.string.tab_cloud))
                .setContent(intent));
        mTabHost.getTabWidget().getChildAt(0).getLayoutParams().height = 
        	60;
        mTabHost.getTabWidget().getChildAt(1).getLayoutParams().height = 
        	60; 
        
    }

    @Override
    public void onNewIntent(Intent intent) {
        setCurrentTab(intent);
    }
}
