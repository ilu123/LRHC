package com.lrkj.views;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.lrkj.ctrl.R;
import com.lrkj.utils.LrSocketBridgeViewBase;
import com.lrkj.utils.LrSocketSurfaceView;

public class LrActMakeMap extends Activity implements LrSocketBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "LrActMakeMap::Activity";

    private LrSocketSurfaceView mCameraVideo;

    public LrActMakeMap() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        OpenCVLoader.initDebug();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_make_map);

        mCameraVideo = (LrSocketSurfaceView) findViewById(R.id.camera_video);
        //mCameraVideo.setupSocketIpAndPort(getIntent().getStringExtra("ip"), getIntent().getIntExtra("port", 0));
        //mCameraVideo.setupSocketIpAndPort("192.168.100.177", 8234);
        mCameraVideo.setupSocketIpAndPort("10.0.2.2", 8234);
        mCameraVideo.setVisibility(SurfaceView.VISIBLE);

        mCameraVideo.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mCameraVideo != null)
            mCameraVideo.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mCameraVideo != null)
            mCameraVideo.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mCameraVideo != null)
            mCameraVideo.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(LrSocketBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

}
