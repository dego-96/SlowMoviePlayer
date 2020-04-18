package jp.mydns.dego.slowmovieplayer;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

class ViewStatusManager {

    private static final String TAG = "ViewStatusManager";

    private static int mStatus;

    private ImageView mNoVideoImageView;
    private ImageView mGalleryImageView;
    private ImageView mBackwardImageView;
    private ImageView mForwardImageView;
    private ImageView mPlayImageView;
    private ImageView mStopImageView;
    private ImageView mSpeedUpImageView;
    private ImageView mSpeedDownImageView;
    private SeekBar mSeekBar;
    private TextView mCurrentTimeTextView;
    private TextView mRemainTimeTextView;

    static final int VIEW_STATUS_INIT = 0;
    static final int VIEW_STATUS_VIDEO_SELECTED = 1;
    static final int VIEW_STATUS_PLAYING = 2;
    static final int VIEW_STATUS_PAUSED = VIEW_STATUS_VIDEO_SELECTED;
//    static final int VIEW_STATUS_ERROR = -1;

    /**
     * ViewStatusManager
     *
     * @param aActivity activity
     */
    ViewStatusManager(final Activity aActivity) {
        Log.d(TAG, "ViewStatusManager");
        mNoVideoImageView = aActivity.findViewById(R.id.image_no_video);
        mGalleryImageView = aActivity.findViewById(R.id.button_gallery);
        mBackwardImageView = aActivity.findViewById(R.id.button_backward);
        mForwardImageView = aActivity.findViewById(R.id.button_forward);
        mPlayImageView = aActivity.findViewById(R.id.button_play);
        mStopImageView = aActivity.findViewById(R.id.button_stop);
        mSpeedUpImageView = aActivity.findViewById(R.id.button_speed_up);
        mSpeedDownImageView = aActivity.findViewById(R.id.button_speed_down);
        mSeekBar = aActivity.findViewById(R.id.seek_bar_progress);
        mCurrentTimeTextView = aActivity.findViewById(R.id.text_view_current_time);
        mRemainTimeTextView = aActivity.findViewById(R.id.text_view_remain_time);
        this.setButtonState(VIEW_STATUS_INIT);
    }

    /**
     * setButtonState
     *
     * @param aStatus status
     */
    void setButtonState(int aStatus) {
        Log.d(TAG, "setButtonState(" + aStatus + ")");
        mStatus = aStatus;
        switch (aStatus) {
            case VIEW_STATUS_INIT:
//            case VIEW_STATUS_ERROR:
                mNoVideoImageView.setVisibility(View.VISIBLE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.GONE);    // not support
                mForwardImageView.setVisibility(View.INVISIBLE);
                mPlayImageView.setVisibility(View.INVISIBLE);
                mStopImageView.setVisibility(View.INVISIBLE);
                mSpeedUpImageView.setVisibility(View.GONE);     // not support
                mSpeedDownImageView.setVisibility(View.GONE);   // not support
                mSeekBar.setVisibility(View.INVISIBLE);
                mCurrentTimeTextView.setVisibility(View.INVISIBLE);
                mRemainTimeTextView.setVisibility(View.INVISIBLE);
                break;
            case VIEW_STATUS_VIDEO_SELECTED:
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.GONE);    // not support
                mForwardImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setImageResource(R.drawable.play);
                mPlayImageView.setVisibility(View.VISIBLE);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.GONE);     // not support
                mSpeedDownImageView.setVisibility(View.GONE);   // not support
                mSeekBar.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            case VIEW_STATUS_PLAYING:
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.INVISIBLE);
                mBackwardImageView.setVisibility(View.GONE);
                mForwardImageView.setVisibility(View.INVISIBLE);
                mPlayImageView.setImageResource(R.drawable.pause);
                mPlayImageView.setVisibility(View.VISIBLE);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.GONE);
                mSpeedDownImageView.setVisibility(View.GONE);
                mSeekBar.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * getStatus
     *
     * @return view status
     */
    int getStatus() {
        Log.d(TAG, "getStatus");
        return mStatus;
    }

    /**
     * setDuration
     *
     * @param aDuration duration
     */
    void setDuration(int aDuration) {
        Log.d(TAG, "setDuration");
        Log.d(TAG, "video duration :" + aDuration);
        mSeekBar.setMax(aDuration);
    }
}
