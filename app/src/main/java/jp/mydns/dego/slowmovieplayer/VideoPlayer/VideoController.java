package jp.mydns.dego.slowmovieplayer.VideoPlayer;

import android.app.Activity;
import android.media.MediaMetadataRetriever;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;

import jp.mydns.dego.slowmovieplayer.R;
import jp.mydns.dego.slowmovieplayer.Util.DebugLog;
import jp.mydns.dego.slowmovieplayer.VideoSurfaceView;
import jp.mydns.dego.slowmovieplayer.ViewController;

public class VideoController {

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // public constant values
    // ---------------------------------------------------------------------------------------------
    public enum MANIPULATION {
        EXPAND_CONTRACT,
        FRAME_CONTROL
    }

    public static final double[] VIDEO_SPEED = {
        0.125, 0.25, 0.5, 1.0
    };

    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "VideoController";
    private static final int TOUCH_X_DIFF_SIZE = 50;
    private static final int SPEED_LEVEL_MAX = VIDEO_SPEED.length - 1;
    private static final int SPEED_LEVEL_MIN = 0;

    // ---------------------------------------------------------------------------------------------
    // private fields
    // ---------------------------------------------------------------------------------------------
    private ViewController viewController;
    private VideoRunnable player;
    private String filePath;
    private Thread videoThread;
    private long touchDownTime;
    private float touchStartX;
    private int touchXDiffLevelLast;
    private MANIPULATION manipulation;
    private int speedLevel;

    // ---------------------------------------------------------------------------------------------
    // static fields
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private static method
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // constructor
    // ---------------------------------------------------------------------------------------------

    /**
     * VideoController
     *
     * @param activity activity
     */
    public VideoController(final Activity activity) {
        DebugLog.d(TAG, "VideoController");
        this.viewController = new ViewController(activity);
        this.viewController.setVisibility(VideoRunnable.STATUS.INIT);
        this.player = VideoRunnable.getInstance();
        this.player.setVideoHandler(new VideoPlayerHandler(this.viewController));

        this.touchDownTime = 0;
        this.manipulation = MANIPULATION.EXPAND_CONTRACT;
        this.speedLevel = SPEED_LEVEL_MAX;  // x1.0
        this.viewController.setManipulation(this.manipulation);

        this.player.setOnVideoStatusChangeListener(new OnVideoStatusChangeListener() {
            @Override
            public void onDurationChanged(int duration) {
                DebugLog.d(TAG, "onDurationChanged");
                viewController.setDuration(duration);
            }
        });

        VideoSurfaceView surfaceView = (VideoSurfaceView) this.viewController.getView(R.id.player_surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                DebugLog.d(TAG, "surfaceCreated");
                setSurfaceViewSize();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                DebugLog.d(TAG, "surfaceChanged");
                videoSuspend(holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                DebugLog.d(TAG, "surfaceDestroyed");
                videoRelease();
            }
        });

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                DebugLog.d(TAG, "onTouch");
                if (!(view instanceof VideoSurfaceView)) {
                    return false;
                }

                // video pause
                if (player != null &&
                    player.getStatus() != VideoRunnable.STATUS.PAUSED &&
                    player.getStatus() != VideoRunnable.STATUS.FORWARD &&
                    player.getStatus() != VideoRunnable.STATUS.BACKWARD
                ) {
                    videoPause();
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchDownTime = System.nanoTime() / 1000 / 1000;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    long diffTime = System.nanoTime() / 1000 / 1000 - touchDownTime;
                    if (diffTime < 100) {
                        view.performClick();
                        viewController.animFullscreenPreview();
                        return true;
                    }
                }

                // touch process
                if (manipulation == MANIPULATION.EXPAND_CONTRACT) {
                    touchExpandContract((VideoSurfaceView) view, event);
                } else if (manipulation == MANIPULATION.FRAME_CONTROL && event.getPointerCount() == 1) {
                    touchFrameControl(event);
                }
                return true;
            }
        });

        SeekBar seekBar = (SeekBar) this.viewController.getView(R.id.seek_bar_progress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DebugLog.d(TAG, "onProgressChanged");
                if (fromUser) {
                    videoSeek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                DebugLog.d(TAG, "onStartTrackingTouch");
                videoPause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DebugLog.d(TAG, "onStopTrackingTouch");
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // public method
    // ---------------------------------------------------------------------------------------------

    /**
     * setVideoPath
     *
     * @param path video file path
     */
    public void setVideoPath(String path) {
        this.filePath = path;
        this.viewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED, this.speedLevel);
    }

    /**
     * videoPlay
     */
    public void videoPlay() {
        DebugLog.d(TAG, "videoPlay");
        if (this.player.getStatus() == VideoRunnable.STATUS.PAUSED) {
            // pause => play
            if (this.videoThread == null || !this.videoThread.isAlive()) {
                this.viewController.setVisibility(VideoRunnable.STATUS.PLAYING, this.speedLevel);
                this.videoThread = new Thread(this.player);
                this.videoThread.start();
            }
        } else if (this.player.getStatus() == VideoRunnable.STATUS.PLAYING) {
            // play => pause
            this.videoPause();
        } else if (this.player.getStatus() == VideoRunnable.STATUS.VIDEO_END) {
            this.player.release();
            this.player.prepare(this.filePath);
            this.viewController.setVisibility(VideoRunnable.STATUS.PLAYING, this.speedLevel);
            this.videoThread = new Thread(this.player);
            this.videoThread.start();
        }
    }

    /**
     * videoStop
     */
    public void videoStop() {
        DebugLog.d(TAG, "videoStop");
        if (this.videoThread.isAlive()) {
            try {
                DebugLog.d(TAG, "join");
                this.videoThread.interrupt();
                this.videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.player.release();
        this.player.prepare(this.filePath);
        this.player.setStatus(VideoRunnable.STATUS.VIDEO_SELECTED);
        this.viewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED, this.speedLevel);
        this.videoThread = new Thread(this.player);
        this.videoThread.start();
    }

    /**
     * videoForward
     */
    public void videoForward() {
        DebugLog.d(TAG, "videoForward");
        if (this.videoThread.isAlive()) {
            return;
        }
        if (this.player.getStatus() == VideoRunnable.STATUS.PAUSED) {
            this.player.setStatus(VideoRunnable.STATUS.FORWARD);
            this.viewController.setVisibility(VideoRunnable.STATUS.FORWARD, this.speedLevel);
            this.videoThread = new Thread(this.player);
            this.videoThread.start();
        }
    }

    /**
     * videoBackward
     */
    public void videoBackward() {
        DebugLog.d(TAG, "videoBackward");
        if (this.videoThread.isAlive()) {
            DebugLog.d(TAG, "Thread is alive.");
            return;
        }
        this.player.toPreviousKeyFrame();
        this.player.setStatus(VideoRunnable.STATUS.BACKWARD);
        this.viewController.setVisibility(VideoRunnable.STATUS.BACKWARD, this.speedLevel);
        this.videoThread = new Thread(this.player);
        this.videoThread.start();
    }

    /**
     * videoSpeedUp
     */
    public void videoSpeedUp() {
        DebugLog.d(TAG, "videoSpeedUp");

        this.speedLevel++;
        if (this.speedLevel > SPEED_LEVEL_MAX) {
            this.speedLevel = SPEED_LEVEL_MAX;
        }

        this.viewController.setPlaySpeedText(this.speedLevel);
        this.player.setSpeed(VIDEO_SPEED[this.speedLevel]);
    }

    /**
     * videoSpeedDown
     */
    public void videoSpeedDown() {
        DebugLog.d(TAG, "videoSpeedDown");

        this.speedLevel--;
        if (this.speedLevel < SPEED_LEVEL_MIN) {
            this.speedLevel = SPEED_LEVEL_MIN;
        }

        this.viewController.setPlaySpeedText(this.speedLevel);
        this.player.setSpeed(VIDEO_SPEED[this.speedLevel]);
    }

    /**
     * setManipulation
     *
     * @param manipulation 操作モード
     */
    public void setManipulation(MANIPULATION manipulation) {
        this.manipulation = manipulation;
        this.viewController.setManipulation(this.manipulation);
    }

    // ---------------------------------------------------------------------------------------------
    // private method (package private)
    // ---------------------------------------------------------------------------------------------

    /**
     * setSurfaceViewSize
     */
    private void setSurfaceViewSize() {
        if (this.filePath != null) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this.filePath);

            logMetaData(retriever);

            int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            this.viewController.setSurfaceViewSize(width, height, rotation);
        }
    }

    /**
     * videoSuspend
     *
     * @param holder surface holder
     */
    private void videoSuspend(SurfaceHolder holder) {
        if (this.filePath != null) {
            if (this.videoThread != null && this.videoThread.isAlive()) {
                try {
                    this.videoThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }

            // 再初期化
            if (this.player.getStatus() == VideoRunnable.STATUS.INIT ||
                this.player.getStatus() == VideoRunnable.STATUS.VIDEO_SELECTED) {
                this.player.init(this.filePath, holder.getSurface());
                this.speedLevel = SPEED_LEVEL_MAX;
                this.viewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED, this.speedLevel);
                this.viewController.setPlaySpeedText(this.speedLevel);
                this.videoThread = new Thread(this.player);
                this.videoThread.start();
            }
        }
    }

    /**
     * videoRelease
     */
    private void videoRelease() {
        if (this.filePath != null) {
            this.player.setStatus(VideoRunnable.STATUS.PAUSED);
            if (this.videoThread != null && videoThread.isAlive()) {
                try {
                    DebugLog.d(TAG, "join");
                    this.videoThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.player.release();
            this.player.setStatus(VideoRunnable.STATUS.VIDEO_SELECTED);
        }
    }

    /**
     * touchExpandContract
     *
     * @param view        video surface view
     * @param motionEvent motion event
     */
    private void touchExpandContract(VideoSurfaceView view, MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() == 1) {
            DebugLog.d(TAG, "move");
            view.move(motionEvent);
        } else if (motionEvent.getPointerCount() == 2) {
            DebugLog.d(TAG, "scale");
            view.setGestureMotionEvent(motionEvent);
        }
    }

    /**
     * touchFrameControl
     *
     * @param motionEvent motion event
     */
    private void touchFrameControl(MotionEvent motionEvent) {
        int touchXDiffLevel;
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            this.touchStartX = motionEvent.getX();
            this.touchXDiffLevelLast = 0;
            DebugLog.d(TAG, "Diff Level : " + this.touchXDiffLevelLast);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            int diffX = (int) (motionEvent.getX() - this.touchStartX);
            DebugLog.d(TAG, "diff x : " + diffX);

            touchXDiffLevel = diffX / TOUCH_X_DIFF_SIZE;
            DebugLog.d(TAG, "Diff Level : " + this.touchXDiffLevelLast + " => " + touchXDiffLevel);
            if (this.player != null && player.getStatus() == VideoRunnable.STATUS.PAUSED) {
                if (this.touchXDiffLevelLast < touchXDiffLevel) {
                    videoForward();
                } else if (this.touchXDiffLevelLast > touchXDiffLevel) {
                    videoBackward();
                }
                this.touchXDiffLevelLast = touchXDiffLevel;
            }
        }
    }

    /**
     * videoPause
     */
    private void videoPause() {
        DebugLog.d(TAG, "videoPause");
        if (this.videoThread.isAlive()) {
            DebugLog.d(TAG, "interrupt");
            this.videoThread.interrupt();
        }
        this.player.setStatus(VideoRunnable.STATUS.PAUSED);
        this.viewController.setVisibility(VideoRunnable.STATUS.PAUSED, this.speedLevel);
    }

    /**
     * videoSeek
     *
     * @param progress video progress
     */
    private void videoSeek(int progress) {
        DebugLog.d(TAG, "videoSeek");
        if (this.player.getStatus() == VideoRunnable.STATUS.SEEKING) {
            return;
        }
        if (this.videoThread.isAlive()) {
            try {
                DebugLog.d(TAG, "join");
                this.videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.player.seekTo(progress);
        this.player.setStatus(VideoRunnable.STATUS.SEEKING);
        this.viewController.setVisibility(VideoRunnable.STATUS.SEEKING, this.speedLevel);
        this.videoThread = new Thread(this.player);
        this.videoThread.start();
    }

    /**
     * logMetaData
     *
     * @param retriever media meta data retriever
     */
    private void logMetaData(MediaMetadataRetriever retriever) {
        DebugLog.d(TAG, "logMetaData");

        DebugLog.d(TAG, "==================================================");
        DebugLog.d(TAG, "has audio  :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
        DebugLog.d(TAG, "has video  :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
        DebugLog.d(TAG, "date       :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
        DebugLog.d(TAG, "width      :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        DebugLog.d(TAG, "height     :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        DebugLog.d(TAG, "duration   :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        DebugLog.d(TAG, "rotation   :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        DebugLog.d(TAG, "num tracks :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
        DebugLog.d(TAG, "title      :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        DebugLog.d(TAG, "==================================================");
    }

}