package com.lrkj;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.devspark.appmsg.AppMsg;
import com.dornbachs.zebra.TGApplication;
import com.lrkj.utils.CrashHandler;
import com.lrkj.utils.LrToast;

import java.io.File;

/**
 * Created by stevejobs on 17/12/16.
 */

public class LrApplication extends TGApplication {

    public static LrApplication sApplication = null;
    public Activity sActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        CrashHandler.getInstance().init(this, true);
        mkMapFolder();
    }

    public static void startMsgService() {
        Intent i = new Intent(LrApplication.sApplication, LrMsgService.class);
        LrApplication.sApplication.startService(i);
    }

    public static void stopMsgService() {
        Intent i = new Intent(LrApplication.sApplication, LrMsgService.class);
        LrApplication.sApplication.stopService(i);
    }

    public static void mkMapFolder() {
        File f = new File("/mnt/sdcard/"+LrApplication.sApplication.getPackageName()+"/maps");
        if (f.exists() || f.mkdirs()){
        }
        else {
            LrToast.toast("请在设置中开启文件访问权限！");
        }
        f = new File("/mnt/sdcard/"+LrApplication.sApplication.getPackageName()+"/navi");
        if (f.exists() || f.mkdirs()){
        }
        else {
            LrToast.toast("请在设置中开启文件访问权限！");
        }
    }

    public static void saveIP(String ip) {
        SharedPreferences sp = LrApplication.sApplication.getSharedPreferences("ip",0);
        sp.edit().putString("ip", ip).commit();
    }

    public static String getIP() {
        SharedPreferences sp = LrApplication.sApplication.getSharedPreferences("ip",0);
        return sp.getString("ip", null);
    }

    public static int getSpeed() {
        SharedPreferences sp = LrApplication.sApplication.getSharedPreferences("robot",0);
        return sp.getInt("speed", 3);
    }

    public static void saveSpeed(int s) {
        SharedPreferences sp = LrApplication.sApplication.getSharedPreferences("robot",0);
        sp.edit().putInt("speed", s).commit();
    }
}
