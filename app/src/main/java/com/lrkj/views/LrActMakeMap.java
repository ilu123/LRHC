package com.lrkj.views;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.lrkj.LrApplication;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrSocketBridgeViewBase;
import com.lrkj.utils.LrSocketSurfaceView;
import com.lrkj.utils.LrToast;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.R.attr.switchMinWidth;
import static android.R.attr.x;

public class LrActMakeMap extends Activity implements LrSocketBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "LrActMakeMap::Activity";

    private LrSocketSurfaceView mCameraVideo, mCameraDot;
    private String mRobotIp;
    private String mMapName;
    private Thread mThreadCmd;
    private volatile boolean mStopThread;
    private volatile int mCmd = -1;
    private TextView mStatusTv ;

    public LrActMakeMap() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRobotIp = getIntent().getStringExtra("ip");
        mMapName = getIntent().getStringExtra("name");

        setContentView(R.layout.activity_make_map);

        mStatusTv = (TextView) findViewById(R.id.tv_status);
        mCameraVideo = (LrSocketSurfaceView) findViewById(R.id.camera_video);
        mCameraDot = (LrSocketSurfaceView) findViewById(R.id.camera_dot);

        if (mRobotIp != null) {
            mCameraVideo.setupSocketIpAndPort(mRobotIp, LrDefines.PORT_READ_LASER);
            mCameraDot.setupSocketIpAndPort(mRobotIp, LrDefines.PORT_DOT);
            //mCameraVideo.setupSocketIpAndPort("10.0.2.2", 8234);
            //mCameraDot.setupSocketIpAndPort("10.0.2.2", 8234);

            mCameraVideo.setVisibility(SurfaceView.VISIBLE);
            mCameraDot.setVisibility(SurfaceView.VISIBLE);

            mCameraVideo.setCvCameraViewListener(new LrSocketBridgeViewBase.CvCameraViewListener2() {
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
            mCameraDot.setCvCameraViewListener(new LrSocketBridgeViewBase.CvCameraViewListener2() {
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

        mThreadCmd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket skt = new Socket();
                    skt.setSoTimeout(3000);
                    skt.setReuseAddress(true);
                    skt.setKeepAlive(true);
                    skt.connect(new InetSocketAddress(mRobotIp, LrDefines.PORT_READ_VIDEO));

                    int slamstate = -1;
                    boolean mapSaved = false;
                    byte[] buff = new byte[100];
                    while (!mStopThread) {
                        try {
                            if (skt.getInputStream().read(buff, 0, 4) == -1) continue;
                            slamstate = buff[0];
                        }catch (Throwable e) {
                            continue;
                        }
                        try {
                            if (skt.getInputStream().read(buff, 0, 1) == -1) continue;
                            mapSaved = buff[0] == 1;
                        }catch (Throwable e) {
                            continue;
                        }
                        switch (slamstate){
                            case -1: setStatus("system not ready"); break;
                            case 0: setStatus("no image yet"); break;
                            case 1: setStatus("not initilized"); break;
                            case 2: setStatus("ok"); break;
                            case 3: setStatus("lost"); break;
                            default : break ;
                        }

                        try {
                            byte[] cmd = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(mCmd).array();
                            skt.getOutputStream().write(cmd);
                            skt.getOutputStream().flush();
                        }catch (Throwable e) {

                        }
                        mCmd = -1;
                        while (!mStopThread && mCmd == -1) {}
                    }
                    skt.close();
                }catch (Throwable e) {
                    setStatus("没有连接机器人");
                    LrToast.toast("没有连接机器人", LrApplication.sApplication);
                }
            }
        });
        mThreadCmd.start();
    }

    public void setStatus(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusTv.setText(s);
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mCameraVideo != null)
            mCameraVideo.disableView();
        if (mCameraDot != null)
            mCameraDot.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mCameraVideo != null)
            mCameraVideo.enableView();
        if (mCameraDot != null)
            mCameraDot.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        mStopThread = true;
        try {
            mThreadCmd.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThreadCmd = null;
        if (mCameraVideo != null)
            mCameraVideo.disableView();
        if (mCameraDot != null)
            mCameraDot.disableView();
    }

    /** Clicks **/
    public void onClickBtn(View v) {
        switch (v.getId()) {
            case R.id.btn_reset:
                mCmd = LrDefines.Cmds.CMD_MAP_RESET;
                break;
            case R.id.btn_finish:
                LrRobot.getRobot(mRobotIp).sendCommand(LrDefines.Cmds.CMD_MAP_FINISH, null);
                break;
            case R.id.btn_save:
                mCmd = LrDefines.Cmds.CMD_MAP_SAVE;
                break;
            case R.id.btn_exit:
                LrRobot.destroyRobot();
                Intent i = new Intent(this, LrMainEntryAct.class);
                startActivity(i);

                this.finish();
                break;
        }
        LrToast.toast("命令已发送", this);
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

    @Override
    public void onBackPressed() {}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
