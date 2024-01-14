package com.alcorlink.smartcard;


import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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

import amlib.ccid.Reader;
import amlib.ccid.ReaderException;
import amlib.ccid.SCError;
import amlib.hw.HWType;
import amlib.hw.HardwareInterface;
import amlib.hw.ReaderHwException;

public class ISO7816_Activity  extends AppCompatActivity {
	private Reader mReader;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private Builder  mSwitchModeDialog;
	private Builder  mSlotDialog;
	private Builder  mPowerDialog;
	private byte mSlotNum;
	//	private byte mMode;
	private PendingIntent mPermissionIntent;
	private Button mOpenButton;
	private Button mPowerButton;
	private Button mATRButton;
	private Button mAPDUButton;
	private Button mProtocolButton;
	private Button mSNButton;
	private Button mCloseButton;
	private TextView mTextViewResult; 
	private EditText mEditTextApdu;
	private ProgressDialog mWaitProgress;
	private boolean mIsNFC;

	private String mStrMessage;


	private static final String TAG = "Alcor-7816";
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
		
		setContentView(R.layout.activity_7816);
		setupViews();


		mMyDev = new HardwareInterface(HWType.eUSB, this.getApplicationContext());


		// Get USB manager
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		toRegisterReceiver();
		if (mUsbDev == null)
			mTextViewResult.setText("No Device been selected");
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
			try {
				closeReaderUp(ISO7816_Activity.this);
				closeReaderBottom(ISO7816_Activity.this);
				
			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}
			Log.d(TAG,"finish");
			ISO7816_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}


	public void OnClickISO7816Open(View view){
		Log.d(TAG, "7816OpenOnClick");
		if (mUsbDev != null)
			checkSlotNumber(mUsbDev);
	}

	public void OnClickISO7816Close(View view){
		new CloseTask(ISO7816_Activity.this).execute();
	}

	public void OnClickISO7816Power(View view){
		Log.d(TAG, "7816PowerOnClick");
		mPowerDialog.show();


	}
	public void OnClickISO7816getATR(View view){

		String atr;
		Log.d(TAG, "getATROnClick");
		atr = mReader.getAtrString();
		mTextViewResult.setText(" ATR:" + atr);

	}




	public void OnClickISO7816sendAPDUk(View view){
		byte[] pSendAPDU ;
		byte[] pRecvRes = new byte[300];
		int[] pRevAPDULen = new int[1];
		String apduStr;
		int sendLen, result;

		Log.d(TAG, "sendAPDUkOnClick");
		pRevAPDULen[0] = 300;
		apduStr = mEditTextApdu.getText().toString();
		pSendAPDU = MainActivity.toByteArray(apduStr);
		sendLen = pSendAPDU.length;

		result = mReader.transmit(pSendAPDU, sendLen, pRecvRes, pRevAPDULen);
		if (result == SCError.READER_SUCCESSFUL) {
			mTextViewResult.setText("Receive APDU: " + MainActivity.byte2String(pRecvRes, pRevAPDULen[0]));
		} else {
			mTextViewResult.setText("Fail to Send APDU: " + SCError.errorCode2String(result)
					+"(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
			Log.e(TAG, "Fail to Send APDU: " + SCError.errorCode2String(result)
					+ "(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
		}

	}



	public void OnClickISO7816getProtocol(View view){
		int status;
		byte []proto = new byte[1];
		Log.d(TAG, "getProtocolOnClick");


			//status = mReader.getProtocol(proto);
			//status = mReader.badTx();
			status = mReader.getProtocol(proto);


		if (status != SCError.READER_SUCCESSFUL){
			mTextViewResult.setText("getProtocol fail: "+ SCError.errorCode2String(status)
					+ "(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
			Log.e(TAG, "getProtocol fail: "+ SCError.errorCode2String(status)
					+ "(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
		}else{
			if (proto[0] == Reader.CCID_PROTOCOL_T0)
				mTextViewResult.setText("T0");
			if (proto[0] == Reader.CCID_PROTOCOL_T1)
				mTextViewResult.setText("T1");
		}
	}

	public void OnClickISO7816getSN(View view){
		//boolean status;
		int status = 0;
		byte[] pLen = new byte[1];
		byte []pSN;
		Log.d(TAG, "getSNOnClick");
		byte DEFAULT_SN_LEN = 32;
		pLen[0] = DEFAULT_SN_LEN;
		pSN = new byte[pLen[0]];

		status = mReader.getSN(pSN, pLen);

		if (status != SCError.READER_SUCCESSFUL){

			mTextViewResult.setText("getSN fail: " +SCError.errorCode2String(status)
					+ "(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
		}else{
			String str = new String(pSN);
			//mTextSN.setText(logBuffer(pSN, pLen[0]));
			mTextViewResult.setText(str);
		}
	}


	private void checkSlotNumber(UsbDevice uDev){
		mIsNFC = false;
		if(uDev.getProductId() == 0x9522 || uDev.getProductId() == 0x9525 )
			setReaderTwoSlotView();
		else if(uDev.getProductId() == 0x9526 ) 
			setReaderNFCSlotView();
		else if(uDev.getProductId() == 0x9572 )
			setReaderNFCSlotView();
		
		else{
			mSlotNum = (byte)0;
			requestDevPerm();
		}
	}



	private static int closeReaderUp(ISO7816_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.mReader != null)
		{
			ret = act.mReader.close();
		}
		return ret;
	}

	private static void closeReaderBottom(ISO7816_Activity act){
		onCloseButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
		act.mSlotNum = (byte)0;
	}

	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<ISO7816_Activity> weakActivity;

		CloseTask(ISO7816_Activity myActivity) {
			this.weakActivity = new WeakReference<ISO7816_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			ISO7816_Activity activity = weakActivity.get();
			setUpWaitDialog(activity, "Closing Reader");
		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			ISO7816_Activity activity = weakActivity.get();
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
			ISO7816_Activity activity = weakActivity.get();
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


	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/

		ret = mReader.getCardStatus(pCardStatus);
		if (ret == SCError.READER_SUCCESSFUL) {
			if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_ABSENT) {
				ret = SCError.READER_NO_CARD;
			} else if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_INACTIVE) {
				ret = SCError.READER_CARD_INACTIVE;
			} else {
				ret = SCError.READER_SUCCESSFUL;
			}
		}

		return ret;
	}



	private static int InitReader(ISO7816_Activity act)
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
			act.mReader = new Reader(act.mMyDev);
			Status = act.mReader.open();
		}
		catch (ReaderException e)
		{
			act.mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, "InitReader fail "+act.mStrMessage);
			return -1;
		}
		//		catch (Exception e)
		//		{
		//			mStrMessage = "Get Exception : " + e.getMessage();
		//			Log.d(TAG, "InitReader fail "+ mStrMessage);
		//			return -1;
		//		}
		act.mReader.setSlot(act.mSlotNum);

		return Status;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "ISO7816 Broadcast Receiver");

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
	

	private void onCreateButtonSetup(){
		if (mUsbDev != null)
			mOpenButton.setEnabled(true);
		else
			mOpenButton.setEnabled(false);
		mPowerButton.setEnabled(false);
		mATRButton.setEnabled(false);
		mAPDUButton.setEnabled(false);
		mSNButton.setEnabled(false);
		mProtocolButton.setEnabled(false);
		mCloseButton.setEnabled(false);
	}


	private static void onCloseButtonSetup(ISO7816_Activity act){
		act.mOpenButton.setEnabled(true);
		act.mPowerButton.setEnabled(false);
		act.mATRButton.setEnabled(false);
		act.mAPDUButton.setEnabled(false);
		act.mSNButton.setEnabled(false);
		act.mProtocolButton.setEnabled(false);
		act.mCloseButton.setEnabled(false);
	}


	private void onDetach(Intent intent){
		UsbDevice   udev = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (udev != null ) {
			if (udev.equals(mUsbDev) ){
				closeReaderUp(ISO7816_Activity.this);
				closeReaderBottom(ISO7816_Activity.this);
				ISO7816_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}
	}

	private void onDevPermit(UsbDevice dev){
		try {    		
			setUpWaitDialog(ISO7816_Activity.this, "Opening...");
			new OpenTask(ISO7816_Activity.this).execute(dev);
		}
		catch(Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
		}
	}
	private static void onOpenButtonSetup(ISO7816_Activity act){
		act.mOpenButton.setEnabled(false);
		act.mPowerButton.setEnabled(true);
		act.mATRButton.setEnabled(true);
		act.mAPDUButton.setEnabled(true);
		act.mSNButton.setEnabled(true);
		if (!act.mIsNFC)//meaningless for NFC
			act.mProtocolButton.setEnabled(true);
		act.mCloseButton.setEnabled(true);
	}

	private static class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {
		private final WeakReference<ISO7816_Activity> weakActivity;

		OpenTask(ISO7816_Activity myActivity) {
			this.weakActivity = new WeakReference<ISO7816_Activity>(myActivity);
		}
		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			ISO7816_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				return status;
			}
			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			ISO7816_Activity activity = weakActivity.get();
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


		result = mReader.setPower(Reader.CCID_POWERON);

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

		result = mReader.setPower(Reader.CCID_POWEROFF);

		return result;
	}







	private void requestDevPerm(){

		if (mUsbDev != null)
		{
			mManager.requestPermission(mUsbDev, mPermissionIntent);
		}
		else
		{
			Log.e(TAG,"selected not found");
			mTextViewResult.setText("Device not found");
		}
	}

	private void setReaderNFCSlotView(){
		String [] s = new String[] {"SmartCard","NFC"};
		setReaderSlotView(s);
	}
	private void setReaderTwoSlotView(){
		String [] s = new String[] {"slot-0","slot-1"};
		setReaderSlotView(s);
	}

	private void setReaderSlotView(String []arraySlot){

		mSlotDialog = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener Select = new DialogInterface.OnClickListener(){

			@Override  
			public void onClick(DialogInterface dialog, int which) {
				mSlotNum = (byte) which;
				if (which == 0x01)// it is NFC slot
					mIsNFC = true;
			}  
		};

		DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {
				requestDevPerm();
			}
		}; 

		mSlotDialog.setPositiveButton("OK",OkClick );
		mSlotDialog.setTitle("Select Slot Number");
		mSlotDialog.setSingleChoiceItems(arraySlot, 0, Select) ;
		mSlotDialog.show();
	}



	private static void setUpWaitDialog(ISO7816_Activity act, String msg){
		act.mWaitProgress.setMessage(msg);
		act.mWaitProgress.setCancelable(false);
		act.mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		act.mWaitProgress.show();
		act.mIsNFC = false;
	}

	private void setupViews(){
		mPowerButton = (Button)findViewById(R.id.ISO7816buttonPower);
		mOpenButton = (Button)findViewById(R.id.ISO7816buttonOpen);
		mATRButton  = (Button)findViewById(R.id.ISO7816buttonATR);
		mAPDUButton = (Button)findViewById(R.id.ISO7816buttonAPDU);
		mProtocolButton = (Button)findViewById(R.id.ISO7816buttonProtocol);
		mSNButton = (Button)findViewById(R.id.ISO7816buttonSN);
		mCloseButton = (Button)findViewById(R.id.ISO7816buttonClose);


		mTextViewResult = (TextView)findViewById(R.id.ISO7816textResult);
		mEditTextApdu = (EditText)findViewById(R.id.ISO7816editTextAPDU);
		mEditTextApdu.setText("A0A40000023F00");

		//mEditTextMode = (EditText)findViewById(R.id.textMode);
		//mEditTextMode.setText("1");
		mWaitProgress =  new ProgressDialog(ISO7816_Activity.this);
		onCreateButtonSetup();


		setReaderSlotView();
		setPowerView();
	}


	private void setReaderSlotView(){
		final String [] arraySlot = new String[] {"Slot:0","Slot:1"};
		mSlotDialog = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener Select = new DialogInterface.OnClickListener(){

			@Override  
			public void onClick(DialogInterface dialog, int which) {
				mSlotNum = (byte) which;
			}  
		};

		DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {
				requestDevPerm();
			}
		}; 
		mSlotDialog.setPositiveButton("OK",OkClick );
		mSlotDialog.setTitle("Select Slot Number");
		mSlotDialog.setSingleChoiceItems(arraySlot, 0, Select) ;
	}

	private void setPowerView(){
        mPowerDialog = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener poweroffClick = new DialogInterface.OnClickListener(){
			@Override  
			public void onClick(DialogInterface dialog, int which) {
				TextView textViewResult;
				int ret;
				textViewResult = (TextView)findViewById(R.id.ISO7816textResult);
				ret =	poweroff();
				if (ret == SCError.READER_SUCCESSFUL)
					textViewResult.setText("power off successfully");
				else if (SCError.maskStatus(ret) == SCError.READER_NO_CARD){
					textViewResult.setText("Card Absent");
				}
				else
				{
					textViewResult.setText("power off fail:"+  SCError.errorCode2String(ret)
							+"(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
				}
			}  
		};

		DialogInterface.OnClickListener poweronClick = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {
				TextView textViewResult;
				int ret;
				textViewResult = (TextView)findViewById(R.id.ISO7816textResult);
				ret =	poweron();
				if (ret == SCError.READER_SUCCESSFUL)
				{
					textViewResult.setText("power on successfully");
				}
				else if (SCError.maskStatus(ret) == SCError.READER_NO_CARD){
					textViewResult.setText("Card Absent");
				}
				else
				{
					textViewResult.setText("power on fail:"+  SCError.errorCode2String(ret) + "(0x"+Integer.toHexString(mReader.getCmdFailCode())+")");
				}
			}
		}; 
		mPowerDialog.setTitle("Set Power");
		mPowerDialog.setNeutralButton("PowerON", poweronClick);
		mPowerDialog.setPositiveButton("PowerOFF", poweroffClick);

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
