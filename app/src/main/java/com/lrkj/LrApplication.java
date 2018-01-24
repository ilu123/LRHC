package com.lrkj;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.dornbachs.zebra.TGApplication;
import com.lrkj.utils.LrToast;

import java.io.File;

/**
 * Created by stevejobs on 17/12/16.
 */

public class LrApplication extends TGApplication {

    public static LrApplication sApplication = null;

    public static String MAP_FOLDER = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;

        mkMapFolder();
    }

    public static boolean mkMapFolder() {
        File f = new File("/mnt/sdcard/"+LrApplication.sApplication.getPackageName()+"/maps");
        if (f.exists() || f.mkdirs()){
            MAP_FOLDER = f.getAbsolutePath();
        }
        else {
            MAP_FOLDER = null;
            LrToast.toast("请在设置中开启权限！", sApplication);
        }
        return MAP_FOLDER != null;
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
        return sp.getInt("speed", 1);
    }

    public static void saveSpeed(int s) {
        SharedPreferences sp = LrApplication.sApplication.getSharedPreferences("robot",0);
        sp.edit().putInt("speed", s).commit();
    }
}
