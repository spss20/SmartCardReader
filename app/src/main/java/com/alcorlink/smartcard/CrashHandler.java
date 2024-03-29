package com.alcorlink.smartcard;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Process;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.util.Log;


public class CrashHandler implements UncaughtExceptionHandler {  
	    private static final String TAG = "CrashHandler";  
	    private static final boolean DEBUG = true;  
	  
	    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/AlcorlinkSmartCardApp/";  
	    private static final String FILE_NAME = "crash";  
	  

	    private static final String FILE_NAME_SUFFIX = ".trace";  
	  
	    private static CrashHandler sInstance = new CrashHandler();  
	  

	    private UncaughtExceptionHandler mDefaultCrashHandler;  
	  
	    private Context mContext;  
	  

	    private CrashHandler() {  
	    }  
	  
	    public static CrashHandler getInstance() {  
	        return sInstance;  
	    }  
	  
	    public void init(Context context) {  
	        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();  
	        Thread.setDefaultUncaughtExceptionHandler(this);  
	        mContext = context.getApplicationContext();  
	    }  
	  

	    @Override  
	    public void uncaughtException(Thread thread, Throwable ex) {

	    	dumpExceptionToSDCard(ex);
//	            uploadExceptionToServer();

	  
	        ex.printStackTrace();  
	  
	        if (mDefaultCrashHandler != null) {  
	            mDefaultCrashHandler.uncaughtException(thread, ex);  
	        } else {  
	            Process.killProcess(Process.myPid());  
	        }  
	  
	    }  
	  
	    private void dumpExceptionToSDCard(Throwable ex)  {
	        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {  
	            if (DEBUG) {  
	                Log.w(TAG, "sdcard unmounted,skip dump exception");  
	                return;  
	            }  
	        }  
	  
	        File dir = new File(PATH);  
	        if (!dir.exists()) {  
	            dir.mkdirs();  
	        }  
	        long current = System.currentTimeMillis();  
	        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(current));
	        File file = new File(PATH + FILE_NAME + time + FILE_NAME_SUFFIX);  
	  
	        try {  
	            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));  
	            pw.println(time);  
	  
	            dumpHostInfo(pw);  
	  
	            pw.println();  
	            ex.printStackTrace(pw);  
	  
	            pw.close();  
	        } catch (Exception e) {  
	            Log.e(TAG, "dump crash info failed");  
	        }  
	    }  
	  
	    private void dumpHostInfo(PrintWriter pw) throws NameNotFoundException {  
	        PackageManager pm = mContext.getPackageManager();  
	        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);  
	        pw.print("App Version: ");  
	        pw.print(pi.versionName);  
	        pw.print('_');  
	        pw.println(pi.versionCode);  
	  
	        pw.print("OS Version: ");  
	        pw.print(Build.VERSION.RELEASE);  
	        pw.print("_");  
	        pw.println(Build.VERSION.SDK_INT);  
	  
	        pw.print("Vendor: ");  
	        pw.println(Build.MANUFACTURER);  
	  
	        pw.print("Model: ");  
	        pw.println(Build.MODEL);  
	  
	
	    }  
	  
	  
	}  
