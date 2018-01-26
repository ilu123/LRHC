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

        LrNativeApi.loadLibrary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LrApplication.sApplication.sActivity = this;
        LrApplication.mkMapFolder();
    }

    @Override
    public void finish() {
        super.finish();
        LrApplication.sApplication.sActivity = null;
    }

    @Override
    protected void onDestroy() {
        LrApplication.sApplication.sActivity = null;
        super.onDestroy();
    }
}
