package com.lrkj.views;

import android.app.Activity;

import com.lrkj.LrApplication;

/**
 * Created by wangchenglong1 on 17/12/18.
 */

public class LrBaseAct extends Activity {

    @Override
    protected void onResume() {
        super.onResume();

        LrApplication.mkMapFolder();
    }
}
