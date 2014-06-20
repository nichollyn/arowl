/*
 * Copyright (C) 2012, 2013 TPV Display Technology Ltd.
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

package com.xmic.arowl.download;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import com.xmic.arowl.R;

import java.util.ArrayList;

import android.util.Slog;

public class DownloadNotification {
    private static final String TAG = "Arowl-DownloadNotification";
    private static final boolean KLOG = true;

    private Context mContext;

    private Toast mDownloadStatusHint;

    private Handler mUiHandler;

    public DownloadNotification(Context context) {
        mContext = context;
        mUiHandler = new Handler(Looper.getMainLooper());
    }

    public DownloadNotification getInstance() {
        return this;
    }

    /**
     * Show download notification.
     */
    public synchronized void showDownloadNotification(String downloadTitle, boolean success) {
        Resources resources = mContext.getResources();
        if (resources == null)
            return;

        final String hintText;
        final int statusIcon;
        if (success) {
            hintText = String.format("%s\n%s",
                downloadTitle, resources.getString(R.string.download_completed));
            statusIcon = R.drawable.download_success;
        } else {
            hintText = String.format("%s\n%s",
                downloadTitle, resources.getString(R.string.download_failed));
            statusIcon = R.drawable.download_failed;
        }

        if (hintText != null) {
            mUiHandler.post(new Runnable() {
                public void run() {           
                    if (mDownloadStatusHint == null) {
                        mDownloadStatusHint = new Toast(mContext.getApplicationContext());
                    }

                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.notification_toast, null);
                    ImageView icon = (ImageView) layout.findViewById(R.id.status_icon);
                    icon.setImageResource(statusIcon);
                    TextView textInfo = (TextView) layout.findViewById(R.id.status_info);
                    textInfo.setText(hintText);

                    mDownloadStatusHint.setView(layout);
                    mDownloadStatusHint.setDuration(Toast.LENGTH_LONG);
                    mDownloadStatusHint.setGravity(Gravity.BOTTOM|Gravity.RIGHT, -37 , -37);
                    mDownloadStatusHint.show();
                }  
            });
        }
    }
}
