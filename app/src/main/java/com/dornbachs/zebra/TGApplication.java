/**
 * TODO
 * By ztb, 2016-11-21
 */
package com.dornbachs.zebra;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

import com.dornbachs.zebra.utils.StorageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO
 *
 * @author ztb, 2016-11-21
 */
public class TGApplication extends Application {
    private static TGApplication sInstance = null;

    public static TGApplication getInstance() {

        return sInstance;
    }

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        TGApplication.SCREEN_WIDTH = displayMetrics.widthPixels;
        TGApplication.SCREEN_HEIGHT = displayMetrics.heightPixels;
        Log.e("App", "ScreenSIze==" + SCREEN_WIDTH + ", " + SCREEN_HEIGHT);
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int px = (int)(dp*displayMetrics.density + 0.5);
        return px;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dp = (int)(px/displayMetrics.density + 0.5);
        return dp;
    }

    public static void saveSp(String key, String value) {
        SharedPreferences sp = sInstance.getSharedPreferences("ssppp", 0);
        sp.edit().putString(key, value).apply();
    }

    public static String getSp(String key) {
        SharedPreferences sp = sInstance.getSharedPreferences("ssppp", 0);
        return sp.getString(key, "");
    }

    public static Bitmap getAssetBitmap(String p) {
        try {
            Bitmap bm = BitmapFactory.decodeStream(sInstance.getResources().getAssets().open(p));
            return bm;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int SCREEN_WIDTH = 0;
    public static int SCREEN_HEIGHT = 0;
}
