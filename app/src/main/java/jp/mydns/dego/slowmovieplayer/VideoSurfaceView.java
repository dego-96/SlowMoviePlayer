package jp.mydns.dego.slowmovieplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView {

    private static final String TAG = "VideoSurfaceView";

    private static final float MINIMUM_SCALE = 0.5f;
    private static final float MAXIMUM_SCALE = 4.0f;

    private ScaleGestureDetector mScaleGestureDetector;
    private float mScale;
    private int mInitialWidth;
    private int mInitialHeight;
    private boolean mCanChangeLayout;
    private Display mDisplay;
    private Point mDisplayCenter;
    private Point mMoveStart;
    private int mMoveX;
    private int mMoveY;
    private boolean mIsMove;

    /**
     * VideoSurfaceView
     *
     * @param aContext context
     */
    public VideoSurfaceView(Context aContext) {
        super(aContext);
        Log.d(TAG, "VideoSurfaceView");
        init(aContext);
    }

    /**
     * VideoSurfaceView
     *
     * @param aContext      context
     * @param aAttributeSet attribute set
     */
    public VideoSurfaceView(Context aContext, AttributeSet aAttributeSet) {
        super(aContext, aAttributeSet);
        Log.d(TAG, "VideoSurfaceView");
        init(aContext);
    }

    /**
     * VideoSurfaceView
     *
     * @param aContext      context
     * @param aAttributeSet attribute set
     * @param defStyleAttr  define style attribute
     */
    public VideoSurfaceView(Context aContext, AttributeSet aAttributeSet, int defStyleAttr) {
        super(aContext, aAttributeSet, defStyleAttr);
        Log.d(TAG, "VideoSurfaceView");
        init(aContext);
    }

    /**
     * performClick
     *
     * @return disable next touch event
     */
    @Override
    public boolean performClick() {
        Log.d(TAG, "performClick");
        super.performClick();
        return false;
    }

    /**
     * setGestureMotionEvent
     *
     * @param aEvent motion event
     */
    void setGestureMotionEvent(MotionEvent aEvent)
    {
        Log.d(TAG, "setGestureMotionEvent");
        mIsMove = false;
        mScaleGestureDetector.onTouchEvent(aEvent);
    }

    /**
     * move
     *
     * @param aEvent Motion Event
     */
    void move(MotionEvent aEvent) {
        int x = (int) aEvent.getX();
        int y = (int) aEvent.getY();
        if (aEvent.getAction() == MotionEvent.ACTION_DOWN) {
            mIsMove = true;
            mMoveStart = new Point(x, y);
        } else if (aEvent.getAction() == MotionEvent.ACTION_UP) {
            mMoveStart = null;
            mIsMove = false;
        } else if (mIsMove && aEvent.getAction() == MotionEvent.ACTION_MOVE) {
            int dx = x - mMoveStart.x;
            int dy = y - mMoveStart.y;
            mMoveX += dx;
            mMoveY += dy;

            int left = this.getLeft() + dx;
            int top = this.getTop() + dy;
            int right = this.getRight() + dx;
            int bottom = this.getBottom() + dy;

            limit(left, top, right, bottom);
        }
    }

    /**
     * layout
     *
     * @param l left
     * @param t top
     * @param r right
     * @param b bottom
     */
    @Override
    public void layout(int l, int t, int r, int b) {
        Log.d(TAG, "layout(" + l + ", " + t + ", " + r + ", " + b + ")");

        if (mCanChangeLayout) {
            Log.d(TAG, "call super.layout(...)");
            super.layout(l, t, r, b);
        }
    }

    /**
     * setInitialSize
     *
     * @param aWidth  width
     * @param aHeight height
     */
    void setInitialSize(int aWidth, int aHeight) {
        Log.d(TAG, "setInitialSize(" + aWidth + ", " + aHeight + ")");
        mInitialWidth = aWidth;
        mInitialHeight = aHeight;

        if (mDisplay == null) {
            return;
        }

        int left = mDisplayCenter.x - (mInitialWidth / 2);
        int top = mDisplayCenter.y - (mInitialHeight / 2);
        int right = mDisplayCenter.x + (mInitialWidth / 2);
        int bottom = mDisplayCenter.y + (mInitialHeight / 2);
        mCanChangeLayout = true;
        this.layout(left, top, right, bottom);
        mCanChangeLayout = false;
        Log.d(TAG, "(l, t, r, b) : (" + left + ", " + top + ", " + right + ", " + bottom + ")");
    }

    /**
     * init
     *
     * @param aContext context
     */
    private void init(Context aContext) {
        Log.d(TAG, "init");
        mScaleGestureDetector = new ScaleGestureDetector(aContext, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.d(TAG, "onScale");
                resize(detector.getScaleFactor());
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                Log.d(TAG, "onScaleBegin");
                resize(detector.getScaleFactor());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                Log.d(TAG, "onScaleEnd");
            }
        });

        mScale = 1.0f;
        mInitialWidth = 0;
        mInitialHeight = 0;
        mCanChangeLayout = true;
        mMoveX = 0;
        mMoveY = 0;
        mIsMove = false;

        if (aContext instanceof Activity) {
            mDisplay = ((Activity) aContext).getWindowManager().getDefaultDisplay();
            mDisplayCenter = new Point();
            mDisplay.getSize(mDisplayCenter);
            mDisplayCenter.x = mDisplayCenter.x / 2;
            mDisplayCenter.y = mDisplayCenter.y / 2;
            Log.d(TAG, "display center : (" + mDisplayCenter.x + ", " + mDisplayCenter.y + ")");
        } else {
            mDisplay = null;
        }
    }

    /**
     * resize
     *
     * @param aScale scale
     */
    private void resize(float aScale) {
        Log.d(TAG, "resize");
        if (mDisplay == null) {
            return;
        }

        /* calc scale */
        mScale *= aScale;
        if (mScale > MAXIMUM_SCALE) {
            mScale = MAXIMUM_SCALE;
        } else if (mScale < MINIMUM_SCALE) {
            mScale = MINIMUM_SCALE;
        }
        Log.d(TAG, "scale : " + mScale);

        int width = (int) ((float) mInitialWidth * mScale);
        int height = (int) ((float) mInitialHeight * mScale);
        Log.d(TAG, "(w, h) : (" + width + ", " + height + ")");

        int left = mDisplayCenter.x - (width / 2) + mMoveX;
        int top = mDisplayCenter.y - (height / 2) + mMoveY;
        int right = mDisplayCenter.x + (width / 2) + mMoveX;
        int bottom = mDisplayCenter.y + (height / 2) + mMoveY;
        Log.d(TAG, "(l, t, r, b) : (" + left + ", " + top + ", " + right + ", " + bottom + ")");

        limit(left, top, right, bottom);
    }

    /**
     * limit
     *
     * @param left   setting value of left
     * @param top    setting value of top
     * @param right  setting value of right
     * @param bottom setting value of bottom
     */
    private void limit(int left, int top, int right, int bottom) {
        Log.d(TAG, "limit");
        // 横位置の限界を設定
        int displayWidth = mDisplayCenter.x * 2;
        int movieWidth = right - left;
        if (movieWidth <= displayWidth) {
            if (left < 0) {
                mMoveX -= left;
                right -= left;
                left = 0;
            } else if (right > displayWidth) {
                mMoveX -= right - displayWidth;
                left -= right - displayWidth;
                right = displayWidth;
            }
        }
        if (movieWidth >= displayWidth) {
            if (left > 0) {
                mMoveX -= left;
                right -= left;
                left = 0;
            } else if (right < displayWidth) {
                mMoveX += displayWidth - right;
                left += displayWidth - right;
                right = displayWidth;
            }
        }

        // 縦位置の限界を設定
        int displayHeight = mDisplayCenter.y * 2;
        int movieHeight = bottom - top;
        if (movieHeight <= displayHeight) {
            if (top < 0) {
                mMoveY -= top;
                bottom -= top;
                top = 0;
            } else if (bottom > displayHeight) {
                mMoveY -= bottom - displayHeight;
                top -= bottom - displayHeight;
                bottom = displayHeight;
            }
        }
        if (movieHeight >= displayHeight) {
            if (top > 0) {
                mMoveY -= top;
                bottom -= top;
                top = 0;
            } else if (bottom < displayHeight) {
                mMoveY += displayHeight - bottom;
                top += displayHeight - bottom;
                bottom = displayHeight;
            }
        }

        Log.d(TAG, "(l, t, r, b) : (" + left + ", " + top + ", " + right + ", " + bottom + ")");

        mCanChangeLayout = true;
        this.layout(left, top, right, bottom);
        mCanChangeLayout = false;
    }
}
