/*
 * Copyright (C) 2013 TPV Display Technology Ltd.
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
import android.os.Environment;
import android.os.Build.VERSION;
import android.os.storage.StorageVolume;
import android.os.storage.StorageManager;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.util.Log;

public class StorageUtil {
    private static final String TAG = "Arowl-StorageUtil";
    private static boolean KLOG = true;
    
    public static final boolean PRODUCT_SMART_MNT = false;
    // A special switch for product with built-in sdcard other than exposed sdcard slot
    public static final boolean BUILT_IN_SDCARD = PRODUCT_SMART_MNT;

    // Storage position
    public static final int USBPort1 = 1;
    public static final int USBPort2 = 2;
    public static final int USBPort3 = 3;
    public static final int USBPort4 = 4;
    public static final int SDSlot = 5;
    public static final int InternalSD = 6;

    private final Context mContext;
    /* external storage volumes */
    private final ArrayList<StorageVolume> mStorageVolumes = new ArrayList<StorageVolume>();
    /* mount points and backup mount points */
    // mount points should be unique, so we use HashSet
    private HashSet<String> mMountPoints = new HashSet<String>();
    private final HashMap<String, StorageVolume> mStorageVolumeMap = new HashMap<String, StorageVolume>();
    
    public StorageUtil(Context context) {
        mContext = context;

        initializeStorageVolumeInfo();
        initializeStorageIds();
    }

    public int getMountPort(String path) {
        if (path == null)
            return -1;

        if (path.contains("sdcard")
            || path.contains("emulated")) {
            if (BUILT_IN_SDCARD) {
                return InternalSD;
            } else {
                return SDSlot;
            }
        } else if (path.contains("extsd")) {
            return SDSlot;
        } else if (path.contains("usb1")) {
            return USBPort1;
        } else if (path.contains("usb2")) {
            return USBPort2;
        } else if (path.contains("usb3")) {
            return USBPort3;
        } else if (path.contains("usb4")) {
            return USBPort4;
        }
            
        return -1;
    }

    // diskid file related
    public static final String DISKID_FILE_NAME = ".diskid";
    public static final String DISKID_TAG_DISKNAME = "DISKNAME";

    // volid file related
    public static final String VOLID_FILE_NAME = ".volid";
    public static final String VOLID_TAG_LABEL = "LABEL";
    public static final String VOLID_TAG_TYPE = "TYPE";
    public static final String VOLID_TAG_ENCODING = "ENCODING";
    public static final String BRACKET = "\"";

    // HashMap to keep disk names and volume labels for mount points
    private final HashMap<String, String> mDiskNames = new HashMap<String, String>();
    private final HashMap<String, String> mVolumeLabels = new HashMap<String, String>();
    // HashMap to keep a top level table for interested ids and its hash map
    public final HashMap<String, HashMap> mStorageIds = new HashMap<String, HashMap>();
    
    private void initializeStorageVolumeInfo() {
        StorageManager storageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] list = storageManager.getVolumeList();
        
        if (list == null) {
            return;
        }
        
        if (!mStorageVolumeMap.isEmpty()) {
            mStorageVolumeMap.clear();
        }

        if (!mStorageVolumes.isEmpty()) {
            mStorageVolumes.clear();
        }

        if (!mMountPoints.isEmpty()) {
            mMountPoints.clear();
        }

        int length = list.length;
        for (int i = 0; i < length; i++) {
            mStorageVolumeMap.put(list[i].getPath(), list[i]);
            mStorageVolumes.add(list[i]);
            mMountPoints.add(list[i].getPath());
        }
    }

    private String getExternalStorageStateFromMountPoint(String mountPoint) {
        try {
            StorageManager storageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
            if (storageManager.getVolumeState(mountPoint).equals(Environment.MEDIA_MOUNTED)) {
                return Environment.MEDIA_MOUNTED;
            } else {
                return Environment.MEDIA_UNMOUNTED;
            }
        } catch (Exception e) {
            return Environment.MEDIA_REMOVED;
        }
    }
    
    // kevin@xmic: Be sure to get this method called before using getCachedId method
    public void initializeStorageIds() {
        // Currently we are interested in disk name and volume label
        mStorageIds.put(DISKID_TAG_DISKNAME, mDiskNames);
        mStorageIds.put(VOLID_TAG_LABEL, mVolumeLabels);
    }
    
    public String getCachedId(String path, String idTag) {
        HashMap<String, String> map = mStorageIds.get(idTag);
        if (map == null) {
            if (KLOG) Log.i(TAG, "== kevin@xmic == Can't get hash map for " + idTag);
            return null;
        }
        
        return map.get(path);
    }
    
    protected String cacheId(String path, String idTag, String idValue) {
        String newValue = idValue;
        HashMap<String, String> map = mStorageIds.get(idTag);
        if (map == null) {  // Can't cache, just return
            return newValue;
        }
        
        map.put(path, idValue);
        return newValue;
    }

    public String readDiskIdForPath(String path, String diskidTag) {
        String diskidFilePath = path + "/" + DISKID_FILE_NAME;
        File diskidFile = new File(diskidFilePath);
        if (diskidFile == null) {
            return cacheId(path, diskidTag, null);
        }
        
        FileInputStream fistream;
        try {
         fistream = new FileInputStream(diskidFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Can't find diskid file from " + diskidFilePath);
            return cacheId(path, diskidTag, null);
        }
        
        InputStreamReader isreader = null;
        BufferedReader bufreader = null;
        
        String result = null;
        String diskid = null;
        
        // All known disk ids are named in Latin language, UTF-8 would always be enough
        try {
            isreader = new InputStreamReader(fistream, "UTF-8");
            bufreader = new BufferedReader(isreader);
            while ((diskid = bufreader.readLine()) != null) {
                if (diskid.contains(diskidTag)) {
                    if (KLOG) Log.i(TAG, "Get disk id: " + diskid + "for " + path);
                    int openBracket = diskid.indexOf(BRACKET);
                    int closeBracket = diskid.lastIndexOf(BRACKET);
                    if (closeBracket > (openBracket+1)) { 
                        result = diskid.substring(openBracket + 1, closeBracket).trim();
                    } else {
                        result = null;
                    }
                }
            }

            bufreader.close();
            isreader.close();
            fistream.close();
            return cacheId(path, diskidTag, result);
        } catch (IOException e) {
            Log.e(TAG, "Failed due exception while read {" + diskidTag + "} from " + diskidFilePath);
        }
        
        return cacheId(path, diskidTag, result);
    }

    public String readVolIdForPath(String path, String volidTag) {
        String volidFilePath = path + "/" + VOLID_FILE_NAME;
        File volidFile = new File(volidFilePath);
        if (volidFile == null) {
            return cacheId(path, volidTag, null);
        }

        FileInputStream fistream;
        try {
         fistream = new FileInputStream(volidFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Can't find volid file from " + volidFilePath);
            return cacheId(path, volidTag, null);
        }
        
        InputStreamReader isreader = null;
        BufferedReader bufreader = null;
        
        String result = null;
        String volid = null;
        boolean isUtf8 = true;
        
        try {
            // firstly try to read volid file in UTF-8 encoding
            isreader = new InputStreamReader(fistream, "UTF-8");
            bufreader = new BufferedReader(isreader);
            while ((volid = bufreader.readLine()) != null) {
                if (volid.contains(volidTag)) {
                    if (KLOG) Log.i(TAG, "Get vol id: " + volid + "for " + path);
                    int openBracket = volid.indexOf(BRACKET);
                    int closeBracket = volid.lastIndexOf(BRACKET);
                    if (closeBracket > (openBracket+1)) {
                        result = volid.substring(openBracket + 1, closeBracket).trim();
                    } else {
                        result = null;
                    }
                }
                
                if (volid.contains("ENCODING=\"ansi\"")) {
                    // stop read as UTF-8 and reset reader
                    bufreader.close();
                    bufreader = null;
                    isreader.close();
                    isreader = null;
                    fistream.close();
                    fistream = null;
                    isUtf8 = false;
                    break;
                }
            }
                
            if (!isUtf8) {
                try {
                    fistream = new FileInputStream(volidFile);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Can't find volid file from " + volidFilePath);
                    return cacheId(path, volidTag, null);
                }
                isreader = new InputStreamReader(fistream, "GBK");
                bufreader = new BufferedReader(isreader);
                while ((volid = bufreader.readLine()) != null) {
                    if (volid.contains(volidTag)) {
                        int openBracket = volid.indexOf(BRACKET);
                        int closeBracket = volid.lastIndexOf(BRACKET);
                        result = volid.substring(openBracket + 1, closeBracket);
                    }
                }
            }
            
            bufreader.close();
            isreader.close();
            fistream.close();
            return cacheId(path, volidTag, result);
        } catch (IOException e) {
            Log.e(TAG, "Failed due exception while read {" + volidTag + "} from " + volidFilePath, e);
        }

        return cacheId(path, volidTag, result);
    }

    public ArrayList<String> getMountpointsForMountedBuddyVolumes(String path) {
        if (path == null) {
            return null;
        }
        
        int port = getMountPort(path);
        ArrayList<String> matchs = new ArrayList<String>();
        HashSet<String> all = mMountPoints;
        for (String item : all) {
            if (getMountPort(item) == port
                && !item.equals(path)
                && getExternalStorageStateFromMountPoint(item).equals(Environment.MEDIA_MOUNTED)) {
                matchs.add(item);
            }
        }
        
        return matchs;
    }
}