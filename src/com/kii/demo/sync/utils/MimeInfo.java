package com.kii.demo.sync.utils;

import com.kii.sync.KiiFileUtil;

public class MimeInfo {
    String mimeType;
    int icon;

    MimeInfo(int icon, String mimeType) {
        this.mimeType = mimeType;
        this.icon = icon;
        MimeUtil.mimeInfos.put(mimeType.toLowerCase(), this);
        // update the SDK Type table
        KiiFileUtil.putTypeMap(mimeType, mimeType.substring(0, mimeType.indexOf("/")));
    }

    public int getIconID() {
        return icon;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isType(String type) {
        return mimeType.startsWith(type.toLowerCase());
    }
}
