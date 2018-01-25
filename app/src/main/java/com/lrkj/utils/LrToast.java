package com.lrkj.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.widget.Toast;


/**
 * Created by stevejobs on 17/12/15.
 */

public final class LrToast {
    public static void toast(String msg, Context context) {
        Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void toast(int msg, Context context) {
        Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
