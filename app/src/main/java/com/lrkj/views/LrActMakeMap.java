package com.lrkj.views;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
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
import com.lrkj.widget.JoystickHVView;

import org.opencv.core.Mat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LrActMakeMap extends LrBaseAct implements LrSocketBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "LrActMakeMap::Activity";

    private LrSocketSurfaceView mCameraVideo, mCameraDot;
    private String mRobotIp;
    private String mMapName;
    private volatile boolean mStopThread;
    private volatile int mStatus = -1;
    private TextView mStatusTv ;
    private volatile int mSpeed = 1;

    private Thread mThreadMove;
    private volatile int mDirection = 0;
    private volatile int mAngle = 0;

    private int mCurrStep = 0;
    private boolean hasShownDotOK = false;
    private boolean hasShownSaveMap = false;

    private Handler mHandlerCmd = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            LrToast.stopLoading();
            if (msg.what == 0x101) {
                mCameraDot.setupSocketIpAndPort(mRobotIp, LrDefines.PORT_DOT);
            }else if (msg.what == 0x103) {
                mCameraVideo.setupSocketIpAndPort(mRobotIp, LrDefines.PORT_READ_LASER, mMapName);
            }
            else if (msg.what == 0x102) {
                LrToast.toast("设置Localization失败！");
            }else if (msg.what == 0x104) {
                LrToast.toast("设置LaserMap失败!");
            }
        }
    };

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
                    mStatus = inputFrame.state();
                    String s = "system not ready";
                    switch (mStatus){
                        case -1: s = "system not ready"; break;
                        case 0: s =  "no image yet"; break;
                        case 1: s =  "not initilized"; break;
                        case 2: s =  "ok"; break;
                        case 3: s =  "lost"; break;
                        case LrDefines.State.MAP_SAVED:
                            mStatus = 2;
                            if (!hasShownSaveMap && mCurrStep == R.id.btn_save) {
                                hasShownSaveMap = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        LrToast.toast("地图保存完成！");
                                    }
                                });
                            }
                            return inputFrame.rgba();
                        case LrDefines.State.DOT_OK:
                            mStatus = 2;
                            if (hasShownDotOK == false && mCurrStep == R.id.btn_start_laser) {
                                hasShownDotOK = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        LrToast.toast("点云地图加载完成！");
                                    }
                                });
                            }
                            return inputFrame.rgba();
                        default : break ;

                    }
                    setStatus(s);
                    return inputFrame.rgba();
                }
            });
        }else{
            LrToast.toast("没有连接机器人", this);
            return;
        }

        LrToast.showLoading(this, "准备中...");
        mHandlerCmd.sendEmptyMessageDelayed(0x101, 1000);

        setupJoystick();
    }

    private void startMoveThread() {
        if (mThreadMove == null || !mThreadMove.isAlive()) {
            mThreadMove = new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket socket = new Socket();
                    try {
                        socket.setReuseAddress(true);
                    }catch (Throwable e) {}
                    boolean startOK = LrRobot.sendCommand(mRobotIp, 1079, null);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {

                    }
                    while (!mStopThread) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {

                        }
                        if (!startOK && !mStopThread){
                            startOK = LrRobot.sendCommand(mRobotIp, 1079, null);
                            //setStatus("无法控制移动");
                            continue;
                        }
                        if (socket.isClosed() && !mStopThread) {
                            try {
                                socket = new Socket();
                                try {
                                    socket.setReuseAddress(true);
                                }catch (Throwable e) {}
                                socket.connect(new InetSocketAddress(mRobotIp, 4108));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }else if (!socket.isConnected() && !mStopThread) {
                            try {
                                socket.connect(new InetSocketAddress(mRobotIp, 4108));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //setStatus("无法控制移动");
                            continue;
                        }
                        try {
                            byte[] cmd = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(mDirection*mSpeed).array();
                            socket.getOutputStream().write(cmd);
                            socket.getOutputStream().flush();
                        }catch (Throwable e) {}
                        try {
                            byte[] cmd = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(mAngle*mSpeed).array();
                            socket.getOutputStream().write(cmd);
                            socket.getOutputStream().flush();
                        }catch (Throwable e) {}
                    }


                    try {
                        try{
                            socket.shutdownInput();
                        }catch (Throwable e) {}
                        try {
                            socket.shutdownOutput();
                        }catch (Throwable e) {}
                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            mThreadMove.start();
        }
    }

    public void setStatus(final String s) {
        if (s == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mStopThread == false)
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

        mSpeed = LrApplication.getSpeed();

        if (mCameraVideo != null)
            mCameraVideo.enableView();
        if (mCameraDot != null)
            mCameraDot.enableView();
    }

    @Override
    public void finish() {
        super.finish();
    }

    public void onDestroy() {
        super.onDestroy();
        destroyThread();
        if (mCameraVideo != null)
            mCameraVideo.disableView();
        if (mCameraDot != null)
            mCameraDot.disableView();
    }

    void destroyThread() {
        mStopThread = true;
        if (mThreadMove != null) {
            mThreadMove.interrupt();
            mThreadMove = null;
        }
    }

    private void sendCmd(final int c) {
        LrToast.showLoading(this, c ==  LrDefines.Cmds.CMD_MAP_LASER ? "地图加载中" : "发送命令...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                if (c == LrDefines.Cmds.CMD_MAP_SAVE) {
                    ok = LrRobot.sendCmd(mRobotIp, LrDefines.PORT_READ_VIDEO, c, null);
                }else if (c == LrDefines.Cmds.CMD_MAP_LASER) {
                    while (!mStopThread && !LrRobot.sendCmd(mRobotIp, LrDefines.PORT_COMMANDS, LrDefines.Cmds.CMD_SLAM, null));
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (!mStopThread && !LrRobot.sendCmd(mRobotIp, LrDefines.PORT_COMMANDS, LrDefines.Cmds.CMD_SLAM, mMapName));
                    try {
                        mStatus = -1;
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (!mStopThread && 2 != mStatus);
                    if (!mStopThread && 2 == mStatus)
                        ok = LrRobot.sendCmd(mRobotIp, LrDefines.PORT_READ_VIDEO, c, null);
                }else{
                    if (c == LrDefines.Cmds.CMD_MAP_RESET) {
                        if (!mStopThread)
                            ok = LrRobot.sendCmd(mRobotIp, LrDefines.PORT_READ_VIDEO, c, null);
                    }else {
                        if (!mStopThread && 2 == mStatus)
                            ok = LrRobot.sendCmd(mRobotIp, LrDefines.PORT_READ_VIDEO, c, null);
                    }
                }
                final String msg = ok ? "命令已发送" : "命令发送失败";
                if (mHandlerCmd == null)
                    return;
                LrActMakeMap.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mHandlerCmd == null)
                            return;
                        if (c == LrDefines.Cmds.CMD_MAP_LASER) {
                            LrToast.stopLoading();
                            LrToast.toast(msg);
                            mHandlerCmd.sendEmptyMessageDelayed(0x103, 3000);
                        }else if (c == LrDefines.Cmds.CMD_MAP_SAVE) {
                            mHandlerCmd.sendEmptyMessageDelayed(-1, 3000);
                            LrToast.toast(msg);
                        }else{
                            LrToast.stopLoading();
                            LrToast.toast(msg);
                        }
                    }
                });
            }
        }).start();
    }
    /** Clicks **/
    public void onClickBtn(View v) {
        mCurrStep = v.getId();
        switch (v.getId()) {
            case R.id.btn_reset:
                sendCmd(LrDefines.Cmds.CMD_MAP_RESET);
                mCameraDot.resetCanvas(255);
                break;
            case R.id.btn_finish:
                sendCmd(LrDefines.Cmds.CMD_MAP_FINISH);
                break;
            case R.id.btn_save:
                sendCmd(LrDefines.Cmds.CMD_MAP_SAVE);
                break;
            case R.id.btn_start_laser:
                sendCmd(LrDefines.Cmds.CMD_MAP_LASER);
                break;
            case R.id.btn_reset_laser:
                mCameraVideo.resetCanvas(0);
                break;
            case R.id.btn_save_laser:
                mCameraVideo.saveLaserFrame();
                break;
            case R.id.btn_exit:
                destroyThread();
                if (mCameraVideo != null)
                    mCameraVideo.disableView();
                if (mCameraDot != null)
                    mCameraDot.disableView();

                LrToast.showLoading(this, "关闭中...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LrRobot.sendCommand(mRobotIp, LrDefines.Cmds.CMD_RESET_SYSTEM, null);
                        LrActMakeMap.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LrToast.stopLoading();

                                Intent i = new Intent(LrActMakeMap.this, LrMainEntryAct.class);
                                i.putExtra("ip", mRobotIp);
                                startActivity(i);
                                LrActMakeMap.this.finish();
                            }
                        });
                    }
                }).start();
                break;
        }
    }

    // Setup joystick
    private void setupJoystick() {
        JoystickHVView joystick = (JoystickHVView) findViewById(R.id.joystick_hor);
        joystick.setOnJoystickMoveListener(new JoystickHVView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction, boolean isUp) {
                if (isUp) {
                    mAngle = 0;
                    return;
                }
                switch (direction) {
                    case JoystickHVView.RIGHT:
                        mAngle = -1;
                        break;
                    case JoystickHVView.LEFT:
                        mAngle = 1;
                        break;
                    default:
                        mAngle = 0;
                        break;
                }
            }
        }, JoystickHVView.DEFAULT_LOOP_INTERVAL);

        joystick = (JoystickHVView) findViewById(R.id.joystick_ver);
        joystick.setOnJoystickMoveListener(new JoystickHVView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction, boolean isUp) {
                if (isUp) {
                    mDirection = 0;
                    return;
                }
                switch (direction) {
                    case JoystickHVView.FRONT:
                        mDirection = 1;
                        break;
                    case JoystickHVView.BOTTOM:
                        mDirection = -1;
                        break;
                    default:
                        mDirection = 0;
                        break;
                }
            }
        }, JoystickHVView.DEFAULT_LOOP_INTERVAL);

        startMoveThread();
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
    public void onBackPressed() {
        if (LrToast.isLoading()) {
            LrToast.stopLoading();
        }else{
            onClickBtn(findViewById(R.id.btn_exit));
        }
    }
}
