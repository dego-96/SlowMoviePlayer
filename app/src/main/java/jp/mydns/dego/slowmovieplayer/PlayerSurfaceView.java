package jp.mydns.dego.slowmovieplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PlayerSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "PlayerSurfaceView";
    private String mVideoPath;
    private VideoPlayer mPlayer = null;

    /**
     * PlayerSurfaceView
     *
     * @param aContext context
     */
    public PlayerSurfaceView(Context aContext) {
        super(aContext);
        Log.d(TAG, "PlayerSurfaceView");
        init();
    }

    /**
     * PlayerSurfaceView
     *
     * @param aContext      context
     * @param aAttributeSet attribute set
     */
    public PlayerSurfaceView(Context aContext, AttributeSet aAttributeSet) {
        super(aContext, aAttributeSet);
        Log.d(TAG, "PlayerSurfaceView");
        init();
    }

    /**
     * PlayerSurfaceView
     *
     * @param aContext      context
     * @param aAttributeSet attribute set
     * @param defStyleAttr  default style attribute
     */
    public PlayerSurfaceView(Context aContext, AttributeSet aAttributeSet, int defStyleAttr) {
        super(aContext, aAttributeSet, defStyleAttr);
        Log.d(TAG, "PlayerSurfaceView");
        init();
    }

    /**
     * setVideoPlayer
     *
     * @param aPlayer video player
     */
    void setVideoPlayer(VideoPlayer aPlayer) {
        mPlayer = aPlayer;
    }

    /**
     * setVideoPath
     *
     * @param aPath video path
     */
    void setVideoPath(String aPath) {
        mVideoPath = aPath;
    }

    /**
     * surfaceCreated
     *
     * @param aSurfaceHolder surface holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder aSurfaceHolder) {
        Log.d(TAG, "surfaceCreated");
    }

    /**
     * surfaceChanged
     *
     * @param aSurfaceHolder surface holder
     * @param aFormat        format
     * @param aWidth         surface width
     * @param aHeight        surface height
     */
    @Override
    public void surfaceChanged(SurfaceHolder aSurfaceHolder, int aFormat, int aWidth, int aHeight) {
        Log.d(TAG, "surfaceChanged");
        if (null != mVideoPath && null != mPlayer) {
            if (mPlayer.init(aSurfaceHolder.getSurface(), mVideoPath)) {
                mPlayer.start();
            }
        }
    }

    /**
     * surfaceDestroyed
     *
     * @param aSurfaceHolder surface holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder aSurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        mPlayer = null;
    }

    /**
     * init
     */
    private void init() {
        Log.d(TAG, "init");
        mPlayer = null;
        mVideoPath = null;
        getHolder().addCallback(this);
    }
}
