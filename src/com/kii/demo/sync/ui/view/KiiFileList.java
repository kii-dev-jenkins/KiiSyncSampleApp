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

package com.kii.demo.sync.ui.view;

import android.text.TextUtils;

import com.kii.sync.KiiFile;

public class KiiFileList {
    private KiiFile root = null;
    private KiiFile[] children = null;
    private String title = null;

    KiiFileList(KiiFile kFile) {
        root = kFile;
        if (kFile.isDirectory()) {
            children = kFile.listFiles();
            title = root.getBucketName();
        } else {
            title = root.getAppData();
            if (TextUtils.isEmpty(title)) {
                title = root.getTitle();
            }
        }
    }

    /**
     * Create a empty KiiFile with title only
     * 
     * @param title
     */
    KiiFileList(String title) {
        this.title = title;
    }

    /**
     * Create a empty KiiFile with title only
     * 
     * @param title
     */
    KiiFileList(String title, KiiFile[] list) {
        this.title = title;
        children = list;
    }

    boolean hasChildren() {
        if (children == null) {
            return false;
        }
        return true;
    }

    KiiFile getChild(int index) {
        if ((children != null) && (children.length > index)) {
            return children[index];
        }
        return null;
    }

    int getChildrenCount() {
        if (children != null) {
            return children.length;
        }
        return 0;
    }

    KiiFile getParent() {
        return root;
    }

    String getTitle() {
        return title + " (" + getChildrenCount() + ")";
    }
}