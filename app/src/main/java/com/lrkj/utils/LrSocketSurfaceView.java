package com.lrkj.utils;

import android.content.Context;
import android.os.Handler;
import android.os.StrictMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.lrkj.LrApplication;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import static android.R.attr.x;
import static android.R.attr.y;


/**
 * Created by tianbao.zhao on 2017/12/13.
 */
public class LrSocketSurfaceView extends LrSocketBridgeViewBase {

    public static final String TAG = "LrNativeSurfaceView";
    private boolean mStopThread;
    private Thread mThread;

    private volatile String mIp;
    private volatile int mPort;
    private volatile int mCmd = -1;
    String mMapName;
    protected Socket mSocket;
    protected NativeCameraFrame mFrame;

    private ImageView mArrow;
    private int mX, mY; //robot target position
    private float mAngle; //robot direction

    public LrSocketSurfaceView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public LrSocketSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setupSocketIpAndPort(String ip, int port) {
        mIp = ip;
        mPort = port;
    }

    public void setupSocketIpAndPort(String ip, int port, String mapName) {
        mIp = ip;
        mPort = port;
        mMapName = mapName;
    }

    public void sendCmd(int cmd) {
        mCmd = cmd;
    }

    @Override
    protected boolean connectCamera(int width, int height) {

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        if (!initializeCamera(width, height))
            return false;

        /* now we can start update thread */
        mThread = new Thread(new CameraWorker());
        mThread.start();

        return true;
    }

    @Override
    protected void disconnectCamera() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.mSocket = null;

        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        if (mThread != null) {
            try {
                mStopThread = true;
                if (mCameraIndex == LrDefines.PORT_DOT) {
                    stopDotSocket();
                } else if (mCameraIndex == LrDefines.PORT_READ_LASER) {
                    stopLaserSocket();
                } else if (mCameraIndex == LrDefines.PORT_NAVIGATION) {
                    stopNaviSocket();
                }
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mThread = null;
                mStopThread = false;
            }
        }

        /* Now release camera */
        releaseCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCameraIndex == LrDefines.PORT_NAVIGATION) {
            if (mArrow == null) {
                mArrow = (ImageView) ((ViewGroup) this.getParent()).findViewById(R.id.arrow);
            }
            if (originImg != null && originImg.rows() > 0) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mX = (int) event.getX();
                    mY = (int) event.getY();
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mArrow.getLayoutParams();
                    lp.leftMargin = mX;
                    lp.topMargin = mY;
                    mArrow.setPivotX(0);
                    mArrow.setPivotY(0);
                    mArrow.setLayoutParams(lp);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    mArrow.setVisibility(View.VISIBLE);
                    mAngle = (float) (-Math.atan2((mY - event.getY()), mX - event.getX()));
                    float degrees = (float) (-mAngle * 180 / Math.PI) + 180.f;
                    mArrow.setRotation(degrees);
                    mAngle = (float) (-Math.atan2((event.getY() - mY), event.getX() - mX));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mArrow.setVisibility(View.INVISIBLE);
                    clickNaviTo((int) (mX / mScale), (int) (mY / mScale), mAngle);
                } else{
                    mArrow.setVisibility(View.INVISIBLE);
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public static class OpenCvSizeAccessor implements ListItemAccessor {

        public int getWidth(Object obj) {
            Size size = (Size) obj;
            return (int) size.width;
        }

        public int getHeight(Object obj) {
            Size size = (Size) obj;
            return (int) size.height;
        }

    }

    private boolean initializeCamera(int width, int height) {
        synchronized (this) {
            java.util.List<Size> sizes = new ArrayList<>();
            sizes.add(new Size(width, height));

            /* Select the size that fits surface considering maximum size allowed */
            Size frameSize = calculateCameraFrameSize(sizes, new OpenCvSizeAccessor(), width, height);

            mFrameWidth = (int) frameSize.width;
            mFrameHeight = (int) frameSize.height;

            if (mFpsMeter != null) {
                mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
            }

            int type = CvType.CV_8UC3;
            if (mCameraIndex == LrDefines.PORT_DOT) {
                type = CvType.CV_8UC3;
                AllocateCache(mFrameWidth, mFrameHeight, type);
            } else if (mCameraIndex == LrDefines.PORT_READ_LASER) {
                type = CvType.CV_8UC1;
                AllocateCache(mFrameWidth, mFrameHeight, type);
            } else if (mCameraIndex == LrDefines.PORT_NAVIGATION) {

            }

        }

        Log.i(TAG, "Selected camera frame size = (" + mFrameWidth + ", " + mFrameHeight + ")");

        return true;
    }

    protected void AllocateCache(int w, int h, int type) {
        super.AllocateCache(w, h);
        mFrame = new NativeCameraFrame(w, h, type);
    }

    private void releaseCamera() {
        synchronized (this) {
            if (mFrame != null) mFrame.release();
            if (originImg != null) originImg.release();
            mFrame = null;
            originImg = null;
        }
    }

    private static class NativeCameraFrame implements CvCameraViewFrame {

        @Override
        public Mat rgba() {
            return mRgba;
        }

        @Override
        public Mat gray() {
            return null;
        }

        public NativeCameraFrame(int w, int h, int type) {
            mRgba = new Mat(h, w, type);
        }

        public void release() {
            if (mRgba != null) mRgba.release();
        }

        @Override
        public int state() {
            return mState;
        }

        public int mState;
        private Mat mRgba;
    }

    private Mat originImg = null;

    public class CameraWorker implements Runnable {
        public void run() {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
            InputStream input = null;
            OutputStream output = null;
            while (mIp == null && !mStopThread) ;

            if (mCameraIndex == LrDefines.PORT_DOT) {
                originImg = new Mat(600, 600, CvType.CV_8UC3);
                while (!mStopThread) {
                    try {
                        getDotFrame(LrSocketSurfaceView.this, originImg.getNativeObjAddr());
                        //LrSocketSurfaceView.this.postDotFrameFromNative();
                    } catch (Throwable e) {

                    }
                }
            } else if (mCameraIndex == LrDefines.PORT_READ_LASER) {
                originImg = mFrame.mRgba;
                while (!mStopThread) {
                    try {
                        getLaserFrame(LrSocketSurfaceView.this, originImg.getNativeObjAddr(), mMapName + "");
                    } catch (Throwable e) {

                    }
                }
            } else if (mCameraIndex == LrDefines.PORT_NAVIGATION) {
                Mat originImg2 = Highgui.imread("/mnt/sdcard/com.lrkj.ctrl/navi/" + mMapName + ".pgm");
                AllocateCache(originImg2.cols(), originImg2.rows(), CvType.CV_8UC1);
                originImg = originImg2.clone();
                //while (!mStopThread) {
                try {
                    getNaviFrame(LrSocketSurfaceView.this, mIp, mMapName, originImg2.getNativeObjAddr(), originImg.getNativeObjAddr(),
                            originImg.cols(), originImg.rows());
                } catch (Throwable e) {
                    Log.e(",", e + "");
                }
                Log.e(",", "ewewewewewewewe");
                //}
            } else {
                if (true) return;
            }
        }
    }

    public void resetCanvas(int color) {
        if (originImg != null) {
            originImg.setTo(new Scalar(color));
            if (mCameraIndex == LrDefines.PORT_READ_LASER) {
                resetLaserFrame();
            }
        }
    }

    ////// All Native calls ////////

    public void postDotFrameFromNative() {
        //Mat tmp = Highgui.imdecode(originImg, Highgui.CV_LOAD_IMAGE_COLOR);
        //Log.d(TAG, "run SHAPe: ----------[" + tmp.channels() + "]");
        //Imgproc.cvtColor(tmp, originImg, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(originImg, mFrame.mRgba, mFrame.mRgba.size());
        deliverAndDrawFrame(mFrame);
        //tmp.release();
    }

    public void postLaserFrameFromNative() {
        //Mat tmp = Highgui.imdecode(originImg, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        //Log.d(TAG, "run SHAPe: ----------[" + tmp.channels() + "]");
        //Imgproc.cvtColor(tmp, originImg, Imgproc.COLOR_Gr);
        Imgproc.resize(originImg, mFrame.mRgba, mFrame.mRgba.size());
        deliverAndDrawFrame(mFrame);
        //tmp.release();
    }

    private boolean mNaviMapShownTip = false;
    public void postNaviFrameFromNative() {
        if (mFrame != null && mFrame.mRgba.rows() > 0) {
            Imgproc.resize(originImg, mFrame.mRgba, mFrame.mRgba.size());
            deliverAndDrawFrame(mFrame);
        }
        //tmp.release();
        if (!mNaviMapShownTip) {
            mNaviMapShownTip = true;
            new Handler(LrApplication.sApplication.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    LrToast.stopLoading();
                    LrToast.toast("载入地图成功，可以开始导航");
                }
            });
        }
    }

    public void saveLaserFrameFromNativeDone() {
        if (getContext() == null)
            return;
        new Handler(getContext().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                LrToast.toast("保存完成！");
            }
        });
    }

    public void setSlamState(final int s) {
        if (getContext() == null)
            return;
        new Handler(getContext().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mFrame != null)
                    mFrame.mState = s;
            }
        });
    }

    public static native boolean getDotFrame(LrSocketSurfaceView obj, long ioMat);

    public static native void stopDotSocket();

    public static native boolean getLaserFrame(LrSocketSurfaceView obj, long ioMat, String mappgm);

    public static native void saveLaserFrame();
    public static native void resetLaserFrame();

    public static native void stopLaserSocket();

    public static native boolean getNaviFrame(LrSocketSurfaceView obj, String ip, String map, long matMap, long ioMat, int mw, int mh);

    public static native void stopNaviSocket();

    public static native void clickNaviTo(int x, int y, float a);
}
