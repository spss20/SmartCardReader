package com.alcorlink.smartcard;



import amlib.ccid.Reader;
import amlib.ccid.ReaderAT88SC1608;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class AT88SC1608_Activity extends AppCompatActivity {

	private String mStrMessage;
	private ReaderAT88SC1608 m88sc1608;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;

	private Builder  mPowerDialog;

	private Button mBtOpen;
	private Button mBtClose;
	private Button mBtPower;
	private Button mBtWriteUser;
	private Button mBtReadUser;
	private Button mBtWriteConfig;
	private Button mBtReadConfig;
	private Button mBtSetUserZoneAddr;
	private Button mBtVerifyPassWd;

	private RadioGroup mRadioGroup; 

	private TextView mTextViewResult;

	private EditText mEdtAddrUser;
	private EditText mEdtLenUser;
	private EditText mEdtDataUser;
	private EditText mEdtAddrConfig;
	private EditText mEdtLenConfig;
	private EditText mEdtDataConfig;
	private EditText mEdtAddrSet;
	private EditText mEdtVerifyPw1;
	private EditText mEdtVerifyPw2;
	private EditText mEdtVerifyPw3;


	private Spinner mZoneSpinner;

	private ProgressDialog mWaitProgress;

	private static final String TAG = "Alcor-AT88SC1608";
	private static final String ACTION_USB_PERMISSION = "com.alcorlink.smartcard.USB_PERMISSION";
	private static final boolean VERIFY_TYPE_WRITE = false;
	private static final boolean VERIFY_TYPE_READ = true;
	private static final int MAX_WRITE_LENGTH = 256;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_at88sc1608);

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
				closeReaderUp(AT88SC1608_Activity.this);
				closeReaderBottom(AT88SC1608_Activity.this);
			} catch (Exception e) {
				mStrMessage = "Get Exception : " + e.getMessage();
				//mTextViewResult.setText( mStrMessage); 
			}
			Log.d(TAG,"finish");
			AT88SC1608_Activity.this.finish();
		}  
		return super.onKeyDown(keyCode, event);  

	}

	public void OnClickAT88SC1608Open (View view){
		requestDevPerm();
	}

	public void OnClickAT88SC1608Close (View view){
		new CloseTask(AT88SC1608_Activity.this).execute();
	}

	public void OnClickAT88SC1608Power(View view){
		mPowerDialog.show();
	}

	public void OnClickAT88SC1608WriteUserZone(View view){
		byte []pAddr = new byte[1];
		byte []pWrite = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];
		int result;
		//get addr
		result = getEditTextHex(mEdtAddrUser, pAddr, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		//get data
		result = getEditTextHex(mEdtDataUser, pWrite, MAX_WRITE_LENGTH*2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		result = m88sc1608.AT88SC1608Cmd_WriteUserZone(pAddr[0], (byte)pIntReturnLen[0], pWrite);
		showResult("WriteUserZone", result);
	}

	public void OnClickAT88SC1608ReadUserZone(View view){
		byte []pAddr = new byte[1];
		int []pRxLen = new int[1];
		byte []pRead = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];

		Log.d(TAG, "");
		int result;
		//get addr
		result = getEditTextHex(mEdtAddrUser, pAddr, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		//get lenght
		result = getEditTextDec(mEdtLenUser, pRxLen, 3);
		if (result != 0)
		{
			return;
		}
		result = m88sc1608.AT88SC1608Cmd_ReadUserZone(pAddr[0], (byte)pRxLen[0], pRead, pIntReturnLen);
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			//shows data
			showBufResult(pIntReturnLen[0], pRead);
		}
		else
			showResult("ReadUserZone", result);
	}

	public void OnClickAT88SC1608WriteConfigZone(View view){
		byte []pAddr = new byte[1];
		byte []pWrite = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];

		int result;
		//get addr
		result = getEditTextHex(mEdtAddrConfig, pAddr, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		//get data
		result = getEditTextHex(mEdtDataConfig, pWrite, MAX_WRITE_LENGTH*2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		result = m88sc1608.AT88SC1608Cmd_WriteConfigurationZone( pAddr[0], (byte)pIntReturnLen[0], pWrite);
		showResult("WriteConfigZone", result);
	}

	public void OnClickAT88SC1608ReadConfigZone(View view){
		byte []pAddr = new byte[1];
		int []pTxLen = new int[1];
		byte []pRead = new byte[MAX_WRITE_LENGTH];
		int []pIntReturnLen = new int[1];

		Log.d(TAG, "");
		int result;
		//get addr
		result = getEditTextHex(mEdtAddrConfig, pAddr, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		//get lenght
		result = getEditTextDec(mEdtLenConfig, pTxLen, 3);
		if (result != 0)
		{
			return;
		}
		result = m88sc1608.AT88SC1608Cmd_ReadConfigurationZone(pAddr[0], (byte)pTxLen[0], pRead, pIntReturnLen);
		if (result == 0)
		{
			Log.d(TAG,"result, pIntReturnLen="+pIntReturnLen[0]);
			//shows data
			showBufResult(pIntReturnLen[0], pRead);
		}
		else
			showResult("ReadConfigZone", result);
	}

	public void OnClickAT88SC1608SetUserZoneAddr(View view){
		byte []pAddr = new byte[1];
		int []pIntReturnLen = new int[1];
		int result;
		//get addr
		result = getEditTextHex(mEdtAddrSet, pAddr, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		result = m88sc1608.AT88SC1608Cmd_SetUserZoneAddress(pAddr[0]);
		showResult("SetUserZoneAddr", result);
	}

	public void OnClickAT88SC1608VerifyPassWd(View view){
		boolean function;
		byte zone;
		int []pIntReturnLen = new int[1];
		int result ;
		byte []pPw1 = new byte[1];
		byte []pPw2 = new byte[1];
		byte []pPw3 = new byte[1];

		function = getRadioRWSelect();
		zone = (byte) getSpinnerSelectZone();
		Log.d(TAG, "function="+function + "/zone="+zone);
		result = getEditTextHex(mEdtVerifyPw1, pPw1, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		result = getEditTextHex(mEdtVerifyPw2, pPw2, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		result = getEditTextHex(mEdtVerifyPw3, pPw3, 2, pIntReturnLen);
		if (result != 0)
		{
			return;
		}
		result = m88sc1608.AT88SC1608Cmd_VerifyPassword(zone, function, pPw1[0], pPw2[0], pPw3[0]);
		showResult("VerifyPassWd", result);
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

	private static int closeReaderUp(AT88SC1608_Activity act){
		Log.d(TAG, "Closing reader...");
		int ret = 0;

		if (act.m88sc1608 != null)
		{
			ret = act.m88sc1608.close();
		}
		return ret;
	}

	private static void closeReaderBottom(AT88SC1608_Activity act){
		onCreateButtonSetup(act);
		act.mTextViewResult.setText("");
		act.mMyDev.Close();
	}

	private static class CloseTask extends AsyncTask <Void, Void, Integer> {
		private final WeakReference<AT88SC1608_Activity> weakActivity;

		CloseTask(AT88SC1608_Activity myActivity) {
			this.weakActivity = new WeakReference<AT88SC1608_Activity>(myActivity);
		}

		@Override 
		protected void onPreExecute() {
			AT88SC1608_Activity activity = weakActivity.get();
			setUpWaitDialog(activity,"Closing Reader");

		}
		@Override
		protected Integer doInBackground(Void... params) {
			int status = 0;
			AT88SC1608_Activity activity = weakActivity.get();
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
			AT88SC1608_Activity activity = weakActivity.get();
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


	private boolean getRadioRWSelect()
	{
		RadioButton bt = (RadioButton) findViewById(R.id.radioWrite);
		if (bt.isChecked())
			return VERIFY_TYPE_WRITE;
		else
			return VERIFY_TYPE_READ;
	}
	private int getSpinnerSelectZone(){
		long zone;
		zone = mZoneSpinner.getSelectedItemId();
		return (int)zone;
	}


	private int getSlotStatus(){
		int ret = SCError.READER_NO_CARD;
		byte []pCardStatus = new byte[1];

		/*detect card hotplug events*/
		ret = m88sc1608.getCardStatus(pCardStatus);
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


	private static int InitReader(AT88SC1608_Activity act)
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
			act.m88sc1608 = new ReaderAT88SC1608(act.mMyDev);
			Status = act.m88sc1608.open();
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
		private final WeakReference<AT88SC1608_Activity> weakActivity;

		OpenTask(AT88SC1608_Activity myActivity) {
			this.weakActivity = new WeakReference<AT88SC1608_Activity>(myActivity);
		}

		@Override
		protected Integer doInBackground(UsbDevice... params) {
			int status = 0;
			AT88SC1608_Activity activity = weakActivity.get();
			status = InitReader(activity) ;
			if ( status != 0){
				Log.e(TAG, "fail to initial reader");
				return status;
			}

			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			AT88SC1608_Activity activity = weakActivity.get();
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


	private static void onCreateButtonSetup(AT88SC1608_Activity act){
		if (act.mUsbDev != null)
			act.mBtOpen.setEnabled(true);
		else
			act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(false);
		act.mBtPower.setEnabled(false);
		act.mBtWriteUser.setEnabled(false);
		act.mBtReadUser.setEnabled(false);
		act.mBtWriteConfig.setEnabled(false);
		act.mBtReadConfig.setEnabled(false);
		act.mBtSetUserZoneAddr.setEnabled(false);
		act.mBtVerifyPassWd.setEnabled(false);
		act.mZoneSpinner.setEnabled(false);
		act.mRadioGroup.setEnabled(false);
	}

	private static void onOpenButtonSetup(AT88SC1608_Activity act){
		act.mBtOpen.setEnabled(false);
		act.mBtClose.setEnabled(true);
		act.mBtPower.setEnabled(true);
		act.mBtWriteUser.setEnabled(true);
		act.mBtReadUser.setEnabled(true);
		act.mBtWriteConfig.setEnabled(true);
		act.mBtReadConfig.setEnabled(true);
		act.mBtSetUserZoneAddr.setEnabled(true);
		act.mBtVerifyPassWd.setEnabled(true);
		act.mZoneSpinner.setEnabled(true);
		act.mRadioGroup.setEnabled(true);
	}




	private void onDevPermit(UsbDevice dev){
		try {    		
			setUpWaitDialog(AT88SC1608_Activity.this,"Opening...");
			new OpenTask(AT88SC1608_Activity.this).execute(dev);
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
				closeReaderUp(AT88SC1608_Activity.this);
				closeReaderBottom(AT88SC1608_Activity.this);
				AT88SC1608_Activity.this.finish();
			}
		}
		else {
			Log.d(TAG,"usb device is null");
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

		result = m88sc1608.setPower(Reader.CCID_POWERON);
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
		result = m88sc1608.setPower(Reader.CCID_POWEROFF);

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
		mBtOpen = (Button)findViewById(R.id.AT88SC1608buttonOpen);
		mBtClose = (Button)findViewById(R.id.AT88SC1608buttonClose);
		mBtPower = (Button)findViewById(R.id.AT88SC1608buttonPower);
		mBtWriteUser = (Button)findViewById(R.id.AT88SC1608WriteUser);
		mBtReadUser = (Button)findViewById(R.id.AT88SC1608ReadUser);
		mBtWriteConfig = (Button)findViewById(R.id.AT88SC1608WriteConfig);
		mBtReadConfig = (Button)findViewById(R.id.AT88SC1608ReadConfig);
		mBtSetUserZoneAddr = (Button)findViewById(R.id.AT88SC1608SetUserZoneAddr);
		mBtVerifyPassWd = (Button)findViewById(R.id.AT88SC1608VerifyPassWd);

	}
	
	private void setEditView()
	{
		mEdtAddrUser = (EditText)findViewById(R.id.AT88SC1608EditTextAddrUser);
		mEdtLenUser = (EditText)findViewById(R.id.AT88SC1608EditTextUserLen);
		mEdtDataUser = (EditText)findViewById(R.id.AT88SC1608EditTextDataUser);
		mEdtAddrConfig = (EditText)findViewById(R.id.AT88SC1608EditTextAddrConfig);
		mEdtLenConfig = (EditText)findViewById(R.id.AT88SC1608EditTextLenConfig);
		mEdtDataConfig = (EditText)findViewById(R.id.AT88SC1608EditTextDataConfig);
		mEdtAddrSet = (EditText)findViewById(R.id.AT88SC1608EditTextAddrSet);
		mEdtVerifyPw1 = (EditText)findViewById(R.id.AT88SC1608EditTextPw1);
		mEdtVerifyPw2 = (EditText)findViewById(R.id.AT88SC1608EditTextPw2);
		mEdtVerifyPw3 = (EditText)findViewById(R.id.AT88SC1608EditTextPw3);
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
		mRadioGroup = (RadioGroup) findViewById(R.id.radioPwGroup);        
		//radioGroup.setOnCheckedChangeListener( listener);
		RadioButton bt = (RadioButton) findViewById(R.id.radioWrite);
		bt.setChecked(true);
	}

	private void setTextView()
	{
		//		mTextViewWriteAddr = (TextView)findViewById(R.id.AT88SC1608TextWriteAddr);
		//		mTextViewWriteData = (TextView)findViewById(R.id.AT88SC1608TextData);
		//		mTextViewPageSize = (TextView)findViewById(R.id.AT88SC1608TextPageSize);
		//		mTextViewReadAddr = (TextView)findViewById(R.id.AT88SC1608TextReadAddr);
		//		mTextViewReadLen = (TextView)findViewById(R.id.AT88SC1608TextLen);
		mTextViewResult = (TextView)findViewById(R.id.AT88SC1608TextResult);
	}

	private static void setUpWaitDialog(AT88SC1608_Activity act, String msg){

		act.mWaitProgress.setMessage(msg);
		act.mWaitProgress.setCancelable(false);
		act.mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		act.mWaitProgress.show();
	}

	private void setupSpinner(){
		String[] lunch = {"0","1","2","3","4","5","6","7"};
		// Initialize  spinner
		ArrayAdapter<String> mZoneAdapter = new ArrayAdapter<String>(this,
				R.layout.myspinner, lunch);

		mZoneSpinner = (Spinner) findViewById(R.id.AT88SC1608spinnerZone);
		mZoneSpinner.setAdapter(mZoneAdapter);
	}


	private void setupViews(){
		Log.d(TAG,"setupViews");
		setButtonView();
		setTextView();
		setEditView();
		setPowerView();
		setRadioButtonView();
		setupSpinner();
		onCreateButtonSetup(AT88SC1608_Activity.this);
		mWaitProgress =  new ProgressDialog(AT88SC1608_Activity.this);  
		//setReaderSlotView();
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
			ccidErrCode = Integer.toHexString(m88sc1608.getCmdFailCode() & 0x0000ffff);
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

