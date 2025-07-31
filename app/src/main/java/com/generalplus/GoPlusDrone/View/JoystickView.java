package com.generalplus.GoPlusDrone.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A custom View that implements a simple two‑dimensional joystick.  The view draws
 * a large circle representing the joystick's bounds and a smaller circle for the
 * thumb.  Users can drag the thumb within the bounds and the view computes
 * angle, power (magnitude) and direction indices.  A listener can be
 * registered via {@link #setOnJoystickMoveListener(OnJoystickMoveListener, long)}
 * to receive periodic callbacks while the user is interacting.
 */
public class JoystickView extends View implements Runnable {
    // Conversion factor from radians to degrees
    private static final double RAD = 57.2957795;
    /** Default interval (ms) between move callbacks */
    public static final long DEFAULT_LOOP_INTERVAL = 100;
    // Direction constants
    public static final int FRONT = 3;
    public static final int FRONT_RIGHT = 4;
    public static final int RIGHT = 5;
    public static final int RIGHT_BOTTOM = 6;
    public static final int BOTTOM = 7;
    public static final int BOTTOM_LEFT = 8;
    public static final int LEFT = 1;
    public static final int LEFT_FRONT = 2;

    // Listener for move events
    private OnJoystickMoveListener onJoystickMoveListener;
    private Thread thread = new Thread(this);
    private long loopInterval = DEFAULT_LOOP_INTERVAL;
    // Touch positions
    private int xPosition = 0;
    private int yPosition = 0;
    // Center of the view
    private double centerX = 0;
    private double centerY = 0;
    // Paint objects for drawing
    private Paint mainCircle;
    private Paint button;
    // Radii
    private int joystickRadius;
    private int buttonRadius;
    // Last computed angle and power
    private int lastAngle = 0;
    private int lastPower = 0;
    // Whether this joystick controls the left stick (affects power calculation)
    private boolean m_bLeft = true;

    public JoystickView(Context context) {
        super(context);
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initJoystickView();
    }

    /**
     * Set whether this joystick represents the left control stick.  Left sticks compute
     * power based on the vertical axis only.
     */
    public void setLeft(boolean left) {
        m_bLeft = left;
    }

    /**
     * Initialize paints and default properties for the joystick.
     */
    protected void initJoystickView() {
        mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainCircle.setColor(Color.WHITE);
        mainCircle.setStyle(Paint.Style.FILL_AND_STROKE);
        mainCircle.setAlpha(50);
        button = new Paint(Paint.ANTI_ALIAS_FLAG);
        button.setColor(0xFF0080FF);
        button.setStyle(Paint.Style.FILL);
        button.setAlpha(100);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        xPosition = getWidth() / 2;
        yPosition = getWidth() / 2;
        int d = Math.min(xNew, yNew);
        buttonRadius = (int) (d / 2f * 0.25f);
        joystickRadius = (int) (d / 2f * 0.75f);
        if (m_bLeft) {
            // Position the thumb at the bottom of the circle for left joystick
            yPosition = getWidth() - buttonRadius;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
        setMeasuredDimension(d, d);
    }

    private int measure(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.UNSPECIFIED) {
            return 200;
        } else {
            return specSize;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        centerX = getWidth() / 2.0;
        centerY = getHeight() / 2.0;
        canvas.drawCircle((float) centerX, (float) centerY, joystickRadius, mainCircle);
        canvas.drawCircle(xPosition, yPosition, buttonRadius, button);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        xPosition = (int) event.getX();
        yPosition = (int) event.getY();
        double distance = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        // Constrain the thumb to the joystick bounds
        if (distance > joystickRadius) {
            xPosition = (int) ((xPosition - centerX) * joystickRadius / distance + centerX);
            yPosition = (int) ((yPosition - centerY) * joystickRadius / distance + centerY);
        }
        invalidate();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Reset to center on release
            xPosition = (int) centerX;
            if (!m_bLeft) {
                yPosition = (int) centerY;
            }
            thread.interrupt();
            if (onJoystickMoveListener != null) {
                onJoystickMoveListener.onValueChanged(getAngle(), getPower(), getDirection());
            }
        }
        if (onJoystickMoveListener != null && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread = new Thread(this);
            thread.start();
            onJoystickMoveListener.onValueChanged(getAngle(), getPower(), getDirection());
        }
        return true;
    }

    private int getAngle() {
        if (xPosition > centerX) {
            if (yPosition < centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD + 90);
            } else if (yPosition > centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD) + 90;
            } else {
                return lastAngle = 90;
            }
        } else if (xPosition < centerX) {
            if (yPosition < centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD - 90);
            } else if (yPosition > centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY) / (xPosition - centerX)) * RAD) - 90;
            } else {
                return lastAngle = -90;
            }
        } else {
            if (yPosition <= centerY) {
                return lastAngle = 0;
            } else {
                return lastAngle = lastAngle < 0 ? -180 : 180;
            }
        }
    }

    private int getPower() {
        if (m_bLeft) {
            return getLeftPower();
        }
        return (int) (100 * Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY)) / joystickRadius);
    }

    private int getLeftPower() {
        double dPower = (Math.abs((yPosition - buttonRadius - 2 * joystickRadius)) * 100.0) / (2 * joystickRadius);
        return Math.round((float) dPower);
    }

    private int getDirection() {
        if (lastPower == 0 && lastAngle == 0) {
            return 0;
        }
        int a;
        if (lastAngle <= 0) {
            a = (lastAngle * -1) + 90;
        } else if (lastAngle > 0) {
            if (lastAngle <= 90) {
                a = 90 - lastAngle;
            } else {
                a = 360 - (lastAngle - 90);
            }
        } else {
            a = 0;
        }
        int direction = (int) (((a + 22) / 45) + 1);
        if (direction > 8) {
            direction = 1;
        }
        return direction;
    }

    /**
     * Register a listener to receive periodic angle/power/direction updates while the
     * joystick is being manipulated.  The listener will be invoked every
     * {@code repeatInterval} milliseconds.  If set to zero, the listener will only
     * receive a callback on ACTION_DOWN and ACTION_UP.
     */
    public void setOnJoystickMoveListener(OnJoystickMoveListener listener, long repeatInterval) {
        this.onJoystickMoveListener = listener;
        this.loopInterval = repeatInterval;
    }

    /** Interface definition for callbacks invoked when the joystick is moved. */
    public interface OnJoystickMoveListener {
        void onValueChanged(int angle, int power, int direction);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (onJoystickMoveListener != null) {
                        onJoystickMoveListener.onValueChanged(getAngle(), getPower(), getDirection());
                    }
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