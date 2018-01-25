package com.lrkj;

import android.app.Application;

import com.dornbachs.zebra.TGApplication;

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

        File f = new File("/mnt/sdcard/"+getPackageName()+"/maps");
        f.mkdirs();
        MAP_FOLDER = f.getAbsolutePath();
    }
}
