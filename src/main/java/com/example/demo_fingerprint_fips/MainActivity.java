package com.example.demo_fingerprint_fips;

import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.example.demo_fingerprint_fips.fragment.AcquisitionFragment;
import com.example.demo_fingerprint_fips.fragment.GRABFragment;
import com.example.demo_fingerprint_fips.fragment.IdentificationFragment;
import com.example.demo_fingerprint_fips.fragment.TemplateVerify;
import com.rscja.deviceapi.FingerprintWithFIPS;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Delayed;

/**
 * FIPS指纹模块使用demo
 *
 * 1、使用前请确认您的机器已安装此模块。
 * 2、要正常使用模块需要在\libs\armeabi\目录放置libDeviceAPI.so文件，同时在\libs\目录下放置DeviceAPI.jar文件。
 * 3、在操作设备前需要调用 init()打开设备，使用完后调用 free() 关闭设备
 *
 *
 * 更多函数的使用方法请查看API说明文档
 *
 * @author liuruifeng
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity111";
    public FingerprintWithFIPS mFingerprint;
    private FragmentTabHost mTabHost;
    private FragmentManager fm;
    public  boolean   isPower=true;

    public String textClient = "";

    public String login = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_main);

        //RECEIVING DATA FROM CLIENT APP
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri data = intent.getData();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }
        initSound();
        initViewPageData();
        try {

            mFingerprint = FingerprintWithFIPS.getInstance();

        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, ex.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

    }


    protected void initViewPageData() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fm = getSupportFragmentManager();
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, fm, R.id.realtabcontent);

        if (login.equals("")){
            mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.fingerprint_tab_acquisition)).setIndicator(getString(R.string.fingerprint_tab_acquisition)),
                    AcquisitionFragment.class, null);
        }
        else
        {
            mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.fingerprint_tab_identification)).setIndicator(getString(R.string.fingerprint_tab_identification)),
                    IdentificationFragment.class, null);
        }

        /*
        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.fingerprint_tab_identification)).setIndicator(getString(R.string.fingerprint_tab_identification)),
                IdentificationFragment.class, null);

       mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.fingerprint_tab_verify)).setIndicator(getString(R.string.fingerprint_tab_verify)),
              TemplateVerify.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.fingerprint_tab_acquisition)).setIndicator(getString(R.string.fingerprint_tab_acquisition)),
                AcquisitionFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec("GRAB").setIndicator("GRAB"),
                GRABFragment.class, null);
                */

        boolean result;
        File file=new File(FileUtils.PATH);
        if(!file.exists()){
            result = file.mkdirs();
        }
    }



    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        Log.d(TAG,"onPause");
        if (mFingerprint != null) {
            mFingerprint.free();
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.d(TAG,"onResume");
        new InitTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public void init(){
        mFingerprint.free();
        new InitTask().execute();
    }
    /**
     * 设备上电异步类
     *
     * @author liuruifeng
     */
    public class InitTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            return mFingerprint.init();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            mypDialog.cancel();

            if (!result) {
                isPower=false;
                Toast.makeText(MainActivity.this, "init fail",
                        Toast.LENGTH_SHORT).show();
            }else{
                isPower=true;
                Toast.makeText(MainActivity.this, "init success", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();

            mypDialog = new ProgressDialog(MainActivity.this);
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("init...");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.show();
        }

    }



    HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();
    private SoundPool soundPool;
    private float volumnRatio;
    private AudioManager am;
    private void initSound(){
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
        soundMap.put(1, soundPool.load(this, R.raw.barcodebeep, 1));
        soundMap.put(2, soundPool.load(this, R.raw.serror, 1));
        am = (AudioManager) this.getSystemService(AUDIO_SERVICE);// 实例化AudioManager对象
    }
    /**
     * 播放提示音
     *
     * @param id 成功1，失败2
     */
    public void playSound(int id) {

        float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 返回当前AudioManager对象的最大音量值
        float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);// 返回当前AudioManager对象的音量值
        volumnRatio = audioCurrentVolumn / audioMaxVolumn;
        try {
            soundPool.play(soundMap.get(id), volumnRatio, // 左声道音量
                    volumnRatio, // 右声道音量
                    1, // 优先级，0为最低
                    0, // 循环次数，0无不循环，-1无永远循环
                    1 // 回放速度 ，该值在0.5-2.0之间，1为正常速度
            );
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

        void handleSendText(Intent intent) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.equals("")) {
                // Update UI to reflect text being shared
                Toast.makeText(this, "The login is: " + sharedText + " !", Toast.LENGTH_LONG).show();
                textClient = sharedText;
                login = sharedText;
            }
        }

        void handleSendImage(Intent intent) {
            Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                // Update UI to reflect image being shared
            }
        }

        void handleSendMultipleImages(Intent intent) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (imageUris != null) {
                // Update UI to reflect multiple images being shared
            }
        }

}

