package jp.mydns.dego.slowmovieplayer;

import android.app.Activity;
import android.media.MediaMetadataRetriever;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;

class VideoController {

//    private static final String TAG = "VideoController";
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
        mViewController = new ViewController(aActivity);
        mViewController.setVisibility(VideoRunnable.STATUS.INIT);
        mPlayer = new VideoRunnable(new VideoPlayerHandler(mViewController));

        mPlayer.setOnVideoStatusChangeListener(new OnVideoStatusChangeListener() {
            @Override
            public void onDurationChanged(int duration) {
                mViewController.setDuration(duration);
            }
        });

        VideoSurfaceView surfaceView = (VideoSurfaceView) mViewController.getView(R.id.player_surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mFilePath != null) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mFilePath);

                    int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                    mViewController.setSurfaceViewSize(width, height, rotation);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
                        mThread = new Thread(mPlayer);
                        mThread.start();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mFilePath != null) {
                    mPlayer.setStatus(VideoRunnable.STATUS.PAUSED);
                    if (mThread.isAlive()) {
                        try {
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

                if (aMotionEvent.getPointerCount() != 1) {
                    return false;
                }

                switch (aMotionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchDownTime = System.nanoTime() / 1000 / 1000;
                        mTouchStartX = aMotionEvent.getX();
                        mTouchXDiffLevel = 0;
                        mTouchXDiffLevelLast = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float touchX = aMotionEvent.getX();
                        int diffX = (int) (touchX - mTouchStartX);

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
                        aView.performClick();
                        if (mPlayer != null &&
                                mPlayer.getStatus() != VideoRunnable.STATUS.INIT &&
                                (System.nanoTime() / 1000 / 1000) - mTouchDownTime < TOUCH_DETECT_TIME_MAX) {
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
                if (fromUser) {
                    videoSeek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                videoPause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
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
        mPlayer.setStatus(VideoRunnable.STATUS.PAUSED);
        if (mThread.isAlive()) {
            try {
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
        if (mPlayer.getStatus() == VideoRunnable.STATUS.SEEKING) {
            return;
        }
        if (mThread.isAlive()) {
            try {
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
}
