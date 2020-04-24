package jp.mydns.dego.slowmovieplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "VideoSurfaceView";

    private VideoPlayer mPlayer;
    private String mVideoPath;

    /**
     * VideoSurfaceView
     *
     * @param aContext context
     */
    public VideoSurfaceView(Context aContext) {
        super(aContext);
        Log.d(TAG, "VideoSurfaceView");
        init();
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
        init();
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
        init();
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
     * surfaceCreated
     *
     * @param holder surface holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    /**
     * surfaceChanged
     *
     * @param holder surface holder
     * @param format format
     * @param width  view width
     * @param height view height
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        if (null != mVideoPath && !"".equals(mVideoPath) && null != mPlayer) {
            Log.d(TAG, "video path: " + mVideoPath);
            mPlayer.init(holder.getSurface(), mVideoPath);
        }
    }

    /**
     * surfaceDestroyed
     *
     * @param holder surface holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }

    /**
     * setPlayer
     *
     * @param aPlayer video player
     */
    void setPlayer(VideoPlayer aPlayer) {
        Log.d(TAG, "setPlayer");
        mPlayer = aPlayer;
    }

    /**
     * setVideoPath
     *
     * @param aPath video file path
     */
    void setVideoPath(String aPath) {
        Log.d(TAG, "setVideoPath");
        mVideoPath = aPath;
    }

    /**
     * init
     */
    private void init() {
        Log.d(TAG, "init");
        this.getHolder().addCallback(this);
    }
}
