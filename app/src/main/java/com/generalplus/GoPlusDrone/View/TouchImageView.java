package com.generalplus.GoPlusDrone.View;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

/**
 * An {@link ImageView} implementation that supports pinch‑to‑zoom and panning.
 * The view maintains its own transformation matrix and provides optional
 * constraints such as minimum and maximum scale factors.  Double tapping will
 * reset the image to its original scale and re‑center it.  This class is
 * inspired by various open source implementations but has been adapted to
 * follow modern Android best practices.
 */
public class TouchImageView extends ImageView {

    private final Matrix matrix = new Matrix();
    // Gesture modes
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Zooming variables
    private final PointF last = new PointF();
    private final PointF start = new PointF();
    private float minScale = 1f;
    private float maxScale = 3f;
    private final float[] m = new float[9];

    private int viewWidth, viewHeight;
    private static final int CLICK = 3;
    private float saveScale = 1f;
    protected float fOrigWidth, fOrigHeight;
    private int oldMeasuredWidth, oldMeasuredHeight;

    private ScaleGestureDetector mScaleDetector;
    private final Context context;

    private boolean bAllowSlidePage = true;
    private boolean bCenterFlag = false;
    private boolean bPointerDownFlag = false;
    private boolean bDoubleClickFlag = false;

    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;
    private static final int DOUBLE_CLICK_TIMEOUT = 200;

    public TouchImageView(Context context) {
        this(context, null);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        sharedConstructing();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void sharedConstructing() {
        super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                PointF curr = new PointF(event.getX(), event.getY());
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Double click detection
                        mCurrentDownEvent = MotionEvent.obtain(event);
                        if (mPreviousUpEvent != null && mCurrentDownEvent != null && !bPointerDownFlag
                                && isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, event)) {
                            bDoubleClickFlag = true;
                        } else {
                            bDoubleClickFlag = false;
                        }
                        last.set(curr);
                        start.set(last);
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            float fixTransX = getFixDragTrans(deltaX, viewWidth, fOrigWidth * saveScale);
                            float fixTransY = getFixDragTrans(deltaY, viewHeight, fOrigHeight * saveScale);
                            matrix.postTranslate(fixTransX, fixTransY);
                            fixTrans();
                            last.set(curr.x, curr.y);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        mPreviousUpEvent = MotionEvent.obtain(event);
                        mode = NONE;
                        int xDiff = (int) Math.abs(curr.x - start.x);
                        int yDiff = (int) Math.abs(curr.y - start.y);
                        if (xDiff < CLICK && yDiff < CLICK) {
                            performClick();
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        bPointerDownFlag = false;
                        mode = NONE;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        bPointerDownFlag = true;
                        mode = NONE;
                        break;
                    default:
                        break;
                }
                determineAllowSlidePage(saveScale);
                setImageMatrix(matrix);
                invalidate();
                return true;
            }
        });
    }

    /**
     * Determines whether the gesture should allow parent views (such as a ViewPager) to intercept
     * touch events based on the current scale factor.  When zoomed in beyond 1x, panning is
     * handled entirely by this view; otherwise parent scrolling is permitted.
     */
    private void determineAllowSlidePage(float scale) {
        bAllowSlidePage = scale <= 1.0f;
        getParent().requestDisallowInterceptTouchEvent(!bAllowSlidePage);
    }

    /**
     * Helper to detect whether two consecutive taps should be considered a double tap.
     */
    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
        if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_CLICK_TIMEOUT) {
            return false;
        }
        int deltaX = (int) firstUp.getX() - (int) secondDown.getX();
        int deltaY = (int) firstUp.getY() - (int) secondDown.getY();
        return (deltaX * deltaX + deltaY * deltaY) < 10000;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float mOrigScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / mOrigScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / mOrigScale;
            }
            if (fOrigWidth * saveScale <= viewWidth || fOrigHeight * saveScale <= viewHeight) {
                matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f);
            } else {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
            }
            fixTrans();
            return true;
        }
    }

    /**
     * Apply necessary translation corrections to keep the image within the view bounds.  Also
     * handles centering and double-tap behavior.
     */
    private void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];
        float fixTransX = getFixTrans(transX, viewWidth, fOrigWidth * saveScale);
        float fixTransY = getFixTrans(transY, viewHeight, fOrigHeight * saveScale);
        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
        // Determine if we should re-center the image
        if (transY < 0) {
            bCenterFlag = true;
        }
        if (bDoubleClickFlag) {
            saveScale = 1f;
            bCenterFlag = true;
            bDoubleClickFlag = false;
        }
        if (saveScale == 1f && bCenterFlag) {
            bCenterFlag = false;
            centerImage();
        }
    }

    private float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans;
        float maxTrans;
        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }
        if (trans < minTrans) return -trans + minTrans;
        if (trans > maxTrans) return -trans + maxTrans;
        return 0;
    }

    private float getFixDragTrans(float delta, float viewSize, float contentSize) {
        return contentSize <= viewSize ? 0 : delta;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        if ((oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight)
                || viewWidth == 0 || viewHeight == 0) {
            return;
        }
        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;
        centerImage();
    }

    /**
     * Centers the image in the view when the scale is 1.  Calculates the scale factor
     * required to fit the image within the view and applies appropriate translation.
     */
    private void centerImage() {
        if (saveScale == 1f) {
            Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
                return;
            }
            int bmWidth = drawable.getIntrinsicWidth();
            int bmHeight = drawable.getIntrinsicHeight();
            float scaleX = (float) viewWidth / (float) bmWidth;
            float scaleY = (float) viewHeight / (float) bmHeight;
            float scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);
            // Center the image
            float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
            float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
            redundantYSpace /= 2f;
            redundantXSpace /= 2f;
            matrix.postTranslate(redundantXSpace, redundantYSpace);
            fOrigWidth = viewWidth - 2 * redundantXSpace;
            fOrigHeight = viewHeight - 2 * redundantYSpace;
            setImageMatrix(matrix);
        }
        fixTrans();
    }
}