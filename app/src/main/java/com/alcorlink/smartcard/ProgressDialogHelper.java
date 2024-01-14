package com.alcorlink.smartcard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

class ProgressDialogHelper {
    private ProgressDialog mProgressDialog;
    private Activity mcontext;

    public ProgressDialogHelper() {

    }

    public ProgressDialogHelper(Activity context) {
        this.mcontext=context;
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setMessage("Loading");
        mProgressDialog.setCancelable(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);
    }
    public ProgressDialogHelper(Context context,  String message) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
    }

    public ProgressDialogHelper(Context context, String title, String message) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
    }

    public ProgressDialog getProgressDialog() {
        return mProgressDialog;
    }

    public void setProgressDialog(ProgressDialog mProgressDialog) {
        //make sure the previous dialog is hidden
        hide();
        this.mProgressDialog = mProgressDialog;
    }

    public void show() {
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    public void create(Context context, String title, String message) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = ProgressDialog.show(context, title, message);
    }

    private void hide() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
    public void dismiss(){
        mProgressDialog.dismiss();
        mProgressDialog = null;
//        if (mProgressDialog != null && mProgressDialog.isShowing()) {
//            mcontext.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mProgressDialog.dismiss();
//                }
//            });
//            mProgressDialog = null;
//        }
    }

    public void onDestroy() {
        hide();
    }
}