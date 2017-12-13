package hwj.opencvjni;

/**
 * Created by Administrator on 2016/12/13.
 */

public class OpenCVHelper {
    static {
        System.loadLibrary("my-lib");
    }

    public static native String getStringTmp();

    public static native int[] getGrayImage(int[] pixels, int w, int h);
}
