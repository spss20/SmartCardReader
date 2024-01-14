package com.alcorlink.smartcard;






import amlib.ccid.Reader;
import amlib.ccid.ReaderAT24C;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class AT24C_Activity extends AppCompatActivity {

	private String mStrMessage;
	private ReaderAT24C m24C;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;
	private Button mBtOpen;
	private Button mBtClose;
	private Button mBtPower;
	private Button mBtWrite;
	private Button mBtRead;

	private TextView mTextViewResult;

	private EditText mEdtWriteAddr;
	private EditText mEdtWriteData;
	private EditText mEdtReadAddr;
	private EditText mEdtPageSize;
	private EditText mEdtReadLen;

	private byte mCardType;

	private ProgressDialog mWaitProgress; 

	private static final String TAG = "Alcor-AT24C";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";
	private static final int MAX_WRITE_LENGTH = 128*1024;
	private static final byte CARD_TYPE_SINGLE = 0;
	private static final byte CARD_TYPE_DOUBLE = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_at24c);

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
	

	public void OnClickAT24COpen (View view){
			requestDevPerm();
	}

	public void OnClickAT24CClose (View view){
		new CloseTask(this).execute();
	}

	public void OnClickAT24CPower(View view){
		mPowerDialog.show();
	}



	public void OnClickAT24CWrite(View view){
		int []pAddr = new int[1];
		byte []pPageSize = new byte[1];
		byte []pWrite = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];

		//get data
		int result;
		//get addr
		result = getAddress(mEdtWriteAddr, pAddr);
		if (result != 0)
		{
			return;
		}
		//get page size
		result = getEditTextHex(mEdtPageSize, pPageSize, 2,pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		//get data
		result = getEditTextHex(mEdtWriteData, pWrite, MAX_WRITE_LENGTH, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		Log.d(TAG, "pAddress="+ Integer.toHexString(pAddr[0]));
		Log.d(TAG, "pPageSize="+ Integer.toHexString(pPageSize[0]));
		result = m24C.AT24CSeriesCmd_Write(pAddr[0], mCardType, pPageSize[0], pIntReturnLen[0], pWrite); 
		showResult("Write", result);
	}

	public void OnClickAT24CRead(View view){
		int []pAddr = new int[1];
		int []pLen = new int[1];
		byte []pRxBuf = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];
		int result;
		//get addr
		result = getAddress(mEdtReadAddr, pAddr);
		if (result != 0)
		{
			return;
		}
		//get Length, 2 bytes
		result = getAddress(mEdtReadLen, pLen );
		if (result != 0)
		{
			return;
		}
		Log.d(TAG, "pAddress="+ Integer.toHexString(pAddr[0]));
		Log.d(TAG, "pLen="+ Integer.toHexString(pLen[0]));
		result = m24C.AT24CSeriesCmd_Read(pAddr[0], mCardType, pLen[0], pRxBuf, pIntReturnLen);
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			//shows data
			showBufResult(pIntReturnLen[0], pRxBuf);
		}
		else
			showResult("Read", result);
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

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
			try {
				closeReaderUp(this);
				closeReaderBottom(this);

			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}
			Log.d(TAG,"finish");
			AT24C_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}




	private static int closeReaderUp(AT24C_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.m24C != null)
		{
			ret = act.m24C.close();
		}
		return ret;
	}

	private static void closeReaderBottom(AT24C_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}


	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<AT24C_Activity> weakActivity;

		CloseTask(AT24C_Activity myActivity) {
			this.weakActivity = new WeakReference<AT24C_Activity>(myActivity);
		}


		@Override 
		protected void onPreExecute() {
			AT24C_Activity activity = weakActivity.get();
			setUpWaitDialog(activity, "Closing Reader");

		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			AT24C_Activity activity = weakActivity.get();
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
			AT24C_Activity activity = weakActivity.get();
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



	private int getAddress(EditText edt, int []pAddress) {
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
			pAddress[0] |= pBuf[i]<< (8*(pIntReturnLen[0]-1-i));
		}
		return 0;
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

	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/

		ret = m24C.getCardStatus(pCardStatus);
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


	private static int InitReader(AT24C_Activity act)
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
			act.m24C = new ReaderAT24C(act.mMyDev);
			Status = act.m24C.open();
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


	private RadioGroup.OnCheckedChangeListener listener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.radioSingle:
				Log.d(TAG, "select Single");
				mCardType = CARD_TYPE_SINGLE;
				break;
			case R.id.radioDouble:
				Log.d(TAG, "select duble");
				mCardType = CARD_TYPE_DOUBLE;
				break;
			}
		}
	};

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


	private static void onCreateButtonSetup(AT24C_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtWrite.setEnabled(false);
		act.mBtRead.setEnabled(false);
	}

	
	
	private static void onOpenButtonSetup(AT24C_Activity act){
		act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(true);
		act.mBtPower.setEnabled(true);
		act.mBtWrite.setEnabled(true);
		act.mBtRead.setEnabled(true);
	}

	private void onDevPermit(UsbDevice dev){
		try {    		
			setUpWaitDialog(this,"Opening...");
			new OpenTask(this).execute(dev);
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
				closeReaderUp(AT24C_Activity.this);
				closeReaderBottom(AT24C_Activity.this);
				AT24C_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}

	}


	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<AT24C_Activity> weakActivity;

		OpenTask(AT24C_Activity myActivity) {
			this.weakActivity = new WeakReference<AT24C_Activity>(myActivity);
		}
		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			AT24C_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			AT24C_Activity activity = weakActivity.get();
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


		result = m24C.setPower(Reader.CCID_POWERON);
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
		result = m24C.setPower(Reader.CCID_POWEROFF);

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
		mBtOpen = (Button)findViewById(R.id.AT24CbuttonOpen);
		mBtClose = (Button)findViewById(R.id.AT24CbuttonClose);
		mBtPower = (Button)findViewById(R.id.AT24Cbuttonpower);
		mBtWrite = (Button)findViewById(R.id.AT24CbuttonWrite);
		mBtRead = (Button)findViewById(R.id.AT24CbuttonRead);

	}

	private void setEditView()
	{
		mEdtWriteAddr = (EditText)findViewById(R.id.AT24CEditTextWriteAddr);
		mEdtWriteData = (EditText)findViewById(R.id.AT24CEditTexData);
		mEdtReadAddr = (EditText)findViewById(R.id.AT24CEditTextReadAddr);
		mEdtPageSize = (EditText)findViewById(R.id.AT24CEditTexPageSize);
		mEdtReadLen = (EditText)findViewById(R.id.AT24CEditTexLen);
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

	private void setRadioButtonView()
	{
		mCardType = CARD_TYPE_SINGLE;
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);        
		radioGroup.setOnCheckedChangeListener( listener);
		RadioButton bt = (RadioButton) findViewById(R.id.radioSingle);
		bt.setChecked(true);
	}


	private void setTextView()
	{
		mTextViewResult = (TextView)findViewById(R.id.AT24CTextResult);
	}

	private void setupViews(){
		Log.d(TAG,"setupViews");
		setButtonView();
		setTextView();
		setEditView();
	
		setPowerView();

		setRadioButtonView();
		onCreateButtonSetup(AT24C_Activity.this);
		mWaitProgress =  new ProgressDialog(AT24C_Activity.this);  
		//setReaderSlotView();
	}



	private static void setUpWaitDialog(AT24C_Activity act, String msg){

		act.mWaitProgress.setMessage(msg);
		act.mWaitProgress.setCancelable(false);
		act.mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		act.mWaitProgress.show();
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
			ccidErrCode = Integer.toHexString(m24C.getCmdFailCode() & 0x0000ffff);
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

