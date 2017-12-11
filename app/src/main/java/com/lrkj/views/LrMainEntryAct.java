package com.lrkj.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;

import com.lrkj.views.dialogs.LrIpDialog;
import com.lrkj.widget.CircularRevealView;
import com.lrkj.ctrl.R;

/**
 * Created by ztb.
 */
public class LrMainEntryAct extends Activity implements DialogInterface.OnDismissListener {

    private CircularRevealView revealView;
    private int backgroundColor;
    Handler handler;
    int maxX, maxY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_entry);

        revealView = (CircularRevealView) findViewById(R.id.reveal);

        Display mdisp = getWindowManager().getDefaultDisplay();
        Point mdispSize = new Point();
        mdisp.getSize(mdispSize);
        maxX = mdispSize.x;
        maxY = mdispSize.y;

        final int color = Color.parseColor("#00bcd4");
        final Point p = new Point(maxX / 2, maxY / 2);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                revealView.reveal(p.x, p.y, color, 2, 440, null);
            }
        }, 500);


        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showPowerDialog();
            }
        }, 800);


    }

    private void showPowerDialog() {
        FragmentManager fm = getFragmentManager();
        LrIpDialog powerDialog = new LrIpDialog();
        powerDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppThemeDialog);
        powerDialog.show(fm, "fragment_power");

    }

    public void revealFromTop() {
        final int color = Color.parseColor("#ffffff");

        final Point p = new Point(maxX / 2, maxY / 2);

        revealView.reveal(p.x, p.y, color, 2, 440, null);


    }

    @Override
    public void onDismiss(final DialogInterface dialog) {

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

}
