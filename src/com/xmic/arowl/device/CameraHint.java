



//--ivan.c@tpv-tech.com--2013-8-9

package com.xmic.arowl.device;

import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.hardware.input.InputManager.InputDeviceOpenedListener;

import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.Gravity;
import java.util.Timer;   
import java.util.TimerTask;

import android.util.Slog;
import android.content.Context;
import android.content.res.Resources;

import android.view.InputDevice;
import com.xmic.arowl.R;

public class CameraHint {

	static final String TAG = "CameraHint";


	private InputManager mIm;

	private Context mContext;

	private Toast mHint;

	private Handler mHandler;

	Timer timer;

	private int mNumSecond;

	private boolean show;


	public CameraHint(Context context) {

		mNumSecond = 25;//1.5+25+3.5
		show = false;
		mIm = InputManager.getInstance(); 
		mContext = context;
		//mIm = (InputManager)this.getSystemService(Context.INPUT_SERVICE);
		mIm.registerInputDeviceListener(new InputDeviceListener() {
            @Override
            public void onInputDeviceAdded(int deviceId) {
         		Slog.d(TAG,"onInputDeviceAdded(int deviceId)===============================1===" + deviceId);

				InputDevice mInputDevice = mIm.getInputDevice(deviceId);
				String mDescriptor = mInputDevice.getDescriptor();
				Slog.d(TAG,"===ivan-test====mDescriptor = " + mDescriptor);

				int mSource = mInputDevice.getSources();
				Slog.d(TAG,"===ivan-test====mSource = " + mSource);

				

				if (!mDescriptor.equals("28727f5b0479e728724c401e38ec6e61c7b851c8") && mSource == 33554689) {//CAMERA:sources=0x02000101

					Slog.d(TAG,"=====ivan-test==== no default camera");
					if (show == true) {
						timer.cancel();
					}
					messageHint();
					mNumSecond = 25;//1.5+25+3.5
				} 
            }

	        @Override
            public void onInputDeviceRemoved(int deviceId) {
         		Slog.d(TAG,"onInputDeviceRemoved(int deviceId)===============================2===" + deviceId);
            }

	        @Override
            public void onInputDeviceChanged(int deviceId) {
         		Slog.d(TAG,"onInputDeviceChanged(int deviceId)===============================3===" + deviceId);
            }		
        }, null);

		mIm.registerInputDeviceOpenedListener(new InputDeviceOpenedListener() {

	        @Override
            public void onInputDeviceOpened() {
         		Slog.d(TAG,"onInputDeviceOpened(int deviceId)=============!!!!!!!!!!!!=====================");
				if (show == true) {
					timer.cancel();
				}
				messageHint();
				mNumSecond = 25;//1.5+25+3.5
            }		
        }, null);


		mIm.enableInputDevicesListener();




	}

	private void messageHint(){
		
		show = true;
		Resources resources = mContext.getResources();
		final String showtext = resources.getString(R.string.no_default_camera);
		
		
		if (mHandler == null) {
			Slog.d(TAG,"======ivan-test===== mHandler == null");
			mHandler = new Handler(Looper.getMainLooper());
		}

	    mHandler.post(new Runnable() {
	        public void run() {           
	            if (mHint == null) {
	                mHint = new Toast(mContext.getApplicationContext());
	            }

	            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            View layout = inflater.inflate(R.layout.notification_toast, null);
	            //ImageView icon = (ImageView) layout.findViewById(R.id.status_icon);
	            //icon.setImageResource(statusIcon);
	            TextView textInfo = (TextView) layout.findViewById(R.id.status_info);
	            textInfo.setText(showtext);

	            mHint.setView(layout);
	            mHint.setDuration(Toast.LENGTH_LONG);
	            mHint.setGravity(Gravity.BOTTOM|Gravity.RIGHT, -37 , -37);
				Slog.d(TAG,"===ivan-test==== here we go!");
            	mHint.show();
				execToast(mHint);
					
	        }  
	    });

	}
	
	

	private void execToast(final Toast toast) {

        timer= new Timer();
        timer.schedule(new TimerTask() {
               @Override
               public void run() {
                  //while(true){  
                  	 Slog.d(TAG,"===ivan-test===checkTime======0=========mNumSecond = " + mNumSecond);
                     toast.show();
				  	 checkTime();
                 // } 
               }
        }, 1500, 1000);				
    }

	private void checkTime() {
		
		
		if (mNumSecond <= 0) {
			Slog.d(TAG,"checkTime=======1========");
			timer.cancel();
			show = false;
		}
		mNumSecond--;
	}

}











