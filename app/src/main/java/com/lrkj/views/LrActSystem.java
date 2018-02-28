package com.lrkj.views;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lrkj.LrApplication;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;

public class LrActSystem extends LrBaseAct {
    private static final String TAG = "LrActMakeMap::Activity";

    private String mRobotIp;

    private int mCurrSpeed = 1;
    SeekBar mSeekBar;
    TextView mTvSpeed;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRobotIp = getIntent().getStringExtra("ip");
        mCurrSpeed = LrApplication.getSpeed();

        setContentView(R.layout.activity_system);


        mTvSpeed = (TextView) findViewById(R.id.tvSpeed);
        mTvSpeed.setText("移动速度("+mCurrSpeed+")");
        mSeekBar = ((SeekBar)findViewById(R.id.speedBar));
        mSeekBar.setProgress(mCurrSpeed);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mCurrSpeed = i;
                mTvSpeed.setText("移动速度("+mCurrSpeed+")");
                LrApplication.saveSpeed(mCurrSpeed);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void sendCmd(final int c) {
        LrToast.showLoading(this, "发送中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = LrRobot.sendCommand(mRobotIp, c, null);
                LrActSystem.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LrToast.stopLoading();
                        LrToast.toast(ok ? "命令已发送" : "命令发送失败");
                    }
                });
            }
        }).start();
    }

    /** Clicks **/
    public void onClickBtn(View v) {
        switch (v.getId()) {
            case R.id.btn_shutdown:
                sendCmd(LrDefines.Cmds.CMD_SHUTDOWN);
                break;
            case R.id.btn_reboot:
                sendCmd(LrDefines.Cmds.CMD_REBOOT);
                break;
            case R.id.btn_kill:
// fakeSerial -u
                sendCmd(LrDefines.Cmds.CMD_RESET_SYSTEM);
                break;
            case R.id.btn_check:

                break;
        }
    }

}
