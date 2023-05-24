package com.nastech.myfirstfingerprintapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.nastech.myfirstfingerprintapp.databinding.ActivityMainBinding;
import com.nastech.myfirstfingerprintapp.sample.CaptureFingerprintActivity;
import com.nastech.myfirstfingerprintapp.sample.CompareActivity;
import com.nastech.myfirstfingerprintapp.sample.EnrollmentActivity;
import com.nastech.myfirstfingerprintapp.sample.GetCapabilitiesActivity;
import com.nastech.myfirstfingerprintapp.sample.GetReaderActivity;
import com.nastech.myfirstfingerprintapp.sample.Globals;
import com.nastech.myfirstfingerprintapp.sample.IdentificationActivity;
import com.nastech.myfirstfingerprintapp.sample.StreamImageActivity;
import com.nastech.myfirstfingerprintapp.sample.VerificationActivity;
import com.gne.pm.PM;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

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

    private ActivityMainBinding binding;

    private String m_deviceName = "";
    private String m_versionName = "";

    private final String tag = "UareUSampleJava";

    private Reader m_reader;

    @Override
    protected void onStop() {
        binding.selectedDevice.setText("Device: (No Reader Selected)");

        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Determine if the current Android version is >=23
        // 判断Android版本是否大于23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                // 没有写文件的权限，去申请读写文件的权限，系统会弹出权限许可对话框
                //Without the permission to Write, to apply for the permission to Read and Write, the system will pop up the permission dialog
                ActivityCompat.requestPermissions(MainActivity.this, MY_PERMISSIONS, REQUEST_CODE);

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

    protected void setButtonsEnabled(Boolean enabled) {
        binding.getCapabilities.setEnabled(enabled);
        binding.streamImage.setEnabled(enabled);
        binding.captureFingerprint.setEnabled(enabled);
        binding.compareFingerprint.setEnabled(enabled);
        binding.enrollment.setEnabled(enabled);
        binding.verification.setEnabled(enabled);
        binding.identification.setEnabled(enabled);
    }

    protected void setButtonsEnabled_Capture(Boolean enabled) {
        binding.captureFingerprint.setEnabled(enabled);
        binding.compareFingerprint.setEnabled(enabled);
        binding.enrollment.setEnabled(enabled);
        binding.verification.setEnabled(enabled);
        binding.identification.setEnabled(enabled);
    }

    protected void setButtonsEnabled_Stream(Boolean enabled) {
        binding.streamImage.setEnabled(enabled);
    }

    protected void CheckDevice() {
        try {
            m_reader.Open(Reader.Priority.EXCLUSIVE);
            Reader.Capabilities cap = m_reader.GetCapabilities();
            setButtonsEnabled(true);
            setButtonsEnabled_Capture(cap.can_capture);
            setButtonsEnabled_Stream(cap.can_stream);
            m_reader.Close();
        } catch (UareUException e1) {
            displayReaderNotFound();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "no permission ,plz to request~", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, MY_PERMISSIONS, REQUEST_CODE);
            } else {
                init();
            }
        }

    }


    protected void onDestroy() {
        Toast.makeText(MainActivity.this,"MainActivity onDestroy()" ,Toast.LENGTH_SHORT).show();
        //power off
        PM.powerOff();

        //add by liuhao 20200331 for Kill app by abnormal operation
        //disable false
        disableFunctionLaunch(false);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDestroy();
    }


        private void requestUserPermission() throws DPFPDDUsbException {
            PendingIntent mPermissionIntent;
            mPermissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            getApplicationContext().registerReceiver(mUsbReceiver, filter);

            if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(getApplicationContext(), mPermissionIntent, m_deviceName)) {
                CheckDevice();
            }
        }


    private void init() {

        getUsbPermission();
        //power on
        PM.powerOn();


        //enable tracing
        System.setProperty("DPTRACE_ON", "1");
        //System.setProperty("DPTRACE_VERBOSITY", "10");

        binding = ActivityMainBinding.inflate(getLayoutInflater());


        setContentView(binding.getRoot());

        setButtonsEnabled(false);

        // register handler for UI elements
        binding.getReader.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, GetReaderActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.getCapabilities.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, GetCapabilitiesActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.captureFingerprint.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, CaptureFingerprintActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.compareFingerprint.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, CompareActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.streamImage.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, StreamImageActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.enrollment.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, EnrollmentActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.verification.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, VerificationActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        binding.identification.setOnClickListener(v -> {
            setButtonsEnabled(false);
            Intent i = new Intent(MainActivity.this, IdentificationActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, 1);
        });

        // get the version name
        try {
            m_versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(tag, e.getMessage());
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("DigitalPersona U are U SDK Sample" + " v" + m_versionName);
        }
    }

    private void getUsbPermission() {
        Log.e("getUsbPermission","entry");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
        if (!connectedDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : connectedDevices.entrySet()) {
                UsbDevice dev = entry.getValue();
                Log.e("device name",dev.getDeviceName());
                Log.e("device manufacture",dev.getManufacturerName());
                Log.e("device vendor id", String.valueOf(dev.getVendorId()));
                Log.e("device product id", String.valueOf(dev.getProductId()));
                Log.e("device product name",dev.getProductName());
                Log.e("device serial number",dev.getSerialNumber());

//                if (dev.getVendorId() == VENDOR_ID && dev.getProductId() == PRODUCT_ID) {
//                    device = dev;
//                    isUsbConnected = true;
//                    break;
//                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            displayReaderNotFound();
            return;
        }

        Globals.ClearLastBitmap();
        m_deviceName = (String) data.getExtras().get("device_name");

        switch (requestCode) {
            case GENERAL_ACTIVITY_RESULT:

                if ((m_deviceName != null) && !m_deviceName.isEmpty()) {
                    binding.selectedDevice.setText("Device: " + m_deviceName);

                    try {

                        m_reader = Globals.getInstance().getReader(m_deviceName, getApplicationContext());

                        {
                            requestUserPermission();
                        }
                    } catch (UareUException e1) {
                        displayReaderNotFound();
                    } catch (DPFPDDUsbException e) {
                        displayReaderNotFound();
                    }

                } else {
                    displayReaderNotFound();
                }

                break;
        }
    }

    private void displayReaderNotFound() {
        binding.selectedDevice.setText("Device: (No Reader Selected)");
        setButtonsEnabled(false);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Reader Not Found");
        alertDialogBuilder.setMessage("Plug in a reader and try again.").setCancelable(false).setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        if (!isFinishing()) {
            alertDialog.show();
        }

    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            CheckDevice();
                        }
                    } else {
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