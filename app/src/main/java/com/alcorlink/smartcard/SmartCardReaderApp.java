package com.alcorlink.smartcard;

import android.app.Application;
import android.util.Log;

public class SmartCardReaderApp extends Application  {
	
	 private static SmartCardReaderApp sInstance;  
	@Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();  
        crashHandler.init(this);
        Log.d("SmartCardApp", "OnCreate");
	}
	
	   public static SmartCardReaderApp getInstance() {  
	        return sInstance;  
	    }  
}
