package jp.mydns.dego.slowmovieplayer;

import android.util.Log;
import android.widget.SeekBar;

public class VideoSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VideoSeekBarListener";

    private VideoPlayer mPlayer;

    /**
     * VideoSeekBarChangeListener
     *
     * @param aPlayer video player
     */
    VideoSeekBarChangeListener(VideoPlayer aPlayer) {
        mPlayer = aPlayer;
    }

    /**
     * onProgressChanged
     *
     * @param aSeekBar  video seek bar
     * @param aProgress progress
     * @param aFromUser from user
     */
    @Override
    public void onProgressChanged(SeekBar aSeekBar, int aProgress, boolean aFromUser) {
        Log.d(TAG, "onProgressChanged");
        Log.d(TAG, "progress : " + aProgress);
        if (aFromUser) {
            mPlayer.seek(aProgress);
        }
    }

    /**
     * onStartTrackingTouch
     *
     * @param aSeekBar video seek bar
     */
    @Override
    public void onStartTrackingTouch(SeekBar aSeekBar) {
        Log.d(TAG, "onStartTrackingTouch");
        mPlayer.pause();
    }

    /**
     * onStopTrackingTouch
     *
     * @param aSeekBar video seek bar
     */
    @Override
    public void onStopTrackingTouch(SeekBar aSeekBar) {
        Log.d(TAG, "onStopTrackingTouch");
        mPlayer.seek(aSeekBar.getProgress());
    }
}
