/* 
 * File: 		UareUSampleJava.java
 * Created:		2013/05/03
 * 
 * copyright (c) 2013 DigitalPersona Inc.
 */

package com.nastech.myfirstfingerprintapp.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.nastech.myfirstfingerprintapp.R;
import com.gne.pm.PM;


public class UareUSampleJava extends Activity
{

	public static String[] MY_PERMISSIONS = {
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.WRITE_EXTERNAL_STORAGE",
			"android.permission.MOUNT_UNMOUNT_FILESYSTEMS",
			"android.permission.WRITE_OWNER_DATA",
			"android.permission.READ_OWNER_DATA",
			"android.hardware.usb.accessory",
			"com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION",
			"android.permission.HARDWARE_TEST",
			"android.hardware.usb.host"
	};

	public static final int REQUEST_CODE = 1;

	private final int GENERAL_ACTIVITY_RESULT = 1;

	private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
	
	private TextView m_selectedDevice;
	private Button m_getReader;
	private Button m_getCapabilities;
	private Button m_captureFingerprint;
	private Button m_compareFingerprint;
	private Button m_streamImage;
	private Button m_enrollment;
	private Button m_verification;
	private Button m_identification;
	private String m_deviceName = "";
	private String m_versionName = "";

	private final String tag = "UareUSampleJava";

	private Reader m_reader;

	@Override
	public void onStop()
	{
		// reset you to initial state when activity stops
		m_selectedDevice.setText("Device: (No Reader Selected)");
		setButtonsEnabled(false);
		super.onStop();
	}

	protected void onDestroy() {
		Toast.makeText(UareUSampleJava.this,"UareUSampleJava onDestroy()" ,Toast.LENGTH_SHORT).show();
		//power off
		PM.powerOff();

		//add by liuhao 20200331 for Kill app by abnormal operation
		//disable false
		disableFunctionLaunch(false);

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onDestroy();
	}

	@SuppressLint("SourceLockedOrientationActivity")
	@Override
	protected void onResume() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		//add by liuhao 20200331 for Kill app by abnormal operation
		//disable true
		disableFunctionLaunch(true);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onResume();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		//Determine if the current Android version is >=23
		// 判断Android版本是否大于23
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int checkCallPhonePermission = ContextCompat.checkSelfPermission(UareUSampleJava.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

			if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
				// 没有写文件的权限，去申请读写文件的权限，系统会弹出权限许可对话框
				//Without the permission to Write, to apply for the permission to Read and Write, the system will pop up the permission dialog
				ActivityCompat.requestPermissions(UareUSampleJava.this, MY_PERMISSIONS, REQUEST_CODE);
			}
			else
			{
				init();
			}
		}
		else {
			init();
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_CODE) {
			if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(UareUSampleJava.this,"no permission ,plz to request~",Toast.LENGTH_SHORT).show();
				ActivityCompat.requestPermissions(UareUSampleJava.this, MY_PERMISSIONS, REQUEST_CODE);
			}else
			{
				init();
			}
		}
	}


	private void init()
	{
		//power on
		PM.powerOn();

		//enable tracing
		System.setProperty("DPTRACE_ON", "1");
		//System.setProperty("DPTRACE_VERBOSITY", "10");


		setContentView(R.layout.activity_main);

		m_getReader = (Button) findViewById(R.id.get_reader);
		m_getCapabilities = (Button) findViewById(R.id.get_capabilities);
		m_captureFingerprint = (Button) findViewById(R.id.capture_fingerprint);
		m_compareFingerprint = (Button) findViewById(R.id.compare_fingerprint);
		m_streamImage = (Button) findViewById(R.id.stream_image);
		m_enrollment = (Button) findViewById(R.id.enrollment);
		m_verification = (Button) findViewById(R.id.verification);
		m_identification = (Button) findViewById(R.id.identification);
		m_selectedDevice = (TextView) findViewById(R.id.selected_device);

		setButtonsEnabled(false);

		// register handler for UI elements
		m_getReader.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchGetReader();
			}
		});

		m_getCapabilities.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchGetCapabilities();
			}
		});

		m_captureFingerprint.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchCaptureFingerprint();
			}
		});

		m_compareFingerprint.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchCompareFingerprint();
			}
		});

		m_streamImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchStreamImage();
			}
		});

		m_enrollment.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchEnrollment();
			}
		});

		m_verification.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchVerification();
			}
		});

		m_identification.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				launchIdentification();
			}
		});

		// get the version name
		try {
			m_versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(tag, e.getMessage());
		}

		ActionBar ab = getActionBar();
		ab.setTitle("DigitalPersona U are U SDK Sample" + " v" + m_versionName);
	}

	protected void launchGetReader()
	{
		Intent i = new Intent(UareUSampleJava.this, GetReaderActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchGetCapabilities()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this, GetCapabilitiesActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchCaptureFingerprint()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this,CaptureFingerprintActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchCompareFingerprint()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this,CompareActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchStreamImage()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this, StreamImageActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchEnrollment()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this, EnrollmentActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchVerification()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this, VerificationActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	protected void launchIdentification()
	{
		setButtonsEnabled(false);
		Intent i = new Intent(UareUSampleJava.this,IdentificationActivity.class);
		i.putExtra("device_name", m_deviceName);
		startActivityForResult(i, 1);
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}

	protected void setButtonsEnabled(Boolean enabled)
	{

		m_getCapabilities.setEnabled(enabled);
		m_streamImage.setEnabled(enabled);
		m_captureFingerprint.setEnabled(enabled);
		m_compareFingerprint.setEnabled(enabled);
		m_enrollment.setEnabled(enabled);
		m_verification.setEnabled(enabled);
		m_identification.setEnabled(enabled);
	}

	protected void setButtonsEnabled_Capture(Boolean enabled)
	{
		m_captureFingerprint.setEnabled(enabled);
		m_compareFingerprint.setEnabled(enabled);
		m_enrollment.setEnabled(enabled);
		m_verification.setEnabled(enabled);
		m_identification.setEnabled(enabled);
	}

	protected void setButtonsEnabled_Stream(Boolean enabled)
	{
		m_streamImage.setEnabled(enabled);
	}


	protected void CheckDevice()
	{
		try
		{
			m_reader.Open(Priority.EXCLUSIVE);
			Reader.Capabilities cap = m_reader.GetCapabilities();
			setButtonsEnabled(true);
			setButtonsEnabled_Capture(cap.can_capture);
			setButtonsEnabled_Stream(cap.can_stream);
			m_reader.Close();
		} 
		catch (UareUException e1)
		{
			displayReaderNotFound();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (data == null)
		{
			displayReaderNotFound();
			return;
		}
		
		Globals.ClearLastBitmap();
		m_deviceName = (String) data.getExtras().get("device_name");

		switch (requestCode)
		{
		case GENERAL_ACTIVITY_RESULT:

			if((m_deviceName != null) && !m_deviceName.isEmpty())
			{
				m_selectedDevice.setText("Device: " + m_deviceName);

				try {
					Context applContext = getApplicationContext();
					m_reader = Globals.getInstance().getReader(m_deviceName, applContext);

					{
						PendingIntent mPermissionIntent;
						mPermissionIntent = PendingIntent.getBroadcast(applContext, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
						IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
						applContext.registerReceiver(mUsbReceiver, filter);

						if(DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(applContext, mPermissionIntent, m_deviceName))
						{
							CheckDevice();
						}
					}
				} catch (UareUException e1)
				{
					displayReaderNotFound();
				}
				catch (DPFPDDUsbException e)
				{
					displayReaderNotFound();
				}

			} else
			{ 
				displayReaderNotFound();
			}

			break;
		}
	}

	private void displayReaderNotFound()
	{
		m_selectedDevice.setText("Device: (No Reader Selected)");
		setButtonsEnabled(false);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Reader Not Found");
		alertDialogBuilder.setMessage("Plug in a reader and try again.").setCancelable(false).setPositiveButton("Ok",
				new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog,int id) {}
		});

		AlertDialog alertDialog = alertDialogBuilder.create();
		if(!isFinishing()) {
			alertDialog.show();
		}

	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action))
			{
				synchronized (this)
				{
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						if(device != null)
						{
							//call method to set up device communication
							CheckDevice();
						}
					}
	    			else
	    			{
	    				setButtonsEnabled(false);
	    			}
	    		}
	    	}
	    }
	};

	/*************************************************************************************************
	 *********** disable the power key when the device is boot from alarm but not ipo boot ***********
	 *************************************************************************************************/
	private static final String DISABLE_FUNCTION_LAUNCH_ACTION = "android.intent.action.DISABLE_FUNCTION_LAUNCH";
	private void disableFunctionLaunch(boolean state) {
		Intent disablePowerKeyIntent = new Intent(DISABLE_FUNCTION_LAUNCH_ACTION);
		if (state) {
			disablePowerKeyIntent.putExtra("state", true);
		} else {
			disablePowerKeyIntent.putExtra("state", false);
		}
		sendBroadcast(disablePowerKeyIntent);
	}
}
