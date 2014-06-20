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

package com.xmic.arowl;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IWindowManager;
import com.xmic.arowl.device.CameraHint; 

public class ArowlService extends Service {
    static final String TAG = "ArowlService";

    /**
     * The class names of the stuff to start.
     */
    final Object[] SERVICES = new Object[] {
            com.xmic.arowl.service.NotificationService.class,
            // append more service here
        };

    /**
     * Hold a reference on the stuff we start.
     */
    Growl[] mServices;
	
	CameraHint mCameraHint; //--ivan.c@tpv-tech.com--2013-8-9

    private Class chooseClass(Object o) {
        if (o instanceof Integer) {
            final String cl = getString((Integer)o);
            try {
                return getClassLoader().loadClass(cl);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } else if (o instanceof Class) {
            return (Class)o;
        } else {
            throw new RuntimeException("Unknown growl service: " + o);
        }
    }

    @Override
    public void onCreate() {
        final int N = SERVICES.length;
        mServices = new Growl[N];
        for (int i = 0; i < N; i++) {
            Class cl = chooseClass(SERVICES[i]);
            Slog.d(TAG, "loading: " + cl);
            try {
                mServices[i] = (Growl)cl.newInstance();
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            }
            mServices[i].mContext = this;
            Slog.d(TAG, "running: " + mServices[i]);
            mServices[i].start();
        }

		mCameraHint = new CameraHint(this); //--ivan.c@tpv-tech.com--2013-8-9
			
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        for (Growl ui: mServices) {
            ui.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Nobody binds to us.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args == null || args.length == 0) {
            for (Growl ui: mServices) {
                pw.println("dumping growl service: " + ui.getClass().getName());
                ui.dump(fd, pw, args);
            }
        } else {
            String svc = args[0];
            for (Growl ui: mServices) {
                String name = ui.getClass().getName();
                if (name.endsWith(svc)) {
                    ui.dump(fd, pw, args);
                }
            }
        }
    }
}

