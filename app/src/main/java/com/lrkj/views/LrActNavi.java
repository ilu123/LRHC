package com.lrkj.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.lrkj.LrApplication;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrSocketBridgeViewBase;
import com.lrkj.utils.LrSocketSurfaceView;
import com.lrkj.utils.LrToast;

import org.opencv.core.Mat;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class LrActNavi extends LrBaseAct implements LrSocketBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "LrActMakeMap::Activity";

    private LrSocketSurfaceView mCameraNavi;
    private String mRobotIp;
    private String mMapName;

    public LrActNavi() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRobotIp = getIntent().getStringExtra("ip");
        mMapName = getIntent().getStringExtra("map");

        setContentView(R.layout.activity_navi);

        mCameraNavi = (LrSocketSurfaceView) findViewById(R.id.camera_navi);

        if (mRobotIp != null) {
            mCameraNavi.setupSocketIpAndPort(mRobotIp, LrDefines.PORT_NAVIGATION, mMapName);
            mCameraNavi.setVisibility(SurfaceView.VISIBLE);
            mCameraNavi.setCvCameraViewListener(this);
        }else{
            LrToast.toast("没有连接机器人", this);
        }

        new Handler(LrApplication.sApplication.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                LrToast.showLoading(LrActNavi.this, "正在载入地图...");
            }
        }, 1200);
    }

    public void setStatus(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mCameraNavi != null)
            mCameraNavi.disableView();
        LrToast.stopLoading();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mCameraNavi != null)
            mCameraNavi.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mCameraNavi != null)
            mCameraNavi.disableView();
    }


    private void sendCmd(final int c) {
        LrToast.showLoading(this, "发送中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = LrRobot.sendCommand(mRobotIp, c, null);
                LrActNavi.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LrToast.stopLoading();
                        if (c == LrDefines.Cmds.CMD_NAVI_STOP) {
                            LrActNavi.this.finish();
                        }
                    }
                });
            }
        }).start();
    }

    /** Clicks **/
    public void onClickBtn(View v) {
        switch (v.getId()) {
            case R.id.btn_finish:
                sendCmd(LrDefines.Cmds.CMD_STOP_MISSION);
                break;
            case R.id.btn_close:
                sendCmd(LrDefines.Cmds.CMD_NAVI_STOP);
                break;
        }
    }


    /** Camera delegate */

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(LrSocketBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

//    @Override
//    public void onBackPressed() {}
//
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
