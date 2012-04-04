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
