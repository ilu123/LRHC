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

package com.lrkj.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.dornbachs.zebra.utils.DrawUtils;
import com.dornbachs.zebra.utils.FloodFill;
import com.dornbachs.zebra.utils.Progress;
import com.lrkj.business.LrNativeApi;
import com.lrkj.utils.PgmImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class PaintMapView extends View {
    public interface LifecycleListener {
        // After this method it is allowed to load resources.
        public void onPreparedToLoad();
    }

    public PaintMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _state = new State();
        _paint = new Paint();
        _paint.setStrokeWidth(14);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setColor(Color.BLACK);
        _paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public PaintMapView(Context context) {
        this(context, null);
    }

    public synchronized void setLifecycleListener(LifecycleListener l) {
        _lifecycleListener = l;
    }

    public synchronized Object getState() {
        return _state;
    }

    public synchronized void setState(Object o) {
        _state = (State) o;
    }

    public void setIsPan(boolean pan) {
        _isPan = pan;
    }

    public void loadFromBitmap(Bitmap originalOutlineBitmap, int ow, int oh, Handler progressHandler) {
        Log.e("size", "size = {" + ow + ", " + oh + "}");

        // Proportion of progress in various places.
        // The sum of all progress should be 100.
        final int PROGRESS_RESIZE = 10;
        final int PROGRESS_SCAN = 90;

        int w = 0;
        int h = 0;
        _state._width = ow;
        _state._height = oh;
        State newState = new State();
        synchronized (this) {
            w = _state._width;
            h = _state._height;
            newState._color = _state._color;
            newState._width = w;
            newState._height = h;
        }
        final int n = w * h;

        // Resize so that it matches our paint size.
        Bitmap resizedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        DrawUtils.convertSizeClip(originalOutlineBitmap, resizedBitmap);
        Progress.sendIncrementProgress(progressHandler, PROGRESS_RESIZE);

        newState._pixels = new int[n];
        resizedBitmap.getPixels(newState._pixels, 0, w, 0, 0, w, h);

        // Initialize the rest.
        newState._paintedBitmap = resizedBitmap;
        newState._canvas = new Canvas(newState._paintedBitmap);

        // Commit our changes. So far we have only worked on local variables
        // so we only synchronize now.
        synchronized (this) {
            _state = newState;
        }
        progressHandler.sendEmptyMessage(Progress.MESSAGE_DONE_OK);
    }

    public synchronized void saveToFile(File file, Bitmap originalOutlineBitmap, Handler progressHandler) {
        // Proportion of progress in various places.
        // The sum of all progress should be 100.
        final int PROGRESS_DRAW_PAINTED = 45;
        final int PROGRESS_SAVE = 55;

        // First, get a copy of the painted bitmap. After that we do not have
        // to deal with class instance any more.
        Bitmap painted;
        synchronized (this) {
            painted = _state._paintedBitmap.copy(_state._paintedBitmap.getConfig(), true);
        }

        // Calculate the proportions of the result and create it. The result
        // has more pixels than the bitmap we paint, it has the maximum
        // number of pixels possible with the original outline (while
        // maintaining the same aspect ratio as the drawing).
        final float aspectRatio = (float) painted.getWidth() / painted.getHeight();
        int hr = originalOutlineBitmap.getHeight();
        int wr = (int) (hr * aspectRatio);
        if (wr > originalOutlineBitmap.getWidth()) {
            wr = originalOutlineBitmap.getWidth();
            hr = (int) (wr / aspectRatio);
        }
        int nr = wr * hr;
        Bitmap result = Bitmap.createBitmap(wr, hr, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Draw and scale the painted bitmap onto the result.
        canvas.drawBitmap(painted, new Rect(0, 0, painted.getWidth(), painted.getHeight()), new Rect(0, 0, wr, hr), paint);
        Progress.sendIncrementProgress(progressHandler, PROGRESS_DRAW_PAINTED);

        try {
            // Write the result to the dest file.
            file.getParentFile().mkdirs();
            int[] pixels = new int[nr];
            result.getPixels(pixels, 0, wr, 0, 0, wr, hr);
            if (LrNativeApi.writeBitmapToPgm(file.getAbsolutePath(), pixels, wr, hr)) {
                Progress.sendIncrementProgress(progressHandler, PROGRESS_SAVE);
            }else{
                progressHandler.sendEmptyMessage(Progress.MESSAGE_DONE_ERROR);
                return;
            }
        } catch (Exception e) {
            progressHandler.sendEmptyMessage(Progress.MESSAGE_DONE_ERROR);
            return;
        }

        progressHandler.sendEmptyMessage(Progress.MESSAGE_DONE_OK);
    }

    public synchronized boolean isInitialized() {
        return _state._paintedBitmap != null;
    }

    public synchronized void setPaintColor(int color) {
        _state._color = color;
        _paint.setColor(color);
    }
    public int getPaintColor() {
        return _state._color;
    }
    public int getPaintSize() {
        return (int)_paint.getStrokeWidth();
    }
    public synchronized void setPaintSize(float size) {
        _paint.setStrokeWidth(size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        synchronized (this) {
            if (_state._width == 0 || _state._height == 0) {
                if (_lifecycleListener != null) {
                    _lifecycleListener.onPreparedToLoad();
                }
            }
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (_state._paintedBitmap != null)
            canvas.drawBitmap(_state._paintedBitmap, 0, 0, _paint);
    }

    private static final int THRESHHOLD_MOVE = 15;
    private int mLastX, mLastY;
    public boolean onTouchEvent(MotionEvent e) {
        final int X = (int) e.getRawX();
        final int Y = (int) e.getRawY();
        int action = e.getAction() & MotionEvent.ACTION_MASK;
        if (_isPan) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.getLayoutParams();
            if (action == MotionEvent.ACTION_MOVE) {
                lp.leftMargin = X - _delta.x;
                lp.topMargin = Y - _delta.y;
                lp.width = _state._width;
                lp.height = _state._height;
                this.setLayoutParams(lp);
            } else {
                _delta.x = X - lp.leftMargin;
                _delta.y = Y - lp.topMargin;
            }
        } else {
            if (action == MotionEvent.ACTION_DOWN){
                mLastX = (int)e.getX();
                mLastY = (int)e.getY();
            }else if (action == MotionEvent.ACTION_MOVE) {
                paint((int) e.getX(), (int) e.getY());

            }
        }
        return true;
    }

    public synchronized void paint(int x, int y) {
        if (_state._canvas != null && _state._paintedBitmap != null) {
            //_state._canvas.drawPoint(x, y, _paint);
            _state._canvas.drawLine(mLastX, mLastY, x, y, _paint);
            mLastX = x;
            mLastY = y;
            invalidate();
        }
    }

    private static final int ALPHA_TRESHOLD = 100;

    // The listener whom we notify when ready to load images.
    private LifecycleListener _lifecycleListener;

    // We keep the state of the current drawing in a different class so that
    // we can quickly save and restore it when an orientation change happens.
    // Members of this class are not allowed to contain any references to the
    // view hierarchy.
    private static class State {
        // Bitmap containing everything we have painted so far.
        private Bitmap _paintedBitmap;
        private Canvas _canvas;

        // Dimensions of both bitmaps.
        private int _height;
        private int _width;

        // Paint with the currently selected color.
        private int _color;

        // All the pixels in _paintedBitmap. Because accessing an int array is
        // much faster than accessing pixels in a bitmap, we operate on this
        // and use setPixels() on the bitmap to copy them back.
        private int _pixels[];
    }

    private State _state;
    private Paint _paint;
    private boolean _isPan = false;
    private Point _delta = new Point(0, 0);

}