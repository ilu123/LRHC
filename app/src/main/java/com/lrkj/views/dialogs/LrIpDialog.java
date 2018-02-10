package com.lrkj.views.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.lrkj.LrApplication;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.views.LrMainEntryAct;
import com.lrkj.widget.CircularRevealView;

/**
 * Created by ztb.
 */
public class LrIpDialog extends DialogFragment {

    public LrIpDialog() {

    }

    FrameLayout frame, frame2;
    private CircularRevealView revealView;
    private int backgroundColor;
    ProgressBar progress;
    EditText etIp;
    Button btnConnect;

    private static final int BG_PRIO = android.os.Process.THREAD_PRIORITY_BACKGROUND;
    private static final int RUNNABLE_DELAY_MS = 6000;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ip, container, false);

        revealView = (CircularRevealView) view.findViewById(R.id.reveal);
        backgroundColor = Color.parseColor("#ffffff");

        frame = (FrameLayout) view.findViewById(R.id.frame);
        frame2 = (FrameLayout) view.findViewById(R.id.frame2);

        progress = (ProgressBar) view.findViewById(R.id.progress);
        progress.getIndeterminateDrawable().setColorFilter(
                Color.parseColor("#ffffff"),
                android.graphics.PorterDuff.Mode.SRC_IN);

        btnConnect = (Button) view.findViewById(R.id.btn_connect);
        etIp = (EditText) view.findViewById(R.id.et_ip);

        etIp.setText(LrApplication.getIP());

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int color = Color.parseColor(LrDefines.COLOR_IP_DIALOG_THEME);
                final Point p = getLocationInView(revealView, v);
                final int h2 = v.getHeight() / 2;

                revealView.reveal(p.x / 2, p.y / 2, color, h2, 440, null);
                ((LrMainEntryAct) getActivity()).revealFromTop("#ffffff");
                frame.setVisibility(View.GONE);
                frame2.setVisibility(View.VISIBLE);

                new BackgroundThread(new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (msg.what == 100) {
                            LrApplication.saveIP(etIp.getText().toString());
                            LrIpDialog.this.dismiss();
                        } else if (msg.what == -100) {
                            revealView.hide(p.x, p.y, backgroundColor, 0, 330, null);
                            ((LrMainEntryAct) getActivity()).revealFromTop(LrDefines.COLOR_MAIN_THEME);
                            frame.setVisibility(View.VISIBLE);
                            frame2.setVisibility(View.GONE);
                        }
                    }
                }, etIp.getText().toString().trim()).start();


            }
        });

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.setCancelable(false);
        return view;

    }

    public String getIp() {
        return etIp.getText().toString().trim();
    }

    private static void setThreadPrio(int prio) {
        android.os.Process.setThreadPriority(prio);
    }

    private static class BackgroundThread extends Thread {
        private Handler mHandler;
        private String mIp;

        private BackgroundThread(Handler cmd, String ip) {
            this.mHandler = cmd;
            this.mIp = ip;
        }

        @Override
        public void run() {
            super.run();
            //setThreadPrio(BG_PRIO);

            try {
                int result = LrRobot.sendCommand(mIp, LrDefines.Cmds.CMD_RESET_SYSTEM, null) ? 100 : -100;
                if (result == 100) {
                    try {
                        Thread.sleep(4000);
                    }catch (Throwable e){
                        e.printStackTrace();
                    }
                }
                this.mHandler.sendEmptyMessageDelayed(result, RUNNABLE_DELAY_MS);
                this.mHandler = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.0f;

        window.setAttributes(windowParams);
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity activity = getActivity();
        if (activity != null && activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }
    }

    private Point getLocationInView(View src, View target) {
        final int[] l0 = new int[2];
        src.getLocationOnScreen(l0);

        final int[] l1 = new int[2];
        target.getLocationOnScreen(l1);

        l1[0] = l1[0] - l0[0] + target.getWidth() / 2;
        l1[1] = l1[1] - l0[1] + target.getHeight() / 2;

        return new Point(l1[0], l1[1]);
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.DialogAnimation;
    }


}

