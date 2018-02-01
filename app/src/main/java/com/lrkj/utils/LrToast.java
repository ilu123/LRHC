package com.lrkj.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.tu.loadingdialog.LoadingDailog;
import com.lrkj.LrApplication;

import java.io.IOException;


/**
 * Created by stevejobs on 17/12/15.
 */

public final class LrToast {
    private static LoadingDailog sLD = null;

    public static void toast(final String msg, Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()){
            Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_LONG).show();
            return;
        }
        Looper lp = LrApplication.sApplication.getMainLooper();
        new Handler(lp).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toast(final int msg, Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()){
            Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_LONG).show();
            return;
        }
        Looper lp = LrApplication.sApplication.getMainLooper();
        new Handler(lp).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void toast(final int msg) {
        if (Looper.myLooper() == Looper.getMainLooper()){
            Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
            return;
        }
        Looper lp = LrApplication.sApplication.getMainLooper();
        new Handler(lp).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void toast(final String msg) {
        if (Looper.myLooper() == Looper.getMainLooper()){
            Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
            return;
        }
        Looper lp = LrApplication.sApplication.getMainLooper();
        new Handler(lp).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LrApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static LoadingDailog showLoading(Context c, String msg) {
        stopLoading();
        LoadingDailog.Builder loadBuilder = new LoadingDailog.Builder(c)
                .setMessage(msg)
                .setCancelable(true)
                .setCancelOutside(false);
        LoadingDailog dialog=loadBuilder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                sLD = null;
            }
        });
        dialog.show();
        sLD = dialog;
        return dialog;
    }

    public static void stopLoading(){
        if (sLD != null) {
            sLD.dismiss();
            sLD = null;
        }
    }

    public static boolean isLoading() {
        return sLD != null;
    }
}
