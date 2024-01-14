package com.alcorlink.smartcard;


import amlib.ccid.Reader;
import amlib.ccid.Reader6636;
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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class SLE6636_Activity extends AppCompatActivity {

	
	private String mStrMessage;
	private Reader6636 m6636;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;
	private Button mBtOpen;
	private Button mBtClose;
	private Button mBtPower;
	private Button mBtWriteMem;
	private Button mBtReadMem;
	private Button mBtWriteCounter;
	private Button mBtReadCounter;
	private Button mBtReload;
	private Button mBtAuthentication;
	private Button mBtVerification;
	
	private TextView mTextViewResult;
	
	private EditText mEdtAddrMem;
	private EditText mEdtAddrCounter;
	private EditText mEdtAddrReload;
	private EditText mEdtLength;
	private EditText mEdtDataMem;
	private EditText mEdtDataCounter;
	private EditText mEdtClock;
	private EditText mEdtKey;
	private EditText mEdtChallenge;
	private EditText mEdtTC1;
	private EditText mEdtTC2;
	private EditText mEdtTC3;
	

	private ProgressDialog mWaitProgress; 

	private static final String TAG = "Alcor-SLE6636";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";
	
	private static final int MAX_WRITE_LENGTH = 48;
	private static final int MAX_WRITE_DATA_LENGTH = 48;
	private static final int MAX_CHALLENGE_LEN = 6;
	private static final byte DIGIT_OF_BYTE = 2;
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = this.getIntent();  
		Bundle b = intent.getExtras();
		if (b != null)
		{
			mUsbDev = (UsbDevice) b.getParcelable(USB_SERVICE);
		}
		if (mUsbDev != null)
		{
			Log.d(TAG, "mdev=" + Integer.toHexString(mUsbDev.getProductId()));
		}
		setContentView(R.layout.activity_sle6636);
		setupViews();

		try {
			mMyDev = new HardwareInterface(HWType.eUSB, this.getApplicationContext());

		}catch(Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			return;
		}
		// Get USB manager
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		toRegisterReceiver();
		if (mUsbDev == null)
			mTextViewResult.setText("No Device been selected");
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
			try {
				closeReaderUp(SLE6636_Activity.this);
				closeReaderBottom(SLE6636_Activity.this);
			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}

			Log.d(TAG,"finish");
			SLE6636_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}

	public void  OnClickSLE6636Open(View view){
		Log.d(TAG, "OnClickSLE6636Open");
		requestDevPerm();
		
	}
	public void  OnClickSLE6636Close(View view){
		Log.d(TAG, "OnClickSLE6636Close");
		new CloseTask(SLE6636_Activity.this).execute();
		
	}
	
	public void OnClickSLE6636Power(View view){
		mPowerDialog.show();
	}
	

	public void  OnClickSLE6636WriteMem(View view){

		byte []pAddr = new byte[1];
		byte []pLen = new byte[1];
		byte []pWrite = new byte[MAX_WRITE_LENGTH];
		byte []pRxBuf = new byte[3];
		int []pIntReturnLen = new int[1];
		int result;
		//get addr
		result = getEditTextHexByte(mEdtAddrMem, pAddr);
		if (result != 0)
		{
			 return;
		}
		//get data
		byte []pReturnLen = new byte[1];
		result = getData(mEdtDataMem, pWrite, MAX_WRITE_DATA_LENGTH, pReturnLen);
		if (result != 0)
		{
			 return;
		}
		Log.d(TAG, "address/writeLen/getData Len="+ Integer.toString(pAddr[0])+ "/"+Integer.toString(pLen[0])+"/"+Integer.toString(pReturnLen[0]));
		for (byte i=0; i<pReturnLen[0]; i++ )
		{
			Log.d(TAG,"write buf[" +i+"="+ Integer.toString(pWrite[i]));
		}
		//send cmd
		result = m6636.SLE6636Cmd_WriteMemory(0, pAddr[0], pReturnLen[0], pWrite, (byte)0   , pRxBuf, pIntReturnLen);
		//result
		Log.d(TAG,"result, result="+Integer.toHexString(result));
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			showBufResult(pIntReturnLen[0], pRxBuf);
		}
		else
			showResult("WriteMemory", result);
	}
	
	public void  OnClickSLE6636ReadMem(View view){

		byte []pAddr = new byte[1];
		byte []pLen = new byte[1];
		byte []pRead = new byte[MAX_WRITE_LENGTH];
		int []tmp = new int[1];
		int []pIntReturnLen = new int[1];
		int result;
		final byte sle6636ReadLenHead = 3;
		//get addr
		result = getEditTextHexByte(mEdtAddrMem, pAddr);
		if (result != 0)
		{
			 return;
		}
		//get length
		result = getEditTextDec(mEdtLength, tmp, 2);//1~0x29
		if (result != 0)
		{
			 return;
		}
		pLen[0] = (byte)(0xff&tmp[0] );
		Log.d(TAG, "address/readle/getData Len="+ Integer.toString(pAddr[0])+ "/"+Integer.toString(pLen[0])+"/"+Integer.toString(tmp[0]));
		
		//send cmd
		result = m6636.SLE6636Cmd_ReadMemory(pAddr[0], pLen[0],  (byte) (pLen[0]+ sle6636ReadLenHead), pRead, pIntReturnLen);
		Log.d(TAG,"result, result="+Integer.toHexString(result));
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			//shows data
			showBufResult(pIntReturnLen[0], pRead);
		}
		else
			showResult("ReadMemory", result);
	}
	
	public void  OnClickSLE6636WriteCounter(View view){

		byte []pAddr = new byte[1];
		byte []pWrite = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];
		byte []pRxBuf = new byte[3];
		int result;
		//get addr
		result = getEditTextHexByte(mEdtAddrCounter, pAddr);
		if (result != 0)
		{
			 return;
		}
		//get data
		result = getEditTextHexByte(mEdtDataCounter, pWrite);
		if (result != 0)
		{
			 return;
		}
		Log.d(TAG, "address/getData ="+ Integer.toString(pAddr[0])+ "/"+"/"+Integer.toString(pWrite[0]));
		//send cmd
		result = m6636.SLE6636Cmd_WriteCounter( pAddr[0], pWrite[0], (byte)0, pRxBuf, pIntReturnLen);
		//result
		Log.d(TAG,"result, result="+Integer.toHexString(result));
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			showBufResult(pIntReturnLen[0], pRxBuf);
		}
		else
			showResult("WriteCounter", result);
	}
	
	public void  OnClickSLE6636ReadCounter(View view){

		byte []pAddr = new byte[1];
		byte []pRead = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];
		int result;
		//get addr
		result = getEditTextHexByte(mEdtAddrCounter, pAddr);
		if (result != 0)
		{
			 return;
		}
		Log.d(TAG, "address="+ Integer.toString(pAddr[0]));
		//send cmd
		result = m6636.SLE6636Cmd_ReadCounters((byte)8, pRead, pIntReturnLen);
		//result
		Log.d(TAG,"result, result="+Integer.toHexString(result));
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			showBufResult(pIntReturnLen[0], pRead);
		}
		else
			showResult("ReadCounter", result);
	}
	
	public void OnClickSLE6636Reload(View view){

		byte []pAddr = new byte[1];
		int []pIntReturnLen = new int[1];
		byte []pRxBuf = new byte[3];
		int result;
		//get addr
		result = getEditTextHexByte(mEdtAddrReload, pAddr);
		if (result != 0)
		{
			 return;
		}
		Log.d(TAG, "address="+ Integer.toString(pAddr[0]));
		//send cmd
		result = m6636.SLE6636Cmd_Reload(pAddr[0], (byte)3, pRxBuf, pIntReturnLen);
		Log.d(TAG,"result, result="+Integer.toHexString(result));
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			showBufResult(pIntReturnLen[0], pRxBuf);
		}
		else
			showResult("Reload", result);
	}
	

	public void  OnClickSLE6636Authenticate(View view){

		byte []pKey = new byte[1];
		byte []pClock = new byte[1];
		byte []pChallenge = new byte[MAX_CHALLENGE_LEN];
		int []pIntReturnLen = new int[1];
		byte []pRxBuf = new byte[5];
		int result;
		//get key
		result = getEditTextHexByte(mEdtKey, pKey);
		//get clk
		result = getEditTextHexByte(mEdtClock, pClock);
		//get challenge
		byte []pReturnLen = new byte[1];
		result = getData(mEdtChallenge, pChallenge, MAX_CHALLENGE_LEN, pReturnLen);
		//send 
		Log.d(TAG, "pKey/pClock ="+ Integer.toString(pKey[0])+ "/"+"/"+Integer.toString(pClock[0]));
		Log.d(TAG,"Challenge length="+ Integer.toString(pReturnLen[0]));
		for (byte i=0; i<pReturnLen[0]; i++ )
		{
			Log.d(TAG,"Challenge buf[" +i+"="+ Integer.toString(pChallenge[i]));
		}
		//send cmd
		result = m6636.SLE6636Cmd_Authentication(pKey[0], pClock[0], pReturnLen[0], pChallenge, (byte)0, pRxBuf, pIntReturnLen);
		//result
		Log.d(TAG,"result, result="+Integer.toHexString(result));
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			showBufResult(pIntReturnLen[0], pRxBuf);
		}
		else
			showResult("Authenticate", result);
	}
	
	public void  OnClickSLE6636Verification(View view){
		Log.d(TAG, "");
		
		byte []pTc1 = new byte[1];
		byte []pTc2 = new byte[1];
		byte []pTc3 = new byte[1];
		int []pIntReturnLen = new int[1];
		int result;
		//get tc1
		result = getEditTextHexByte(mEdtTC1, pTc1);
		//get tc2
		result = getEditTextHexByte(mEdtTC2, pTc2);
		//get tc3
		result = getEditTextHexByte(mEdtTC3, pTc3);
		Log.d(TAG, "pTc1/pTc2/pTc3 ="+ Integer.toString(pTc1[0])+ "/"+"/"+Integer.toString(pTc2[0])+"/"+Integer.toString(pTc3[0]));
		//send cmd
		result = m6636.SLE6636Cmd_Verification(pTc1[0], pTc2[0], pTc3[0], (byte)0, null, pIntReturnLen);
		showResult("Verification", result);
	}
	
	@Override
	protected void   onDestroy() {
		Log.e(TAG,"onDestroy");
		unregisterReceiver(mReceiver);
		if (mMyDev != null)
		{
			mMyDev.Close();
		}
		super.onDestroy();
	}


	
	private static void closeReaderBottom(SLE6636_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}
	
	private static int closeReaderUp(SLE6636_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.m6636 != null)
		{
			ret = act.m6636.close();
		}
		return ret;
	}

	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<SLE6636_Activity> weakActivity;

		CloseTask(SLE6636_Activity myActivity) {
			this.weakActivity = new WeakReference<SLE6636_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			SLE6636_Activity activity = weakActivity.get();
			setUpWaitDialog(activity, "Closing Reader");

		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			SLE6636_Activity activity = weakActivity.get();
			try {
				do{
					status = closeReaderUp(activity);
				}while(SCError.maskStatus(status) == SCError.READER_CMD_BUSY);

			} catch (Exception e) {
				activity.mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
				return -1;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			SLE6636_Activity activity = weakActivity.get();
			if (result != 0) {
				activity.mTextViewResult.setText("Close fail: "+ SCError.errorCode2String(result) +". " + activity.mStrMessage);
				Log.e(TAG, "Close fail: "+ SCError.errorCode2String(result));
			}else{
				activity.mTextViewResult.setText("Close successfully");
				Log.e(TAG,"Close successfully");
			}
			closeReaderBottom(activity);
			activity.mWaitProgress.dismiss();
		}
	}

	
	/*
	 * 
	 * @param edt
	 * @param pBuf
	 * @param length number of digits
	 * @param pRetLen  how many bytes
	 * @return
	 */
	private int getData(EditText edt, byte []pBuf, int length, byte []pRetLen)
	{
		byte []pByteArrary;
		String strText; 
		int len;
		strText = edt.getText().toString();
		
		len = strText.length() ;
		Log.d(TAG, "Lenght="+ len);
		if (len > (length*2) || len == 0 || (len%2 != 0))
		{
			mStrMessage = "Wrong length in Data Fields ";
			Log.e(TAG, mStrMessage);
			mTextViewResult.setText(mStrMessage);
			return -1;
		}
		pByteArrary = MainActivity.toByteArray(strText);
		len = pByteArrary.length;
		
		Log.d(TAG, "Byte array Lenght="+ len);
	
		
		for (int i=0; i<(len); i++)
		{
			pBuf[i] = pByteArrary[i];
			Log.d(TAG, "pBuf[" + i +"]=0x"+Integer.toHexString(pBuf[i]));
		}
		 
		pRetLen[0] = (byte)len;
		return 0;
	}



	/*
	 * length: number of max digits in this field
	 * */ 
	private int getEditTextHexByte(EditText edt, byte []pData)
	{
		byte []pBuf = new byte[2];
		byte []pReturnLen = new byte[1];
		int result;
		result = getData(edt, pBuf, DIGIT_OF_BYTE, pReturnLen);
		if (result != 0)
			return result;
		if (pReturnLen[0] != 1) //one byte
			return -1;
		pData[0] = pBuf[0];
		Log.d(TAG, "Read Byte =" + Integer.toHexString(pBuf[0]));
		return 0;
	}
	
	/*
	 * length: number of max digits in this field 
	 */
	private int getEditTextDec(EditText edt, int []pBuf, int length) {
		
		String strText; 
		int len;
		strText = edt.getText().toString();
		len = strText.length() ;
		if (len > length || len == 0)
		{
			mStrMessage = "Wrong length in text Fields ";
			Log.e(TAG, mStrMessage);
			mTextViewResult.setText(mStrMessage);
			return -1;
		}
		pBuf[0] =Integer.parseInt(edt.getText().toString());
		return 0;
	}

	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/
		ret = m6636.getCardStatus(pCardStatus);
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
	
	
	private static int InitReader(SLE6636_Activity act)
	{
		int Status = 0;
		boolean init;// 
		Log.d(TAG, "InitReader");
		try {
			init = act.mMyDev.Init(act.mManager, act.mUsbDev);
			if (!init){
				Log.e(TAG, "Device init fail");
				return -1;
			}
		} catch (ReaderHwException e) {
			act.mStrMessage = "Get Exception : " + e.getMessage();
			return -1;
		}

		try {
			act.m6636 = new Reader6636(act.mMyDev);
			Status = act.m6636.open();
		}
		catch (ReaderException e)
		{
			act.mStrMessage = "Get Exception : " + e.getMessage();
			Log.d(TAG, "InitReader fail "+ act.mStrMessage);
			return -1;
		}
		catch (IllegalArgumentException e)
		{
			act.mStrMessage = "Get Exception : " + e.getMessage();
			Log.d(TAG, "InitReader fail "+ act.mStrMessage);
			return -1;
		}

		return Status;
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


	
	private static void onCreateButtonSetup(SLE6636_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtWriteMem.setEnabled(false);
		act.mBtReadMem.setEnabled(false);
		act.mBtWriteCounter.setEnabled(false);
		act.mBtReadCounter.setEnabled(false);
		act.mBtReload.setEnabled(false);
		act.mBtAuthentication.setEnabled(false);
		act.mBtVerification.setEnabled(false);
	}

	private void onDevPermit(UsbDevice dev){
		try {    		
			setUpWaitDialog(SLE6636_Activity.this,"Opening...");
			new OpenTask(SLE6636_Activity.this).execute(dev);
		}
		catch(Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
		}
	}

	private void onDetach(Intent intent){
		UsbDevice   udev = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (udev != null ) {
			if (udev.equals(mUsbDev) ){
				closeReaderUp(SLE6636_Activity.this);
				closeReaderBottom(SLE6636_Activity.this);
				SLE6636_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}
		
	}
	
	private static void onOpenButtonSetup(SLE6636_Activity act){
		act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(true);
		act.mBtPower.setEnabled(true);
		act.mBtWriteMem.setEnabled(true);
		act.mBtReadMem.setEnabled(true);
		act.mBtWriteCounter.setEnabled(true);
		act.mBtReadCounter.setEnabled(true);
		act.mBtReload.setEnabled(true);
		act.mBtAuthentication.setEnabled(true);
		act.mBtVerification.setEnabled(true);
	}

	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<SLE6636_Activity> weakActivity;

		OpenTask(SLE6636_Activity myActivity) {
			this.weakActivity = new WeakReference<SLE6636_Activity>(myActivity);
		}
		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			SLE6636_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}
			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			SLE6636_Activity activity = weakActivity.get();
			activity.mWaitProgress.dismiss();
			if (result != 0) {
				activity.mTextViewResult.setText("Open fail: "+ SCError.errorCode2String(result)+". " + activity.mStrMessage);
				Log.e(TAG,"Open fail: "+ SCError.errorCode2String(result)+". " + activity.mStrMessage);

			}else{
				activity.mTextViewResult.setText("Open successfully");
				Log.e(TAG,"Open successfully");
				onOpenButtonSetup(activity);
			}
		}
	}


	private void requestDevPerm(){
		if (mUsbDev != null)
			mManager.requestPermission(mUsbDev, mPermissionIntent);
		else
		{
			Log.e(TAG,"selected not found");
			mTextViewResult.setText("Device not found");
		}
	}

	private void setButtonView()
	{
		mBtOpen = (Button)findViewById(R.id.SLE6636buttonOpen);
		mBtClose = (Button)findViewById(R.id.SLE6636buttonClose);
		mBtPower = (Button)findViewById(R.id.SLE6636buttonPower);
		mBtWriteMem = (Button)findViewById(R.id.SLE6636buttonWriteMem);
		mBtReadMem = (Button)findViewById(R.id.SLE6636buttonReadMem);
		mBtWriteCounter = (Button)findViewById(R.id.SLE6636buttonWriteCounter);
		mBtReadCounter = (Button)findViewById(R.id.SLE6636buttonReadCounter);
		mBtReload = (Button)findViewById(R.id.SLE6636buttonReload);
		mBtAuthentication = (Button)findViewById(R.id.SLE6636buttonAuthentication);
		mBtVerification = (Button)findViewById(R.id.SLE6636buttonVerification);
	}
	
	private void setEditView()
	{
		mEdtAddrMem = (EditText)findViewById(R.id.SLE6636EditTextAddrMem);
		mEdtAddrCounter = (EditText)findViewById(R.id.SLE6636EditTextAddrCounter);
		mEdtAddrReload = (EditText)findViewById(R.id.SLE6636EditTextAddrReload);
		mEdtLength = (EditText)findViewById(R.id.SLE6636EditTextLen);
		mEdtDataMem = (EditText)findViewById(R.id.SLE6636EditTextDataMem);		
		mEdtDataCounter = (EditText)findViewById(R.id.SLE6636EditTextDataCounter);
		mEdtClock = (EditText)findViewById(R.id.SLE6636EditTextClock);
		mEdtKey = (EditText)findViewById(R.id.SLE6636EditTextKey);	
		mEdtChallenge = (EditText)findViewById(R.id.SLE6636EditTexChallenge);
		mEdtTC1 = (EditText)findViewById(R.id.SLE6636EditTextTc1);
		mEdtTC2 = (EditText)findViewById(R.id.SLE6636EditTextTc2);
		mEdtTC3 = (EditText)findViewById(R.id.SLE6636EditTextTc3);
	}
	
	private void setPowerView(){
		mPowerDialog = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener poweroffClick = new DialogInterface.OnClickListener(){
			@Override  
			public void onClick(DialogInterface dialog, int which) {
				int ret;
				ret =	poweroff();
				if (ret == SCError.READER_SUCCESSFUL)
					mTextViewResult.setText("power off successfully");
				else if (SCError.maskStatus(ret) == SCError.READER_NO_CARD){
					mTextViewResult.setText("Card Absent");
				}
				else
				{
					showResult("PowerOff", ret);
				}
			}  
		};

		DialogInterface.OnClickListener poweronClick = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {
				int ret;
				ret =	poweron();
				if (ret == SCError.READER_SUCCESSFUL)
				{
					mTextViewResult.setText("power on successfully");
				}
				else if (SCError.maskStatus(ret) == SCError.READER_NO_CARD){
					mTextViewResult.setText("Card Absent");
				}
				else
				{
					showResult("PowerOn", ret);
				}
			}
		}; 
		mPowerDialog.setTitle("Set Power");
		mPowerDialog.setNeutralButton("PowerON", poweronClick);
		mPowerDialog.setPositiveButton("PowerOFF", poweroffClick);
	}
	
	private void setTextView()
	{

		mTextViewResult = (TextView)findViewById(R.id.SLE6636TextResult);    
	}

	private static void setUpWaitDialog(SLE6636_Activity act, String msg){

		act.mWaitProgress.setMessage(msg);
		act.mWaitProgress.setCancelable(false);
		act.mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		act.mWaitProgress.show();
	}

	private void setupViews(){
		setButtonView();
		setTextView();
		setEditView();
		setPowerView();

		onCreateButtonSetup(SLE6636_Activity.this);
		mWaitProgress =  new ProgressDialog(SLE6636_Activity.this);  
		//setReaderSlotView();
	}
	
	private void showBufResult(int len, byte[]pBuf) {
		String s;
		s = MainActivity.byte2String(pBuf, len);
		mTextViewResult.setText(s); 
	}
	
	private void showResult(String cmd, int result)
	{
		String  msg, ccidErrCode;
		if (result == SCError.READER_SUCCESSFUL)
		{
			mTextViewResult.setText(cmd + " Success");
			return;
		}


		msg = new String("Fail:"+SCError.errorCode2String(result)+ "("+Integer.toHexString(result)+")");

        if (SCError.maskStatus(result) == SCError.READER_CMD_FAIL)
		{
			ccidErrCode = Integer.toHexString(m6636.getCmdFailCode() & 0x0000ffff);
			msg = cmd + " " +msg + "(" + ccidErrCode +")";
		}
		mTextViewResult.setText(msg);
	}
	

	private int poweron(){
		int result = SCError.READER_SUCCESSFUL;
		Log.d(TAG,"poweron");
		//check slot status first
		result = getSlotStatus();
		switch (result)
		{
		case SCError.READER_NO_CARD:
			mTextViewResult.setText("Card Absent");
			Log.d(TAG,"Card Absent");
			return SCError.READER_NO_CARD;
		case SCError.READER_CARD_INACTIVE:
		case SCError.READER_SUCCESSFUL:
			break;
		default://returns other error case
			return result;
		}

		result = m6636.setPower(Reader.CCID_POWERON);
		Log.d(TAG,"power on exit");
		return result;
	}


	private int poweroff(){
		int result =  SCError.READER_SUCCESSFUL;
		Log.d(TAG,"poweroff");
		result = getSlotStatus();
		switch (result)
		{
		case SCError.READER_NO_CARD:
			mTextViewResult.setText("Card Absent");
			Log.d(TAG,"Card Absent");
			return SCError.READER_NO_CARD;
		case SCError.READER_CARD_INACTIVE:
		case SCError.READER_SUCCESSFUL:
			break;
		default://returns other error case
			return result;
		}
		//----------poweroff card------------------
		result = m6636.setPower(Reader.CCID_POWEROFF);
		return result;
	}


	private void toRegisterReceiver(){
		// Register receiver for USB permission
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mReceiver, filter);
	}
}
