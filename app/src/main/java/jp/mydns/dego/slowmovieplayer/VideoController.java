package jp.mydns.dego.slowmovieplayer;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
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

class VideoController {

    private static final String TAG = "VideoController";
    private static final int ANIMATOR_DURATION = 300;
    private static final int TOUCH_DETECT_TIME_MAX = 200;

    private Activity mActivity;
    private VideoPlayer mPlayer;

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("mm:ss:SSS", Locale.JAPAN);

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
    private TextView mCurrentTimeTextView;
    private TextView mRemainTimeTextView;

    /* Layout */
    private LinearLayout mControlButtonsLayout;
    private LinearLayout mSeekBarLayout;

    private boolean mFrameOuted;
    private long mTouchDownTime;

    /**
     * VideoController
     *
     * @param aActivity activity
     */
    VideoController(final Activity aActivity) {
        Log.d(TAG, "VideoController");
        mActivity = aActivity;
        mPlayer = new VideoPlayer();
        mPlayer.setVideoStatusChangeListener(new OnVideoStatusChangeListener() {

            @Override
            public void onPlayerStatusChanged(VideoPlayer aPlayer) {
                Log.d(TAG, "onPlayerStatusChanged(" + aPlayer.getPlayerStatus().name() + ")");
                updateVisibility();
            }

            @Override
            public void onProgressChanged(int aProgress) {
                Log.d(TAG, "onProgressChanged( " + aProgress + " )");

                mSeekBar.setProgress(aProgress);

                mCurrentTimeTextView.setText(sDateFormat.format(new Date(aProgress)));
                int duration = mSeekBar.getMax();
                if (aProgress < duration) {
                    mRemainTimeTextView.setText(sDateFormat.format(new Date(duration - aProgress)));
                } else {
                    mRemainTimeTextView.setText(mActivity.getString(R.string.remain_time_init));
                }
            }

            @Override
            public void onDurationChanged(int aDuration) {
                Log.d(TAG, "onDurationChanged");
                mSeekBar.setMax(aDuration);
            }

            @Override
            public void onPlayToEnd() {
                Log.d(TAG, "onPlayToEnd");
                updateVisibility();
            }
        });

        mSurfaceView = aActivity.findViewById(R.id.player_surface_view);
        mSurfaceView.setPlayer(mPlayer);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View aView, MotionEvent aMotionEvent) {
                Log.d(TAG, "onTouch");

                switch (aMotionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "ACTION_DOWN");
                        mTouchDownTime = System.nanoTime() / 1000 / 1000;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "ACTION_MOVE");
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "ACTION_UP");
                        aView.performClick();
                        if (mPlayer != null &&
                                mPlayer.getPlayerStatus() != null &&
                                mPlayer.getPlayerStatus() != VideoPlayer.PLAYER_STATUS.INITIALIZED &&
                                (System.nanoTime() / 1000 / 1000) - mTouchDownTime < TOUCH_DETECT_TIME_MAX) {
                            Log.d(TAG, "player status :" + mPlayer.getPlayerStatus().name());
                            fullScreenAnimationStart();
                        }
                        break;
                }
                return true;
            }
        });

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

        mControlButtonsLayout = aActivity.findViewById(R.id.layout_control_buttons);
        mSeekBarLayout = aActivity.findViewById(R.id.layout_seek_bar);

        mSeekBar.setOnSeekBarChangeListener(new VideoSeekBarChangeListener(mPlayer));

        mFrameOuted = false;

        updateVisibility();
    }

    void setVisibility(VideoPlayer.PLAYER_STATUS aStatus) {
        Log.d(TAG, "setVisibility( " + aStatus.name() + " )");
        switch (aStatus) {
            case INITIALIZED:
                mNoVideoImageView.setVisibility(View.VISIBLE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.GONE);
                mForwardImageView.setVisibility(View.GONE);
                mPlayImageView.setVisibility(View.GONE);
                mStopImageView.setVisibility(View.GONE);
                mSpeedUpImageView.setVisibility(View.GONE);
                mSpeedDownImageView.setVisibility(View.GONE);
                mSeekBar.setVisibility(View.GONE);
                mCurrentTimeTextView.setVisibility(View.GONE);
                mRemainTimeTextView.setVisibility(View.GONE);
                break;
            case PLAYING:
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.INVISIBLE);
                mBackwardImageView.setVisibility(View.INVISIBLE);
                mForwardImageView.setVisibility(View.INVISIBLE);
                mPlayImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setImageResource(R.drawable.pause);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.VISIBLE);
                mSpeedDownImageView.setVisibility(View.VISIBLE);
                mSeekBar.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
            case STOPPED:
            case FORWARD:
            case BACKWARD:
            case SEEK_RENDER_START:
            case SEEK_RENDER_FINISH:
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
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            default:
                Log.e(TAG, "invalid player status :" + mPlayer.getPlayerStatus());
                break;
        }
    }

    /**
     * setVisibility
     */
    private void updateVisibility() {
        Log.d(TAG, "updateVisibility");
        if (mPlayer.getPlayerStatus() != null) {
            setVisibility(mPlayer.getPlayerStatus());
        } else {
            setVisibility(VideoPlayer.PLAYER_STATUS.INITIALIZED);
        }
    }

    /**
     * setVideoPath
     *
     * @param aPath video file path
     */
    void setVideoPath(String aPath) {
        mSurfaceView.setVideoPath(aPath);
    }

    /**
     * videoPlay
     */
    void videoPlay() {
        Log.d(TAG, "videoPlay");
        if (mPlayer != null) {
            if (mPlayer.getPlayerStatus() == VideoPlayer.PLAYER_STATUS.PLAYING) {
                mPlayer.pause();
            } else if (mPlayer.getPlayerStatus() == VideoPlayer.PLAYER_STATUS.PAUSED ||
                    mPlayer.getPlayerStatus() == VideoPlayer.PLAYER_STATUS.SEEK_RENDER_FINISH) {
                mPlayer.play();
            }
        }
    }

    /**
     * videoStop
     */
    void videoStop() {
        Log.d(TAG, "videoStop");
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    /**
     * videoForward
     */
    void videoForward() {
        Log.d(TAG, "videoForward");
        if (mPlayer != null) {
            mPlayer.forward();
        }
    }

    /**
     * videoBackward
     */
    void videoBackward() {
        Log.d(TAG, "videoBackward");
        if (mPlayer != null) {
            mPlayer.backward();
        }
    }

    /**
     * fullScreenAnimationStart
     */
    private void fullScreenAnimationStart() {
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
