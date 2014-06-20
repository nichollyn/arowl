/*
 * Copyright (C) 2012 TPV Display Technology Ltd.
 *
 * Kevin Lin (chenbin.lin@tpv-tech.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xmic.arowl.service;

import com.xmic.arowl.usb.StorageNotification;
import com.xmic.arowl.usb.StorageUtil;

import com.xmic.arowl.download.DownloadNotification;

import android.app.Notification;
import android.net.Uri;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.storage.StorageManager;

import com.xmic.arowl.Growl;
import com.xmic.arowl.R;

import android.util.Slog;

/**
 * This class contains all of the policy about which icons are installed in the growl notification pannel at boot time. 
 */
public class NotificationPolicy {
    private static final String TAG = "Arowl-NotificationPolicy";

    // message codes for the handler
    // ...

    private final Context mContext;
    private final Handler mHandler = new Handler();

    // storage
    private StorageManager mStorageManager;
    private StorageNotification mStorageStateListener;

    // download
    private DownloadNotification mDownloadNotification;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Growl.GROWL_INTENT_ACTION)) {
                String title = intent.getStringExtra("title");
                Slog.i(TAG, "== kevin@xmic == download package: " + title);
                long totalBytes = intent.getLongExtra("totalBytes", 0);
                long currentBytes = intent.getLongExtra("currentBytes", -1);
                boolean success = (totalBytes == currentBytes);

                if (title != null) {
                    mDownloadNotification.showDownloadNotification(title, success);
                }

                return;
            }

            int eventPlace = intent.getIntExtra("eventPlace", 0);
            String mountPort;
            switch (eventPlace) {
            case 1:
                mountPort = "USB1";
                break;
            case 2:
                mountPort = "USB2";
                break;
            case 3:
                mountPort = "USB3";
                break;
            case 4:
                mountPort = "USB4";
                break;
            default:
                mountPort = null;
            }
            // kevin@xmic: might need to declare the string to public
            if (action.equals("overcurrent")) {
                if (mStorageStateListener != null) {
                    mStorageStateListener.notifyOverCurrentDetect(mountPort);
                }
            }
        }
    };

    public NotificationPolicy(Context context) {
        mContext = context;

        // listen for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction("overcurrent");    // handle over current detect
        filter.addAction(Growl.GROWL_INTENT_ACTION);    // handle download growl
        // kevin@xmic: we currently listen to nothing, add action to filter as you need
        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);

        mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        mStorageStateListener = new StorageNotification(mContext);
        mStorageManager.registerListener(mStorageStateListener);

        mDownloadNotification = new DownloadNotification(mContext);
    }
}
