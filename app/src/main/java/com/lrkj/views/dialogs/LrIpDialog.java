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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lrkj.ctrl.R;
import com.lrkj.views.LrMainEntryAct;
import com.lrkj.widget.CircularRevealView;
import com.lrkj.widget.TextDrawable;

/**
 * Created by ztb.
 */
public class LrIpDialog extends DialogFragment {

    public LrIpDialog() {

    }

    FrameLayout frame, frame2;
    private CircularRevealView revealView;
    private View selectedView;
    private int backgroundColor;
    ProgressBar progress;
    EditText etIp;
    Button btnConnect;

    private static final int BG_PRIO = android.os.Process.THREAD_PRIORITY_BACKGROUND;
    private static final int RUNNABLE_DELAY_MS = 3000;


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
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int color = Color.parseColor("#d32f2f");
                final Point p = getLocationInView(revealView, v);

                if (selectedView == v) {
                    revealView.hide(p.x, p.y, backgroundColor, 0, 330, null);
                    selectedView = null;
                } else {
                    revealView.reveal(p.x / 2, p.y / 2, color, v.getHeight() / 2, 440, null);
                    selectedView = v;
                }

                ((LrMainEntryAct) getActivity()).revealFromTop();
                frame.setVisibility(View.GONE);
                frame2.setVisibility(View.VISIBLE);

                new BackgroundThread(new Handler(Looper.getMainLooper()){
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (msg.what == 0xD1) {
                            LrIpDialog.this.dismiss();
                        }
                    }
                }).start();


            }
        });

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.setCancelable(false);
//
//        TextDrawable drawable1 = TextDrawable.builder()
//                .buildRound("P", Color.parseColor("#d32f2f"));
//        ((ImageView) view.findViewById(R.id.ipower)).setImageDrawable(drawable1);
//
//        TextDrawable drawable2 = TextDrawable.builder()
//                .buildRound("S", Color.parseColor("#009688"));
//        ((ImageView) view.findViewById(R.id.isafe)).setImageDrawable(drawable2);
//
//        TextDrawable drawable3 = TextDrawable.builder()
//                .buildRound("B", Color.parseColor("#009688"));
//        ((ImageView) view.findViewById(R.id.ibootloader)).setImageDrawable(drawable3);
//
//        TextDrawable drawable4 = TextDrawable.builder()
//                .buildRound("R", Color.parseColor("#009688"));
//        ((ImageView) view.findViewById(R.id.irecovery)).setImageDrawable(drawable4);
//
//        TextDrawable drawable5 = TextDrawable.builder()
//                .buildRound("S", Color.parseColor("#e91e63"));
//        ((ImageView) view.findViewById(R.id.isoftreboot)).setImageDrawable(drawable5);
//
//        TextDrawable drawable6 = TextDrawable.builder()
//                .buildRound("R", Color.parseColor("#3f51b5"));
//        ((ImageView) view.findViewById(R.id.ireboot)).setImageDrawable(drawable6);
//

        return view;

    }

    private static void setThreadPrio(int prio) {
        android.os.Process.setThreadPriority(prio);
    }

    private static class BackgroundThread extends Thread {
        private Handler mHandler;

        private BackgroundThread(Handler cmd) {
            this.mHandler = cmd;
        }

        @Override
        public void run() {
            super.run();
            setThreadPrio(BG_PRIO);

            /**
             * Sending a system broadcast to notify apps and the system that we're going down
             * so that they write any outstanding data that might need to be flushed
             */
            //Shell.SU.run(SHUTDOWN_BROADCAST);

            this.mHandler.sendEmptyMessageDelayed(0xD1, RUNNABLE_DELAY_MS);
            this.mHandler = null;
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

