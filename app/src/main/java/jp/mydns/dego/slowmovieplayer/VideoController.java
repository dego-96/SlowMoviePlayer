package jp.mydns.dego.slowmovieplayer;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;

class VideoController {

    private static final String TAG = "VideoController";
    private static final int TOUCH_DETECT_TIME_MAX = 200;

    private ViewController mViewController;
    private VideoRunnable mPlayer;
    private String mFilePath;
    private Thread mThread;
    private long mTouchDownTime;

    /**
     * VideoController
     *
     * @param aActivity activity
     */
    VideoController(final Activity aActivity) {
        Log.d(TAG, "VideoController");
        mViewController = new ViewController(aActivity);
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
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
                if (mFilePath != null) {
                    mPlayer.init(mFilePath, holder.getSurface());
                    mViewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
                    mThread = new Thread(mPlayer);
                    mThread.start();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
            }
        });
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
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
    }

    /**
     * videoPlay
     */
    void videoPlay() {
        Log.d(TAG, "videoPlay");
        if (mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED) {
            // pause => play
            if (mThread == null || !mThread.isAlive()) {
                mPlayer.setStatus(VideoRunnable.STATUS.PLAYING);
                mViewController.setVisibility(VideoRunnable.STATUS.PLAYING);
                mThread = new Thread(mPlayer);
                mThread.start();
            }
        } else if (mPlayer.getStatus() == VideoRunnable.STATUS.PLAYING) {
            // play => pause
            videoPause();
        } else if (mPlayer.getStatus() == VideoRunnable.STATUS.STOPPED) {
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
        mPlayer.setStatus(VideoRunnable.STATUS.STOPPED);
        mViewController.setVisibility(VideoRunnable.STATUS.STOPPED);
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
            try {
                Log.d(TAG, "join");
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPlayer.setStatus(VideoRunnable.STATUS.FORWARD);
        mThread = new Thread(mPlayer);
        mThread.start();
    }

    /**
     * videoBackward
     */
    void videoBackward() {
        Log.d(TAG, "videoBackward");
        if (mThread.isAlive()) {
            try {
                Log.d(TAG, "join");
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPlayer.backward();
        mPlayer.setStatus(VideoRunnable.STATUS.BACKWARD);
        mThread = new Thread(mPlayer);
        mThread.start();
    }
}
