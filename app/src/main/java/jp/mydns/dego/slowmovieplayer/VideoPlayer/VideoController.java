package jp.mydns.dego.slowmovieplayer.VideoPlayer;

import android.app.Activity;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;

import jp.mydns.dego.slowmovieplayer.R;
import jp.mydns.dego.slowmovieplayer.VideoSurfaceView;
import jp.mydns.dego.slowmovieplayer.ViewController;

public class VideoController {

    public enum MANIPULATION {
        EXPAND_CONTRACT,
        FRAME_CONTROL
    }

    private static final String TAG = "VideoController";
    private static final int TOUCH_X_DIFF_SIZE = 50;
    private static final double SPEED_MAX = 4.0;
    private static final double SPEED_MIN = 1.0 / 4.0;

    private ViewController mViewController;
    private VideoRunnable mPlayer;
    private String mFilePath;
    private Thread mThread;
    private long mTouchDownTime;
    private float mTouchStartX;
    private int mTouchXDiffLevel;
    private int mTouchXDiffLevelLast;
    private MANIPULATION mManipulation;

    /**
     * VideoController
     *
     * @param aActivity activity
     */
    public VideoController(final Activity aActivity) {
        Log.d(TAG, "VideoController");
        mViewController = new ViewController(aActivity);
        mViewController.setVisibility(VideoRunnable.STATUS.INIT);
        mPlayer = VideoRunnable.getInstance();
        mPlayer.setVideoHandler(new VideoPlayerHandler(mViewController));

        mTouchDownTime = 0;
        mManipulation = MANIPULATION.EXPAND_CONTRACT;
        mViewController.setManipulation(mManipulation);

        mPlayer.setOnVideoStatusChangeListener(new OnVideoStatusChangeListener() {
            @Override
            public void onDurationChanged(int duration) {
                Log.d(TAG, "onDurationChanged");
                mViewController.setDuration(duration);
            }
        });

        final VideoSurfaceView surfaceView = (VideoSurfaceView) mViewController.getView(R.id.player_surface_view);
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
                        mViewController.setPlaybackSpeedText(mPlayer.getSpeed());
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
                if (!(aView instanceof VideoSurfaceView)) {
                    return false;
                }

                // video pause
                if (mPlayer != null &&
                        mPlayer.getStatus() != VideoRunnable.STATUS.PAUSED &&
                        mPlayer.getStatus() != VideoRunnable.STATUS.FORWARD &&
                        mPlayer.getStatus() != VideoRunnable.STATUS.BACKWARD
                ) {
                    videoPause();
                }

                // click animation
                if (aMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mTouchDownTime = System.nanoTime() / 1000 / 1000;
                } else if (aMotionEvent.getAction() == MotionEvent.ACTION_UP) {
                    long diffTime = System.nanoTime() / 1000 / 1000 - mTouchDownTime;
                    if (diffTime < 100) {
                        aView.performClick();
                        mViewController.animFullscreenPreview();
                        return true;
                    }
                }

                // touch process
                if (mManipulation == MANIPULATION.EXPAND_CONTRACT) {
                    if (aMotionEvent.getPointerCount() == 1) {
                        Log.d(TAG, "move");
                        ((VideoSurfaceView) aView).move(aMotionEvent);
                    } else if (aMotionEvent.getPointerCount() == 2) {
                        Log.d(TAG, "scale");
                        ((VideoSurfaceView) aView).setGestureMotionEvent(aMotionEvent);
                    }
                } else if (mManipulation == MANIPULATION.FRAME_CONTROL &&
                        aMotionEvent.getPointerCount() == 1) {
                    if (aMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        mTouchStartX = aMotionEvent.getX();
                        mTouchXDiffLevel = 0;
                        mTouchXDiffLevelLast = 0;
                        Log.d(TAG, "Diff Level : " + mTouchXDiffLevelLast);
                    } else if (aMotionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        int diffX = (int) (aMotionEvent.getX() - mTouchStartX);
                        Log.d(TAG, "diff x : " + diffX);

                        mTouchXDiffLevel = diffX / TOUCH_X_DIFF_SIZE;
                        Log.d(TAG, "Diff Level : " + mTouchXDiffLevelLast + " => " + mTouchXDiffLevel);
                        if (mPlayer != null && mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED) {
                            if (mTouchXDiffLevelLast < mTouchXDiffLevel) {
                                videoForward();
                            } else if (mTouchXDiffLevelLast > mTouchXDiffLevel) {
                                videoBackward();
                            }
                            mTouchXDiffLevelLast = mTouchXDiffLevel;
                        }
                    }
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
    public void setVideoPath(String aPath) {
        mFilePath = aPath;
        mViewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
    }

    /**
     * videoPlayback
     */
    public void videoPlayback() {
        Log.d(TAG, "videoPlayback");
        if (mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED) {
            // pause => playback
            if (mThread == null || !mThread.isAlive()) {
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
            Log.d(TAG, "interrupt");
            mThread.interrupt();
        }
        mPlayer.setStatus(VideoRunnable.STATUS.PAUSED);
        mViewController.setVisibility(VideoRunnable.STATUS.PAUSED);

    }

    /**
     * videoStop
     */
    public void videoStop() {
        Log.d(TAG, "videoStop");
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
        mViewController.setVisibility(VideoRunnable.STATUS.SEEKING);
        mThread = new Thread(mPlayer);
        mThread.start();
    }

    /**
     * videoForward
     */
    public void videoForward() {
        Log.d(TAG, "videoForward");
        if (mThread.isAlive()) {
            return;
        }
        if (mPlayer.getStatus() == VideoRunnable.STATUS.PAUSED) {
            mPlayer.setStatus(VideoRunnable.STATUS.FORWARD);
            mViewController.setVisibility(VideoRunnable.STATUS.FORWARD);
            mThread = new Thread(mPlayer);
            mThread.start();
        }
    }

    /**
     * videoBackward
     */
    public void videoBackward() {
        Log.d(TAG, "videoBackward");
        if (mThread.isAlive()) {
            Log.d(TAG, "Thread is alive.");
            return;
        }
        mPlayer.backward();
        mPlayer.setStatus(VideoRunnable.STATUS.BACKWARD);
        mViewController.setVisibility(VideoRunnable.STATUS.BACKWARD);
        mThread = new Thread(mPlayer);
        mThread.start();
    }

    /**
     * videoSpeedUp
     */
    public void videoSpeedUp() {
        Log.d(TAG, "videoSpeedUp");

        double speed = mPlayer.getSpeed() * 2.0;
        if (speed > SPEED_MAX) {
            speed = SPEED_MAX;
        }

        mViewController.setPlaybackSpeedText(speed);
        mPlayer.setSpeed(speed);
    }

    /**
     * videoSpeedDown
     */
    public void videoSpeedDown() {
        Log.d(TAG, "videoSpeedDown");

        double speed = mPlayer.getSpeed() / 2.0;
        if (speed < SPEED_MIN) {
            speed = SPEED_MIN;
        }

        mViewController.setPlaybackSpeedText(speed);
        mPlayer.setSpeed(speed);
    }

    /**
     * setManipulation
     *
     * @param aManipulation 操作モード
     */
    public void setManipulation(MANIPULATION aManipulation) {
        mManipulation = aManipulation;
        mViewController.setManipulation(mManipulation);
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
