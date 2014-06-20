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

package com.xmic.arowl.usb;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
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

public class StorageNotification extends StorageEventListener {
    private static final String TAG = "Arowl-StorageNotification";
    private static final boolean KLOG = true;

    // kevin@xmic <disable_UMS>
    // For smart TV product, UMS feature is unnecessary
    private static final boolean NEED_UMS = false;
    
    // UI handler messages and delay
    // ...

    private Context mContext;
    private StorageManager mStorageManager;
    private StorageUtil mStorageUtil;
    private Handler mUiHandler;
    
    private Toast mStorageStatusHint;

    public StorageNotification(Context context) {
        mContext = context;

        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        mStorageUtil = new StorageUtil(context);
     
        // ui handler
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };
    }

    public StorageNotification getInstance() {
        return this;
    }

    /*
     * @override com.android.os.storage.StorageEventListener
     */
    @Override
    public void onUsbMassStorageConnectionChanged(final boolean connected) {
        // kevin@xmic <disable_UMS>
        // do nothing currently
    }

    /*
     * @override com.android.os.storage.StorageEventListener
     */
    @Override
    public void onStorageStateChanged(final String path, final String oldState, final String newState) {
        Slog.i(TAG, String.format(
                "Media {%s} state changed from {%s} -> {%s}", path, oldState, newState));

        // Skip update if new state not set
        if (newState == null) {
            return;
        }

        // Skip update for state changes we don't interested
        // 1. UNMOUNTABLE->UNMOUNTED AND UNMOUNTED->UNMOUNTABLE are not state changes we care about, just skip them
        if (oldState != null) {
            if (oldState.equals(Environment.MEDIA_UNMOUNTABLE)
                && newState.equals(Environment.MEDIA_UNMOUNTED)) {
                return;
            } else if (oldState.equals(Environment.MEDIA_UNMOUNTED)
                && newState.equals(Environment.MEDIA_UNMOUNTABLE)) {
                return;
            }
            // 2. UNMOUNTED->REMOVED change could possibly come as following sequence for UNMOUNTABLE->UNMOUNTED, so we skip it either.
            else if (oldState.equals(Environment.MEDIA_UNMOUNTED)
                && newState.equals(Environment.MEDIA_REMOVED)) {
                return;
            }
        }

        Resources resources = mContext.getResources();
        if (resources == null)
            return;

        String deviceName;
        String eventPlace;
        if (Environment.getExternalStorageDirectory().getPath().equals(path)
            || path.contains("/storage/emulated")) {
            deviceName = resources.getString(R.string.primary_default_label);
        } else {
            String diskName;
            // read disk id from file if path mounted and update cache
            if (newState.equals(Environment.MEDIA_MOUNTED)) {
                if (KLOG) Slog.i(TAG, "get disk id: " + StorageUtil.DISKID_TAG_DISKNAME + " from file for path:" + path);
                diskName = mStorageUtil.readDiskIdForPath(path, StorageUtil.DISKID_TAG_DISKNAME);
            } else {
                // read disk id from cache if path is not mounted
                if (KLOG) Slog.i(TAG, "get disk id: " + StorageUtil.DISKID_TAG_DISKNAME + " from cache for path:" + path);
                diskName = mStorageUtil.getCachedId(path, StorageUtil.DISKID_TAG_DISKNAME);
            }
            
            if (diskName != null 
                && !diskName.isEmpty()) {
                deviceName = diskName;
            } else {
                Slog.i(TAG, "Can't get disk name for path: " + path);
                deviceName = resources.getString(R.string.ext_default_label);
            }
        }
        switch (mStorageUtil.getMountPort(path)) {
            case StorageUtil.SDSlot:
                // There are only one SD slot, no need to ditinguish ports
                //eventPlace = resources.getString(R.string.SDSlot);
                eventPlace = null;
                break;
            case StorageUtil.USBPort1:
                eventPlace = resources.getString(R.string.USB1);
                break;
            case StorageUtil.USBPort2:
                eventPlace = resources.getString(R.string.USB2);
                break;
            case StorageUtil.USBPort3:
                eventPlace = resources.getString(R.string.USB3);
                break;
            case StorageUtil.USBPort4:
                eventPlace = resources.getString(R.string.USB4);
                break;
            default:
                eventPlace = null;
        }

        if (newState.equals(Environment.MEDIA_MOUNTED) && (oldState != null)) {
            // Only notifiy mounted if last state is checking
            if (oldState.equals(Environment.MEDIA_CHECKING)) {
                if (eventPlace != null) {
                    showStorageNotification(deviceName, eventPlace,
                        R.drawable.usb,
                        R.string.storage_connected_to);
                } else {
                    showStorageNotification(deviceName, eventPlace,
                    R.drawable.sd,
                    R.string.storage_connected);
                }
            }
        } else if (newState.equals(Environment.MEDIA_UNMOUNTED) && (oldState != null)) {
            if (!mStorageManager.isUsbMassStorageEnabled()) {
                if (oldState.equals(Environment.MEDIA_SHARED)) {
                    /*
                     * The unmount was due to UMS being enabled. Dont' show any
                     * storage notifications
                     */
                }
 
                if (oldState.equals(Environment.MEDIA_MOUNTED)) {
                    if (eventPlace == null) {   // primary storage
                        /* Primary storage might be unremovable, 
                         * but it is always single volume.
                         * Show safe to remove if it is removable
                         */
                        if (Environment.isExternalStorageRemovable()) { 
                            showStorageNotification(deviceName, eventPlace,
                                R.drawable.sd,
                                R.string.storage_safe_to_remove);
                        }
                    } else {
                        /* Non-primary storage should always be removable, 
                         * but it could be multiple volume.
                         * Show safe to remove if all the buddy volumes has been unmounted
                         */
                        ArrayList<String> mountedBuddies = mStorageUtil.getMountpointsForMountedBuddyVolumes(path);
                        if (mountedBuddies == null
                            || mountedBuddies.isEmpty()) {
                            showStorageNotification(deviceName, eventPlace,
                                R.drawable.usb,
                                R.string.storage_safe_to_remove);
                        }
                    }
                }
            } else {
                /*
                 * The unmount was due to UMS being enabled. Don't show any
                 * storage notifications
                 */
            }
        }  else if (newState.equals(Environment.MEDIA_REMOVED) && (oldState != null)) {
            // Storage has been removed. Show nomedia storage notification
            // kevin@xmic: Is this notification supposed to show?
            if (Environment.getExternalStorageDirectory().getPath().equals(path)) {
                showStorageNotification(deviceName, eventPlace,
                    R.drawable.sd,
                    R.string.storage_removed);
            } else {
                showStorageNotification(deviceName, eventPlace,
                    R.drawable.usb,
                    R.string.storage_removed);
            }
        } else if (newState.equals(Environment.MEDIA_BAD_REMOVAL) && (oldState != null)) {
            // Storage has been removed unsafely. Show bad removal storage notification
            if (Environment.getExternalStorageDirectory().getPath().equals(path)) {
                showStorageNotification(deviceName, eventPlace,
                    R.drawable.sd_warning,
                    R.string.storage_bad_removed);
            } else {
                showStorageNotification(deviceName, eventPlace,
                    R.drawable.usb_warning,
                    R.string.storage_bad_removed);
            }
        } else {
            Slog.w(TAG, String.format("Ignoring unknown state {%s}", newState));
        }
    }

    /**
     * Sets the media storage notification.
     */
    private synchronized void showStorageNotification(String deviceName, String eventPlace, 
        int iconId, int messageId) {
        Resources resources = mContext.getResources();
        if (resources == null)
            return;

        final String hintText;
        final int statusIcon = iconId;
        if (eventPlace != null 
            && messageId == R.string.storage_connected_to) {
            hintText = String.format("%s %s %s", deviceName, resources.getString(messageId), eventPlace);
        } else {
            hintText = String.format("%s %s", deviceName, resources.getString(messageId));
        }

        if (hintText != null) {
            mUiHandler.post(new Runnable() {
                public void run() {           
                    if (mStorageStatusHint == null) {
                        mStorageStatusHint = new Toast(mContext.getApplicationContext());
                    }

                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.notification_toast, null);
                    ImageView icon = (ImageView) layout.findViewById(R.id.status_icon);
                    icon.setImageResource(statusIcon);
                    TextView textInfo = (TextView) layout.findViewById(R.id.status_info);
                    textInfo.setText(hintText);

                    mStorageStatusHint.setView(layout);
                    mStorageStatusHint.setDuration(Toast.LENGTH_LONG);
                    mStorageStatusHint.setGravity(Gravity.BOTTOM|Gravity.RIGHT, -37 , -37);
                    mStorageStatusHint.show();
                }  
            });
        }
    }

    public void notifyOverCurrentDetect(String eventPlace) {
        Slog.i(TAG, "Detetct over current at port: " + eventPlace);
        Resources resources = mContext.getResources();
        if (resources == null)
            return;

        final String hintText;
        final int statusIcon = R.drawable.usb_warning;
        if (eventPlace == null) {
            hintText = String.format("%s %s, %s", 
                "USB",
                resources.getString(R.string.detect_overcurrent),
                resources.getString(R.string.reconnect_storage_prompt));
        } else {
            hintText = String.format("%s %s, %s", 
                eventPlace,
                resources.getString(R.string.detect_overcurrent),
                resources.getString(R.string.reconnect_storage_prompt));
        }

        if (hintText != null) {
            mUiHandler.post(new Runnable() {
                public void run() {           
                    if (mStorageStatusHint == null) {
                        mStorageStatusHint = new Toast(mContext.getApplicationContext());
                    }

                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.notification_toast, null);
                    ImageView icon = (ImageView) layout.findViewById(R.id.status_icon);
                    icon.setImageResource(statusIcon);
                    TextView textInfo = (TextView) layout.findViewById(R.id.status_info);
                    textInfo.setText(hintText);

                    mStorageStatusHint.setView(layout);
                    mStorageStatusHint.setDuration(Toast.LENGTH_LONG);
                    mStorageStatusHint.setGravity(Gravity.BOTTOM|Gravity.RIGHT, -37 , -37);
                    mStorageStatusHint.show();
                }  
            });
        }
    }
}
