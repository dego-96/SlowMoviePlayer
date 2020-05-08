package jp.mydns.dego.slowmovieplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView {

    private static final String TAG = "VideoSurfaceView";

    /**
     * VideoSurfaceView
     *
     * @param aContext context
     */
    public VideoSurfaceView(Context aContext) {
        super(aContext);
        Log.d(TAG, "VideoSurfaceView");
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
}
