package com.alcorlink.smartcard;




import amlib.ccid.SCError;
import amlib.ccid.Reader;
import amlib.ccid.Reader4428;
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

public class SLE4428_Activity extends AppCompatActivity {

	private String mStrMessage;
	private Reader4428 m4428;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;
	private Button mBtOpen;
	private Button mBtPower;
	private Button mBtWEwithPB;
	private Button mBtWEwithoutPB;
	private Button mBtwritePBDC;
	private Button mBtread9;
	private Button mBtread8;
	private Button mBtWEC;
	private Button mBtVerify1;
	private Button mBtVerify2;
	private Button mBtVPEEC;
	private Button mBtClose;
	
	private TextView mTextViewResult;
	
	private EditText mEdtAddrWrite;
	private EditText mEdtDataWrite;
	private EditText mEdtAddrRead;
	private EditText mEdtLenRead;
	private EditText mEdtDataWriteErr;
	private EditText mEdtDataVerify1;
	private EditText mEdtDataVerify2;
	private ProgressDialog mWaitProgress;

	private static final String TAG = "Alcor-SLE4428";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG," onCreate");
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
		setContentView(R.layout.activity_sle4428);
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
				closeReaderUp(SLE4428_Activity.this);
				closeReaderBottom(SLE4428_Activity.this);

			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}

			Log.d(TAG,"finish");
			SLE4428_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  
	}


	public void OnClickSLE4428Open(View view){
		Log.d(TAG, "4428OpenOnClick");
		requestDevPerm();
	}
	
	public void OnClickSLE4428Power(View view){
		mPowerDialog.show();
	}

	
	public void  OnClickSLE4428WEwithPB(View view){
		Log.d(TAG, "SLE4428WEwithPBOnClick");
		int result;
		int[]pAddress = new int[1];
		byte []pData = new byte [1];
		getEditTextDec(mEdtAddrWrite, pAddress, 3);
		getEditTextHexByte(mEdtDataWrite, pData);
	
		result = m4428.SLE4428Cmd_WriteEraseWithPB(pAddress[0], pData[0]); 
	
		showResult("WriteEraseWithPB", result);
	}

	public void  OnClickSLE4428WEwithoutPB(View view){
		Log.d(TAG, "SLE4428WEwithoutPBOnClick");
		int result;
		int[]pAddress = new int[1];
		byte []pData = new byte [1];
		getEditTextDec(mEdtAddrWrite, pAddress, 3);
		getEditTextHexByte(mEdtDataWrite, pData);
		result = m4428.SLE4428Cmd_WriteEraseWithoutPB(pAddress[0], pData[0]); 
		showResult("WriteEraseWithoutPB", result);
	}

	public void  OnClickSLE4428WriteProtectDataCompare(View view){
		Log.d(TAG, "SLE4428WPDCOnClick");
		int result;
		int[]pAddress = new int[1];
		byte []pData = new byte [1];
		getEditTextDec(mEdtAddrWrite, pAddress, 3);
		getEditTextHexByte(mEdtDataWrite, pData);
		result = m4428.SLE4428Cmd_WritePBWithDataComparison(pAddress[0], pData[0]); 
		showResult("WritePBWithDataComparison", result);
	}

	public void  OnClickSLE4428Read9(View view){
		Log.d(TAG, "SLE4428Read9OnClick");
		int result;
		int[]pAddress = new int[1];
		byte []pLen = new byte [1];
		byte []pReadData;
		byte []pReadPBData;
		int []pIntReturnLen = new int [1];
		getEditTextDec(mEdtAddrRead, pAddress, 3);
		getEditTextHexByte(mEdtLenRead, pLen);
		pReadData = new byte[pLen[0]];
		pReadPBData = new byte[0x000000ff&pLen[0]];
		result = m4428.SLE4428Cmd_Read9Bits(pAddress[0], pLen[0], pReadData, pReadPBData, pIntReturnLen);
		if (result == SCError.READER_SUCCESSFUL )
		{
			showResultRead9bits(pLen[0],pReadData,pReadPBData);
		}
		else
			showResult("Read9bits", result);
	}

	public void OnClickSLE4428CRead8(View view){
		Log.d(TAG, "SLE4428CRead8OnClick");
		int result;
		int[]pAddress = new int[1];
		byte []pLen = new byte [1];
		byte []pReadData;
		int []pIntReturnLen = new int [1];
		getEditTextDec(mEdtAddrRead, pAddress, 3);
		getEditTextHexByte(mEdtLenRead, pLen);
		pReadData = new byte[pLen[0]];
		result = m4428.SLE4428Cmd_Read8Bits(pAddress[0], pLen[0], pReadData, pIntReturnLen);
		if (result == SCError.READER_SUCCESSFUL )
		{
			showResultRead8bits(pLen[0],pReadData);
		}
		else
			showResult("REad8bits", result);
	}

	public void  OnClickSLE4428WriteErrCount(View view){
		Log.d(TAG, "SLE4428WECOnClick");
		int result;
		byte []pData = new byte [1];
		getEditTextHexByte(mEdtDataWriteErr, pData);
		result = m4428.SLE4428Cmd_WriteErrorCounter(pData[0]);

		showResult("WriteErrorCounter", result);
		
	}

	public void  OnClickSLE4428V1P(View view){
		Log.d(TAG, "SLE4428V1POnClick");
		int result;
		byte []pData = new byte [1];
		getEditTextHexByte(mEdtDataWriteErr, pData);
		result = m4428.SLE4428Cmd_Verify1stPSC(pData[0]);
		showResult("Verify1stPSC", result);
	}

	public void  OnClickSLE4428V2P(View view){
		Log.d(TAG, "SLE4428V2POnClick");
		int result;
		byte []pData = new byte [1];
		getEditTextHexByte(mEdtDataWriteErr, pData);
		result = m4428.SLE4428Cmd_Verify2ndPSC(pData[0]);
		showResult("Verify2ndPSC", result);
	}

	public void  OnClickSLE4428VPEEC(View view){
		Log.d(TAG, "SLE4428VPEECOnClick");
		int result;
		byte []pDataV1 = new byte [1];
		byte []pDataV2 = new byte [1];
		int []pIntErrorReason = new int[1];
		getEditTextHexByte(mEdtDataVerify1, pDataV1);
		getEditTextHexByte(mEdtDataVerify2, pDataV2);
		result = m4428.SLE4428Cmd_VerifyPSCAndEraseErrorCounter(pDataV1[0], pDataV2[0], pIntErrorReason);
		showResult("VerifyPSCAndEraseErrorCounter", result);
	}


	public void OnClickSLE4428Close(View view){
		new CloseTask(SLE4428_Activity.this).execute();
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


	private static int closeReaderUp(SLE4428_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.m4428 != null)
		{
			ret = act.m4428.close();
		}
		return ret;
	}

	private static void closeReaderBottom(SLE4428_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}


	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<SLE4428_Activity> weakActivity;

		CloseTask(SLE4428_Activity myActivity) {
			this.weakActivity = new WeakReference<SLE4428_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			SLE4428_Activity activity = weakActivity.get();
			setUpWaitDialog(activity, "Closing Reader");

		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			SLE4428_Activity activity = weakActivity.get();
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
			SLE4428_Activity activity = weakActivity.get();
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
		 
		
		//Log.d(TAG, "pBuf[1]=0x"+Integer.toHexString(pBuf[1]));
		pRetLen[0] = (byte)len;
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
	
	/*
	 * length: number of max digits in this field
	 * */ 
	private int getEditTextHexByte(EditText edt, byte []pData)
	{
		byte []pBuf = new byte[2];
		byte []pReturnLen = new byte[1];
		int result;
		result = getData(edt, pBuf, 2, pReturnLen);
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
		ret = m4428.getCardStatus(pCardStatus);
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

	private static int InitReader(SLE4428_Activity act)
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
			act.m4428 = new Reader4428(act.mMyDev);
			Status = act.m4428.open();
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
		mBtOpen = (Button)findViewById(R.id.SLE4428buttonOpen);
		mBtPower = (Button)findViewById(R.id.SLE4428buttonPower);
		mBtWEwithPB = (Button)findViewById(R.id.SLE4428WEwithPB);
		mBtWEwithoutPB = (Button)findViewById(R.id.SLE4428WEwithoutPB);
		mBtwritePBDC = (Button)findViewById(R.id.SLE4428WritePBwithDataComp);
		mBtread9= (Button)findViewById(R.id.SLE4428ReadP9Bits);
		mBtread8 = (Button)findViewById(R.id.SLE4428ReadP8Bits);
		mBtWEC = (Button)findViewById(R.id.SLE4428WriteErrorCounter);
		mBtVerify1 = (Button)findViewById(R.id.SLE4428Verify1stPSC);
		mBtVerify2 = (Button)findViewById(R.id.SLE4428Verify2stPSC);
		mBtVPEEC = (Button)findViewById(R.id.SLE4428VerifyPSCandEraseErrorCount);
		mBtClose = (Button)findViewById(R.id.SLE4428buttonClose);
	}
	
	private void setEditView()
	{
		mEdtAddrWrite = (EditText)findViewById(R.id.SLE4428EditTextAddrWrite);
		mEdtDataWrite = (EditText)findViewById(R.id.SLE4428EditTextDataWrite);
		mEdtAddrRead = (EditText)findViewById(R.id.SLE4428EditTextAddrRead);
		mEdtLenRead = (EditText)findViewById(R.id.SLE4428EditTextLenRead);
		mEdtDataWriteErr = (EditText)findViewById(R.id.SLE4428EditTextDataVeriy);
		mEdtDataVerify1 = (EditText)findViewById(R.id.SLE4428EditTextDataPSC1);
		mEdtDataVerify2 = (EditText)findViewById(R.id.SLE4428EditTextDataPSC2);
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
		mTextViewResult = (TextView)findViewById(R.id.SLE4428TextResult);
	}
	

	
	private static void setUpWaitDialog(SLE4428_Activity act,String msg){
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
		onCreateButtonSetup(SLE4428_Activity.this);
		mWaitProgress =  new ProgressDialog(SLE4428_Activity.this);  
	}

	private void showResultRead9bits(byte len, byte []pData, byte[]pPBData)
	{
		int i;

		showResultRead8bits(len, pData);
		StringBuilder msg = new StringBuilder();
		mTextViewResult.getText().toString();
		msg.append(mTextViewResult.getText().toString()).append("\nReadPBData:");
		for (i=0;i< len;i++)
		{
			msg.append(" ").append(Integer.toHexString(0x000000ff & pPBData[i]));
		}
		mTextViewResult.setText(msg.toString());
	}

	private void showResultRead8bits(byte len, byte []pData)
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
			ccidErrCode = Integer.toHexString(m4428.getCmdFailCode() & 0x0000ffff);
			msg = cmd + " " +msg + "(" + ccidErrCode +")";
		}
		mTextViewResult.setText(msg);
	}
	
	private static void onCreateButtonSetup(SLE4428_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtWEwithPB.setEnabled(false);
		act.mBtWEwithoutPB.setEnabled(false);
		act.mBtwritePBDC.setEnabled(false);
		act.mBtread9.setEnabled(false);
		act.mBtread8.setEnabled(false);
		act.mBtWEC.setEnabled(false);
		act.mBtVerify1.setEnabled(false);
		act.mBtVerify2.setEnabled(false);
		act.mBtVPEEC.setEnabled(false);
		act.mBtClose.setEnabled(false);
	}


	private static void onOpenButtonSetup(SLE4428_Activity act){
		act.mBtOpen.setEnabled(false);
		act.mBtPower.setEnabled(true);
		act.mBtWEwithPB.setEnabled(true);
		act.mBtWEwithoutPB.setEnabled(true);
		act.mBtwritePBDC.setEnabled(true);
		act.mBtread9.setEnabled(true);
		act.mBtread8.setEnabled(true);
		act.mBtWEC.setEnabled(true);
		act.mBtVerify1.setEnabled(true);
		act.mBtVerify2.setEnabled(true);
		act.mBtVPEEC.setEnabled(true);
		act.mBtClose.setEnabled(true);
	}

	private void onDevPermit(UsbDevice dev){
		setUpWaitDialog(SLE4428_Activity.this,"Openging");
		try {    		
			new OpenTask(SLE4428_Activity.this).execute(dev);
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
				closeReaderUp(SLE4428_Activity.this);
				closeReaderBottom(SLE4428_Activity.this);
				SLE4428_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}	
	}
	
	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<SLE4428_Activity> weakActivity;

		OpenTask(SLE4428_Activity myActivity) {
			this.weakActivity = new WeakReference<SLE4428_Activity>(myActivity);
		}

		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			SLE4428_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			SLE4428_Activity activity = weakActivity.get();
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

		result = m4428.setPower(Reader.CCID_POWERON);
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
		result = m4428.setPower(Reader.CCID_POWEROFF);
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
