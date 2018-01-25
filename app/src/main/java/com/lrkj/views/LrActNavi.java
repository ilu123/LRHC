package com.lrkj.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

public class LrActNavi extends Activity implements LrSocketBridgeViewBase.CvCameraViewListener2{
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
            mCameraNavi.setupSocketIpAndPort(mRobotIp, LrDefines.PORT_NAVIGATION);
            mCameraNavi.setVisibility(SurfaceView.VISIBLE);
            mCameraNavi.setCvCameraViewListener(new LrSocketBridgeViewBase.CvCameraViewListener2() {
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
            });
        }else{
            LrToast.toast("没有连接机器人", this);
        }
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

    /** Clicks **/
    public void onClickBtn(View v) {
        LrToast.toast("命令已发送", this);
        switch (v.getId()) {
            case R.id.btn_finish:
                LrRobot.getRobot(mRobotIp).sendCommand(LrDefines.Cmds.CMD_STOP_MISSION, null);
                break;
            case R.id.btn_close:
                LrRobot.getRobot(mRobotIp).sendCommand(LrDefines.Cmds.CMD_NAVI_STOP, null);
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
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
}
