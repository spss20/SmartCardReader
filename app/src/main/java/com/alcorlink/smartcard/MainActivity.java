package com.alcorlink.smartcard;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;


import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


import amlib.ccid.Reader;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
	private static final  int MY_PERMISSIONS_REQUEST_READ = 0x345;
	private UsbManager mManager;
	private Builder  mSwitchNFCDialog;
    private Builder  mSwitchMemoryModeDialog;
	private AlertDialog mCardModeDialog;
	private Button mListButton;
	private ArrayAdapter<String> mReaderAdapter;
	private TextView mTextViewInfo;
	private TextView mTextViewResult; 
	private Spinner mReaderSpinner;
	private String mStrMessage;

	private final static String logFileName = "AlcorlinkSClog.txt";
	private static final String TAG = "Alcor-Test";
	private static final int MODE_7816 = Reader.CardModeASYNC;
	private static final int MODE_24C = Reader.CardModeI2C;
	private static final int MODE_4428 = Reader.CardModeSLE4428;
	private static final int MODE_4442 = Reader.CardModeSLE4442;
	private static final int MODE_88SC1608 = Reader.CardModeAT88SC1608;
	private static final int MODE_AT45D = Reader.CardModeAT45D041;
	private static final int MODE_6636 = Reader.CardModeSLE6636;
	private static final int MODE_88SC102 = Reader.CardModeAT88SC102;
	private static final int MODE_88SC153 = Reader.CardModeAT88SC153;

	private static final int MODE_NFC_MifareS50 = 0x200;
	private static final int INTERFACE_SMARTCARD = 0xB;
	private Builder mSaveLogDialog;
	public static final int LogTypeLogCat = 0;
	private static final int LogTypeSDKLog= 1;

	private PermissionCheck permitChecker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupViews();
		Log.d(TAG," onCreate");
		String s="Device-info:";
		s += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
		s += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
		s += "\n Device: " + android.os.Build.DEVICE;
		s += "\n Brand: " + android.os.Build.BRAND;
		s += "\n Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
		Log.d(TAG," info = "+ s);
		mTextViewInfo.setText(s);
		
		LinearLayout mLayout = (LinearLayout)findViewById(R.id.MainActivityLayout);
		mLayout.setOnTouchListener(new MytouchListener());

		permitChecker = new PermissionCheck();
		permitChecker.getReadWritePermission(this);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String[] permissions, int[] grantResults) {
		//nothing todo

	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		getResources().getDisplayMetrics().scaledDensity = getResources().getDisplayMetrics().scaledDensity/newConfig.fontScale;
		super.onConfigurationChanged(newConfig);
	}

		private long lastScreanTouch = 0;
		private long screanTouchcount = 0;
		private class MytouchListener implements OnTouchListener{

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			long timeMili = System.currentTimeMillis();
			long duaration = 999999;
			final long minDuration = 400;
			final byte successCount = 4;
			if (lastScreanTouch != 0)
			{
				duaration = timeMili - lastScreanTouch;
			}
			lastScreanTouch = timeMili;

			if (duaration <= minDuration)
			{
				screanTouchcount++;
			}
			else
				screanTouchcount = 0;
			Log.d(TAG," onTouch: " +duaration +" touch cnt=" +screanTouchcount);
			if (screanTouchcount >= successCount )
				selectLogToShow();
			return false;
		}


	}
	

	
	public void OnClickISO7816(View view){
		switchMode(MODE_7816);
	}

	public void  OnClickNonISO7816(View view) {
        setNon7816DialogView();
		mCardModeDialog = mSwitchMemoryModeDialog.show();
    }


	
	public void OnClickNFC(View view){
		setNFCModeView();
		mSwitchNFCDialog.show();
	}






	protected void onPause() {
		Log.d(TAG, "Activity OnPause");
		unregisterReceiver(mReceiver);
		super.onPause();
	}
	protected void onResume() {
		Log.d(TAG, "Activity onResume");
		cleanText();
		mReaderAdapter.clear();
		onCreateButtonSetup();
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		toRegisterReceiver();
		boolean isCanceled = permitChecker.isItCancelByUser(this);
		if ((!permitChecker.checkReadWritePermit(this)) && isCanceled)
		{
			permitChecker.getReadWritePermission(this);
		}
		super.onResume();
	}


	
	protected void   onStop() {
		Log.d(TAG, "Activity onStop");
		super.onStop();
	}
	private void cleanText(){
		mTextViewResult.setText("");

	}

	private byte logSelection;
	private void selectLogToShow() {

		final String [] arrayLog = new String[] {"show logcat","show log"};
		Builder	mLogSelectionDialog = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener Select = new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.d(TAG, "onClick which-"+which);
				logSelection = (byte) which;

			}
		};

		DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {
				Log.d(TAG, "logSelection-"+logSelection);
				if (logSelection == 0)
				{
					Log.d(TAG, "select 0");
					new ShowLogTask(MainActivity.this).execute(LogTypeLogCat);
				}
				else
				{
					Log.d(TAG, "select  "+logSelection);
					new ShowLogTask(MainActivity.this).execute(LogTypeSDKLog);
				}
			}
		};

		mLogSelectionDialog.setPositiveButton("OK",OkClick );
		mLogSelectionDialog.setTitle("");
		mLogSelectionDialog.setSingleChoiceItems(arrayLog, 0, Select) ;
		mLogSelectionDialog.show();
	}

	private LogDialogFragment logDialog;
	private static void doShowLog(MainActivity act, int logType) {
		FileInputStream inputStream;

		try {

			act.logDialog = new LogDialogFragment();
			act.logDialog.setLogType(logType);
			if (logType == LogTypeSDKLog)
			{
				final String PATH = Environment.getExternalStorageDirectory().getPath();
			//	inputStream = getApplicationContext().openFileInput(PATH + "/" + logFileName);
				inputStream = new FileInputStream(PATH + "/" +logFileName);
				act.logDialog.setInputStream(inputStream);
			}

			act.logDialog.show(act.getFragmentManager(),null);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class ShowLogTask extends AsyncTask<Integer, Void, Integer> {
		private final WeakReference<MainActivity> weakActivity;

		ShowLogTask(MainActivity myActivity) {
			this.weakActivity = new WeakReference<MainActivity>(myActivity);
		}
		@Override
		protected void onPreExecute() {

		}
		@Override
		protected Integer doInBackground(Integer ... logType) {
			int status = 0;
			MainActivity activity = weakActivity.get();
			doShowLog(activity, logType[0]);
			return status;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result != 0) {

			}else{

			}

		}
	}



	private int EnumeDev() {
		Log.d(TAG, " EnumeDev");
		boolean isReaderFound = false;
		UsbDevice device = null;
		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList;
		try {
			deviceList = manager.getDeviceList();
		} catch (NullPointerException e) {
			Log.d(TAG, "Exception to get device list");
			return -1;
		}
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		mReaderAdapter.clear();
		while(deviceIterator.hasNext()){
			device = deviceIterator.next();   
			Log.d(TAG," "+ Integer.toHexString(device.getVendorId()) +" " +Integer.toHexString(device.getProductId()));

			if(isAlcorReader(device)) 
			{   
				isReaderFound = true;
				Log.d(TAG,"Found Device");
				mReaderAdapter.add(pid2DevName (device.getProductId())+"-"+device.getDeviceName());					
			}
			else
			{
				//mReaderAdapter.add("Not supportted reader");
			}
		}
		if (!isReaderFound)
		{
			mTextViewResult.setText("No Supported Reader Been Found");
		}
		else
		{
			mTextViewResult.setText("");
		}
		return 0;
	}

	private boolean isAlcorReader(UsbDevice udev){
		if (udev.getVendorId() == 0x058f
				&& ((udev.getProductId() == 0x9540) 
						|| (udev.getProductId() == 0x9520)  || (udev.getProductId() == 0x9522)
						|| (udev.getProductId() == 0x9525) || (udev.getProductId() == 0x9526) 
						)
				)
			return true;
		else if (udev.getVendorId() == 0x2CE3 
				&& ((udev.getProductId() == 0x9571) || (udev.getProductId() == 0x9572)
				|| (udev.getProductId() == 0x9563)) || (udev.getProductId() == 0x9573)) {
			return true;
		}
		return false;
	}

	private boolean isSmartCardReader(UsbDevice udev){
		int intfCnt = 0, i;
		UsbInterface interf;
		intfCnt = udev.getInterfaceCount();
		Log.d(TAG," interface cnt=" + intfCnt);
		/*
		 * bug: in Android5.x , only the last plugged device has the 
		 * right correct number of interfaces, other devices report 0 interfaces.
		 */
		for (i=0 ; i<intfCnt; i++)
		{
			interf = udev.getInterface(i);
			if (interf.getInterfaceClass() == INTERFACE_SMARTCARD)
			{
				Log.d(TAG," INTERFACE_SMARTCARD");
				return true;
			}
		}
		return false;
	}


	private UsbDevice getSpinnerSelect(){
		String deviceName;
		deviceName= (String) mReaderSpinner.getSelectedItem();
		if (deviceName != null) {
			// For each device
			for (UsbDevice device : mManager.getDeviceList().values()) {
				if (deviceName.equals(pid2DevName (device.getProductId())+"-"+device.getDeviceName())) {
					return device;
				}
			}
		}
		return null;
	}






	public void ListOnClick(View view){
		Log.d(TAG, "ListOnClick");
		EnumeDev();
	}
	
	public static String byte2String(byte[] buffer, int bufferLength) {

		StringBuilder bufferString = new StringBuilder();
		StringBuilder dbgString = new StringBuilder();

		for (int i = 0; i < bufferLength; i++) {

			String hexChar = Integer.toHexString(buffer[i] & 0xFF);
			if (hexChar.length() == 1) {
				hexChar = "0" + hexChar;
			}

			if (i % 16 == 0) {
				if (!dbgString.toString().equals("")) {
					//	                    Log.d(LOG_TAG, dbgString);
					bufferString.append(dbgString);
					dbgString = new StringBuilder("\n");
				}
			}

			dbgString.append(hexChar.toUpperCase()).append(" ");
		}

		if (!dbgString.toString().equals("")) {
			//	        	Log.d(LOG_TAG, dbgString);
			bufferString.append(dbgString);
		}

		return bufferString.toString();
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Main Acivity Broadcast Receiver");

			String action = intent.getAction();
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				Log.d(TAG, "Device Detached");
				onDetach(intent);
				synchronized (this) {
					updateReaderList(intent);
				} 
			}
		}/*end of onReceive(Context context, Intent intent) {*/
	};



	private void onCreateButtonSetup(){
		mListButton.setEnabled(true);
		setMemModeDisable(true);
		setNFCModeDisable(true);
	}



	private void onDetach(Intent intent){
		UsbDevice   udev = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (udev != null ) {
			if (isAlcorReader(udev))
			{
				cleanText();
				setMemModeDisable(true);
				setNFCModeDisable(true);
			}
		}
		else {
			Log.d(TAG,"usb device is null");
		}
	}
	
	private void onDeviceSelect(int pid) {
		switch (pid)
		{
	
		case 0x9571:
		case 0x9572:
		case 0x9573:
		case 0x9526:
			setMemModeDisable(false);
			setNFCModeDisable(true);
			break;
		default:
			setMemModeDisable(true);
			setNFCModeDisable(false);
			break;
		}
	}

	private String pid2DevName(int pid) {
		String name = null;
		switch (pid)
		{
		case 0x9520:
		case 0x9522:
		case 0x9540:
		case 0x9525:
		case 0x9563:
			name = new String("SAM Card Reader");
			break;
		case 0x9571:
			name = new String("NFC Reader");
			break;
		case 0x9526:
		case 0x9572:
		case 0x9573:
			name = new String("Smart Card Reader/NFC Reader");
			break;
		}
		return name; 
	}
	

	
	private void setMemModeDisable(boolean enable)
	{

		Button bt ;
		bt = (Button)findViewById(R.id.buttonNonIso7816);
		bt.setEnabled(enable);

	
	}
	
	private void setNFCModeDisable(boolean enable) {
		Button bt ;
		bt = (Button)findViewById(R.id.buttonNFC);
		bt.setEnabled(enable);
	}

	private void setNon7816DialogView() {
		Log.d(TAG, "OnClickNonISO7816");
        final String [] arrayMemMode= new String[]
                {
                        "AT24C", //0
                        "AT45D", //1
                        "AT88SC10X",//2
                        "AT88SC160X",//3
                        "SLE4442",//4
                        "SLE4428",//5
                        "SLE6636",//6
						"AT88SC153"//7
                };
		mSwitchMemoryModeDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogTheme));


        DialogInterface.OnClickListener Select = new DialogInterface.OnClickListener(){
            private int id ;
            @Override
            public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					if (id <= 0)
						;
					else
						switchMode(id);
				} else {
					switch (which) {
						case 0:
							Log.d(TAG, "radioButton24C  ");
							id = MODE_24C;
							break;
						case 1:
							id = MODE_AT45D;
							break;
						case 2:
							id = MODE_88SC102;
							break;
						case 3:
							id = MODE_88SC1608;
							break;
						case 4:
							id = MODE_4442;
							break;
						case 5:
							id = MODE_4428;
							break;
						case 6:
							id = MODE_6636;
							break;
						case 7:
							id = MODE_88SC153;
							break;
						default:
							id = 0xff;
							break;
					}
				}

			}//onClick
        };
		mSwitchMemoryModeDialog.setPositiveButton("OK",Select );
     //   mSwitchMemoryModeDialog.setPositiveButton("OK",Select );
        mSwitchMemoryModeDialog.setTitle("Select Memory Card Mode ");
		/*
		UsbDevice dev = getSpinnerSelect();
		if (dev.getProductId() == 0x9540)
		{
			mSwitchModeDialog.setSingleChoiceItems(arraySlotMem, -1, Select) ;
		}
		else
		{
			mSwitchModeDialog.setSingleChoiceItems(arraySlotNoMemMode, -1, Select) ;
		}
		*/
       mSwitchMemoryModeDialog.setSingleChoiceItems(arrayMemMode, -1, Select) ;
    }


	private void setNFCModeView(){
		
		final String [] arrayNFCMode= new String[] 
				{
				"MifareS50", //0
				};

		mSwitchNFCDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogTheme));
		DialogInterface.OnClickListener Select = new DialogInterface.OnClickListener(){
			private int id ;
			@Override  
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE)
				{
					if (id  <= 0)
						;
					else
						switchMode(id);
				}
				else {
					switch (which)
					{
					case 0:
						id = MODE_NFC_MifareS50;
						break;		
					default :
						id =   0xff;
						break;
					}
				}
			}//onClick
		};

		mSwitchNFCDialog.setPositiveButton("OK",Select );
		mSwitchNFCDialog.setTitle("Select External Card Mode ");
		/*
		UsbDevice dev = getSpinnerSelect();
		if (dev.getProductId() == 0x9540)
		{
			mSwitchModeDialog.setSingleChoiceItems(arraySlotMem, -1, Select) ;
		}
		else
		{
			mSwitchModeDialog.setSingleChoiceItems(arraySlotNoMemMode, -1, Select) ;
		}
		*/
		mSwitchNFCDialog.setSingleChoiceItems(arrayNFCMode, -1, Select) ;
	}

	private void setupReaderSpinner(){
		// Initialize reader spinner
		mReaderAdapter = new ArrayAdapter<String>(this,
				R.layout.myspinner);

		mReaderSpinner = (Spinner) findViewById(R.id.spinnerDevice);
		mReaderSpinner.setAdapter(mReaderAdapter);
		mReaderSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,int position, long arg3) {
				UsbDevice udev = getSpinnerSelect();
				try {
					onDeviceSelect(udev.getProductId());
				} catch (NullPointerException e) {
					e.printStackTrace();
				}

			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
			}
		});
	}


	private void setupViews(){
		mListButton = (Button)findViewById(R.id.buttonList);

		mTextViewInfo = (TextView)findViewById(R.id.textInfo);
		mTextViewResult = (TextView)findViewById(R.id.textResult);
		setupReaderSpinner();
		// setReaderSwitchModeView();

	}



	public void SpinnerDeviceOnClick(View view){
		Log.d(TAG, "SpinnerDeviceOnClick");
		EnumeDev();
	}

	

	private void switchMode(int mode)
	{
		try{
			UsbDevice dev = getSpinnerSelect();
			if (dev == null)
			{
				// error
				//return;
			}
			Intent intent = new Intent();
			Bundle b = new  Bundle();
			switch (mode)
			{
			case MODE_7816:
				intent.setClass(this, com.alcorlink.smartcard.ISO7816_Activity.class);
				break;
			case MODE_4428:
				intent.setClass(this, com.alcorlink.smartcard.SLE4428_Activity.class);
				break;
			case MODE_4442:
				intent.setClass(this, com.alcorlink.smartcard.SLE4442_Activity.class);
				break;
			case MODE_24C :
				intent.setClass(this, com.alcorlink.smartcard.AT24C_Activity.class);
				break;
			case MODE_88SC1608:
				intent.setClass(this, com.alcorlink.smartcard.AT88SC1608_Activity.class);
				break;
			case  MODE_AT45D:
				intent.setClass(this, com.alcorlink.smartcard.AT45D041_Activity.class);
				break;
			case  MODE_6636:
				intent.setClass(this, com.alcorlink.smartcard.SLE6636_Activity.class);
				break;
			case  MODE_88SC102:
				intent.setClass(this, com.alcorlink.smartcard.AT88SC102_Activity.class);
				break;
			case  MODE_NFC_MifareS50:
				intent.setClass(this, com.alcorlink.smartcard.MifareS50_Activity.class);
				break;
			case MODE_88SC153:
					intent.setClass(this, com.alcorlink.smartcard.AT88SC153_Activity.class);
					break;
			default :
				break;
			}

			b.putParcelable(USB_SERVICE, dev);
			intent.putExtras(b);
			this.startActivity(intent);

		}catch (Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			mTextViewResult.setText(mStrMessage);
		}
	}







	public static byte[] toByteArray(String hexString) {

		int hexStringLength = hexString.length();
		byte[] byteArray = null;
		int count = 0;
		char c;
		int i;

		// Count number of hex characters
		for (i = 0; i < hexStringLength; i++) {

			c = hexString.charAt(i);
			if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
					&& c <= 'f') {
				count++;
			}
		}

		byteArray = new byte[(count + 1) / 2];
		boolean first = true;
		int len = 0;
		int value;
		for (i = 0; i < hexStringLength; i++) {

			c = hexString.charAt(i);
			if (c >= '0' && c <= '9') {
				value = c - '0';
			} else if (c >= 'A' && c <= 'F') {
				value = c - 'A' + 10;
			} else if (c >= 'a' && c <= 'f') {
				value = c - 'a' + 10;
			} else {
				value = -1;
			}

			if (value >= 0) {

				if (first) {

					byteArray[len] = (byte) (value << 4);

				} else {

					byteArray[len] |= value;
					len++;
				}

				first = !first;
			}
		}

		return byteArray;
	}

	private void toRegisterReceiver(){
		// Register receiver for USB permission
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mReceiver, filter);
	}

	private void updateReaderList(Intent intent){
		// Update reader list
		mReaderAdapter.clear();
		for (UsbDevice device : mManager.getDeviceList().values()) {
			if(isAlcorReader(device)) {
				mReaderAdapter.add(pid2DevName(device.getProductId()) + "-" + device.getDeviceName());
			}
		}   
	}

}


