package com.alcorlink.smartcard;


import amlib.ccid.SCError;
import amlib.ccid.Reader;
import amlib.ccid.Reader4442;
import amlib.ccid.ReaderException;
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

public class SLE4442_Activity extends AppCompatActivity {

	private String mStrMessage;
	private Reader4442 m4442;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;
	private Button mBtOpen;
	private Button mBtClose;
	private Button mBtPower;
	private Button mBtVerify;
	private Button mBtReadMM;
	private Button mBtUpdateMM;
	private Button mBtReadPM;
	private Button mBtWritePM;
	private Button mBtReadSM;
	private Button mBtUpdataSM;
	private Button mBtCompare;
	
	private TextView mTextViewResult;
	
	private EditText mEdtVerifyPin1;
	private EditText mEdtVerifyPin2;
	private EditText mEdtVerifyPin3;
	private EditText mEdtDataMM;
	private EditText mEdtAddrMM;
	private EditText mEdtLenMM;
	private EditText mEdtAddrPM;
	private EditText mEdtDataPM;
	private EditText mEdtAddrSM;
	private EditText mEdtDataSM;
	

	private ProgressDialog mWaitProgress; 

	private static final String TAG = "Alcor-SLE4442";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";
	private static final int DIGIT_OF_BYTE = 2;
	private static final byte FIXED_LEN_TO_READ = 4;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG," onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sle4442);
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
		setContentView(R.layout.activity_sle4442);
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
				closeReaderUp(SLE4442_Activity.this);
				closeReaderBottom(SLE4442_Activity.this);
			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}

			Log.d(TAG,"finish");
			SLE4442_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}

	public void OnClickSLE4442Open(View view){
		Log.d(TAG, "4442OpenOnClick");
		requestDevPerm();
	}
	

	public void OnClickSLE4442Power(View view){
		mPowerDialog.show();
	}

	
	public void OnClickSLE4442Verify(View view){
		byte []pData = new byte[1];
		byte []pPin = new byte[3];
		int result;
		getEditTextHexByte(mEdtVerifyPin1, pData);
		pPin[0] = pData[0];
		getEditTextHexByte(mEdtVerifyPin2, pData);
		pPin[1] = pData[0];
		getEditTextHexByte(mEdtVerifyPin3, pData);
		pPin[2] = pData[0];
		
		result = m4442.SLE4442Cmd_Verify(3, pPin);
		showResult("Verify", result);
	}
	
	public void OnClickSLE4442ReadMainMemory(View view){
		Log.d(TAG, "SLE4442ReadMainMemoryOnClick");
		int result;
		byte[]pAddress = new byte[1];
		byte []pLen = new byte [1];
		byte []pReadData = null;
		int []pIntReturnLen = new int[1];
		getEditTextHexByte(mEdtAddrMM, pAddress);
		getEditTextHexByte(mEdtLenMM, pLen);
	
		pReadData = new byte[pLen[0]];
		result = m4442.SLE4442Cmd_ReadMainMemory(pAddress[0], pLen[0], pReadData, pIntReturnLen) ;
		if (result == SCError.READER_SUCCESSFUL )
		{
			Log.d(TAG,"read len "+pIntReturnLen[0]);
			showTextResult(pIntReturnLen[0], pReadData);
		}
		else
			showResult("ReadMainMemory", result);
	}

	public void  OnClickSLE4442UpdataMainMemory(View view){
		Log.d(TAG, "SLE4442UpdataMainMemoryOnClick");
		byte[]pAddress = new byte[1];
		byte []pData = new byte[1];
		getEditTextHexByte(mEdtAddrMM, pAddress);
		getEditTextHexByte(mEdtDataMM, pData);
		showResult("UpdataMainMemory", m4442.SLE4442Cmd_UpdateMainMemory(pAddress[0], pData[0]));
	}
	
	public void  OnClickSLE4442ReadProtectionMemory(View view){
		Log.d(TAG, "SLE4442ReadProtectionMemoryOnClick");
		int result;
		byte[]pAddress = new byte[1];
		byte []pReadData = null;
		int []pIntReturnLen = new int[1];
		getEditTextHexByte(mEdtAddrPM, pAddress);
	
		pReadData = new byte[FIXED_LEN_TO_READ];
		result = m4442.SLE4442Cmd_ReadProtectionMemory(FIXED_LEN_TO_READ, pReadData, pIntReturnLen);
		if (result == SCError.READER_SUCCESSFUL )
		{
			Log.d(TAG,"read len "+pIntReturnLen[0]);
			showTextResult(pIntReturnLen[0], pReadData);
		}
		else
			showResult("ReadProtectionMemory", result);
	}

	public void  OnClickSLE4442WriteProtectionMemory(View view){
		Log.d(TAG, "SLE4442WriteProtectionMemoryOnClick");
		int result;
		byte[]pAddress = new byte[1];
		byte []pData = new byte[1];
		getEditTextHexByte(mEdtAddrPM, pAddress);
		getEditTextHexByte(mEdtDataPM, pData);
		result = m4442.SLE4442Cmd_WriteProtectionMemory(pAddress[0], pData[0]);
		
		showResult("WriteProtectionMemory", result);
	}

	public void  OnClickSLE4442ReadSecurityMemory(View view){
		Log.d(TAG, "SLE4442CReadSecurityMemoryOnClick");
		int result;
		byte[]pAddress = new byte[1];
		byte []pReadData = null;
		int []pIntReturnLen = new int[1];
		getEditTextHexByte(mEdtAddrSM, pAddress);
	
		pReadData = new byte[FIXED_LEN_TO_READ];
		result = m4442.SLE4442Cmd_ReadSecurityMemory( FIXED_LEN_TO_READ, pReadData, pIntReturnLen);
		if (result == SCError.READER_SUCCESSFUL )
		{
			Log.d(TAG,"read len "+pIntReturnLen[0]);
			showTextResult(pIntReturnLen[0], pReadData);
		}
		else
			showResult("ReadSecurityMemory", result);
	}

	public void  OnClickSLE4442UpdataSecurityMemory(View view){
		Log.d(TAG, "SLE4442UpdataSecurityMemoryOnClick");
		int result;
		byte[]pAddress = new byte[1];
		byte []pData = new byte[1];
		getEditTextHexByte(mEdtAddrSM, pAddress);
		getEditTextHexByte(mEdtDataSM, pData);
		result = m4442.SLE4442Cmd_UpdateSecurityMemory(pAddress[0], pData[0]);
		showResult("UpdataSecurityMemory", result);
	}

	public void  OnClickSLE4442ComparaeVerification(View view){
		Log.d(TAG, "SLE4442ComparaeVerificationOnClick");
		int result;
		byte[]pAddress = new byte[1];
		byte []pData = new byte[1];
		getEditTextHexByte(mEdtAddrSM, pAddress);
		getEditTextHexByte(mEdtDataSM, pData);
		result = m4442.SLE4442Cmd_CompareVerificationData(pAddress[0], pData[0]);
		showResult("ComparaeVerification", result);
	}

	
	public void OnClickSLE4442Close(View view){
		new CloseTask(SLE4442_Activity.this).execute();
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

	private void cleanText(){

	}

	
	private static int closeReaderUp(SLE4442_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.m4442 != null)
		{
			ret = act.m4442.close();
		}
		return ret;
	}

	private static void closeReaderBottom(SLE4442_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}

	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<SLE4442_Activity> weakActivity;

		CloseTask(SLE4442_Activity myActivity) {
			this.weakActivity = new WeakReference<SLE4442_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			SLE4442_Activity activity = weakActivity.get();
			setUpWaitDialog(activity,"Closing Reader");
		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			SLE4442_Activity activity = weakActivity.get();
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
			SLE4442_Activity activity = weakActivity.get();
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

	
	private static int InitReader(SLE4442_Activity act)
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
			act.m4442 = new Reader4442(act.mMyDev);
			Status = act.m4442.open();
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

	
	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/
		ret = m4442.getCardStatus(pCardStatus);
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
	

	private static void onCreateButtonSetup(SLE4442_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtVerify.setEnabled(false);
		act.mBtReadMM.setEnabled(false);
		act.mBtUpdateMM.setEnabled(false);
		act.mBtReadPM.setEnabled(false);
		act.mBtWritePM.setEnabled(false);
		act.mBtReadSM.setEnabled(false);
		act.mBtUpdataSM.setEnabled(false);
		act.mBtCompare.setEnabled(false);
		act.mBtClose.setEnabled(false);
	}


	private void onDevPermit(UsbDevice dev){
		try {    		
			setUpWaitDialog(SLE4442_Activity.this,"Opening...");
			new OpenTask(SLE4442_Activity.this).execute(dev);
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
				closeReaderUp(SLE4442_Activity.this);
				closeReaderBottom(SLE4442_Activity.this);
				SLE4442_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}
	}

	private static void onOpenButtonSetup(SLE4442_Activity act) {
		act.mBtOpen.setEnabled(false);
		act.mBtPower.setEnabled(true);
		act.mBtVerify.setEnabled(true);
		act.mBtReadMM.setEnabled(true);
		act.mBtUpdateMM.setEnabled(true);
		act.mBtReadPM.setEnabled(true);
		act.mBtWritePM.setEnabled(true);
		act.mBtReadSM.setEnabled(true);
		act.mBtUpdataSM.setEnabled(true);
		act.mBtCompare.setEnabled(true);
		act.mBtClose.setEnabled(true);
	}

	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<SLE4442_Activity> weakActivity;

		OpenTask(SLE4442_Activity myActivity) {
			this.weakActivity = new WeakReference<SLE4442_Activity>(myActivity);
		}
		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			SLE4442_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			SLE4442_Activity activity = weakActivity.get();
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
			ccidErrCode = Integer.toHexString(m4442.getCmdFailCode() & 0x0000ffff);
			msg = cmd + " " +msg + "(" + ccidErrCode +")";
		}
        mTextViewResult.setText(msg);

	}
	
	private void showTextResult(int len, byte []pData)
	{
		int i;
		StringBuilder msg = new StringBuilder();
		msg.append("ReadData:");
		for (i=0;i< len;i++)
		{
			msg.append(" ").append(Integer.toHexString(0x000000ff & pData[i]));
		}
		mTextViewResult.setText(msg.toString());
	}

	private void setButtonView()
	{
		mBtOpen = (Button)findViewById(R.id.SLE4442buttonOpen);
		mBtClose = (Button)findViewById(R.id.SLE4442buttonClose);
		mBtPower = (Button)findViewById(R.id.SLE4442buttonPower);
		mBtVerify = (Button)findViewById(R.id.SLE4442buttonVerify);
		mBtReadMM = (Button)findViewById(R.id.SLE4442buttonReadMM);
		mBtUpdateMM = (Button)findViewById(R.id.SLE4442buttonUpdataMM);
		mBtReadPM = (Button)findViewById(R.id.SLE4442buttonReadPM);
		mBtWritePM= (Button)findViewById(R.id.SLE4442buttonWritePM);
		mBtReadSM = (Button)findViewById(R.id.SLE4442buttonReadSM);
		mBtUpdataSM = (Button)findViewById(R.id.SLE4442buttonUpdataSM);
		mBtCompare = (Button)findViewById(R.id.SLE4442buttonCompare);
	}
	
	private void setEditView()
	{
		mEdtVerifyPin1 = (EditText)findViewById(R.id.SLE4442EditTextPIN1);
		mEdtVerifyPin2 = (EditText)findViewById(R.id.SLE4442EditTextPIN2);
		mEdtVerifyPin3 = (EditText)findViewById(R.id.SLE4442EditTextPIN3);
		mEdtAddrMM = (EditText)findViewById(R.id.SLE4442EditTextAddrMM);
		mEdtLenMM = (EditText)findViewById(R.id.SLE4442EditTextLenMM);
		mEdtDataMM = (EditText)findViewById(R.id.SLE4442EditTextDataMM);
		mEdtAddrPM = (EditText)findViewById(R.id.SLE4442EditTextAddrPM);
		mEdtDataPM = (EditText)findViewById(R.id.SLE4442EditTextDataPM);
		mEdtAddrSM = (EditText)findViewById(R.id.SLE4442EditTextAddrSM);
		mEdtDataSM = (EditText)findViewById(R.id.SLE4442EditTextDataSM);
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
		mTextViewResult = (TextView)findViewById(R.id.SLE4442TextResult);  
	}

	private static void setUpWaitDialog(SLE4442_Activity act, String msg){

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

		onCreateButtonSetup(SLE4442_Activity.this);
		mWaitProgress =  new ProgressDialog(SLE4442_Activity.this);
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

		result = m4442.setPower(Reader.CCID_POWERON);
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
		result = m4442.setPower(Reader.CCID_POWEROFF);
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
