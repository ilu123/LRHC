package com.lrkj.business;

/**
 * Created by tianbao.zhao on 2017/12/13.
 */

public class LrNativeApi {
    static {
        System.loadLibrary("my-lib");
    }

    public static native String getStringTmp();

    public static native int[] getGrayImage(int[] pixels, int w, int h);
}
