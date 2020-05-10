package jp.mydns.dego.slowmovieplayer;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class ViewController {

    private static final String TAG = "ViewController";
    private static final int ANIMATOR_DURATION = 300;

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("mm:ss:SSS", Locale.JAPAN);

    private Activity mActivity;
    private boolean mFrameOuted;

    /* Views */
    private VideoSurfaceView mSurfaceView;
    private ImageView mNoVideoImageView;
    private ImageView mGalleryImageView;
    private ImageView mBackwardImageView;
    private ImageView mForwardImageView;
    private ImageView mPlayImageView;
    private ImageView mStopImageView;
    private ImageView mSpeedUpImageView;
    private ImageView mSpeedDownImageView;
    private SeekBar mSeekBar;
    private TextView mPlaybackSpeedTextView;
    private TextView mCurrentTimeTextView;
    private TextView mRemainTimeTextView;

    /* Layout */
    private LinearLayout mControlButtonsLayout;
    private LinearLayout mSeekBarLayout;

    /**
     * ViewController
     *
     * @param aActivity main activity
     */
    ViewController(Activity aActivity) {
        Log.d(TAG, "ViewController");

        mActivity = aActivity;

        mSurfaceView = aActivity.findViewById(R.id.player_surface_view);
        mNoVideoImageView = aActivity.findViewById(R.id.image_no_video);
        mGalleryImageView = aActivity.findViewById(R.id.button_gallery);
        mBackwardImageView = aActivity.findViewById(R.id.button_backward);
        mForwardImageView = aActivity.findViewById(R.id.button_forward);
        mPlayImageView = aActivity.findViewById(R.id.button_play);
        mStopImageView = aActivity.findViewById(R.id.button_stop);
        mSpeedUpImageView = aActivity.findViewById(R.id.button_speed_up);
        mSpeedDownImageView = aActivity.findViewById(R.id.button_speed_down);
        mSeekBar = aActivity.findViewById(R.id.seek_bar_progress);
        mPlaybackSpeedTextView = aActivity.findViewById(R.id.text_view_speed);
        mCurrentTimeTextView = aActivity.findViewById(R.id.text_view_current_time);
        mRemainTimeTextView = aActivity.findViewById(R.id.text_view_remain_time);

        mControlButtonsLayout = aActivity.findViewById(R.id.layout_control_buttons);
        mSeekBarLayout = aActivity.findViewById(R.id.layout_seek_bar);

        mFrameOuted = false;
    }

    /**
     * getView
     *
     * @param aId view id
     * @return view
     */
    View getView(int aId) {
        return mActivity.findViewById(aId);
    }

    /**
     * setSurfaceViewSize
     *
     * @param aVideoWidth  width
     * @param aVideoHeight height
     * @param aRotation    rotation
     */
    @SuppressWarnings("noinspection")
    void setSurfaceViewSize(int aVideoWidth, int aVideoHeight, int aRotation) {
        Log.d(TAG, "setSurfaceViewSize");

        Point point = new Point();
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        display.getSize(point);
        int displayWidth = point.x;
        int displayHeight = point.y;
        Log.d(TAG, "display size : (" + displayWidth + ", " + displayHeight + ")");

        int width;
        int height;
        if ((aRotation % 180) == 0) {
            // 横画面映像
            if (((float) displayWidth / (float) displayHeight) > ((float) aVideoWidth / (float) aVideoHeight)) {
                // 画面より縦長
                width = (int) ((float) aVideoWidth * ((float) displayHeight / (float) aVideoHeight));
                height = displayHeight;
            } else {
                // 画面より横長
                width = displayWidth;
                height = (int) ((float) aVideoHeight * ((float) displayWidth / (float) aVideoWidth));
            }
        } else {
            // 縦画面映像
            if (((float) displayWidth / (float) displayHeight) > ((float) aVideoHeight / (float) aVideoWidth)) {
                // 画面より縦長
                width = (int) ((float) aVideoHeight * ((float) displayHeight / (float) aVideoWidth));
                height = displayHeight;
            } else {
                // 画面より横長
                width = displayWidth;
                height = (int) ((float) aVideoWidth * ((float) displayWidth / (float) aVideoHeight));
            }
        }
        mSurfaceView.setInitialSize(width, height);
    }

    /**
     * setVisibility
     *
     * @param aStatus video status
     */
    void setVisibility(VideoRunnable.STATUS aStatus) {
        Log.d(TAG, "setVisibility( " + aStatus.name() + " )");
        switch (aStatus) {
            case INIT:
                mSurfaceView.setVisibility(View.GONE);
                mNoVideoImageView.setVisibility(View.VISIBLE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.GONE);
                mForwardImageView.setVisibility(View.GONE);
                mPlayImageView.setVisibility(View.GONE);
                mStopImageView.setVisibility(View.GONE);
                mSpeedUpImageView.setVisibility(View.GONE);
                mSpeedDownImageView.setVisibility(View.GONE);
                mSeekBar.setVisibility(View.GONE);
                mPlaybackSpeedTextView.setVisibility(View.GONE);
                mCurrentTimeTextView.setVisibility(View.GONE);
                mRemainTimeTextView.setVisibility(View.GONE);
                break;
            case PLAYING:
                mSurfaceView.setVisibility(View.VISIBLE);
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.INVISIBLE);
                mBackwardImageView.setVisibility(View.INVISIBLE);
                mForwardImageView.setVisibility(View.INVISIBLE);
                mPlayImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setImageResource(R.drawable.pause);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.INVISIBLE);
                mSpeedDownImageView.setVisibility(View.INVISIBLE);
                mSeekBar.setVisibility(View.VISIBLE);
                mPlaybackSpeedTextView.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            case VIDEO_SELECTED:
            case PAUSED:
            case VIDEO_END:
            case FORWARD:
            case BACKWARD:
            case SEEKING:
                mSurfaceView.setVisibility(View.VISIBLE);
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.VISIBLE);
                mForwardImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setImageResource(R.drawable.play);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.VISIBLE);
                mSpeedDownImageView.setVisibility(View.VISIBLE);
                mSeekBar.setVisibility(View.VISIBLE);
                mPlaybackSpeedTextView.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            default:
                Log.e(TAG, "invalid player status :" + aStatus);
                break;
        }
    }

    /**
     * setProgress
     *
     * @param aProgress progress
     */
    void setProgress(int aProgress) {
        Log.d(TAG, "setProgress (" + aProgress + ")");
        mSeekBar.setProgress(aProgress);

        mCurrentTimeTextView.setText(sDateFormat.format(new Date(aProgress)));
        int duration = mSeekBar.getMax();
        if (aProgress < duration) {
            mRemainTimeTextView.setText(sDateFormat.format(new Date(duration - aProgress)));
        } else {
            mRemainTimeTextView.setText(mActivity.getString(R.string.remain_time_init));
        }
    }

    /**
     * setDuration
     *
     * @param aDuration video duration
     */
    void setDuration(int aDuration) {
        Log.d(TAG, "setDuration (" + aDuration + ")");
        mSeekBar.setMax(aDuration);

        mRemainTimeTextView.setText(sDateFormat.format(new Date(aDuration)));
    }

    /**
     * setPlaybackSpeed
     *
     * @param aSpeed playback speed (-4, -2, 0, 2, 4)
     */
    void setPlaybackSpeed(int aSpeed) {
        Log.d(TAG, "setPlaybackSpeed");
        switch (aSpeed) {
            case -4:
                mPlaybackSpeedTextView.setText(mActivity.getString(R.string.playback_speed_x1_4));
                break;
            case -2:
                mPlaybackSpeedTextView.setText(mActivity.getString(R.string.playback_speed_x1_2));
                break;
            case 0:
                mPlaybackSpeedTextView.setText(mActivity.getString(R.string.playback_speed_init));
                break;
            case 2:
                mPlaybackSpeedTextView.setText(mActivity.getString(R.string.playback_speed_x2_0));
                break;
            case 4:
                mPlaybackSpeedTextView.setText(mActivity.getString(R.string.playback_speed_x4_0));
                break;
            default:
                break;
        }
    }

    /**
     * animFullscreenPreview
     */
    void animFullscreenPreview() {
        Log.d(TAG, "fullScreenAnimationStart");

        float toX, fromX, toY, fromY;
        List<Animator> animatorList = new ArrayList<>();

        if (mFrameOuted) {
            // animation (frame in)
            fromX = 280.0f;
            toX = 0.0f;
            fromY = 280.0f;
            toY = 0.0f;
        } else {
            // animation (frame out)
            fromX = 0.0f;
            toX = 280.0f;
            fromY = 0.0f;
            toY = 280.0f;
        }
        mFrameOuted = !mFrameOuted;

        ObjectAnimator animatorTrans_Buttons = ObjectAnimator.ofFloat(mControlButtonsLayout, "translationY", fromY, toY);
        ObjectAnimator animatorTrans_SeekBar = ObjectAnimator.ofFloat(mSeekBarLayout, "translationY", fromY, toY);
        ObjectAnimator animatorTrans_Forward = ObjectAnimator.ofFloat(mForwardImageView, "translationX", fromX, toX);
        ObjectAnimator animatorTrans_Backward = ObjectAnimator.ofFloat(mBackwardImageView, "translationX", -1.0f * fromX, -1.0f * toX);
        ObjectAnimator animatorTrans_Gallery = ObjectAnimator.ofFloat(mGalleryImageView, "translationY", -1.0f * fromY, -1.0f * toY);

        animatorTrans_Buttons.setDuration(ANIMATOR_DURATION);
        animatorTrans_SeekBar.setDuration(ANIMATOR_DURATION);
        animatorTrans_Forward.setDuration(ANIMATOR_DURATION);
        animatorTrans_Backward.setDuration(ANIMATOR_DURATION);
        animatorTrans_Gallery.setDuration(ANIMATOR_DURATION);
        animatorList.add(animatorTrans_Buttons);
        animatorList.add(animatorTrans_SeekBar);
        animatorList.add(animatorTrans_Forward);
        animatorList.add(animatorTrans_Backward);
        animatorList.add(animatorTrans_Gallery);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorList);
        animatorSet.start();
    }
}
