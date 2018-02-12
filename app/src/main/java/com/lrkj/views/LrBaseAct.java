package com.lrkj.views;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.lrkj.LrApplication;
import com.lrkj.business.LrNativeApi;

import org.opencv.android.OpenCVLoader;

/**
 * Created by wangchenglong1 on 17/12/18.
 */

public class LrBaseAct extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LrApplication.mkMapFolder();
        LrNativeApi.loadLibrary();
        LrApplication.sApplication.sActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        LrApplication.mkMapFolder();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
