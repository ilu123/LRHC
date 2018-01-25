package com.lrkj.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;

public class LrActSystem extends Activity {
    private static final String TAG = "LrActMakeMap::Activity";

    private String mRobotIp;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRobotIp = getIntent().getStringExtra("ip");

        setContentView(R.layout.activity_system);


    }

    /** Clicks **/
    public void onClickBtn(View v) {
        switch (v.getId()) {
            case R.id.btn_shutdown:
                LrRobot.getRobot(mRobotIp).sendCommand(LrDefines.Cmds.CMD_SHUTDOWN, null);
            case R.id.btn_reboot:
                LrRobot.getRobot(mRobotIp).sendCommand(LrDefines.Cmds.CMD_REBOOT, null);
                break;
            case R.id.btn_kill:

                break;
            case R.id.btn_check:

                break;
        }
    }

}
