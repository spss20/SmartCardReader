package com.alcorlink.smartcard;


import amlib.ccid.Reader;
import amlib.ccid.ReaderAT45D041;
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

public class AT45D041_Activity extends AppCompatActivity {

	private String mStrMessage;
	private ReaderAT45D041 m45D;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;
	private Button mBtOpen;
	private Button mBtClose;
	private Button mBtPower;
	private Button mBtMainMemRead;
	private Button mBtBuff1Read;
	private Button mBtBuff2Read;
	private Button mBtMainMemPagetoBuf1Xfr;
	private Button mBtMainMemPagetoBuf2Xfr;
	private Button mBtMainMemPagetoBuf1Comp;
	private Button mBtMainMemPagetoBuf2Comp;
	private Button mBtBuff1Write;
	private Button mBtBuff2Write;
	private Button mBtB1toMwithErase;
	private Button mBtB2toMwithErase;
	private Button mBtB1toMwithoutErase;
	private Button mBtB2toMwithoutErase;
	private Button mBtMemProgThrB1;
	private Button mBtMemProgThrB2;
	private Button mBtAutoReWriteThrB1;
	private Button mBtAutoReWriteThrB2;
	private Button mBtGetStatusReg;

	private TextView mTextViewResult;
	//private TextView mTextView

	private EditText mEdtPageNum;
	private EditText mEdtAddress;
	private EditText mEdtLength;
	private EditText mEdtDatawrite;

	private ProgressDialog mWaitProgress; 

	private static final String TAG = "Alcor-AT45D041";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";
	protected static final byte CARD_TYPE_SINGLE = 0;
	protected static final byte CARD_TYPE_DOUBLE = 1;

	private final static byte Op_MainMRead = (byte)0x52;
	private final static byte Op_MainPage2Buff1 = (byte)0x53 ;
	private final static byte Op_Buff1Read = (byte)0x54 ;
	private final static byte Op_MainPage2Buff2 = (byte)0x55 ;
	private final static byte Op_Buff2Read = (byte)0x56 ;
	private final static byte Op_StatusRegRead = (byte)0x57 ;
	private final static byte Op_AutoPageReWriteBuff1 = (byte)0x58 ;
	private final static byte Op_AutoPageReWriteBuff2 = (byte)0x59 ;
	private final static byte Op_MainMPageBuff1Comp = (byte)0x60 ;
	private final static byte Op_MainMPageBuff2Comp = (byte)0x61 ;
	private final static byte Op_MainMPageProgBuff1 = (byte)0x82 ;
	private final static byte Op_Buff1toMainMPageProgWithErase = (byte)0x83 ;
	private final static byte Op_Buff1Write = (byte)0x84 ;
	private final static byte Op_MainMPageProgBuff2 = (byte)0x85 ;
	private final static byte Op_Buff2toMainMPageProgWithErase = (byte)0x86 ;
	private final static byte Op_Buff2Write = (byte)0x87 ;
	private final static byte Op_Buff1toMainMPageProgWithoutErase = (byte)0x88 ;
	private final static byte Op_Buff2toMainMPageProgWithoutErase = (byte)0x89 ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_at45_d041);

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
				closeReaderUp(AT45D041_Activity.this);
				closeReaderBottom(AT45D041_Activity.this);
			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}
			Log.d(TAG,"finish");
			AT45D041_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}

	public void OnClickAT45D041Open(View view){
		requestDevPerm();
	}

	public void OnClickAT45D041Close(View view){
		new CloseTask(AT45D041_Activity.this).execute();
	}

	public void OnClickAT45D041Power(View view){
		mPowerDialog.show();
	}

	public void OnClickAT45D041MainMemRead(View view){
		commondCmd("MainMemRead", Op_MainMRead);
	}

	public void OnClickAT45D041Buff1Read(View view){
		commondCmd("Buff1 Read", Op_Buff1Read);
	}

	public void OnClickAT45D041Buff2Read(View view){
		commondCmd("Buff2 Read", Op_Buff2Read);
	}

	public void OnClickAT45D041MainMemPagetoBuf1Xfr(View view){
		commondCmd("MainMemPage to Buf1 Xfr ", Op_MainPage2Buff1);
	}

	public void OnClickAT45D041MainMemPagetoBuf2Xfr (View view){
		commondCmd("MainMemPage to Buf2 Xfr", Op_MainPage2Buff2);
	}

	public void OnClickAT45D041MainMemPagetoBuf1Comp(View view){
		commondCmd("MainMemPage to Buf1 Compare", Op_MainMPageBuff1Comp);
	}

	public void OnClickAT45D041MainMemPagetoBuf2Comp(View view){
		commondCmd("MainMemPage to Buf2 Compare", Op_MainMPageBuff2Comp);
	}

	public void OnClickAT45D041Buff1Write(View view){
		commondCmd("Buff1Write", Op_Buff1Write);
	}

	public void OnClickAT45D041Buff2Write(View view){
		commondCmd("Buff2 Write", Op_Buff2Write);
	}

	public void OnClickAT45D041B1toMwithErase(View view){
		commondCmd("Buffer1to MainMemmory with Erase", Op_Buff1toMainMPageProgWithErase);
	}

	public void OnClickAT45D041B2toMwithErase(View view){
		commondCmd("Buffer2 to MainMemmory with Erase", Op_Buff2toMainMPageProgWithErase);
	}

	public void OnClickAT45D041B1toMwithoutErase(View view){
		commondCmd("Buffer1 to MainMemmory without Erase", Op_Buff1toMainMPageProgWithoutErase);
	}

	public void OnClickAT45D041B2toMwithoutErase(View view){
		commondCmd("Buffer2 to MainMemmory without Erase" ,Op_Buff2toMainMPageProgWithoutErase);
	}

	public void OnClickAT45D041MemProgThrB1(View view){
		commondCmd("Mem Program Through Buffer1", Op_MainMPageProgBuff1);
	}

	public void OnClickAT45D041MemProgThrB2(View view){
		commondCmd("Mem Program Through Buffer2", Op_MainMPageProgBuff2);
	}

	public void OnClickAT45D041AutoReWriteThrB1(View view){
		commondCmd("AutoReWrite Through Buffer1", Op_AutoPageReWriteBuff1);
	}
	public void OnClickAT45D041AutoReWriteThrB2(View view){
		commondCmd("AutoReWrite Through Buffer2", Op_AutoPageReWriteBuff2);
	}
	public void OnClickAT45D041GetStatusReg(View view){
		byte []pWriteData = new byte [1];
		int []pIntReadLen = new int [1];
		byte []pReadData = new byte[1];
		int []pIntReturnLen = new int [1];
		int result;
		pIntReadLen[0] = 1;
		result = m45D.AT45D041Cmd(
				Op_StatusRegRead, 
				0, 
				0, 
				0, 
				pWriteData, 
				pIntReadLen[0], 
				pReadData, 
				pIntReturnLen);
		if (result == 0)
			showBufResult(pIntReturnLen[0], pReadData);
		else
			showResult("GetStatusReg" ,result);
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
	private static int closeReaderUp(AT45D041_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;
		if (act.m45D != null)
		{
			ret = act.m45D.close();
		}
		return ret;
	}

	private static void closeReaderBottom(AT45D041_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}




	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<AT45D041_Activity> weakActivity;

		CloseTask(AT45D041_Activity myActivity) {
			this.weakActivity = new WeakReference<AT45D041_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			AT45D041_Activity activity = weakActivity.get();
			setUpWaitDialog(activity,"Closing Reader");

		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			AT45D041_Activity activity = weakActivity.get();
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
			AT45D041_Activity activity = weakActivity.get();
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


	private void commondCmd(String cmd, byte opCode) {
		int []pIntPageNo = new int [1];
		int []pIntStartAddr = new int[1];
		int intWriteLen;
		byte []pWriteData = new byte [256];
		int []pIntReadLen = new int [1];
		byte []pReadData = new byte[256];
		int []pIntReturnLen = new int [1];
		int result;
		getEditTextInt(mEdtPageNum, pIntPageNo);
		getEditTextInt(mEdtAddress, pIntStartAddr);

		switch (opCode)
		{
		case Op_MainMRead:
		case Op_Buff1Read:
		case Op_Buff2Read:
			intWriteLen = 0;
			getEditTextInt(mEdtLength, pIntReadLen);
			break;
		case Op_Buff1Write:
		case Op_Buff2Write:
		case Op_MainMPageProgBuff1:
		case Op_MainMPageProgBuff2:
			getEditTextHex(mEdtDatawrite, pWriteData, 256*2, pIntReturnLen);
			intWriteLen = pIntReturnLen[0];
			break;
		default :
			pIntReadLen[0] = 0;
			intWriteLen = 0;
			break;
		}
		result = m45D.AT45D041Cmd(
				opCode, 
				pIntPageNo[0], 
				pIntStartAddr[0], 
				intWriteLen, 
				pWriteData, 
				pIntReadLen[0], 
				pReadData, 
				pIntReturnLen);

		switch (opCode)
		{
		case Op_MainMRead:
		case Op_Buff1Read:
		case Op_Buff2Read:
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			//shows data
			showBufResult(pIntReturnLen[0], pReadData);
			return;
		}
		showResult(cmd, result);
	}

	/*
	 * length: number of max digits in this field
	 * */ 
	private int getEditTextHex(EditText edt, byte []pBuf, int length, int []pReturnLen)
	{
		byte []pByteArrary;
		String strText; 
		int len;
		strText = edt.getText().toString();
		len = strText.length() ;
		Log.d(TAG, "get length="+len);
		if (len > length || len == 0 || (len%2 !=0))
		{
			mStrMessage = "Wrong length in text Fields ";
			Log.e(TAG, mStrMessage);
			mTextViewResult.setText(mStrMessage);
			return -1;
		}
		pByteArrary = MainActivity.toByteArray(strText);
		Log.d(TAG, "pByteArrary length="+pByteArrary.length);
		System.arraycopy(pByteArrary, 0, pBuf, 0, pByteArrary.length);
		pReturnLen[0] = pByteArrary.length;
		Log.d(TAG, "pBuf[0]=0x"+Integer.toHexString(pBuf[0]));
		return 0;
	}



	private int getEditTextInt(EditText edt, int []pData) {
		byte []pBuf = new byte[4];
		int []pIntReturnLen = new int[1];

		int result;
		result = getEditTextHex(edt, pBuf, 6, pIntReturnLen);//MAX 3 bytes
		if (result != 0)
		{
			return -1;
		}

		for (int i=(pIntReturnLen[0]-1);i>=0;i--)
		{
			pData[0] |= pBuf[i]<< (8*(pIntReturnLen[0]-1-i));
		}
		return 0;
	}

	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/

			ret = m45D.getCardStatus(pCardStatus);
			if (ret == SCError.READER_SUCCESSFUL){	
				if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_ABSENT ){
					ret = SCError.READER_NO_CARD;
				}
				else if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_INACTIVE)
				{
					ret = SCError.READER_CARD_INACTIVE;
				}
				else 
					ret = SCError.READER_SUCCESSFUL;
			}

		return ret;
	}

	private static int InitReader(AT45D041_Activity act)
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
			act.m45D = new ReaderAT45D041(act.mMyDev);
			Status = act.m45D.open();
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




	private static void onCreateButtonSetup(AT45D041_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtMainMemRead.setEnabled(false);
		act.mBtBuff1Read.setEnabled(false);
		act.mBtBuff2Read.setEnabled(false);
		act.mBtMainMemPagetoBuf1Xfr.setEnabled(false);
		act.mBtMainMemPagetoBuf2Xfr.setEnabled(false);
		act.mBtMainMemPagetoBuf1Comp.setEnabled(false);
		act.mBtMainMemPagetoBuf2Comp.setEnabled(false);
		act.mBtBuff1Write.setEnabled(false);
		act.mBtBuff2Write.setEnabled(false);
		act.mBtB1toMwithErase.setEnabled(false);
		act.mBtB2toMwithErase.setEnabled(false);
		act.mBtB1toMwithoutErase.setEnabled(false);
		act.mBtB2toMwithoutErase.setEnabled(false);
		act.mBtMemProgThrB1.setEnabled(false);
		act.mBtMemProgThrB2.setEnabled(false);
		act.mBtAutoReWriteThrB1.setEnabled(false);
		act.mBtAutoReWriteThrB2.setEnabled(false);
		act.mBtGetStatusReg.setEnabled(false);
	}

	private void onDevPermit(UsbDevice dev){
		try {    		
			setUpWaitDialog(AT45D041_Activity.this,"Opening...");
			new OpenTask(AT45D041_Activity.this).execute(dev);
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
				closeReaderUp(AT45D041_Activity.this);
				closeReaderBottom(AT45D041_Activity.this);
				AT45D041_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}
	}

	private static void onOpenButtonSetup(AT45D041_Activity act){
		act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(true);
		act.mBtPower.setEnabled(true);
		act.mBtMainMemRead.setEnabled(true);
		act.mBtBuff1Read.setEnabled(true);
		act.mBtBuff2Read.setEnabled(true);
		act.mBtMainMemPagetoBuf1Xfr.setEnabled(true);
		act.mBtMainMemPagetoBuf2Xfr.setEnabled(true);
		act.mBtMainMemPagetoBuf1Comp.setEnabled(true);
		act.mBtMainMemPagetoBuf2Comp.setEnabled(true);
		act.mBtBuff1Write.setEnabled(true);
		act.mBtBuff2Write.setEnabled(true);
		act.mBtB1toMwithErase.setEnabled(true);
		act.mBtB2toMwithErase.setEnabled(true);
		act.mBtB1toMwithoutErase.setEnabled(true);
		act.mBtB2toMwithoutErase.setEnabled(true);
		act.mBtMemProgThrB1.setEnabled(true);
		act.mBtMemProgThrB2.setEnabled(true);
		act.mBtAutoReWriteThrB1.setEnabled(true);
		act.mBtAutoReWriteThrB2.setEnabled(true);
		act.mBtGetStatusReg.setEnabled(true);
	}


	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<AT45D041_Activity> weakActivity;

		OpenTask(AT45D041_Activity myActivity) {
			this.weakActivity = new WeakReference<AT45D041_Activity>(myActivity);
		}

		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			AT45D041_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			AT45D041_Activity activity = weakActivity.get();
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
			return SCError.READER_NO_CARD;
		case SCError.READER_CARD_INACTIVE:
		case SCError.READER_SUCCESSFUL:
			break;
		default://returns other error case
			return result;
		}

		result = m45D.setPower(Reader.CCID_POWERON);

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
			return SCError.READER_NO_CARD;
		case SCError.READER_CARD_INACTIVE:
		case SCError.READER_SUCCESSFUL:
			break;
		default://returns other error case
			return result;
		}
		//----------poweroff card------------------
		result = m45D.setPower(Reader.CCID_POWEROFF);
		return result;
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

	private static void setUpWaitDialog(AT45D041_Activity act, String msg){

		act.mWaitProgress.setMessage(msg);
		act.mWaitProgress.setCancelable(false);
		act.mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		act.mWaitProgress.show();
	}

	private void setupViews(){
		Log.d(TAG,"setupViews");
		setButtonView();
		setTextView();
		setEditView();
		setPowerView();

		onCreateButtonSetup(AT45D041_Activity.this);
		mWaitProgress =  new ProgressDialog(AT45D041_Activity.this);  
		//setReaderSlotView();
	}

	private void setButtonView()
	{
		mBtOpen = (Button)findViewById(R.id.AT45D041buttonOpen);
		mBtClose = (Button)findViewById(R.id.AT45D041buttonClose);
		mBtPower = (Button)findViewById(R.id.AT45D041buttonPower);
		mBtMainMemRead = (Button)findViewById(R.id.AT45D041buttonMainMemRead);
		mBtBuff1Read = (Button)findViewById(R.id.AT45D041buttonBuff1Read);
		mBtBuff2Read = (Button)findViewById(R.id.AT45D041buttonBuff2Read);
		mBtMainMemPagetoBuf1Xfr = (Button)findViewById(R.id.AT45D041buttonMainMemPagetoBuf1Xfr);
		mBtMainMemPagetoBuf2Xfr = (Button)findViewById(R.id.AT45D041buttonMainMemPagetoBuf2Xfr);
		mBtMainMemPagetoBuf1Comp = (Button)findViewById(R.id.AT45D041buttonMainMemPagetoBuf1Comp);
		mBtMainMemPagetoBuf2Comp = (Button)findViewById(R.id.AT45D041buttonMainMemPagetoBuf2Comp);
		mBtBuff1Write = (Button)findViewById(R.id.AT45D041buttonBuff1Write);
		mBtBuff2Write = (Button)findViewById(R.id.AT45D041buttonBuff2Write);
		mBtB1toMwithErase = (Button)findViewById(R.id.AT45D041buttonB1toMwithErase);
		mBtB2toMwithErase = (Button)findViewById(R.id.AT45D041buttonB2toMwithErase);
		mBtB1toMwithoutErase = (Button)findViewById(R.id.AT45D041buttonB1toMwithoutErase);
		mBtB2toMwithoutErase = (Button)findViewById(R.id.AT45D041buttonB2toMwithoutErase);
		mBtMemProgThrB1 = (Button)findViewById(R.id.AT45D041buttonMemProgThrB1);
		mBtMemProgThrB2 = (Button)findViewById(R.id.AT45D041buttonMemProgThrB2);
		mBtAutoReWriteThrB1 = (Button)findViewById(R.id.AT45D041buttonAutoReWriteThrB1);
		mBtAutoReWriteThrB2 = (Button)findViewById(R.id.AT45D041buttonAutoReWriteThrB2);
		mBtGetStatusReg = (Button)findViewById(R.id.AT45D041buttonGetStatusReg);
	}

	private void setEditView()
	{
		mEdtPageNum = (EditText)findViewById(R.id.AT45D041EdtPageNum);
		mEdtAddress = (EditText)findViewById(R.id.AT45D041EdtAddressInBuf);
		mEdtLength = (EditText)findViewById(R.id.AT45D041EdtLength);
		mEdtDatawrite = (EditText)findViewById(R.id.AT45D041EdtData);
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
		mTextViewResult = (TextView)findViewById(R.id.AT45D041TextResult);
	}



	private void showBufResult(int len, byte[]pBuf) {
		String s;
		//		for (int i=0;i<len;i++)
		//		{
		//			 
		//		}
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
			ccidErrCode = Integer.toHexString(m45D.getCmdFailCode() & 0x0000ffff);
			msg = cmd + " " +msg + "(" + ccidErrCode +")";
		}
		mTextViewResult.setText(msg);
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


