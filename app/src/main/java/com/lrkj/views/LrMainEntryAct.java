package com.lrkj.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.widget.FrameLayout;

import com.lrkj.LrApplication;
import com.lrkj.business.LrNativeApi;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;
import com.lrkj.views.dialogs.LrIpDialog;
import com.lrkj.widget.CircularRevealView;

import org.opencv.android.OpenCVLoader;

/**
 * Created by ztb.
 */
public class LrMainEntryAct extends LrBaseAct implements DialogInterface.OnDismissListener {

    private FrameLayout mFragmentContainer;
    private LrIpDialog mFragDialog;
    private Fragment mFragMenu;

    private CircularRevealView revealView;
    private int backgroundColor;
    Handler handler;
    int maxX, maxY;
    String mIp;

    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) //系统回收bug
            return;

        LrApplication.mkMapFolder();

        setContentView(R.layout.activity_main_entry);

        mFragmentContainer = (FrameLayout)findViewById(R.id.fragment_container);
        revealView = (CircularRevealView) findViewById(R.id.reveal);

        Display mdisp = getWindowManager().getDefaultDisplay();
        Point mdispSize = new Point();
        mdisp.getSize(mdispSize);
        maxX = mdispSize.x;
        maxY = mdispSize.y;

        final int color = Color.parseColor(LrDefines.COLOR_MAIN_THEME);
        final Point p = new Point(maxX / 2, maxY / 2);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                revealView.reveal(p.x, p.y, color, 2, 440, null);
            }
        }, 500);


        final String ip = getIntent().getStringExtra("ip");
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ip != null) {
                    showMenuFragment();
                }else {
                    showIpDialog();
                }
            }
        }, 800);
    }

    private void showIpDialog() {
        FragmentManager fm = getFragmentManager();
        if (mFragDialog == null)
            mFragDialog = new LrIpDialog();
        mFragDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppThemeDialog);
        mFragDialog.show(fm, "fragment_ip");
    }

    private void showMenuFragment() {
        if (mFragMenu == null)
            mFragMenu = new LrFragMenu();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, mFragMenu);
        ft.commitAllowingStateLoss();
        final Point p = new Point(maxX / 2, maxY / 2);
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                revealView.hide(p.x, p.y, backgroundColor, 0, 330, null);
            }
        }, 300);
        LrApplication.startMsgService();
    }

    public void revealFromTop(String colorr) {
        final int color = Color.parseColor(colorr);

        final Point p = new Point(maxX / 2, maxY / 2);

        revealView.reveal(p.x, p.y, color, 2, 440, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIp = LrApplication.getIP();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        this.mIp = this.mFragDialog.getIp();
        LrNativeApi.setRobotIp(mIp);
        showMenuFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (revealView == null) //GC bug
            return;
        final Point p = new Point(maxX / 2, maxY / 2);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                revealView.hide(p.x, p.y, backgroundColor, 0, 330, null);
            }
        }, 300);
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                overridePendingTransition(0, 0);
            }
        }, 500);
    }

    public void gotoMapActivity(final String name) {
        if (LrRobot.sendCommand(mIp, LrDefines.Cmds.CMD_SLAM, name)){
            LrToast.showLoading(this, "准备中...");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LrToast.stopLoading();
                    Intent i = new Intent(LrMainEntryAct.this, LrActMakeMap.class);
                    i.putExtra("ip", mIp);
                    i.putExtra("name", name);
                    startActivity(i);
                    LrMainEntryAct.this.finish(); //GC bug
                }
            }, 3000);
        }
        else{
            LrToast.toast("启动SLAM失败!");
        }

    }

    public void gotoAllMaps(boolean isNavi){
        Intent i = new Intent(this, LrActAllMap.class);
        i.putExtra("ip", this.mIp);
        i.putExtra("navi", isNavi);
        startActivity(i);
    }

    public void gotoSystem(){
        Intent i = new Intent(this, LrActSystem.class);
        i.putExtra("ip", this.mIp);
        startActivity(i);
    }
}
