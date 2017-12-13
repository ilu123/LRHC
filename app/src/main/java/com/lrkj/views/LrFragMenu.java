package com.lrkj.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lrkj.ctrl.R;
import com.lrkj.widget.CircleMenuLayout;
import com.lrkj.widget.CircularRevealView;

/**
 * Created by ztb.
 */
public class LrFragMenu extends Fragment {

    private CircleMenuLayout mCircleMenuLayout;

    private String[] mItemTexts = new String[]{"创建地图", "地图导航", "管理地图",
            "进程管理"};
    private int[] mItemImgs = new int[]{R.drawable.home_mbank_1_normal,
            R.drawable.home_mbank_2_normal, R.drawable.home_mbank_3_normal,
            R.drawable.home_mbank_4_normal};

    private CircularRevealView revealView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        revealView = (CircularRevealView) view.findViewById(R.id.reveal);

        mCircleMenuLayout = (CircleMenuLayout) view.findViewById(R.id.id_menulayout);
        mCircleMenuLayout.setMenuItemIconsAndTexts(mItemImgs, mItemTexts);
        mCircleMenuLayout.setOnMenuItemClickListener(new CircleMenuLayout.OnMenuItemClickListener() {

            @Override
            public void itemClick(View view, int pos) {
                if (pos == 0) {
                    Intent i = new Intent(getActivity(), LrActMakeMap.class);
                    i.putExtra("ip", "192.168.100.177");
                    i.putExtra("port", 8234);
                    getActivity().startActivity(i);
                }
            }

            @Override
            public void itemCenterClick(View view) {

            }
        });


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

    @Override
    public void onStart() {
        super.onStart();

        Window window = getActivity().getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.0f;

        window.setAttributes(windowParams);
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
    }


}

