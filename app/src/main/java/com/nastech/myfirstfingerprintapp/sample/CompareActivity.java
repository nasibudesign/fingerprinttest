/*
 * File: 		CaptureFingerprintActivity.java
 * Created:		2013/05/03
 *
 * copyright (c) 2013 DigitalPersona Inc.
 */

package com.nastech.myfirstfingerprintapp.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Importer;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.dpfj.ImporterImpl;
import com.nastech.myfirstfingerprintapp.R;
import com.gne.pm.PM;
import com.nastech.myfirstfingerprintapp.utils.DBManager;
import com.nastech.myfirstfingerprintapp.utils.MyFileUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CompareActivity extends Activity {
    //创建文件的名称
    public static final String FP_FILENAME = "mFPData.txt";

    private Button m_back;
    private String m_deviceName = "";

    private Reader m_reader = null;
    private int m_DPI = 0;
    private Bitmap m_bitmap = null;
    private ImageView m_imgView;
    private TextView m_text_selectedDevice;
    private TextView m_text_title;
    private boolean m_reset = false;
    private TextView m_text_conclusion;
    private String m_text_conclusionString;
    private Reader.CaptureResult cap_result = null;

    private byte[] bytesCapture;

    private String fileName = "";

    private DBManager mDBManager = null;
    private ImageView iv_compare;
    private TextView tv_result,tv_tip;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private void initializeActivity() {
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        m_text_title = (TextView) findViewById(R.id.title);
        m_text_title.setText("Capture");
        m_text_selectedDevice = (TextView) findViewById(R.id.selected_device);
        m_deviceName = getIntent().getExtras().getString("device_name");

        m_text_selectedDevice.setText("Device: " + m_deviceName);

        m_imgView = (ImageView) findViewById(R.id.bitmap_image);
        iv_compare = (ImageView) findViewById(R.id.iv_compare);
        m_bitmap = Globals.GetLastBitmap();
        if (m_bitmap == null)
            m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
        m_imgView.setImageBitmap(m_bitmap);

        m_text_conclusion = (TextView) findViewById(R.id.text_conclusion);
        tv_result = (TextView) findViewById(R.id.tv_result);
        tv_tip = (TextView) findViewById(R.id.tv_tip);
        tv_result.setMovementMethod(ScrollingMovementMethod.getInstance());

        m_back = (Button) findViewById(R.id.back);

        m_back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    private void setValue(int val) {
        sp = getSharedPreferences("FpData", MODE_PRIVATE);
        editor = sp.edit();
        editor.putInt("count", val);
        editor.commit();
    }

    private int getValue() {
        sp = getSharedPreferences("FpData", MODE_PRIVATE);
        int value = sp.getInt("count", 1);
        return value;
    }

    @Override
    protected void onResume() {
        super.onResume();
        FID=getValue();
        Log.e("liuhao","count = "+FID);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //add by liuhao 20200330
        //MyApplication.getInstance().addActivity(this);

        //power on
        PM.powerOn();

        //no title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_compare);

        initializeActivity();

        fileName = MyFileUtils.getStoragePath(CompareActivity.this, false) + "/" + FP_FILENAME;

        mDBManager = DBManager.getInstance(CompareActivity.this);

        //initReader();

    }

    @Override
    protected void onDestroy() {

        //power off
        //PM.powerOff();
        //add by liuhao 20200330
        //MyApplication.getInstance().destory();

        super.onDestroy();

        try {
            m_reset = true;
            Log.e("liuhao CompareActivity","onDestroy m_reset = " + m_reset);
            m_reader.CancelCapture();
            m_reader.Close();
        } catch (Exception e) {
            Log.e("liuhao CompareActivity", "onDestroy() Exception e:" + e.toString());
        }
    }

    // called when orientation has changed to manually destroy and recreate activity
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_capture_stream);
        initializeActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compare,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()== R.id.menu_clean){
            mDBManager.deleteDatas();
            sbResult.delete(0,sbResult.toString().length()-1);
            tv_result.setText("");
            return true;
        }
//        switch (item.getItemId()){
//
//            case R.id.menu_clean:
//                mDBManager.deleteDatas();
//                sbResult.delete(0,sbResult.toString().length()-1);
//                tv_result.setText("");
//                break;
//            default:
//                break;
//        }

        return true;
    }

    public void onBackPressed() {
        try {
            m_reset = true;
            Log.e("CompareActivity","onBackPressed() m_reset = " + m_reset);
            try {
                m_reader.CancelCapture();
            } catch (Exception e) {
            }
            m_reader.Close();
        } catch (Exception e) {
            Log.e("CompareActivity", "onBackPressed() exception:" + e.toString());
        }

        Intent i = new Intent();
        i.putExtra("device_name", m_deviceName);
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    public void initReader() {
        // initiliaze dp sdk
        if (m_reader != null) {
            Log.e("liuhao","initReader() m_reader != null");
            return;
        }

        try {
            Context applContext = getApplicationContext();
            m_reader = Globals.getInstance().getReader(m_deviceName, applContext);
            m_reader.Open(Reader.Priority.EXCLUSIVE);
            m_DPI = Globals.GetFirstDPI(m_reader);

//            final String product_name = m_reader.GetDescription().id.product_name;
//            runOnUiThread(new Runnable() { public void run() {Toast.makeText(CompareActivity.this,"product_name : " +product_name , Toast.LENGTH_SHORT).show();}});

        } catch (Exception e) {
            Log.e("CompareActivity", "error during init of reader");
            m_deviceName = "";
//            onBackPressed();
            return;
        }
    }

    public void destroyReader(){
        try {
            if(m_reader!=null){
                m_reader.CancelCapture();
                m_reader.Close();
                m_reader = null;
            }
        } catch (Exception e) {
            Log.e("UareUSampleJava", "destroyReader () error :" + e.toString());
        }
    }

    public void UpdateGUI() {
        int bytes = m_bitmap.getByteCount();
        ByteBuffer buf = ByteBuffer.allocate(bytes);
        tv_result.setText("");
        m_bitmap.copyPixelsToBuffer(buf);
        m_imgView.setImageBitmap(m_bitmap);
        m_imgView.invalidate();
        m_text_conclusion.setText(m_text_conclusionString);
    }

    CaptureThread captureThread = null;
    private boolean mCapture_bThreadFinished = false;
    int FID = 0;

    public class CaptureThread extends Thread {

        public boolean isThreadFinished() {
            return mCapture_bThreadFinished;
        }

        public void run() {

            /****************************************************
             *  modify by liuhao 20181031 for Thread loop START
             ****************************************************/
            initReader();
            /****************************************************
             *  modify by liuhao 20181031 for Thread loop END
             ****************************************************/

            while (!m_reset) {
                Log.e("liuhao","CaptureThread run *****************");
                synchronized (this) {
                    mCapture_bThreadFinished = false;
                    try {
                        /*
                         *Capture Finger Print 采集特征
                         *可根据传递的值，获取不同标准的指纹模板
                         *Different standard fingerprint templates can be obtained based on the passed values
                         */
                        cap_result = m_reader.Capture(Fid.Format.ISO_19794_4_2005, Globals.DefaultImageProcessing, m_DPI, -1);
                        Engine m_engine = UareUGlobal.GetEngine();
                        //add by liuhao 20180918
                        if(cap_result.image == null) return;
                        Fmd m_fmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ISO_19794_2_2005);
                        bytesCapture = m_fmd.getData();
                        Log.e("liuhao", "*******************CAPTURE******************");

                        //WSQ
                        //NfiqAndWsqCompression isoWsq = new NfiqAndWsqCompression();
                        //byte[] getWSQCompressedISOImag = isoWsq.GetWSQCompressedISOImage(bytesCapture);

                        /******************************************************/
                        /***********add by liuhao 20180522 START***************/
                        /*
                         *When you save the retrieved fingerprint ISO / ANSI bytes for database storage, you need to
                         *convert them to the basic format supported by the database. We recommend that you always  *convert them to Base64
                         *将获取到的指纹ISO / ANSI格式bytes进行数据库保存时，需要转换成数据库支持的基本格式，我们建议一定要转换成Base64
                         */
                        String strStore = Base64.encodeToString(bytesCapture, Base64.DEFAULT);
                        //Store to database 存储到数据库
                        mDBManager.insertData(FID, strStore);

                        FID++;

                        setValue(FID);
                        /***********add by liuhao 20180522 END***************/
                        /******************************************************/
                        /*
                             Use Globals.GetBitmapFromRaw from raw to bitmap
                             通过Globals.GetBitmapFromRaw 转换成bitmap
                        */
                        m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                        m_text_conclusionString = Globals.QualityToString(cap_result);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UpdateGUI();
                            }
                        });
                    } catch (UareUException e) {
                            Log.e("UareUSampleJava", "error during capture: " + e.toString());
                            m_deviceName = "";
                            onBackPressed();
                    }
                    mCapture_bThreadFinished = true;
                }
            }
        }
    }

    public void OnClickCapture(View view) {
        Log.e("liuhao", "OnClickCapture");

        mCompare_bThreadFinish = true;
            if (null != captureThread && !captureThread.isThreadFinished()) {
                Log.e("liuhao", "Capture return return");
                //Toast.makeText(CaptureFingerprintActivity.this,"Capture return return",Toast.LENGTH_LONG).show();
                return;
            }
            tv_tip.setText("Please press fingerprint~~~");

            //add by liuhao 20181031 for destroy Thread
            m_reset = false;
            destroyReader();

        // loop capture on a separate thread to avoid freezing the UI
            captureThread = new CaptureThread();
            captureThread.start();
    }

    CompareThread compareThread = null;
    boolean mCompare_bThreadFinish = false;
    StringBuffer sbResult;

    public class CompareThread extends Thread {
        public boolean isThreadFinished() {
            return mCompare_bThreadFinish;
        }

        @Override
        public void run() {
            synchronized (this) {

                initReader();

                mCompare_bThreadFinish = false;
                sbResult =new StringBuffer();
                try {

                    //Capture Finger Print 采集特征
                    cap_result = m_reader.Capture(Fid.Format.ISO_19794_4_2005, Globals.DefaultImageProcessing, m_DPI, -1);
                    Engine m_engine = UareUGlobal.GetEngine();
                    //add by liuhao 20180918
                    if(cap_result.image == null) return;
                    Fmd m_fmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ISO_19794_2_2005);
                    bytesCapture = m_fmd.getData();
                    Log.e("liuhao", "*******************COMPARE******************");

                    //特征比对
                    /*
                    byte1 and byte2 respectively represent the original saved fingerprint data
                    retrieved from the database or newly collected fingerprint data。
                     */
                    //byte1,byte2 是从数据库取出的原来保存的指纹数据
                    //byte[] byte1 = MyFileUtils.read(fileName);
                    ArrayList<String> dataList = mDBManager.queryDatas();
                    Log.e("liuhao", "listSize = " + dataList.size());

                    //byte [] - > Fmd conversion 进行byte[] - > Fmd的转换
                    Importer importer = new ImporterImpl();
                    final Fmd fmdCapture = importer.ImportFmd(bytesCapture, Fmd.Format.ISO_19794_2_2005, Fmd.Format.ISO_19794_2_2005);

                    for (int i = 0; i < dataList.size(); i++) {

                        byte[] bytesStore = Base64.decode(dataList.get(i), Base64.DEFAULT);
                        Log.e("liuhao", "list -> i =" + i + " , \ndata = " + dataList.get(i));

                        //每次new 一个 importer
                        //Importer importer2 = new ImporterImpl();
                        Fmd fmdStore = importer.ImportFmd(bytesStore, Fmd.Format.ISO_19794_2_2005, Fmd.Format.ISO_19794_2_2005);
                        //进行比对,得到比分
                        /*
                        /*
                            *The score is between 0 and 2147483647, with 0 indicating a match and other mismatches at other times,
                            * the closer you get to 0, the more matching  you get, and at other times,
                            * the closer you get to 2147483647, the less matching
                            *分值在 0 和 2147483647 之间 , 0 表示匹配，其他不匹配
                            *越接近0，表示越匹配, 其他时，越接近2147483647，表示越不匹配
                        */
                        final int score = m_engine.Compare(fmdStore, 0, fmdCapture, 0);

                        final int index = i;
                        if (score >= 0 && score <= 2147) {
                            sbResult.append("Compare Success：score = " + score + "  index = " + index + "\n");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tv_tip.setText("Compare Result:");
                                    tv_result.setText(sbResult.toString());
                                }
                            });
                        } else {
                            sbResult.append("Compare Failed : score = " + score + "  index = " + index + "\n");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tv_tip.setText("Compare Result:");
                                    tv_result.setText(sbResult.toString());
                                }
                            });
                        }
                    }

                    //通过Globals.GetBitmapFromRaw 转换成bitmap
                    m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                    m_text_conclusionString = Globals.QualityToString(cap_result);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int bytes = m_bitmap.getByteCount();
                            ByteBuffer buf = ByteBuffer.allocate(bytes);
                            m_bitmap.copyPixelsToBuffer(buf);
                            iv_compare.setImageBitmap(m_bitmap);
                            iv_compare.invalidate();
                        }
                    });

                } catch (UareUException e) {
                        e.printStackTrace();
                        Log.e("liuhao", "Compare exception ->" + e.toString());
                        onBackPressed();
                }
                mCompare_bThreadFinish = true;
                //add by liuhao 20181031 for cancel capture Thread
                //mCapture_bThreadFinished = true;
            }
        }
    }

    public void OnClickCompare(View view) {

        Log.e("liuhao", "OnClickCompare");

        mCapture_bThreadFinished = true;
        m_reset = true;

        if (null != compareThread && !compareThread.isThreadFinished()) {
            Log.e("liuhao", "Compare return return");
            return;
        }
        tv_tip.setText("Press your fingerprint and compare...");

        //add by liuhao 20181031 for destroy Thread
        destroyReader();

        compareThread = new CompareThread();
        compareThread.start();
    }

}
