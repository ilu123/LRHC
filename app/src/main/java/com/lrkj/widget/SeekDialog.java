package com.lrkj.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.lrkj.ctrl.R;

public class SeekDialog extends AlertDialog {

    private SeekBar brightBar;//用于显示屏幕亮度
    TextView tvValue;
    private OnSeekbarChangedListener mListener;//监听SeekBar事件，比如拖动等

    /*自定义构造函数用于初始化*/
    public SeekDialog(Context context) {
        super(context);
        View view = getLayoutInflater().inflate(R.layout.dialog_seekbar,null);
        brightBar = (SeekBar) view.findViewById(R.id.seekbar);
        tvValue = (TextView) view.findViewById(R.id.tv_val);
        setView(view);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        brightBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {


            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
// TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
// TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mListener != null)
                mListener.onChange(progress);
                tvValue.setText(progress+"");
            }
        });
    }

    public SeekDialog setTitle(String title) {
        super.setTitle(title);
        return this;
    }

    public SeekDialog setProgress(int p) {
        brightBar.setProgress(p);
        return this;
    }
    public SeekDialog setMaxMin(int min, int max) {
        brightBar.setMax(max);
        return this;
    }

    /*获取监听对象*/
    public OnSeekbarChangedListener getListener() {
        return mListener;
    }

    /*设置监听对象*/
    public SeekDialog setListener(OnSeekbarChangedListener mListener) {
        this.mListener = mListener;
        return this;
    }

    public interface OnSeekbarChangedListener{
        public void onChange(int progress);
    }
}