package com.lrkj.business;

/**
 * Created by tianbao.zhao on 2017/12/13.
 */

public class LrNativeApi {
    static {
        System.loadLibrary("my-lib");
    }

    //public static native String getStringTmp();

    //public static native int[] getGrayImage(int[] pixels, int w, int h);

    public static native void setRobotIp(String ip);

    public static native int getAllMaps();

    public static native int sendEditMap(String name, String mapPath);

    public static native boolean writeBitmapToPgm(String path, int[] pixels, int w, int h);

}
