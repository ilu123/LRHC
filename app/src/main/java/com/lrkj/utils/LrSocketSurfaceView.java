package com.lrkj.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

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
    protected Socket mSocket;
    protected NativeCameraFrame mFrame;

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
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        if (mThread != null) {
            try {
                mStopThread = true;
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mThread =  null;
                mStopThread = false;
            }
        }

        /* Now release camera */
        releaseCamera();
    }

    public static class OpenCvSizeAccessor implements ListItemAccessor {

        public int getWidth(Object obj) {
            Size size  = (Size)obj;
            return (int)size.width;
        }

        public int getHeight(Object obj) {
            Size size  = (Size)obj;
            return (int)size.height;
        }

    }

    private boolean initializeCamera(int width, int height) {
        synchronized (this) {
            mFrame = new NativeCameraFrame();

            java.util.List<Size> sizes = new ArrayList<>();
            sizes.add(new Size(100, 100));

            /* Select the size that fits surface considering maximum size allowed */
            Size frameSize = calculateCameraFrameSize(sizes, new OpenCvSizeAccessor(), width, height);

            mFrameWidth = 100; //(int)frameSize.width;
            mFrameHeight = 100; //(int)frameSize.height;

            if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
                mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
            else
                mScale = 0;

            if (mFpsMeter != null) {
                mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
            }

            AllocateCache();
        }

        Log.i(TAG, "Selected camera frame size = (" + mFrameWidth + ", " + mFrameHeight + ")");

        return true;
    }

    private void releaseCamera() {
        synchronized (this) {
            if (mFrame != null) mFrame.release();
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

        public NativeCameraFrame() {
            mRgba = new Mat(100, 100, CvType.CV_8UC3);
        }

        public void release() {
            if (mRgba != null) mRgba.release();
        }

        private Mat mRgba;
    };

    private class CameraWorker implements Runnable {

        public void run() {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
            InputStream input = null;
            OutputStream output = null;
            int MAX_SIZE = (int) (mFrame.mRgba.total() * mFrame.mRgba.elemSize());
            byte[] buffer = new byte[MAX_SIZE];
            Arrays.fill(buffer, (byte) 0xFF);
            int readLen = 0;
            try{
                do {
                    if (mSocket == null) {
                        if (mIp != null) {
                            mSocket = new Socket();
                            mSocket.setReuseAddress(true);
                            mSocket.connect(new InetSocketAddress(mIp, mPort));
                            input = mSocket.getInputStream();
                            output = mSocket.getOutputStream();
                        }
                    }else{
                        readLen = input.read(buffer, 0, MAX_SIZE);
                        if (readLen > 0) {
                            output.write(mCmd);
                        }
                    }
                    if (mCmd == 1) {
                        Arrays.fill(buffer, (byte) 0xFF);
                    }

                    mFrame.mRgba.put(0, 0, buffer);
                    deliverAndDrawFrame(mFrame);
                } while (!mStopThread);
            }catch (Throwable e) {
                Log.e("", e+"");
            }finally {
                try {
                    if (mSocket != null) {
                        mSocket.shutdownInput();
                    }
                }catch (Throwable e) {}
                try {
                    if (mSocket != null) {
                        mSocket.shutdownOutput();
                    }
                }catch (Throwable e) {}
                output = null;
                input = null;
            }
        }
    }

}
