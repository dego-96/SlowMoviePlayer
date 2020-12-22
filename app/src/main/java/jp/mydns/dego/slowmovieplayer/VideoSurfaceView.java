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

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // public constant values
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "VideoSurfaceView";
    private static final float MINIMUM_SCALE = 0.5f;
    private static final float MAXIMUM_SCALE = 4.0f;

    // ---------------------------------------------------------------------------------------------
    // private fields
    // ---------------------------------------------------------------------------------------------
    private ScaleGestureDetector scaleGestureDetector;
    private float scale;
    private int initialWidth;
    private int initialHeight;
    private boolean canChangeLayout;
    private Display display;
    private Point displayCenter;
    private Point moveStart;
    private int moveX;
    private int moveY;
    private boolean isMove;

    // ---------------------------------------------------------------------------------------------
    // static fields
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private static method
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // constructor
    // ---------------------------------------------------------------------------------------------

    /**
     * VideoSurfaceView
     *
     * @param context context
     */
    public VideoSurfaceView(Context context) {
        super(context);
        Log.d(TAG, "VideoSurfaceView");
        init(context);
    }

    /**
     * VideoSurfaceView
     *
     * @param context      context
     * @param attributeSet attribute set
     */
    public VideoSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "VideoSurfaceView");
        init(context);
    }

    /**
     * VideoSurfaceView
     *
     * @param context      context
     * @param attributeSet attribute set
     * @param defStyleAttr  define style attribute
     */
    public VideoSurfaceView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        Log.d(TAG, "VideoSurfaceView");
        init(context);
    }

    // ---------------------------------------------------------------------------------------------
    // public method
    // ---------------------------------------------------------------------------------------------

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
     * @param event motion event
     */
    public void setGestureMotionEvent(MotionEvent event)
    {
        Log.d(TAG, "setGestureMotionEvent");
        this.isMove = false;
        this.scaleGestureDetector.onTouchEvent(event);
    }

    /**
     * move
     *
     * @param event Motion Event
     */
    public void move(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            this.isMove = true;
            this.moveStart = new Point(x, y);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            this.moveStart = null;
            this.isMove = false;
        } else if (this.isMove && event.getAction() == MotionEvent.ACTION_MOVE) {
            int dx = x - this.moveStart.x;
            int dy = y - this.moveStart.y;
            this.moveX += dx;
            this.moveY += dy;

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

        if (this.canChangeLayout) {
            Log.d(TAG, "call super.layout(...)");
            super.layout(l, t, r, b);
        }
    }

    /**
     * setInitialSize
     *
     * @param width  width
     * @param height height
     */
    void setInitialSize(int width, int height) {
        Log.d(TAG, "setInitialSize(" + width + ", " + height + ")");
        this.initialWidth = width;
        this.initialHeight = height;

        if (this.display == null) {
            return;
        }

        int left = this.displayCenter.x - (this.initialWidth / 2);
        int top = this.displayCenter.y - (this.initialHeight / 2);
        int right = this.displayCenter.x + (this.initialWidth / 2);
        int bottom = this.displayCenter.y + (this.initialHeight / 2);
        this.canChangeLayout = true;
        this.layout(left, top, right, bottom);
        this.canChangeLayout = false;
        Log.d(TAG, "(l, t, r, b) : (" + left + ", " + top + ", " + right + ", " + bottom + ")");
    }

    // ---------------------------------------------------------------------------------------------
    // private method (package private)
    // ---------------------------------------------------------------------------------------------

    /**
     * init
     *
     * @param context context
     */
    private void init(Context context) {
        Log.d(TAG, "init");
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
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

        this.scale = 1.0f;
        this.initialWidth = 0;
        this.initialHeight = 0;
        this.canChangeLayout = true;
        this.moveX = 0;
        this.moveY = 0;
        this.isMove = false;

        if (context instanceof Activity) {
            this.display = ((Activity) context).getWindowManager().getDefaultDisplay();
            this.displayCenter = new Point();
            this.display.getSize(this.displayCenter);
            this.displayCenter.x = this.displayCenter.x / 2;
            this.displayCenter.y = this.displayCenter.y / 2;
            Log.d(TAG, "display center : (" + this.displayCenter.x + ", " + this.displayCenter.y + ")");
        } else {
            this.display = null;
        }
    }

    /**
     * resize
     *
     * @param scale scale
     */
    private void resize(float scale) {
        Log.d(TAG, "resize");
        if (this.display == null) {
            return;
        }

        /* calc scale */
        this.scale *= scale;
        if (this.scale > MAXIMUM_SCALE) {
            this.scale = MAXIMUM_SCALE;
        } else if (this.scale < MINIMUM_SCALE) {
            this.scale = MINIMUM_SCALE;
        }
        Log.d(TAG, "scale : " + this.scale);

        int width = (int) ((float) this.initialWidth * this.scale);
        int height = (int) ((float) this.initialHeight * this.scale);
        Log.d(TAG, "(w, h) : (" + width + ", " + height + ")");

        int left = this.displayCenter.x - (width / 2) + this.moveX;
        int top = this.displayCenter.y - (height / 2) + this.moveY;
        int right = this.displayCenter.x + (width / 2) + this.moveX;
        int bottom = this.displayCenter.y + (height / 2) + this.moveY;
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
        int displayWidth = this.displayCenter.x * 2;
        int movieWidth = right - left;
        if (movieWidth <= displayWidth) {
            if (left < 0) {
                this.moveX -= left;
                right -= left;
                left = 0;
            } else if (right > displayWidth) {
                this.moveX -= right - displayWidth;
                left -= right - displayWidth;
                right = displayWidth;
            }
        }
        if (movieWidth >= displayWidth) {
            if (left > 0) {
                this.moveX -= left;
                right -= left;
                left = 0;
            } else if (right < displayWidth) {
                this.moveX += displayWidth - right;
                left += displayWidth - right;
                right = displayWidth;
            }
        }

        // 縦位置の限界を設定
        int displayHeight = this.displayCenter.y * 2;
        int movieHeight = bottom - top;
        if (movieHeight <= displayHeight) {
            if (top < 0) {
                this.moveY -= top;
                bottom -= top;
                top = 0;
            } else if (bottom > displayHeight) {
                this.moveY -= bottom - displayHeight;
                top -= bottom - displayHeight;
                bottom = displayHeight;
            }
        }
        if (movieHeight >= displayHeight) {
            if (top > 0) {
                this.moveY -= top;
                bottom -= top;
                top = 0;
            } else if (bottom < displayHeight) {
                this.moveY += displayHeight - bottom;
                top += displayHeight - bottom;
                bottom = displayHeight;
            }
        }

        Log.d(TAG, "(l, t, r, b) : (" + left + ", " + top + ", " + right + ", " + bottom + ")");

        this.canChangeLayout = true;
        this.layout(left, top, right, bottom);
        this.canChangeLayout = false;
    }
}
