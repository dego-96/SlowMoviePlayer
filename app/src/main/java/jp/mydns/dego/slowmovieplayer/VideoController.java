package jp.mydns.dego.slowmovieplayer;

import android.app.Activity;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;

class VideoController {

    private static final String TAG = "VideoController";
    private static final int TOUCH_DETECT_TIME_MAX = 200;
    private static final int TOUCH_X_DIFF_SIZE = 50;

    private ViewController mViewController;
    private VideoRunnable mPlayer;
    private String mFilePath;
    private Thread mThread;
    private long mTouchDownTime;
    private float mTouchStartX;
    private int mTouchXDiffLevel;
    private int mTouchXDiffLevelLast;

    /**
     * VideoController
     *
     * @param aActivity activity
     */
    VideoController(final Activity aActivity) {
        Log.d(TAG, "VideoController");
        mViewController = new ViewController(aActivity);
        mViewController.setVisibility(VideoRunnable.STATUS.INIT);
        mPlayer = new VideoRunnable(new VideoPlayerHandler(mViewController));

        mPlayer.setOnVideoStatusChangeListener(new OnVideoStatusChangeListener() {
            @Override
            public void onDurationChanged(int duration) {
                Log.d(TAG, "onDurationChanged");
                mViewController.setDuration(duration);
            }
        });

        VideoSurfaceView surfaceView = (VideoSurfaceView) mViewController.getView(R.id.player_surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                if (mFilePath != null) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mFilePath);

                    logMetaData(retriever);

                    int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                    mViewController.setSurfaceViewSize(width, height, rotation);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
                if (mFilePath != null) {
                    if (mThread != null && mThread.isAlive()) {
                        try {
                            mThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (mPlayer.getStatus() == VideoRunnable.STATUS.INIT ||
                            mPlayer.getStatus() == VideoRunnable.STATUS.VIDEO_SELECTED) {
                        mPlayer.init(mFilePath, holder.getSurface());
                        mViewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
                        mViewController.setPlaybackSpeed(mPlayer.getSpeed());
                        mThread = new Thread(mPlayer);
                        mThread.start();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
                if (mFilePath != null) {
                    mPlayer.setStatus(VideoRunnable.STATUS.PAUSED);
                    if (mThread.isAlive()) {
                        try {
                            Log.d(TAG, "join");
                            mThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mPlayer.release();
                    mPlayer.setStatus(VideoRunnable.STATUS.VIDEO_SELECTED);
                }
            }
        });
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View aView, MotionEvent aMotionEvent) {
                Log.d(TAG, "onTouch");

                if (aMotionEvent.getPointerCount() != 1) {
                    return false;
                }

                switch (aMotionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "ACTION_DOWN");
                        mTouchDownTime = System.nanoTime() / 1000 / 1000;
                        mTouchStartX = aMotionEvent.getX();
                        mTouchXDiffLevel = 0;
                        mTouchXDiffLevelLast = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "ACTION_MOVE");
                        float touchX = aMotionEvent.getX();
                        int diffX = (int) (touchX - mTouchStartX);
                        Log.d(TAG, "diff x : " + diffX);

                        mTouchXDiffLevel = diffX / TOUCH_X_DIFF_SIZE;
                        if (mPlayer != null && mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED &&
                                mTouchXDiffLevelLast != mTouchXDiffLevel) {
                            if (mTouchXDiffLevelLast < mTouchXDiffLevel) {
                                videoForward();
                                mTouchXDiffLevelLast = mTouchXDiffLevel;
                            }
                            if (mTouchXDiffLevelLast > mTouchXDiffLevel) {
                                videoBackward();
                            }
                            mTouchXDiffLevelLast = mTouchXDiffLevel;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "ACTION_UP");
                        aView.performClick();
                        if (mPlayer != null &&
                                mPlayer.getStatus() != VideoRunnable.STATUS.INIT &&
                                (System.nanoTime() / 1000 / 1000) - mTouchDownTime < TOUCH_DETECT_TIME_MAX) {
                            Log.d(TAG, "player status :" + mPlayer.getStatus().name());
                            mViewController.animFullscreenPreview();
                        }
                        break;
                }
                return true;
            }
        });

        SeekBar seekBar = (SeekBar) mViewController.getView(R.id.seek_bar_progress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged");
                if (fromUser) {
                    videoSeek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
                videoPause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
            }
        });
    }

    /**
     * setVideoPath
     *
     * @param aPath video file path
     */
    void setVideoPath(String aPath) {
        mFilePath = aPath;
        mViewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
    }

    /**
     * videoPlayback
     */
    void videoPlayback() {
        Log.d(TAG, "videoPlayback");
        if (mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED) {
            // pause => playback
            if (mThread == null || !mThread.isAlive()) {
                mPlayer.setStatus(VideoRunnable.STATUS.PLAYING);
                mViewController.setVisibility(VideoRunnable.STATUS.PLAYING);
                mThread = new Thread(mPlayer);
                mThread.start();
            }
        } else if (mPlayer.getStatus() == VideoRunnable.STATUS.PLAYING) {
            // playback => pause
            videoPause();
        } else if (mPlayer.getStatus() == VideoRunnable.STATUS.VIDEO_END) {
            mPlayer.release();
            mPlayer.prepare(mFilePath);
            mPlayer.setStatus(VideoRunnable.STATUS.PLAYING);
            mViewController.setVisibility(VideoRunnable.STATUS.PLAYING);
            mThread = new Thread(mPlayer);
            mThread.start();
        }
    }

    /**
     * videoPause
     */
    private void videoPause() {
        Log.d(TAG, "videoPause");
        if (mThread.isAlive()) {
            mThread.interrupt();
        }
        mPlayer.setStatus(VideoRunnable.STATUS.PAUSED);
        mViewController.setVisibility(VideoRunnable.STATUS.PAUSED);
    }

    /**
     * videoStop
     */
    void videoStop() {
        Log.d(TAG, "videoStop");
        mPlayer.setStatus(VideoRunnable.STATUS.PAUSED);
        if (mThread.isAlive()) {
            try {
                Log.d(TAG, "join");
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPlayer.release();
        mPlayer.prepare(mFilePath);
        mPlayer.setStatus(VideoRunnable.STATUS.VIDEO_SELECTED);
        mViewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
        mThread = new Thread(mPlayer);
        mThread.start();
    }

    /**
     * videoSeek
     *
     * @param aProgress video progress
     */
    private void videoSeek(int aProgress) {
        Log.d(TAG, "videoSeek");
        if (mPlayer.getStatus() == VideoRunnable.STATUS.SEEKING) {
            return;
        }
        if (mThread.isAlive()) {
            try {
                Log.d(TAG, "join");
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPlayer.seekTo(aProgress);
        mPlayer.setStatus(VideoRunnable.STATUS.SEEKING);
        mThread = new Thread(mPlayer);
        mThread.start();
    }

    /**
     * videoForward
     */
    void videoForward() {
        Log.d(TAG, "videoForward");
        if (mThread.isAlive()) {
            return;
        }
        if (mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED) {
            mPlayer.setStatus(VideoRunnable.STATUS.FORWARD);
            mThread = new Thread(mPlayer);
            mThread.start();
        }
    }

    /**
     * videoBackward
     */
    void videoBackward() {
        Log.d(TAG, "videoBackward");
        if (mThread.isAlive()) {
            return;
        }
        mPlayer.backward();
        mPlayer.setStatus(VideoRunnable.STATUS.BACKWARD);
        mThread = new Thread(mPlayer);
        mThread.start();
    }

    /**
     * videoSpeedUp
     */
    void videoSpeedUp() {
        Log.d(TAG, "videoSpeedUp");

        int speed = mPlayer.getSpeed();
        switch (speed) {
            case -4:
                speed = -2;
                break;
            case -2:
                speed = 0;
                break;
            case 0:
                speed = 2;
                break;
            case 2:
                speed = 4;
                break;
            case 4:
                break;
        }
        mViewController.setPlaybackSpeed(speed);
        mPlayer.setSpeed(speed);
    }

    /**
     * videoSpeedDown
     */
    void videoSpeedDown() {
        Log.d(TAG, "videoSpeedDown");

        int speed = mPlayer.getSpeed();
        switch (speed) {
            case -4:
                break;
            case -2:
                speed = -4;
                break;
            case 0:
                speed = -2;
                break;
            case 2:
                speed = 0;
                break;
            case 4:
                speed = 2;
                break;
        }
        mViewController.setPlaybackSpeed(speed);
        mPlayer.setSpeed(speed);
    }

    /**
     * logMetaData
     *
     * @param aRetriever media meta data retriever
     */
    private void logMetaData(MediaMetadataRetriever aRetriever) {
        Log.d(TAG, "logMetaData");

        Log.d(TAG, "==================================================");
        Log.d(TAG, "has audio  :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
        Log.d(TAG, "has video  :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
        Log.d(TAG, "date       :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
        Log.d(TAG, "width      :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        Log.d(TAG, "height     :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        Log.d(TAG, "duration   :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Log.d(TAG, "rotation   :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        Log.d(TAG, "num tracks :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
        Log.d(TAG, "title      :" + aRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        Log.d(TAG, "==================================================");
    }

}
