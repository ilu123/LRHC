/*
 * Copyright (C) 2016 TomkidGame
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lrkj.views;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.dornbachs.zebra.TGApplication;
import com.dornbachs.zebra.modal.ImageItem;
import com.dornbachs.zebra.utils.Progress;
import com.lrkj.ctrl.R;
import com.lrkj.utils.PGM;
import com.lrkj.widget.PaintMapView;
import com.lrkj.widget.SeekDialog;

import java.io.File;


public class LrActEditMap extends LrBaseAct implements PaintMapView.LifecycleListener {
    boolean _isLocked = true;
    int _scaleSize = 1;
    int _lockedOreitation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    OrientationEventListener mOrientationEventListener = null;
    String mImagePath = null;
    int mColor = Color.BLACK;

    public LrActEditMap() {
        _state = new State();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map);

        mImagePath = getIntent().getStringExtra("map");

        _paintView = (PaintMapView) findViewById(R.id.paint_view);
        _paintView.setLifecycleListener(this);
        _progressBar = (ProgressBar) findViewById(R.id.paint_progress);
        _progressBar.setMax(Progress.MAX);

        final Object previousState = getLastNonConfigurationInstance();
        if (previousState == null) {
            // No previous state, this is truly a new activity.
            // We need to make the paint view INVISIBLE (and not GONE) so that
            // it can measure itself correctly.
            _paintView.setVisibility(View.INVISIBLE);
            _progressBar.setVisibility(View.GONE);
        } else {
            // We have a previous state, so this is a re-created activity.
            // Restore the state of the activity.
            SavedState state = (SavedState) previousState;
            _state = state._paintActivityState;
            _paintView.setState(state._paintViewState);
            _paintView.setVisibility(View.VISIBLE);
            _progressBar.setVisibility(View.GONE);
            if (_state._loadInProgress) {
                new InitPaintView(_state._imageItem);
            }
        }

        this.startOrientationListener();


        if (mImagePath != null) {
            ImageItem item = new ImageItem();
            item.isDrawable = false;
            item.outlinePath = mImagePath;
            new InitPaintView(item);
        }
    }

    /**
     * Called before the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (_originalOutlineBitmap != null)
            _originalOutlineBitmap.recycle();
        _originalOutlineBitmap = null;
    }

    /**
     * 开启监听器
     */
    private final void startOrientationListener() {
        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {
                if (rotation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;  //手机平放时，检测不到有效的角度
                }
                //只检测是否有四个角度的改变
                if (rotation > 350 || rotation < 10) { //0度
                    if (!_isLocked && _lockedOreitation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        _lockedOreitation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                        return;
                    }
                } else if (rotation > 80 && rotation < 100) { //90度
                    if (!_isLocked && _lockedOreitation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        _lockedOreitation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                        return;
                    }
                } else if (rotation > 170 && rotation < 190) { //180度
                    if (!_isLocked && _lockedOreitation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        _lockedOreitation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                        return;
                    }
                } else if (rotation > 260 && rotation < 280) { //270度
                    if (!_isLocked && _lockedOreitation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        _lockedOreitation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        return;
                    }
                } else {
                    return;
                }
            }
        };
        mOrientationEventListener.enable();
    }

    @Override
    public void finish() {
        if (mOrientationEventListener != null)
            mOrientationEventListener.disable();

        super.finish();
    }

    public void onPreparedToLoad() {
        // We need to invoke InitPaintView in a callback otherwise
        // the visibility changes do not seem to be effective.
        new Handler() {
            @Override
            public void handleMessage(Message m) {
                if (_state != null && _state._imageItem != null) {
                    new InitPaintView(_state._imageItem);
                }
            }
        }.sendEmptyMessage(0);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        SavedState state = new SavedState();
        state._paintActivityState = _state;
        state._paintViewState = _paintView.getState();
        return state;
    }

    /*
     * @note by ztb:
     * this method only be called after you set "oreitation" config
     * in the manifest.xml. And if you enable that, the "layout-xxx" maybe
     * not be changed automately when the screen oreitation changed.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (_isLocked) {
        }
    }

    // @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS:
                _progressDialog = new ProgressDialog(LrActEditMap.this);
                _progressDialog.setCancelable(false);
                _progressDialog.setIcon(android.R.drawable.ic_dialog_info);
                _progressDialog.setTitle("保存中...");
                _progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                _progressDialog.setMax(Progress.MAX);
                if (!_saveInProgress) {
                    // This means that the view hierarchy was recreated but there
                    // is no actual save in progress (in this hierarchy), so let's
                    // dismiss the dialog.
                    new Handler() {
                        @Override
                        public void handleMessage(Message m) {
                            _progressDialog.dismiss();
                        }
                    }.sendEmptyMessage(0);
                }

                return _progressDialog;
        }
        return null;
    }

    public void onClickPan(View v) {
        v.setSelected(!v.isSelected());
        if (v.isSelected()) {
            ((ImageButton)v).setImageResource(R.drawable.ic_move);
        }else{
            ((ImageButton)v).setImageResource(R.drawable.ic_move2);
        }
        _paintView.setIsPan(v.isSelected());
    }

    public void onClickLock(View v) {
        //v.setSelected(!v.isSelected());
        //_isLocked = !v.isSelected();
    }

    public void onClickScale(View c) {
        _scaleSize += 1;
        if (_scaleSize > 4) {
            _scaleSize = 1;
        }
        _paintView.setScaleY(_scaleSize);
        _paintView.setScaleX(_scaleSize);
    }
    public void onClickColor(View v) {
        ImageView vv = (ImageView) v;
        if (mColor == Color.BLACK) {
            mColor = Color.WHITE;
            vv.setImageResource(R.drawable.ic_ink_w);
        }else if (mColor == Color.WHITE) {
            mColor = Color.BLACK;
            vv.setImageResource(R.drawable.ic_ink_b);
        }

        _paintView.setPaintColor(mColor);
    }

    public void onClickSize(View v) {
        new SeekDialog(this).setTitle("画笔大小").setProgress(_paintView.getPaintSize()).setMaxMin(0, 100)
                .setListener(new SeekDialog.OnSeekbarChangedListener() {
                    @Override
                    public void onChange(int progress) {
                        _paintView.setPaintSize(progress);
                    }
                }).show();
    }

    public void onClickSave(View v) {
        new BitmapSaver(mImagePath);
    }
    public void onClickExit(View v) {
        this.finish();
    }


    private Bitmap _originalOutlineBitmap;

    private class InitPaintView implements Runnable {
        public InitPaintView(ImageItem item) {
            // Make the progress bar visible and hide the view
            _paintView.setVisibility(View.GONE);
            _progressBar.setProgress(0);
            _progressBar.setVisibility(View.VISIBLE);
            _state._savedImageUri = null;
            _state._loadInProgress = true;
            _state._imageItem = item;

            _handler = new Handler() {
                @Override
                public void handleMessage(Message m) {
                    switch (m.what) {
                        case Progress.MESSAGE_INCREMENT_PROGRESS:
                            // Update progress bar.
                            _progressBar.incrementProgressBy(m.arg1);
                            break;
                        case Progress.MESSAGE_DONE_OK:
                            int w = _originalOutlineBitmap.getWidth();
                            int h = _originalOutlineBitmap.getHeight();
                            LayoutParams lp = _paintView.getLayoutParams();
                            lp.width = w;
                            lp.height = h;
                            _paintView.setLayoutParams(lp);
                        case Progress.MESSAGE_DONE_ERROR:
                            // We are done, hide the progress bar and turn
                            // the paint view back on.
                            _state._loadInProgress = false;
                            _paintView.setVisibility(View.VISIBLE);
                            _progressBar.setVisibility(View.GONE);
                            break;
                        case Progress.MESSAGE_LAYOUT_SIZE:
                            break;
                    }
                }
            };

            new Thread(this).start();
        }

        public void run() {
            String path = _state._imageItem.outlinePath;
            int iw, ih;
            int[] pix;
            PGM pgm = new PGM();
            pgm.readPGMHeader(path);
            iw = pgm.getWidth();
            ih = pgm.getHeight();
            pix = pgm.readData(iw, ih, 5);   //P5-Gray image
            if (iw <= 0 || ih <= 0) {
                _handler.sendEmptyMessage(Progress.MESSAGE_DONE_ERROR);
                return;
            }
            _originalOutlineBitmap = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_4444);
            _originalOutlineBitmap.setPixels(pix, 0, iw, 0, 0, iw, ih);
            _paintView.loadFromBitmap(_originalOutlineBitmap, iw, ih, _handler);
        }

        private Handler _handler;
    }

    private class BitmapSaver implements Runnable {
        public BitmapSaver(String filep) {
            this._fileName = filep;

            class DelayHandler extends Handler {
                @Override
                public void handleMessage(Message m) {
                    // We are done, hide the progress bar and turn
                    // the paint view back on.
                    _saveInProgress = false;
                    _progressDialog.dismiss();
                }
            }

            class ProgressHandler extends Handler {
                @Override
                public void handleMessage(Message m) {
                    switch (m.what) {
                        case Progress.MESSAGE_INCREMENT_PROGRESS:
                            // Update progress bar.
                            _progressDialog.incrementProgressBy(m.arg1);
                            break;
                        case Progress.MESSAGE_DONE_OK:
                        case Progress.MESSAGE_DONE_ERROR:
                            String title = "保存中...";
                            if (m.what == Progress.MESSAGE_DONE_OK)
                                title += "保存成功！";
                            else
                                title += "保存失败！";
                            _progressDialog.setTitle(title);
                            new DelayHandler().sendEmptyMessageDelayed(0, SAVE_DIALOG_WAIT_MILLIS);
                            break;
                    }
                }
            }

            if (_paintView.isInitialized()) {
                _saveInProgress = true;
                showDialog(DIALOG_PROGRESS);
                _progressDialog.setTitle("保存中");
                _progressDialog.setProgress(0);

                _progressHandler = new ProgressHandler();
                new Thread(this).start();
            }
        }

        public void run() {
            _file = new File(_fileName);

            // Save the bitmap to a file.
            _paintView.saveToFile(_file, _originalOutlineBitmap, _progressHandler);
        }

        private String _fileName;
        private File _file;
        private Handler _progressHandler;
    }

    // The state of the whole drawing. This is used to transfer the state if
    // the activity is re-created (e.g. due to orientation change).
    private static class SavedState {
        public State _paintActivityState;
        public Object _paintViewState;
    }

    private static class State {
        // Are we just loading a new outline?
        public boolean _loadInProgress;

        public ImageItem _imageItem;

        // If we have already saved a copy of the image, we store the URI here
        // so that we can delete the previous version when saved again.
        public Uri _savedImageUri;
    }

    private static final int REQUEST_START_NEW = 0;
    private static final int DIALOG_PROGRESS = 1;
    private static final int SAVE_DIALOG_WAIT_MILLIS = 1500;

    // The state that we will carry over if the activity is recreated.
    private State _state;

    // Main UI elements.
    private PaintMapView _paintView;
    private ProgressBar _progressBar;
    private ProgressDialog _progressDialog;

    // Is there a save in progress?
    private boolean _saveInProgress;
}