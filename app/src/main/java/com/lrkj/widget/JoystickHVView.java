package com.lrkj.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.lrkj.ctrl.R;

import static android.R.attr.textColor;
import static android.R.attr.textSize;

/**
 * Created by wangchenglong1 on 17/12/28.
 */

public class JoystickHVView extends View implements Runnable {
    // Constants
    private final double RAD = 57.2957795;
    final int STROKE_WIDTH = 5;

    public final static long DEFAULT_LOOP_INTERVAL = 100; // 100 ms
    public final static int FRONT = 0;
    public final static int BOTTOM = 7;
    public final static int RIGHT = 1;
    public final static int LEFT = 5;

    // Variables
    private OnJoystickMoveListener onJoystickMoveListener; // Listener
    private Thread thread = new Thread(this);
    private long loopInterval = DEFAULT_LOOP_INTERVAL;
    private int xPosition = 0; // Touch x position
    private int yPosition = 0; // Touch y position
    private double centerX = 0; // Center view x position
    private double centerY = 0; // Center view y position
    private Paint paintBg, paintBgStroke;
    private Paint button;
    private int joystickRadius;
    private int buttonRadius;
    private int lastAngle = 0;
    private int lastPower = 0;
    private boolean isHor = false;
    private RectF rectView, rectViewStroke;

    public JoystickHVView(Context context) {
        this(context, null);
    }

    public JoystickHVView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JoystickHVView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.JoystickHVView);
        isHor = a.getBoolean(R.styleable.JoystickHVView_isHor, false);
        a.recycle();

        initJoystickView();
    }

    protected void initJoystickView() {
        paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBg.setColor(Color.argb(128, 255,255,255));
        paintBg.setStyle(Paint.Style.FILL);

        paintBgStroke = new Paint();
        paintBgStroke.setStrokeWidth(STROKE_WIDTH);
        paintBgStroke.setStyle(Paint.Style.STROKE);
        paintBgStroke.setColor(Color.argb(128, 0,102,168));

        button = new Paint(Paint.ANTI_ALIAS_FLAG);
        button.setColor(Color.WHITE);
        button.setStyle(Paint.Style.FILL);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        // before measure, get the center of view
        xPosition = (int) getWidth() / 2;
        yPosition = (int) getHeight() / 2;
        int d = Math.min(xNew, yNew);
        int d2 = Math.max(xNew, yNew);
        buttonRadius = (int) (d / 2 * 0.85);
        joystickRadius = (int) (d2 / 2 - buttonRadius);

        rectView = new RectF(0, 0, (float) getWidth(), (float) getHeight());
        rectViewStroke = new RectF(STROKE_WIDTH, STROKE_WIDTH, (float) (getWidth() - STROKE_WIDTH), (float) (getHeight() - STROKE_WIDTH));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // setting the measured values to resize the view to a certain width and
        // height
        // int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));

        if (isHor) {
            setMeasuredDimension(measuree(widthMeasureSpec, 200), measuree(heightMeasureSpec, 40));
        }else {
            setMeasuredDimension(measuree(widthMeasureSpec, 40), measuree(heightMeasureSpec, 200));
        }
    }

    private int measuree(int measureSpec, int defaultValue) {
        int result = 0;

        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = defaultValue;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // super.onDraw(canvas);
        centerX = (getWidth()) / 2;
        centerY = (getHeight()) / 2;

        if (isHor) {
            canvas.drawRoundRect(rectView, (float) centerY, (float) centerY, paintBg);
            canvas.drawRoundRect(rectViewStroke, (float) centerY, (float) centerY, paintBgStroke);
        }else {
            canvas.drawRoundRect(rectView, (float) centerX, (float) centerX, paintBg);
            canvas.drawRoundRect(rectViewStroke, (float) centerX, (float) centerX, paintBgStroke);
        }
        // painting the move button
        canvas.drawCircle(xPosition, yPosition, buttonRadius, button);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isHor) {
            xPosition = (int) event.getX();
        }else {
            yPosition = (int) event.getY();
        }
        double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        if (abs > joystickRadius) {
            if (isHor)
                xPosition = (int) ((xPosition - centerX) * joystickRadius / abs + centerX);
            else
                yPosition = (int) ((yPosition - centerY) * joystickRadius / abs + centerY);
        }
        invalidate();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            xPosition = (int) centerX;
            yPosition = (int) centerY;
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (onJoystickMoveListener != null)
                onJoystickMoveListener.onValueChanged(getAngle(), getPower(),
                        getDirection(), true);
        }
        if (onJoystickMoveListener != null
                && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread = new Thread(this);
            thread.start();
            if (onJoystickMoveListener != null)
                onJoystickMoveListener.onValueChanged(getAngle(), getPower(),
                        getDirection(), false);
        }
        return true;
    }

    private int getAngle() {
        if (xPosition > centerX) {
            if (yPosition < centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX))
                        * RAD + 90);
            } else if (yPosition > centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX)) * RAD) + 90;
            } else {
                return lastAngle = 90;
            }
        } else if (xPosition < centerX) {
            if (yPosition < centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX))
                        * RAD - 90);
            } else if (yPosition > centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX)) * RAD) - 90;
            } else {
                return lastAngle = -90;
            }
        } else {
            if (yPosition <= centerY) {
                return lastAngle = 0;
            } else {
                if (lastAngle < 0) {
                    return lastAngle = -180;
                } else {
                    return lastAngle = 180;
                }
            }
        }
    }

    private int getPower() {
        return (int) (100 * Math.sqrt((xPosition - centerX)
                * (xPosition - centerX) + (yPosition - centerY)
                * (yPosition - centerY)) / joystickRadius);
    }

    private int getDirection() {
        if (lastPower == 0 && lastAngle == 0) {
            return 0;
        }
        int a = 0;
        if (lastAngle <= 0) {
            a = (lastAngle * -1) + 90;
        } else if (lastAngle > 0) {
            if (lastAngle <= 90) {
                a = 90 - lastAngle;
            } else {
                a = 360 - (lastAngle - 90);
            }
        }

        int direction = (int) (((a + 22) / 45) + 1);

        if (direction > 8) {
            direction = 1;
        }
        return direction;
    }

    public void setOnJoystickMoveListener(OnJoystickMoveListener listener,
                                          long repeatInterval) {
        this.onJoystickMoveListener = listener;
        this.loopInterval = repeatInterval;
    }

    public interface OnJoystickMoveListener {
        public void onValueChanged(int angle, int power, int direction, boolean isUp);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                public void run() {
                    if (onJoystickMoveListener != null)
                        onJoystickMoveListener.onValueChanged(getAngle(),
                                getPower(), getDirection(), false);
                }
            });
            try {
                Thread.sleep(loopInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
