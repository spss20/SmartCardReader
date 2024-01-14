package com.alcorlink.smartcard;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


public class LogDialogFragment extends DialogFragment {
    private StringBuilder log = new StringBuilder();
    private BufferedReader mBbufferedReader;
    private TextView mLogTextView;
    private int mLogType;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Sets the Layout for the UI

        LayoutInflater i = getActivity().getLayoutInflater();
        View rootView = i.inflate(R.layout.log_fragment, null);
        mLogTextView   = (TextView) rootView.findViewById(R.id.logTextView);
        mLogTextView.setMovementMethod(new ScrollingMovementMethod());

        if (mLogType == MainActivity.LogTypeLogCat)
            setTextLogcat();
        else
            setText();
        // Creates and returns the AlertDialog for the logs
        AlertDialog.Builder dialogBuilder =  new  AlertDialog.Builder(getActivity())
                .setTitle("")
                .setNegativeButton(" CLOSE",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                ).setView(rootView);

        return dialogBuilder.create();
    }

    public void setInputStream(FileInputStream inputStream) {

        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        mBbufferedReader = new BufferedReader(inputStreamReader);
    }

    public void setLogType(int type) {
        mLogType = type;
    }

    private void setText() {

        try {
            StringBuilder log = new StringBuilder();
            String line;
            while ((line = mBbufferedReader.readLine()) != null) {
                //  log.append(line.substring(line.indexOf(": ") + 2)).append("\n");
                log.append(line).append("\n");
                //  Log.d("=======",line);
            }

            this.log.append(log.toString().replace(this.log.toString(), ""));
            mLogTextView.setText(this.log.toString());
            //mLogTextView.setText("1234");
            mBbufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTextLogcat() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line).append("\n");
                //  }
            }

            this.log.append(log.toString().replace(this.log.toString(), ""));
            mLogTextView.setText(this.log.toString());
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//	private String fileReadLine() {
//		String receiveString;
//	
//			
//			receiveString = mBbufferedReader.readLine();
////			while ( (receiveString = bufferedReader.readLine()) != null ) {
////				//stringBuilder.append(receiveString);
////
////			mInputStream.close();
//
//		return receiveString;
//	}
}
