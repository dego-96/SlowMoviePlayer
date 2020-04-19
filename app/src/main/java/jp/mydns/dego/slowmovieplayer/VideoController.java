package jp.mydns.dego.slowmovieplayer;

import android.app.Activity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class VideoController {

    private static final String TAG = "VideoController";

    private Activity mActivity;
    private VideoPlayer mPlayer;
    private String mVideoPath;

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("mm:ss:SSS", Locale.JAPAN);

    /* Views */
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

    /**
     * VideoController
     *
     * @param aActivity activity
     */
    VideoController(final Activity aActivity) {
        Log.d(TAG, "VideoController");
        mActivity = aActivity;
        mVideoPath = null;
        mPlayer = new VideoPlayer();
        mPlayer.setVideoStatusChangeListener(new OnVideoStatusChangeListener() {
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
                setVisibility(VideoPlayer.PLAYER_STATUS.PAUSED);
            }
        });

        SurfaceView surfaceView = aActivity.findViewById(R.id.player_surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
                if (null != mVideoPath && !"".equals(mVideoPath) && null != mPlayer) {
                    Log.d(TAG, "video path: " + mVideoPath);
                    mPlayer.init(holder.getSurface(), mVideoPath);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
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

        mSeekBar.setOnSeekBarChangeListener(new VideoSeekBarChangeListener(mPlayer));

        this.setVisibility(VideoPlayer.PLAYER_STATUS.INITIALIZED);
    }

    /**
     * setVisibility
     *
     * @param aPlayerStatus video player status
     */
    void setVisibility(VideoPlayer.PLAYER_STATUS aPlayerStatus) {
        Log.d(TAG, "setVisibility( " + aPlayerStatus.name() + " )");
        switch (aPlayerStatus) {
            case INITIALIZED:
                mNoVideoImageView.setVisibility(View.VISIBLE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.GONE);
                mForwardImageView.setVisibility(View.GONE);
                mPlayImageView.setVisibility(View.GONE);
                mStopImageView.setVisibility(View.GONE);
                mSpeedUpImageView.setVisibility(View.GONE);     // no support
                mSpeedDownImageView.setVisibility(View.GONE);   // no support
                mSeekBar.setVisibility(View.GONE);
                mCurrentTimeTextView.setVisibility(View.GONE);
                mRemainTimeTextView.setVisibility(View.GONE);
                break;
            case PLAYING:
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.INVISIBLE);
                mBackwardImageView.setVisibility(View.GONE);    // no support
                mForwardImageView.setVisibility(View.INVISIBLE);
                mPlayImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setImageResource(R.drawable.pause);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.GONE);     // no support
                mSpeedDownImageView.setVisibility(View.GONE);   // no support
                mSeekBar.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
            case STOPPED:
            case FORWARD:
            case BACKWARD:
            case SEEKING:
            case SEEK_FINISHED:
                mNoVideoImageView.setVisibility(View.GONE);
                mGalleryImageView.setVisibility(View.VISIBLE);
                mBackwardImageView.setVisibility(View.GONE);    // no support
                mForwardImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setVisibility(View.VISIBLE);
                mPlayImageView.setImageResource(R.drawable.play);
                mStopImageView.setVisibility(View.VISIBLE);
                mSpeedUpImageView.setVisibility(View.GONE);     // no support
                mSpeedDownImageView.setVisibility(View.GONE);   // no support
                mSeekBar.setVisibility(View.VISIBLE);
                mCurrentTimeTextView.setVisibility(View.VISIBLE);
                mRemainTimeTextView.setVisibility(View.VISIBLE);
                break;
            default:
                Log.e(TAG, "invalid player status :" + aPlayerStatus);
                break;
        }
    }

    /**
     * setVideoPath
     *
     * @param aPath video file path
     */
    void setVideoPath(String aPath) {
        mVideoPath = aPath;
    }

    /**
     * videoPlay
     */
    void videoPlay() {
        Log.d(TAG, "videoPlay");
        if (mPlayer != null) {
            if (mPlayer.getPlayerStatus() == VideoPlayer.PLAYER_STATUS.PLAYING) {
                this.setVisibility(VideoPlayer.PLAYER_STATUS.PAUSED);
                mPlayer.pause();
            } else if (mPlayer.getPlayerStatus() == VideoPlayer.PLAYER_STATUS.PAUSED) {
                this.setVisibility(VideoPlayer.PLAYER_STATUS.PLAYING);
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
            this.setVisibility(VideoPlayer.PLAYER_STATUS.STOPPED);
            mPlayer.stop();
        }
    }

    /**
     * videoForward
     */
    void videoForward() {
        Log.d(TAG, "videoForward");
        if (mPlayer != null) {
            this.setVisibility(VideoPlayer.PLAYER_STATUS.FORWARD);
            mPlayer.forward();
        }
    }
}
