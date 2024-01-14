package com.alcorlink.smartcard;


import amlib.ccid.Reader;
import amlib.ccid.ReaderException;
import amlib.ccid.SCError;
import amlib.hw.HWType;
import amlib.hw.HardwareInterface;
import amlib.hw.ReaderHwException;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class MifareS50_Activity extends AppCompatActivity {


    private String mStrMessage;
    private Reader mS50;
    private HardwareInterface mMyDev;
    private UsbDevice mUsbDev;
    private UsbManager mManager;
    private PendingIntent mPermissionIntent;
    private Context mAppCtx;
    private Builder mConfigsDialog;
    private Builder mWarnDialog;
    private Button mBtOpen;
    private Button mBtClose;
    private Button mBtConnect;
    private Button mBtGetID;
    private Button mBtWrite;
    private Button mBtRead;
    private Button mBtIncDec;
    private Button mBtAuth;

    private RadioButton mRadioInc;
    private RadioButton mRadioDec;
    private View mSettingsView;

    private LinearLayout mLoPurce;
    private LinearLayout mLoBlock;
    private LinearLayout mLoTrailer;
    private RelativeLayout mLoSelectBlock;
    private TextView mTextViewResult;
    private TextView mTextViewSpinnerBlockLabel;
    private TextView mTextViewBlockHex;

    private TextView mTextViewCurrentSector;
    private TextView mTextViewBlock0Hex;
    private TextView mTextViewBlock1Hex;
    private TextView mTextViewBlock2Hex;
    private TextView mTextViewBlock0Dec;
    private TextView mTextViewBlock1Dec;
    private TextView mTextViewBlock2Dec;

    private TextView mTextViewTrailerHexA;
    private TextView mTextViewTrailerHexB;
    private TextView mTextViewTrailerAccess;

    private CheckBox mCheckKeyA;
    private CheckBox mCheckKeyB;

    private EditText mEdtKeyA;
    private EditText mEdtKeyB;
    private EditText mEdtBlockDec;
    private EditText mEdtTrailer;
    private EditText mEdtTrailerKeyA;
    private EditText mEdtTrailerKeyB;
    private EditText mEdtPurceValue;

    private Spinner mSpinnerSector;
    private Spinner mSpinnerBlock;



    private ArrayAdapter<String> mAdapterSector;
    private ArrayAdapter<String> mAdapterBlock;

    private byte mCurrentOp;
    private byte mUsingKey;

    private ProgressDialog mWaitProgress;


    private static final String TAG = "Alcor-MifareS50";
    private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";
    protected static final boolean VERIFY_TYPE_WRITE = false;
    protected static final boolean VERIFY_TYPE_READ = true;

    protected static final byte OPERATION_WRITE = 0x01;
    protected static final byte OPERATION_READ = 0x03;
    protected static final byte OPERATION_AUTHENTIC = 0x04;
    protected static final byte OPERATION_INC_DEC = 0x05;
    protected static final byte OPERATION_GET_ID = 0x06;
    protected static final byte OPERATION_NONE = (byte) 0xff;//invalid

    protected static final byte USING_KEYA = (byte) 0x60;
    protected static final byte USING_KEYB = (byte) 0x61;
    protected static final byte USING_NONE = (byte) 0xFF;//invalid

    private static final int BYTES_OF_KEY = 6;
    private static final int BYTES_OF_ACCESS_MODE = 4;
    private static final int BLOCK_TRAILER = 0x3;
    private static final int BLOCK_SIZE = 16;
    private static final int PURCE_VALUE_SIZE = 4;
    private static final int ACK_SIZE = 2;
    private static final int GUID_LEN = 4;
    private static final int CMD_GETID_LEN = 5;
    private static final int CMD_READ_LEN = 5;
    private static final int CMD_WRITE_LEN = 5;
    private static final int CMD_PURCE_LEN = 5;
    private static final byte CMD_INC = (byte) 0xF1;
    private static final byte CMD_DEC = (byte) 0xF0;


    protected static final int EVENT_SET_RESULT = 221;
    protected static final int EVENT_CCID_RESULT = 222;
    protected static final int EVENT_ERROR_DIALOG = 223;
    private static final int EVENT_UPDATE_BLOCK = 224;
    private static final int EVENT_UPDATE_TRAILER = 225;

    private byte mSlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppCtx = this.getApplicationContext();
        mCurrentOp = OPERATION_NONE;
        setContentView(R.layout.activity_mifare_s50);

        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            mUsbDev = (UsbDevice) b.getParcelable(USB_SERVICE);
        }
        if (mUsbDev != null) {
            Log.d(TAG, "mdev=" + Integer.toHexString(mUsbDev.getProductId()));
        }
        setupViews();
        try {
            mMyDev = new HardwareInterface(HWType.eUSB, this.getApplicationContext());

        } catch (Exception e) {
            mStrMessage = "Get Exception : " + e.getMessage();
            Log.e(TAG, mStrMessage);
            return;
        }
        // Get USB manager
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        toRegisterReceiver();
        if (mUsbDev == null)
            mTextViewResult.setText("No Device been selected");
        setupHandler();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            try {
                closeReaderUp();
                closeReaderBottom();

            } catch (Exception e) {
                mStrMessage = "Get Exception : " + e.getMessage();
                //mTextViewResult.setText( mStrMessage);
            }
            Log.d(TAG, "finish");
            MifareS50_Activity.this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void OnClickMifareS50Open(View view) {
        requestDevPerm();
    }

    public void OnClickMifareS50Close(View view) {
        new CloseTask().execute();
    }

    public void OnClickMifareS50Connect(View view) {
        int ret;
        ret = poweron();
        if (ret == SCError.READER_SUCCESSFUL) {
            mTextViewResult.setText("power on successfully");
            String atr;

            atr = mS50.getAtrString();
            mTextViewResult.append("\nATR:" + atr);
        } else if (SCError.maskStatus(ret) == SCError.READER_NO_CARD) {
            mTextViewResult.setText("Card Absent");
        }
    }


    public void OnClickMifareWrite(View view) {
        mCurrentOp = OPERATION_WRITE;
        setUpOperationView("Write");
        mConfigsDialog.show();
    }

    public void OnClickMifareRead(View view) {
        mCurrentOp = OPERATION_READ;
        setUpOperationView("Read");
        mConfigsDialog.show();
    }

    public void OnClickMifareAuthenticate(View view) {
        mCurrentOp = OPERATION_AUTHENTIC;
        setUpOperationView("Authenticate");
        mConfigsDialog.show();
    }

    public void OnClickMifareIncreDecre(View view) {
        mCurrentOp = OPERATION_INC_DEC;
        setUpOperationView("Increase/Decrease");
        mConfigsDialog.show();
    }

    public void OnClickMifareGetID(View view) {
        mCurrentOp = OPERATION_GET_ID;
        new OperationTask().execute();
        setUpWaitDialog("Wait..");
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        if (mMyDev != null) {
            mMyDev.Close();
        }
        super.onDestroy();
    }

    private boolean checkCard() {
        int result;
        result = getSlotStatus();
        switch (result) {
            case SCError.READER_NO_CARD:
                showResult("Card Absent");
                return false;
            case SCError.READER_CARD_INACTIVE:
                result = mS50.setPower(Reader.CCID_POWERON);
                if (result == SCError.READER_SUCCESSFUL) {
                    return true;
                }
                showResult("Card is inactive");
                return false;
            case SCError.READER_SUCCESSFUL:
                return true;
            default://returns other error case
                return true;
        }
    }


    public void cleanText() {
        mTextViewResult.setText("");
    }

    private int closeReaderUp() {
        Log.d(TAG, "Closing reader...");
        int ret = 0;
        if (mS50 != null) {
            ret = mS50.close();
        }
        return ret;
    }

    private void closeReaderBottom() {
        onCloseButtonSetup();
        cleanText();
        mMyDev.Close();
    }

    private class CloseTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            setUpWaitDialog("Closing Reader");
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int status = 0;
			try {
                do {
                    status = closeReaderUp();
                } while (SCError.maskStatus(status) == SCError.READER_CMD_BUSY);

            } catch(Exception e){
                mStrMessage = "Get Exception : " + e.getMessage();
                return -1;
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != 0) {
                mTextViewResult.setText("Close fail: " + SCError.errorCode2String(result) + ". " + mStrMessage);
                Log.e(TAG, "Close fail: " + SCError.errorCode2String(result));
            } else {
                mTextViewResult.setText("Close successfully");
                Log.e(TAG, "Close successfully");
            }
            closeReaderBottom();
            mWaitProgress.dismiss();
        }
    }



    /*
     * It should return error when get invalid parameter from EditText
     * And update the result view
     */
    private int getBlockData(byte[] pByteHex, String[] pStringHex) {
        int value = 0;
        long l;
        Editable edt = mEdtBlockDec.getText();
        if (edt.length() == 0) {
            showErrorDialog("The block decimal value is empty");
            return -1;
        }
        l = (Long.valueOf(edt.toString()));

        if ((l & 0xffffffff00000000L) != 0) {
            showErrorDialog("The block decimal value is too large");
            return -1;
        }

        value = (int) (0x00000000ffffffffL & l);

        byte block = (byte) mSpinnerBlock.getSelectedItemPosition();
        byte sector = (byte) mSpinnerSector.getSelectedItemPosition();
        byte addr = (byte) (sector * 4 + block);
        value2BlockHex(value, addr, pByteHex);
        pStringHex[0] = MainActivity.byte2String(pByteHex, 16);
        return 0;
    }

    /*
     * length: number of max digits in this field
     */
    private int getEditTextDec(EditText edt, int[] pBuf, int length) {

        String strText;
        int len;
        if (edt.length() == 0) {
            //showErrorDialog("The block decimal value is empty");
            return -1;
        }
        strText = edt.getText().toString();
        len = strText.length();
        if (len > length || len == 0) {
            showResult("Wrong length in text field");
            showErrorDialog("Wrong length in text field");
            return -1;
        }
        pBuf[0] = Integer.parseInt(edt.getText().toString());
        return 0;
    }

	/*
     * length: number of max digits in this field
	 * */

    private int getEditTextHex(EditText edt, byte[] pBuf, int length, int[] pReturnLen) {
        byte[] pByteArrary;
        String strText;
        int len;
        if (edt.length() == 0) {
            //showErrorDialog("The edit field is empty");
            pReturnLen[0] = 0;
            return -1;
        }
        strText = edt.getText().toString();
        len = strText.length();

        if (len > length || len == 0 || (len % 2 != 0)) {
//			showResult("Wrong length in Key field"); 
//			showErrorDialog("Wrong length in Key field");
            return -1;
        }
        pByteArrary = MainActivity.toByteArray(strText);
        System.arraycopy(pByteArrary, 0, pBuf, 0, pByteArrary.length);
        pReturnLen[0] = pByteArrary.length;

        return 0;
    }


    private int getKeyFromDialog(byte pKey[]) {
        int[] pLen = new int[1];
        EditText edt = null;
        if (mCheckKeyA.isChecked() == true) {
            edt = mEdtKeyA;
            mUsingKey = USING_KEYA;
        } else if (mCheckKeyB.isChecked() == true) {
            edt = mEdtKeyB;
            mUsingKey = USING_KEYB;
        } else {
            showResult("Error:Key is not selected");
            showErrorDialog("Key is not selected");
            return -1;
        }

        getEditTextHex(edt, pKey, BYTES_OF_KEY * 2, pLen);
        if (pLen[0] != BYTES_OF_KEY) {
            showResult("Wrong length in Key field");
            showErrorDialog("Wrong length in Key field");
            return -1;
        }
        //String s = MainActivity.logBuffer(pKey, pLen[0]);

        return 0;
    }

    private int getPurcekData(byte[] pByteHex, String[] pString) {
        int value;
        long l;
        Editable edt = mEdtPurceValue.getText();
        if (edt.length() == 0) {
            showErrorDialog("The purce decimal value is empty");
            return -1;
        }
        l = (Long.valueOf(edt.toString()));
        if ((l & 0xffffffff00000000L) != 0) {
            showErrorDialog("The purce decimal value is too large");
            return -1;
        }

        value = (int) (0x00000000ffffffffL & l);
        pByteHex[0] = (byte) (value & 0x000000ff);
        pByteHex[1] = (byte) ((value & 0x0000ff00) >> 8);
        pByteHex[2] = (byte) ((value & 0x00ff0000) >> 16);
        pByteHex[3] = (byte) ((value & 0xff000000) >> 24);
        pString[0] = MainActivity.byte2String(pByteHex, 4);
        return 0;
    }


    private int getSlotStatus() {
        int ret = SCError.READER_NO_CARD;
        byte[] pCardStatus = new byte[1];

        ret = mS50.getCardStatus(pCardStatus);
        if (ret == SCError.READER_SUCCESSFUL) {
            if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_ABSENT) {
                ret = SCError.READER_NO_CARD;
            } else if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_INACTIVE) {
                ret = SCError.READER_CARD_INACTIVE;
            } else
                ret = SCError.READER_SUCCESSFUL;
        }

        return ret;
    }


    private int getTrailerAccessModeFromDialog(byte pMode[]) {
        int[] pLen = new int[1];

        getEditTextHex(mEdtTrailer, pMode, BYTES_OF_ACCESS_MODE * 2, pLen);
        if (pLen[0] != BYTES_OF_ACCESS_MODE) {
            showResult("Wrong length in Trailer Access field");
            showErrorDialog("Wrong length in Trailer Access field");
            return -1;
        }

        return 0;
    }

    private int getTrailerHexFromDialog(byte pKeyA[], byte pKeyB[]) {
        int[] pLen = new int[1];

        getEditTextHex(mEdtTrailerKeyA, pKeyA, BYTES_OF_KEY * 2, pLen);
        if (pLen[0] != BYTES_OF_KEY) {
            showResult("Wrong length in Trailer KeyA field");
            showErrorDialog("Wrong length in Trailer KeyA field");
            return -1;
        }
        getEditTextHex(mEdtTrailerKeyB, pKeyB, BYTES_OF_KEY * 2, pLen);
        if (pLen[0] != BYTES_OF_KEY) {
            showResult("Wrong length in Trailer KeyB field");
            showErrorDialog("Wrong length in Trailer KeyB field");
            return -1;
        }
        return 0;
    }

    private boolean isTwoInterface(int pid) {
        boolean ret = false;
     switch (pid)
     {
         case 0x9573:
             ret = true;
             break;
         default:
             ret = false;
             break;
     }
     return ret;
    }

    private int InitReader() {
        int Status = 0;
        boolean init;//
        boolean isTwoInterface = isTwoInterface( mUsbDev.getProductId());
        Log.d(TAG, "InitReader");
        try {
            if (isTwoInterface == true ) //9573 has two interfaces
                init = mMyDev.initNFC(mManager, mUsbDev);
            else
                init = mMyDev.Init(mManager, mUsbDev);
            if (!init) {
                Log.e(TAG, "Device init fail");
                return -1;
            }
        } catch (ReaderHwException e) {
            mStrMessage = "Get Exception : " + e.getMessage();
            return -1;
        }


        try {
            mS50 = new Reader(mMyDev);
            if (isTwoInterface == true )
                mSlot = 0;
            else
                mSlot = 1;

            Status = mS50.open(mSlot);
        } catch (ReaderException e) {
            mStrMessage = "Get Exception : " + e.getMessage();
            Log.e(TAG, "InitReader fail " + mStrMessage);
            return -1;
        } catch (IllegalArgumentException e) {
            mStrMessage = "Get Exception : " + e.getMessage();
            Log.e(TAG, "InitReader fail " + mStrMessage);
            return -1;
        }

        return Status;
    }

    private boolean isTrailerValid(byte[] pCondition) {
        int v, vb;
        v = (pCondition[1] & 0x000000f0) >> 4; //C1
        vb = (~pCondition[0]) & 0x0000000f; //C1 inverse
        if (v != vb)
            return false;

        v = (pCondition[2] & 0x000000f0) >> 4; //C3
        vb = (~pCondition[1]) & 0x0000000f; //C3 inverse
        if (v != vb)
            return false;

        v = (pCondition[2] & 0x0000000f); //C2
        vb = ((~pCondition[0]) & 0x000000f0) >> 4; //C2 inverse
        if (v != vb)
            return false;
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast Receiver");

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            onDevPermit(device);
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device " + device.getDeviceName());
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                Log.d(TAG, "Device Detached");
                onDetach(intent);

                synchronized (this) {
                }
            }
        }/*end of onReceive(Context context, Intent intent) {*/
    };
    private Handler mMessageHandler;

    private class OpenTask extends AsyncTask<UsbDevice, Void, Integer> {

        @Override
        protected Integer doInBackground(UsbDevice... params) {
            int status = 0;
            status = InitReader();
            if (status != 0) {
                Log.e(TAG, "fail to initial reader");
                return status;
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mWaitProgress.dismiss();
            if (result != 0) {
                mTextViewResult.setText("Open fail: " + SCError.errorCode2String(result) + ". " + mStrMessage);
                Log.e(TAG, "Open fail: " + SCError.errorCode2String(result) + ". " + mStrMessage);
            } else {
                mTextViewResult.setText("Open successfully");
                Log.e(TAG, "Open successfully");
                onOpenButtonSetup();
            }
        }
    }

    private class OperationTask extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... opration) {
            int status = 0;
            switch (mCurrentOp) {
                case OPERATION_WRITE:
                    opWrite();
                    break;
                case OPERATION_READ:
                    opRead();
                    break;
                case OPERATION_AUTHENTIC:
                    opAuth();
                    break;
                case OPERATION_INC_DEC:
                    opIncDec();
                    break;
                case OPERATION_GET_ID:
                    opGetID();
                    break;
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mWaitProgress.dismiss();
            mCurrentOp = OPERATION_NONE;
            if (result != 0) {
                Log.e(TAG, "Open fail: " + SCError.errorCode2String(result) + ". " + mStrMessage);
            } else {
                Log.e(TAG, "Open successfully");
            }
        }
    }

    private void onCreateButtonSetup() {
        if (mUsbDev != null)
            mBtOpen.setEnabled(true);
        else
            mBtOpen.setEnabled(false);
        mBtClose.setEnabled(false);
        mBtConnect.setEnabled(false);
        mBtGetID.setEnabled(false);
        mBtWrite.setEnabled(false);
        mBtRead.setEnabled(false);
        mBtIncDec.setEnabled(false);
        mBtAuth.setEnabled(false);

    }


    private void onCloseButtonSetup() {
        onCreateButtonSetup();
    }


    private void onDevPermit(UsbDevice dev) {
        try {
            setUpWaitDialog("Opening...");
            new OpenTask().execute(dev);
        } catch (Exception e) {
            mStrMessage = "Get Exception : " + e.getMessage();
            Log.e(TAG, mStrMessage);
        }
    }


    private void onDetach(Intent intent) {
        UsbDevice udev = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (udev != null) {
            if (udev.equals(mUsbDev)) {
                closeReaderUp();
                closeReaderBottom();
                MifareS50_Activity.this.finish();
            }
        } else {
            Log.d(TAG, "usb device is null");
        }
    }


    private void opWrite() {
        int ret = 0;
        byte[] pKeyByte = new byte[BYTES_OF_KEY];
        byte[] pHex = new byte[16];
        int block;
        int sector;
        byte address;
        ret = getKeyFromDialog(pKeyByte);
        if (ret != 0)
            return;
        block = mSpinnerBlock.getSelectedItemPosition();
        sector = mSpinnerSector.getSelectedItemPosition();
        if (block == BLOCK_TRAILER) {
            byte[] pKeyAByte = new byte[BYTES_OF_KEY];
            byte[] pKeyBByte = new byte[BYTES_OF_KEY];
            byte[] pMode = new byte[BYTES_OF_ACCESS_MODE];
            ret = getTrailerAccessModeFromDialog(pMode);
            if (ret != 0)
                return;
            if (isTrailerValid(pMode) == false) {
                showResult("Invalid Condition for Trailer");
                showErrorDialog("Invalid Condition for Trailer");
            }

            ret = getTrailerHexFromDialog(pKeyAByte, pKeyBByte);
            if (ret != 0)
                return;
            System.arraycopy(pKeyAByte, 0, pHex, 0, BYTES_OF_KEY);
            System.arraycopy(pMode, 0, pHex, BYTES_OF_KEY, BYTES_OF_ACCESS_MODE);
            System.arraycopy(pKeyBByte, 0, pHex, BYTES_OF_KEY + BYTES_OF_ACCESS_MODE, BYTES_OF_KEY);

        } else {
            String[] pStringHex = new String[1];
            ret = getBlockData(pHex, pStringHex);
            if (ret != 0)
                return;
        }

        if (checkCard() == false)
            return;
        address = (byte) (sector * 4 + block);
        ret = sendAuthoCmd(address, pKeyByte, mUsingKey);/*address can be any within this sector*/
        if (ret != 0)
            return;
        // send write cmd
        ret = sendWriteBlockCmd(address, pHex);

    }

    private void opRead() {
        int ret = 0;
        byte[] pKeyByte = new byte[BYTES_OF_KEY + 2];
        ret = getKeyFromDialog(pKeyByte);
        if (ret != 0)
            return;
        byte sector = (byte) mSpinnerSector.getSelectedItemPosition();

        if (checkCard() == false)
            return;
        ret = sendAuthoCmd((byte) (sector * 4 + 0), pKeyByte, mUsingKey);/*address can be any within this sector*/
        if (ret != 0)
            return;
        byte[] pBlockData = new byte[BLOCK_SIZE + ACK_SIZE];

        ret = sendReadBlockCmd((byte) (sector * 4 + 0), pBlockData);
        if (ret == 0) {
            showResult("Read Command Success");
            updateBlockInfo(0, pBlockData);
        }

        pBlockData = new byte[BLOCK_SIZE + ACK_SIZE];//allocate a new clean one.
        ret = sendReadBlockCmd((byte) (sector * 4 + 1), pBlockData);
        if (ret == 0) {
            showResult("Read Command Success");
            updateBlockInfo(1, pBlockData);
        }

        pBlockData = new byte[BLOCK_SIZE + ACK_SIZE];//allocate a new clean one.
        ret = sendReadBlockCmd((byte) (sector * 4 + 2), pBlockData);
        if (ret == 0) {
            showResult("Read Command Success");
            updateBlockInfo(2, pBlockData);
        }

        pBlockData = new byte[BLOCK_SIZE + ACK_SIZE];//allocate a new clean one.
        ret = sendReadBlockCmd((byte) (sector * 4 + 3), pBlockData);
        if (ret == 0) {
            showResult("Read Command Success");
            updateTrailerInfo(pBlockData);
        }

    }


    private void opAuth() {
        int ret = 0;
        byte[] pKeyByte = new byte[BYTES_OF_KEY];

        Log.d(TAG, "opAuth");
        ret = getKeyFromDialog(pKeyByte);
        if (ret != 0)
            return;
        byte block = (byte) mSpinnerBlock.getSelectedItemPosition();
        byte sector = (byte) mSpinnerSector.getSelectedItemPosition();
        byte addr = (byte) (sector * 4 + block);

        if (checkCard() == false)
            return;
        ret = sendAuthoCmd(addr, pKeyByte, mUsingKey);
        if (ret == 0)
            showResult("Authentication Command Success");
    }

    private void opGetID() {
        int ret = 0;
        byte[] cmd = new byte[CMD_GETID_LEN];
        byte[] pRxBuf = new byte[GUID_LEN + ACK_SIZE];
        //byte []rxBuf = new byte[BLOCK_SIZE+ACK_SIZE];
        int[] pRxLen = new int[1];

        if (checkCard() == false)
            return;

        cmd[0] = (byte) 0x00;
        cmd[1] = (byte) 0xCA;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = 0x00;

        pRxLen[0] = CMD_GETID_LEN + ACK_SIZE;
        ret = mS50.transmit(cmd, CMD_GETID_LEN, pRxBuf, pRxLen);
        if (ret == 0) {
            if (pRxBuf[GUID_LEN] == (byte) 0x90 && pRxBuf[GUID_LEN + 1] == 0x00) {
                String s = MainActivity.byte2String(pRxBuf, GUID_LEN);
                showResult("GUID:" + s);
                return;
            } else {
                String s = new String(Integer.toHexString(0x000000ff & pRxBuf[0]) + " " + Integer.toHexString(0x000000ff & pRxBuf[1]));
                showResult("GUID Command Fail " + s + "(Hex)");
                return;
            }
        }
        showResult(ret);
        return;
    }

    private void opIncDec() {
        int ret = 0;
        byte[] pKeyByte = new byte[BYTES_OF_KEY];
        int block;
        int sector;
        byte address;
        byte opCode = 0;
        ret = getKeyFromDialog(pKeyByte);
        if (ret != 0)
            return;
        block = mSpinnerBlock.getSelectedItemPosition();
        sector = mSpinnerSector.getSelectedItemPosition();
        if (block == BLOCK_TRAILER) {
            showErrorDialog("This operation cannot be applied to Trailer block");
        }

        if (mRadioInc.isChecked() == true) {
            opCode = CMD_INC;
        } else if (mRadioDec.isChecked() == true) {
            opCode = CMD_DEC;
        }

        byte[] pDataHex = new byte[4];
        String[] pStringHex = new String[1];
        ret = getPurcekData(pDataHex, pStringHex);
        if (ret != 0)
            return;
        if (checkCard() == false)
            return;

        address = (byte) (sector * 4 + block);
        ret = sendAuthoCmd(address, pKeyByte, mUsingKey);/*address can be any within this sector*/
        if (ret != 0)
            return;
        ret = sendPurceCmd(address, pDataHex, opCode);
        if (ret != 0)
            return;
    }

    private void onOpenButtonSetup() {
        mBtOpen.setEnabled(false);
        mBtClose.setEnabled(true);
        mBtConnect.setEnabled(true);
        mBtGetID.setEnabled(true);
        mBtWrite.setEnabled(true);
        mBtRead.setEnabled(true);
        mBtIncDec.setEnabled(true);
        mBtAuth.setEnabled(true);
    }

    private int poweron() {
        int result = SCError.READER_SUCCESSFUL;
        Log.d(TAG, "poweron");
        //check slot status first
        result = getSlotStatus();
        switch (result) {
            case SCError.READER_NO_CARD:
                mTextViewResult.setText("Card Absent");
                Log.d(TAG, "Card Absent");
                return SCError.READER_NO_CARD;
            case SCError.READER_CARD_INACTIVE:
            case SCError.READER_SUCCESSFUL:
                break;
            default://returns other error case
                return result;
        }

        result = mS50.setPower(Reader.CCID_POWERON);
        Log.d(TAG, "power on exit");
        return result;
    }


    private int poweroff() {
        int result = SCError.READER_SUCCESSFUL;
        Log.d(TAG, "poweroff");
        result = getSlotStatus();
        switch (result) {
            case SCError.READER_NO_CARD:
                mTextViewResult.setText("Card Absent");
                Log.d(TAG, "Card Absent");
                return SCError.READER_NO_CARD;
            case SCError.READER_CARD_INACTIVE:
            case SCError.READER_SUCCESSFUL:
                break;
            default://returns other error case
                return result;
        }
        //----------poweroff card------------------
        result = mS50.setPower(Reader.CCID_POWEROFF);

        return result;
    }

    private void requestDevPerm() {
        if (mUsbDev != null)
            mManager.requestPermission(mUsbDev, mPermissionIntent);
        else {
            Log.e(TAG, "selected not found");
            mTextViewResult.setText("Device not found");
        }
    }

    private int sendAuthoCmd(byte address, byte[] pKey, byte keyType) {
        int ret = 0;
        byte[] pKeyByte = new byte[BYTES_OF_KEY];
        byte[] cmd = new byte[11];
        byte[] rxBuf = new byte[10];
        int[] pRxLen = new int[1];

        Log.d(TAG, "sendAuthoCmd");
		/* load keyA 6f 0b 00 00 00 01 seq- FF 82 00 00 06  key(pKeyByte)*/
		/*get 90 00*/
        cmd[0] = (byte) 0x00;
        cmd[1] = (byte) 0x82;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = BYTES_OF_KEY;
        System.arraycopy(pKey, 0, cmd, 5, BYTES_OF_KEY);
        Log.d(TAG, "sendAuthoCmd load key");
        ret = mS50.transmit(cmd, BYTES_OF_KEY + 5, rxBuf, pRxLen);
        if (ret != 0) {
            showResult(ret);
            return ret;
        }
        if (rxBuf[0] != (byte) 0x90 || rxBuf[1] != 0x00) {
            String s = new String(Integer.toHexString(0x000000ff & rxBuf[0]) + " " + Integer.toHexString(0x000000ff & rxBuf[1]));
            showResult("Authentic Command Load Key Fail " + s + "(Hex)");
            return -1;
        }
		/*verify key A addr:addr 6f 0a 00 00 00 01 seq- FF 86 00 00 05 01 00 'addr' 60 00 */
		/*get 90 00*/
        cmd = null;
        cmd = new byte[10];
        rxBuf = null;
        rxBuf = new byte[12];


        cmd[0] = (byte) 0x00;
        cmd[1] = (byte) 0x86;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = 0x05;
        cmd[5] = 0x01;
        cmd[6] = 0x00;
        cmd[7] = address;
        cmd[8] = keyType;
        cmd[6] = 0x00;

        Log.d(TAG, "sendAuthoCmd verify");
        ret = mS50.transmit(cmd, 10, rxBuf, pRxLen);
        if (ret != 0) {
            showResult(ret);
            return ret;
        }
        if (rxBuf[0] == (byte) 0x90 && rxBuf[1] == 0x00) {
            return 0;
        }
        String s = new String(Integer.toHexString(0x000000ff & rxBuf[0]) + " " + Integer.toHexString(0x000000ff & rxBuf[1]));
        showResult("Authentic Command Fail " + s + "(Hex)");
        return -1;
    }

    private int sendPurceCmd(byte address, byte[] pValue, byte opCode) {
        int ret = 0;
        byte[] cmd = new byte[CMD_PURCE_LEN + PURCE_VALUE_SIZE];
        byte[] pRxBuf = new byte[ACK_SIZE];
        int[] pRxLen = new int[1];

        cmd[0] = (byte) 0x00;
        cmd[1] = opCode;
        cmd[2] = 0x00;
        cmd[3] = address;
        cmd[4] = PURCE_VALUE_SIZE;
        System.arraycopy(pValue, 0, cmd, CMD_PURCE_LEN, PURCE_VALUE_SIZE);
        pRxLen[0] = ACK_SIZE;

        ret = mS50.transmit(cmd, CMD_PURCE_LEN + PURCE_VALUE_SIZE, pRxBuf, pRxLen);
        if (ret == 0) {
            if (pRxBuf[0] == (byte) 0x90 && pRxBuf[1] == 0x00) {
                showResult("Purce Command Success");
                return ret;
            } else {
                String s = new String(Integer.toHexString(0x000000ff & pRxBuf[0]) + " " + Integer.toHexString(0x000000ff & pRxBuf[1]));
                showResult("Purce Command Fail " + s + "(Hex)");
                return -1;
            }
        }
        showResult(ret);
        return ret;

    }

    private int sendReadBlockCmd(byte address, byte[] pRxBuf) {
        int ret = 0;
        byte[] cmd = new byte[CMD_READ_LEN];

        int[] pRxLen = new int[1];

        cmd[0] = (byte) 0x00;
        cmd[1] = (byte) 0xB0;
        cmd[2] = 0x00;
        cmd[3] = address;
        cmd[4] = BLOCK_SIZE;
        pRxLen[0] = BLOCK_SIZE + ACK_SIZE;
        ret = mS50.transmit(cmd, CMD_READ_LEN, pRxBuf, pRxLen);
        if (ret == 0) {
            if (pRxBuf[BLOCK_SIZE] == (byte) 0x90 && pRxBuf[BLOCK_SIZE + 1] == 0x00) {
                showResult("Read Command Success");
                return ret;
            } else {
                String s = new String(Integer.toHexString(0x000000ff & pRxBuf[0]) + " " + Integer.toHexString(0x000000ff & pRxBuf[1]));
                showResult("Read Command Fail " + s + "(Hex)");
                return -1;
            }
        } else {
        }
        showResult(ret);

        return ret;
    }

    private int sendWriteBlockCmd(byte address, byte[] pData) {
        int ret = 0;
        byte[] cmd = new byte[CMD_WRITE_LEN + BLOCK_SIZE];

        byte[] pRxBuf = new byte[BLOCK_SIZE + ACK_SIZE];
        int[] pRxLen = new int[1];

        cmd[0] = (byte) 0x00;
        cmd[1] = (byte) 0xD6;
        cmd[2] = 0x00;
        cmd[3] = address;
        cmd[4] = BLOCK_SIZE;
        System.arraycopy(pData, 0, cmd, CMD_WRITE_LEN, BLOCK_SIZE);

        pRxLen[0] = ACK_SIZE;

        ret = mS50.transmit(cmd, CMD_WRITE_LEN + BLOCK_SIZE, pRxBuf, pRxLen);
        if (ret == 0) {
            if (pRxBuf[0] == (byte) 0x90 && pRxBuf[1] == 0x00) {
                showResult("Write Command Success");
                return ret;
            } else {
                String s = new String(Integer.toHexString(0x000000ff & pRxBuf[0]) + " " + Integer.toHexString(0x000000ff & pRxBuf[1]));
                showResult("Write Command Fail " + s + "(Hex)");
                return -1;
            }
        }
        showResult(ret);
        return ret;

    }


    private void setButtonView() {
        mBtOpen = (Button) findViewById(R.id.MifareS50buttonOpen);
        mBtClose = (Button) findViewById(R.id.MifareS50buttonClose);
        mBtConnect = (Button) findViewById(R.id.MifareS50buttonPower);

        mBtGetID = (Button) findViewById(R.id.MifareGetID);
        mBtWrite = (Button) findViewById(R.id.MifareWrite);
        mBtRead = (Button) findViewById(R.id.MifareRead);
        mBtIncDec = (Button) findViewById(R.id.MifareIncrementDecrement);
        mBtAuth = (Button) findViewById(R.id.MifareAuthenticate);

    }

    private void setEditView() {

    }


    private void setTextView() {
        mTextViewCurrentSector = (TextView) findViewById(R.id.textCurrentSector);
        mTextViewBlock0Hex = (TextView) findViewById(R.id.textBlock0DataHex);
        mTextViewBlock1Hex = (TextView) findViewById(R.id.textBlock1DataHex);
        mTextViewBlock2Hex = (TextView) findViewById(R.id.textBlock2DataHex);

        mTextViewBlock0Dec = (TextView) findViewById(R.id.MifareTextBlock0DataDec);
        mTextViewBlock1Dec = (TextView) findViewById(R.id.MifareTextBlock1DataDec);
        mTextViewBlock2Dec = (TextView) findViewById(R.id.MifareTextBlock2DataDec);

        mTextViewTrailerHexA = (TextView) findViewById(R.id.textBlock3KeyAHex);
        mTextViewTrailerHexB = (TextView) findViewById(R.id.textBlock3KeyBHex);
        mTextViewTrailerAccess = (TextView) findViewById(R.id.MifareEditTextBlock3DataAccessMode);
        mTextViewResult = (TextView) findViewById(R.id.MifareS50TextResult);
    }

    private void setupHandler() {
        mMessageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                String msg;
                byte[] pData;
                switch (message.what) {
                    case EVENT_SET_RESULT:
                        msg = message.getData().getString("RESULT");
                        mTextViewResult.setText(msg);
                        break;
                    case EVENT_ERROR_DIALOG:
                        msg = message.getData().getString("ERROR");
                        mWarnDialog.setMessage(msg);
                        mWarnDialog.show();
                        break;
                    case EVENT_UPDATE_BLOCK:
                        TextView tvHex = null;
                        TextView tvDec = null;
                        int block = message.getData().getInt("BLOCK_NUM");
                        switch (block) {
                            case 0:
                                tvHex = mTextViewBlock0Hex;
                                tvDec = mTextViewBlock0Dec;
                                break;
                            case 1:
                                tvHex = mTextViewBlock1Hex;
                                tvDec = mTextViewBlock1Dec;
                                break;
                            case 2:
                                tvHex = mTextViewBlock2Hex;
                                tvDec = mTextViewBlock2Dec;
                                break;

                        }
                        pData = message.getData().getByteArray("BLOCK_DATA");

                        int v;
                        msg = MainActivity.byte2String(pData, BLOCK_SIZE);
                        tvHex.setText(msg);
                        v = pData[0] |
                                (pData[1] & 0x000000ff) << 8 |
                                (pData[2] & 0x000000ff) << 16 |
                                (pData[3] & 0x000000ff) << 24;
                        tvDec.setText(Integer.toString(v));
                        break;
                    case EVENT_UPDATE_TRAILER:
                        pData = message.getData().getByteArray("BLOCK_DATA");
                        msg = MainActivity.byte2String(pData, BYTES_OF_KEY);
                        mTextViewTrailerHexA.setText(msg);

                        byte[] tmp = new byte[BYTES_OF_KEY];

                        System.arraycopy(pData, BYTES_OF_KEY, tmp, 0, BYTES_OF_ACCESS_MODE);
                        msg = MainActivity.byte2String(tmp, BYTES_OF_ACCESS_MODE);
                        mTextViewTrailerAccess.setText(msg);

                        System.arraycopy(pData, BYTES_OF_KEY + BYTES_OF_ACCESS_MODE, tmp, 0, BYTES_OF_KEY);
                        msg = MainActivity.byte2String(tmp, BYTES_OF_KEY);
                        mTextViewTrailerHexB.setText(msg);
                        break;

                    case EVENT_CCID_RESULT:
                        int result = message.getData().getInt("CCID_RESULT");
                        if (result == SCError.READER_SUCCESSFUL) {
                            mTextViewResult.setText("Success");
                        } else {
                            mTextViewResult.setText("Fail:" + SCError.errorCode2String(result) + "(" + Integer.toHexString(result) + ")");
                        }
                }
            }
        };
    }

    public void setupOperationDisplay() {
        switch (mCurrentOp) {
            case OPERATION_WRITE:
                mLoPurce.setVisibility(View.GONE);
                mLoBlock.setVisibility(View.GONE);
                mEdtBlockDec.setEnabled(true);
                break;
            case OPERATION_READ:
                mLoPurce.setVisibility(View.GONE);
                mLoBlock.setVisibility(View.GONE);
                mLoTrailer.setVisibility(View.GONE);
                mTextViewSpinnerBlockLabel.setVisibility(View.INVISIBLE);
                break;
            case OPERATION_AUTHENTIC:
                mLoPurce.setVisibility(View.GONE);
                mLoBlock.setVisibility(View.GONE);
                mLoTrailer.setVisibility(View.GONE);
                //mTextViewSpinnerBlockLabel.setVisibility(View.GONE);
                break;
            case OPERATION_INC_DEC:
                mLoBlock.setVisibility(View.GONE);
                mLoTrailer.setVisibility(View.GONE);
                mLoPurce.setVisibility(View.VISIBLE);
                mEdtBlockDec.setEnabled(false);

                break;
        }
    }

    private int setUpOperationView(String sOp) {

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        mSettingsView = inflater.inflate(R.layout.mifare_s50, (ViewGroup) findViewById(R.id.mifare_s50_config));
        mConfigsDialog = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                new OperationTask().execute();
                setUpWaitDialog("Wait..");
            }
        };
        mConfigsDialog.setTitle(sOp);
        mConfigsDialog.setPositiveButton("OK", OkClick);
        mConfigsDialog.setView(mSettingsView);

        setUpOperationView2(mSettingsView);
        return 0;
    }

    public int setUpOperationView2(View view) {

        mLoPurce = (LinearLayout) view.findViewById(R.id.MifS50LinearPurceLabel);
        mLoBlock = (LinearLayout) view.findViewById(R.id.MifS50LinearBlockLabel);
        mLoTrailer = (LinearLayout) view.findViewById(R.id.MifS50LinearTrailerLabel);

        mTextViewSpinnerBlockLabel = (TextView) view.findViewById(R.id.MifS50textSpinnerBlockLabel);
        mTextViewBlockHex = (TextView) view.findViewById(R.id.MifS50TextBlockHex);

        mCheckKeyA = (CheckBox) view.findViewById(R.id.MifS50CheckBoxA);
        mCheckKeyB = (CheckBox) view.findViewById(R.id.MifS50CheckBoxB);

        mRadioInc = (RadioButton) view.findViewById(R.id.radioIncrease);
        mRadioDec = (RadioButton) view.findViewById(R.id.radioDecrease);

        mEdtKeyA = (EditText) view.findViewById(R.id.MifS50EditTextKeyA);
        mEdtKeyB = (EditText) view.findViewById(R.id.MifS50EditTextKeyB);
        mEdtBlockDec = (EditText) view.findViewById(R.id.MifS50EditTextBlockDataDec);
        mEdtTrailer = (EditText) view.findViewById(R.id.MifS50EditTextBlock3DataMode);
        mEdtTrailerKeyA = (EditText) view.findViewById(R.id.MifS50EditTextBlock3KeyAHex);
        mEdtTrailerKeyB = (EditText) view.findViewById(R.id.MifS50EditTextBlock3KeyBHex);
        mEdtPurceValue = (EditText) view.findViewById(R.id.editTextPurce);
        mEdtBlockDec.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDialogBlockData();
            }
        });


        mCheckKeyA.setChecked(false);
        mCheckKeyB.setChecked(false);
        mEdtKeyA.setEnabled(false);
        mEdtKeyB.setEnabled(false);

        setupOperationDisplay();
        mCheckKeyA.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //mCheckKeyA.setChecked(true);
                if (mCheckKeyA.isChecked() == true) {
                    mCheckKeyB.setChecked(false);
                    mEdtKeyA.setEnabled(true);
                    mEdtKeyB.setEnabled(false);
                } else {
                    mEdtKeyA.setEnabled(false);
                }

            }
        });
        mCheckKeyB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //mCheckKeyA.setChecked(true);
                if (mCheckKeyB.isChecked() == true) {
                    mCheckKeyA.setChecked(false);
                    mEdtKeyA.setEnabled(false);
                    mEdtKeyB.setEnabled(true);
                } else {
                    mEdtKeyB.setEnabled(false);
                }
            }
        });
        setupSpinner(view);
        return 0;
    }


    private void setupSpinner(View view) {
        String[] sector = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};

        String[] block;

        // Initialize  spinner
        mAdapterSector = new ArrayAdapter<String>(mAppCtx,
                R.layout.myspinner, sector);
        mLoSelectBlock = (RelativeLayout) view.findViewById(R.id.MifiS50LayoutSelectBlock);
        mSpinnerSector = (Spinner) view.findViewById(R.id.MifS50SpinnerSector);
        if (mSpinnerSector == null)
            Log.d(TAG, "mSpinnerSector is null");
        if (mAdapterSector == null)
            Log.d(TAG, "mAdapterSector is null");
        mSpinnerSector.setAdapter(mAdapterSector);
        mSpinnerSector.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mTextViewCurrentSector.setText("Current Sector: " + Integer.toString(position));
                if (mCurrentOp == OPERATION_WRITE)
                    updateDialogBlockData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });


        if (mCurrentOp == OPERATION_WRITE || mCurrentOp == OPERATION_AUTHENTIC)
            block = new String[]{"0", "1", "2", "trailer"};
        else
            block = new String[]{"0", "1", "2"};

        mAdapterBlock = new ArrayAdapter<String>(this,
                R.layout.myspinner, block);
        mSpinnerBlock = (Spinner) view.findViewById(R.id.MifS50SpinnerBlock);

        mSpinnerBlock.setAdapter(mAdapterBlock);
        mSpinnerBlock.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (mCurrentOp == OPERATION_AUTHENTIC || mCurrentOp == OPERATION_INC_DEC) {
                    return;
                }
                if (mCurrentOp == OPERATION_READ) {
                    mLoSelectBlock.setVisibility(View.INVISIBLE);
                    mSpinnerBlock.setVisibility(View.INVISIBLE);
                    return;
                }
                if (mCurrentOp == OPERATION_WRITE)
                    updateDialogBlockData();
                int block = mSpinnerBlock.getSelectedItemPosition();
                switch (block) {
                    case 0:
                    case 1:
                    case 2:
                        mLoBlock.setVisibility(View.VISIBLE);
                        mLoTrailer.setVisibility(View.GONE);
                        updateDialogBlockData();
                        break;
                    case 3:
                        mLoBlock.setVisibility(View.GONE);
                        mLoTrailer.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });
    }


    public void setupViews() {
        Log.d(TAG, "setupViews");
        setButtonView();
        setTextView();
        setEditView();
        setUpWarnDialog();
      ;
        onCreateButtonSetup();
        mWaitProgress = new ProgressDialog(MifareS50_Activity.this);
    }

    private void setUpWaitDialog(String msg) {

        mWaitProgress.setMessage(msg);
        mWaitProgress.setCancelable(false);
        mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitProgress.show();
    }

    private void setUpWarnDialog() {

        mWarnDialog = new AlertDialog.Builder(this);

        DialogInterface.OnClickListener errorClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        };
        mWarnDialog.setTitle("Error!!");
        mWarnDialog.setPositiveButton("OK", errorClick);
        mWarnDialog.setCancelable(true);
    }

    private void showBufResult(int len, byte[] pBuf) {
        String s;
        s = MainActivity.byte2String(pBuf, len);
        mTextViewResult.setText(s);
    }

    private void showErrorDialog(String s) {
        Bundle countBundle = new Bundle();
        Message msg = new Message();
        msg.what = EVENT_ERROR_DIALOG;
        countBundle.putString("ERROR", s);
        msg.setData(countBundle);
        mMessageHandler.sendMessage(msg);
    }

    private void showResult(String s) {

        Bundle countBundle = new Bundle();
        Message msg = new Message();
        msg.what = EVENT_SET_RESULT;
        countBundle.putString("RESULT", s);
        msg.setData(countBundle);
        //Toast.makeText(this.getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
        mMessageHandler.sendMessage(msg);
    }

    private void showResult(int result) {
        Bundle countBundle = new Bundle();
        Message msg = new Message();
        msg.what = EVENT_CCID_RESULT;
        countBundle.putInt("CCID_RESULT", result);
        msg.setData(countBundle);
        //Toast.makeText(this.getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
        mMessageHandler.sendMessage(msg);

    }

    private void updateDialogBlockData() {
        int ret = 0;
        byte[] hex = new byte[16];
        String[] dataHex = new String[1];
        ret = getBlockData(hex, dataHex);
        if (ret != 0)
            return;
        mTextViewBlockHex.setText(dataHex[0]);
    }


    private void updateBlockInfo(int block, byte[] pData) {

        Bundle countBundle = new Bundle();
        Message msg = new Message();
        msg.what = EVENT_UPDATE_BLOCK;
        countBundle.putInt("BLOCK_NUM", block);
        countBundle.putByteArray("BLOCK_DATA", pData);
        msg.setData(countBundle);
        //Toast.makeText(this.getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
        mMessageHandler.sendMessage(msg);

    }

    private void updateTrailerInfo(byte[] pData) {
        Bundle countBundle = new Bundle();
        Message msg = new Message();
        msg.what = EVENT_UPDATE_TRAILER;
        countBundle.putInt("BLOCK_NUM", 3);
        countBundle.putByteArray("BLOCK_DATA", pData);
        msg.setData(countBundle);
        //Toast.makeText(this.getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
        mMessageHandler.sendMessage(msg);


    }

    private void toRegisterReceiver() {
        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);
    }


    private void value2BlockHex(int value, byte adr, byte[] pHex) {

        pHex[0] = (byte) (0x000000ff & value);
        pHex[1] = (byte) ((0x0000ff00 & value) >> 8);
        pHex[2] = (byte) ((0x00ff0000 & value) >> 16);
        pHex[3] = (byte) ((0xff000000 & value) >> 24);
        pHex[4 + 0] = (byte) (0x000000ff & ~value);
        pHex[4 + 1] = (byte) ((0x0000ff00 & ~value) >> 8);
        pHex[4 + 2] = (byte) ((0x00ff0000 & ~value) >> 16);
        pHex[4 + 3] = (byte) ((0xff000000 & ~value) >> 24);
        pHex[8 + 0] = (byte) (0x000000ff & value);
        pHex[8 + 1] = (byte) ((0x0000ff00 & value) >> 8);
        pHex[8 + 2] = (byte) ((0x00ff0000 & value) >> 16);
        pHex[8 + 3] = (byte) ((0xff000000 & value) >> 24);

        pHex[12 + 0] = (byte) adr;
        pHex[12 + 1] = (byte) ~adr;
        pHex[12 + 2] = (byte) adr;
        pHex[12 + 3] = (byte) ~adr;

    }
}
