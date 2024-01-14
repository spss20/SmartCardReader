package com.alcorlink.smartcard;

import android.app.Activity;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


class PermissionCheck {

    private static final int DELAY_PERMIT = 200;//0.2second;
    private static final int MY_PERMISSIONS_REQUEST_AUDIO = 0x345678;
    private static final int MY_PERMISSIONS_REQUEST_READ = 0x345;
    private static final int MY_PERMISSIONS_REQUEST_WRITE = 0x346;
    private static final String TAG = "CheckPermission";

    private void requestPermitDelay() {
        try {
            Thread.sleep(DELAY_PERMIT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isItCancelByUser(Activity thiz) {
        return ActivityCompat.shouldShowRequestPermissionRationale(thiz, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public boolean getReadWritePermission(Activity thiz)   {
        boolean ret = true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        String request = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        Log.v(TAG,"to request WRITE_EXTERNAL_STORAGE");
        ret = toCheckSomePermit(thiz, request, MY_PERMISSIONS_REQUEST_WRITE);
        if (!ret) {
            requestPermission(thiz,new String[]{ request}, MY_PERMISSIONS_REQUEST_WRITE);
            requestPermitDelay();
            return false;
        }
        return true;
    }

    public boolean checkReadWritePermit(Activity thiz) {
        boolean ret = true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        String request = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        ret = toCheckSomePermit(thiz, request, MY_PERMISSIONS_REQUEST_WRITE);

        return ret;
    }

    private boolean toCheckSomePermit(Activity thiz, String request, int requestCode) {
        boolean ret = true;
        int permission = ActivityCompat.checkSelfPermission(thiz.getApplicationContext(), request);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ret = false;
        }
        return ret;
    }


    private boolean isDialogDisabled(Activity thiz, String request) {
        boolean toShow = ActivityCompat.shouldShowRequestPermissionRationale(thiz, request);
        Log.d(TAG,"toShow =" + toShow);
       return toShow;
    }

    private void requestPermission(Activity thiz, String[] permission, int requestCode) {
        Log.d(TAG,"requestPermission");
        ActivityCompat.requestPermissions(thiz,
                permission,
                requestCode);

    }

}
