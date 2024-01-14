package com.alcorlink.smartcard;


import amlib.ccid.Reader;
import amlib.ccid.ReaderAT88SC102;
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

public class AT88SC102_Activity extends AppCompatActivity {

	private ReaderAT88SC102 m88SC102;
	private String mStrMessage;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;
	private Button mBtOpen;
	private Button mBtClose;
	private Button mBtPower;
	private Button mBtSetFPinH;
	private Button mBtSetFPinL;
	private Button mBtSetRPinH;
	private Button mBtSetRPinL;
	private Button mBtReset;
	private Button mBtBitsRead;
	private Button mBtBitsIncAddr;
	private Button mBtBitsGotoAddr;
	private Button mBtBitsGetCurAddr; 
	private Button mBtSingleBitWrite; 
	private Button mBtSingleBitErase;
	private Button mBtByteIncAddr;
	private Button mBtBytesRead;
	private Button mBtBytesWrite;
	private Button mBtBytesCompare;

	private TextView mTextBitsGetCurAddr;
	private TextView mTextViewResult;

	private EditText mEdtBitsRead;
	private EditText mEdtBitsIncAddr;
	private EditText mEdtBitsGotoAddr;
	private EditText mEdtByteIncAddr;
	private EditText mEdtBytesRead;
	private EditText mEdtBytesWrite;
	private EditText mEdtBytesCompare;


	private ProgressDialog mWaitProgress; 

	private static final String TAG = "Alcor-AT88SC102";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_at88sc102);

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
				closeReaderUp(AT88SC102_Activity.this);
				closeReaderBottom(AT88SC102_Activity.this);

			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}
			Log.d(TAG,"finish");
			AT88SC102_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}

	public void OnClickAT88SC102Open (View view){
		requestDevPerm();
	}

	public void OnClickAT88SC102Close (View view){
		new CloseTask(AT88SC102_Activity.this).execute();
	}

	public void OnClickAT88SC102Power(View view){
		mPowerDialog.show();
	}


	public void OnClickAT88SC102SetFusPinH (View view){
		int result;
		result = m88SC102.AT88SC102Cmd_SetFusPinHigh();
		showResult("SetFusPinHigh", result);
	}

	public void OnClickAT88SC102SetFusPinL (View view){
		int result;
		result = m88SC102.AT88SC102Cmd_SetFusPinLow();
		showResult("SetFusPinLow", result);
	}

	public void OnClickAT88SC102SetRstPinH (View view){
		int result;
		result = m88SC102.AT88SC102Cmd_SetRstPinHigh();
		showResult("SetRstPinHigh", result);
	}

	public void OnClickAT88SC102SetRstPinL (View view){
		int result;
		result = m88SC102.AT88SC102Cmd_SetRstPinLow();
		showResult("SetRstPinLow", result);
	}

	public void OnClickAT88SC102Reset (View view){
		int result;
		result = m88SC102.AT88SC102Cmd_OpReset();
		showResult("Reset", result);
	}

	public void OnClickAT88SC102BitsRead (View view){
		int []pLen = new int[1];
		int result;
		byte []pDataBuffer = new byte[256];
		//get addr
		result = getEditTextDec(mEdtBitsRead, pLen, 3);
		if (result != 0)
		{
			return;
		}

		result = m88SC102.AT88SC102Cmd_OpBitsRead(pLen[0], pDataBuffer);
		if (result == 0)
		{
			//shows data
			showBufResult(pLen[0], pDataBuffer);
		}
		else
			showResult("BitsRead", result);
	}

	public void OnClickAT88SC102BitsIncAddr (View view){
		int []pLen = new int[1];
		int result;
		//get addr
		result = getEditTextDec(mEdtBitsIncAddr, pLen, 3);
		if (result != 0)
		{
			return;
		}
		result = m88SC102.AT88SC102Cmd_OpBitsIncAddr(pLen[0]);
		showResult("BitsIncAddr", result);
	}


	public void  OnClickAT88SC102BitsGoAddr(View view){
		int []pAddr = new int[1];
		int result;
		//get addr
		result = getEditTextDec(mEdtBitsGotoAddr, pAddr, 4);
		if (result != 0)
		{
			return;
		}

		result = m88SC102.AT88SC102Cmd_OpBitsGotoAddr(pAddr[0]);
		showResult("BitsGotoAddr", result);
	}

	public void  OnClickAT88SC102BitsGetCurAddr(View view){
		int []pAddr = new int[1];
		int result;
		result = m88SC102.AT88SC102Cmd_OpBitGetAddr(pAddr);
		showResult("BitGetAddr", result);
		if (result != 0)
		{
			return;
		}
		mTextBitsGetCurAddr.setText("Current Address(Dec):"+ Integer.toString(pAddr[0]&0x0000ffff));

	}

	public void OnClickAT88SC102SingleBitWrite (View view){
		int result;
		result = m88SC102.AT88SC102Cmd_OpSingleBitWrite();
		showResult("SingleBitWrite", result);
	}

	public void  OnClickAT88SC102SingleBitErase(View view){
		int result;
		result = m88SC102.AT88SC102Cmd_OpSingleBitErase();
		showResult("SingleBitErase", result);
	}

	public void OnClickAT88SC102ByteIncAddr (View view){
		int []pAddr = new int[1];
		int result;
		//byte []pDataBuffer = new byte[256];
		//get addr
		result = getEditTextDec(mEdtByteIncAddr, pAddr, 3);
		if (result != 0)
		{
			return;
		}

		result = m88SC102.AT88SC102Cmd_OpBytesIncAddr(pAddr[0]);
		showResult("ByteIncAddr", result);
	}

	public void OnClickAT88SC102ByteRead (View view){
		int []pLen = new int[1];
		byte []pDataBuffer = new byte[256];
		int result;
		//byte []pDataBuffer = new byte[256];
		//get addr
		result = getEditTextDec(mEdtBytesRead, pLen, 3);
		if (result != 0)
		{
			return;
		}

		result = m88SC102.AT88SC102Cmd_OpBytesRead(pLen[0], pDataBuffer);
		if (result == 0)
		{
			//shows data
			showBufResult(pLen[0], pDataBuffer);
		}
		else
			showResult("ByteRead", result);
	}

	public void  OnClickAT88SC102ByteWrite(View view){
		byte []pDataBuffer = new byte[256];
		byte []pReturnLength = new byte [1];
		int result;
		//byte []pDataBuffer = new byte[256];
		//get addr
		result = getEditTextHex(mEdtBytesWrite, pDataBuffer, 96, pReturnLength);
		if (result != 0)
		{
			return;
		}
		result = m88SC102.AT88SC102Cmd_OpBytesWrite(pReturnLength[0], pDataBuffer);

		showResult("ByteWrite", result);
	}

	public void OnClickAT88SC102ByteCompare (View view){
		byte []pDataBuffer = new byte[256];
		byte []pReturnLength = new byte [1];
		int result;
		//byte []pDataBuffer = new byte[256];
		//get addr
		result = getEditTextHex(mEdtBytesCompare, pDataBuffer, 96, pReturnLength);
		if (result != 0)
		{
			return;
		}

		result = m88SC102.AT88SC102Cmd_OpBytesComp(pReturnLength[0], pDataBuffer);

		showResult("ByteCompare", result);
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



	private static void closeReaderBottom(AT88SC102_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}
	private static int closeReaderUp(AT88SC102_Activity act ){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.m88SC102 != null)
		{
			ret = act.m88SC102.close();
		}
		return ret;
	}

	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<AT88SC102_Activity> weakActivity;

		CloseTask(AT88SC102_Activity myActivity) {
			this.weakActivity = new WeakReference<AT88SC102_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			AT88SC102_Activity activity = weakActivity.get();
			setUpWaitDialog(activity,"Closing Reader");

		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;

			AT88SC102_Activity activity = weakActivity.get();
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
			AT88SC102_Activity activity = weakActivity.get();
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
	


	//=============================================================================

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


	private int getEditTextHex(EditText edt, byte []pBuf, int length, byte []pRetLen)
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

	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/
		try{
			ret = m88SC102.getCardStatus(pCardStatus);
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
		}
		catch (Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			mTextViewResult.setText( mStrMessage); 
		}
		return ret;
	}

	private static int InitReader(AT88SC102_Activity act)
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
			act.m88SC102 = new ReaderAT88SC102(act.mMyDev);
			Status = act.m88SC102.open();
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



	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<AT88SC102_Activity> weakActivity;

		OpenTask(AT88SC102_Activity myActivity) {
			this.weakActivity = new WeakReference<AT88SC102_Activity>(myActivity);
		}

		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			AT88SC102_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			AT88SC102_Activity activity = weakActivity.get();
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



	private static void onCreateButtonSetup(AT88SC102_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtSetFPinH.setEnabled(false);
		act.mBtSetFPinL.setEnabled(false);
		act.mBtSetRPinH.setEnabled(false);
		act.mBtSetRPinL.setEnabled(false);
		act.mBtReset.setEnabled(false);
		act.mBtBitsRead.setEnabled(false);
		act.mBtBitsIncAddr.setEnabled(false);
		act.mBtBitsGotoAddr.setEnabled(false);
		act.mBtBitsGetCurAddr.setEnabled(false);
		act.mBtSingleBitWrite.setEnabled(false);
		act.mBtSingleBitErase.setEnabled(false);
		act.mBtByteIncAddr.setEnabled(false);
		act.mBtBytesRead.setEnabled(false);
		act.mBtBytesWrite.setEnabled(false);
		act.mBtBytesCompare.setEnabled(false);
	}

	private static void onOpenButtonSetup(AT88SC102_Activity act){
		act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(true);
		act.mBtPower.setEnabled(true);
		act.mBtSetFPinH.setEnabled(true);
		act.mBtSetFPinL.setEnabled(true);
		act.mBtSetRPinH.setEnabled(true);
		act.mBtSetRPinL.setEnabled(true);
		act.mBtReset.setEnabled(true);
		act.mBtBitsRead.setEnabled(true);
		act.mBtBitsIncAddr.setEnabled(true);
		act.mBtBitsGotoAddr.setEnabled(true);
		act.mBtBitsGetCurAddr.setEnabled(true);
		act.mBtSingleBitWrite.setEnabled(true);
		act.mBtSingleBitErase.setEnabled(true);
		act.mBtByteIncAddr.setEnabled(true);
		act.mBtBytesRead.setEnabled(true);
		act.mBtBytesWrite.setEnabled(true);
		act.mBtBytesCompare.setEnabled(true);

	}





	private void onDetach(Intent intent){
		UsbDevice   udev = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (udev != null ) {
			if (udev.equals(mUsbDev) ){
				closeReaderUp(AT88SC102_Activity.this);
				closeReaderBottom(AT88SC102_Activity.this);
				AT88SC102_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}
	}

	private void onDevPermit(UsbDevice dev){
		try {    	
			setUpWaitDialog(AT88SC102_Activity.this,"Opening...");
			new OpenTask(AT88SC102_Activity.this).execute(dev);
		}
		catch(Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
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

		try {
			result = m88SC102.setPower(Reader.CCID_POWERON);
		}catch(Exception e){
			Log.e(TAG, "PowerON Get Exception : " + e.getMessage());
		}
		Log.e(TAG,"power on exit");
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
		try {
			result = m88SC102.setPower(Reader.CCID_POWEROFF);	
		}catch(Exception e){
			Log.e(TAG, "PowerOFF Get Exception : " + e.getMessage());
		}	
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


	private void setButtonView()
	{
		mBtOpen = (Button)findViewById(R.id.AT88SC102buttonOpen);
		mBtClose = (Button)findViewById(R.id.AT88SC102buttonClose);
		mBtPower = (Button)findViewById(R.id.AT88SC102buttonPower);
		mBtSetFPinH = (Button)findViewById(R.id.AT88SC102buttonSetFusPinH);
		mBtSetFPinL = (Button)findViewById(R.id.AT88SC102buttonSetFusPinL);
		mBtSetRPinH = (Button)findViewById(R.id.AT88SC102buttonSetRstPinH);
		mBtSetRPinL = (Button)findViewById(R.id.AT88SC102buttonSetRstPinL);
		mBtReset = (Button)findViewById(R.id.AT88SC102buttonReset);
		mBtBitsRead = (Button)findViewById(R.id.AT88SC102buttonBitsRead);
		mBtBitsIncAddr = (Button)findViewById(R.id.AT88SC102buttonBitsIncAddr);
		mBtBitsGotoAddr = (Button)findViewById(R.id.AT88SC102buttonBitsGoAddr );
		mBtBitsGetCurAddr = (Button)findViewById(R.id.AT88SC102buttonBitsGetCurAddr );
		mBtSingleBitWrite = (Button)findViewById(R.id.AT88SC102buttonSingleBitWrite);
		mBtSingleBitErase = (Button)findViewById(R.id.AT88SC102buttonSingleBitErase);
		mBtByteIncAddr = (Button)findViewById(R.id.AT88SC102buttonByteIncAddr);
		mBtBytesRead = (Button)findViewById(R.id.AT88SC102buttonByteRead);
		mBtBytesWrite = (Button)findViewById(R.id.AT88SC102buttonByteWrite);
		mBtBytesCompare = (Button)findViewById(R.id.AT88SC102buttonByteCompare);

	}

	private void setEditView()
	{
		mEdtBitsRead = (EditText)findViewById(R.id.AT88SC102EditTextBitsRead);
		mEdtBitsIncAddr = (EditText)findViewById(R.id.AT88SC102EditTextBitsIncAddr);
		mEdtBitsGotoAddr = (EditText)findViewById(R.id.AT88SC102EditTextBitsGoAddr);
		mEdtByteIncAddr = (EditText)findViewById(R.id.AT88SC102EditTextByteIncAddr);
		mEdtBytesRead = (EditText)findViewById(R.id.AT88SC102EditTextByteRead);
		mEdtBytesWrite = (EditText)findViewById(R.id.AT88SC102EditTextByteWrite);
		mEdtBytesCompare = (EditText)findViewById(R.id.AT88SC102EditTextByteCompare);
		//mmEdtResult = (EditText)findViewById(R.id.

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
		mTextBitsGetCurAddr = (TextView)findViewById(R.id.AT88SC102TextBitsGetCurAddr);
		mTextViewResult = (TextView)findViewById(R.id.AT88SC102TextResult);

	}

	private static void setUpWaitDialog(AT88SC102_Activity act,String msg){
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
		onCreateButtonSetup(AT88SC102_Activity.this);
		mWaitProgress =  new ProgressDialog(AT88SC102_Activity.this);
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
			ccidErrCode = Integer.toHexString(m88SC102.getCmdFailCode() & 0x0000ffff);
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
