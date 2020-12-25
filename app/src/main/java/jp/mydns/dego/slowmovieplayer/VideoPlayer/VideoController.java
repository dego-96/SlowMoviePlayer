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

    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "VideoController";
    private static final int TOUCH_X_DIFF_SIZE = 50;
    private static final double SPEED_MAX = 4.0;
    private static final double SPEED_MIN = 1.0 / 4.0;

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
        Log.d(TAG, "VideoController");
        this.viewController = new ViewController(activity);
        this.viewController.setVisibility(VideoRunnable.STATUS.INIT);
        this.player = VideoRunnable.getInstance();
        this.player.setVideoHandler(new VideoPlayerHandler(this.viewController));

        this.touchDownTime = 0;
        this.manipulation = MANIPULATION.EXPAND_CONTRACT;
        this.viewController.setManipulation(this.manipulation);

        this.player.setOnVideoStatusChangeListener(new OnVideoStatusChangeListener() {
            @Override
            public void onDurationChanged(int duration) {
                Log.d(TAG, "onDurationChanged");
                viewController.setDuration(duration);
            }
        });

        VideoSurfaceView surfaceView = (VideoSurfaceView) this.viewController.getView(R.id.player_surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                setSurfaceViewSize();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
                videoSuspend(holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
                videoRelease();
            }
        });

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Log.d(TAG, "onTouch");
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
        this.viewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
    }

    /**
     * videoPlay
     */
    public void videoPlay() {
        Log.d(TAG, "videoPlay");
        if (this.player.getStatus() == VideoRunnable.STATUS.PAUSED) {
            // pause => play
            if (this.videoThread == null || !this.videoThread.isAlive()) {
                this.viewController.setVisibility(VideoRunnable.STATUS.PLAYING);
                this.videoThread = new Thread(this.player);
                this.videoThread.start();
            }
        } else if (this.player.getStatus() == VideoRunnable.STATUS.PLAYING) {
            // play => pause
            this.videoPause();
        } else if (this.player.getStatus() == VideoRunnable.STATUS.VIDEO_END) {
            this.player.release();
            this.player.prepare(this.filePath);
            this.viewController.setVisibility(VideoRunnable.STATUS.PLAYING);
            this.videoThread = new Thread(this.player);
            this.videoThread.start();
        }
    }

    /**
     * videoStop
     */
    public void videoStop() {
        Log.d(TAG, "videoStop");
        if (this.videoThread.isAlive()) {
            try {
                Log.d(TAG, "join");
                this.videoThread.interrupt();
                this.videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.player.release();
        this.player.prepare(this.filePath);
        this.player.setStatus(VideoRunnable.STATUS.VIDEO_SELECTED);
        this.viewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
        this.videoThread = new Thread(this.player);
        this.videoThread.start();
    }

    /**
     * videoForward
     */
    public void videoForward() {
        Log.d(TAG, "videoForward");
        if (this.videoThread.isAlive()) {
            return;
        }
        if (this.player.getStatus() == VideoRunnable.STATUS.PAUSED) {
            this.player.setStatus(VideoRunnable.STATUS.FORWARD);
            this.viewController.setVisibility(VideoRunnable.STATUS.FORWARD);
            this.videoThread = new Thread(this.player);
            this.videoThread.start();
        }
    }

    /**
     * videoBackward
     */
    public void videoBackward() {
        Log.d(TAG, "videoBackward");
        if (this.videoThread.isAlive()) {
            Log.d(TAG, "Thread is alive.");
            return;
        }
        this.player.backward();
        this.player.setStatus(VideoRunnable.STATUS.BACKWARD);
        this.viewController.setVisibility(VideoRunnable.STATUS.BACKWARD);
        this.videoThread = new Thread(this.player);
        this.videoThread.start();
    }

    /**
     * videoSpeedUp
     */
    public void videoSpeedUp() {
        Log.d(TAG, "videoSpeedUp");

        double speed = this.player.getSpeed() * 2.0;
        if (speed > SPEED_MAX) {
            speed = SPEED_MAX;
        }

        this.viewController.setPlaySpeedText(speed);
        this.player.setSpeed(speed);
    }

    /**
     * videoSpeedDown
     */
    public void videoSpeedDown() {
        Log.d(TAG, "videoSpeedDown");

        double speed = this.player.getSpeed() / 2.0;
        if (speed < SPEED_MIN) {
            speed = SPEED_MIN;
        }

        this.viewController.setPlaySpeedText(speed);
        this.player.setSpeed(speed);
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
                this.viewController.setVisibility(VideoRunnable.STATUS.VIDEO_SELECTED);
                this.viewController.setPlaySpeedText(this.player.getSpeed());
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
                    Log.d(TAG, "join");
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
            Log.d(TAG, "move");
            view.move(motionEvent);
        } else if (motionEvent.getPointerCount() == 2) {
            Log.d(TAG, "scale");
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
            Log.d(TAG, "Diff Level : " + this.touchXDiffLevelLast);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            int diffX = (int) (motionEvent.getX() - this.touchStartX);
            Log.d(TAG, "diff x : " + diffX);

            touchXDiffLevel = diffX / TOUCH_X_DIFF_SIZE;
            Log.d(TAG, "Diff Level : " + this.touchXDiffLevelLast + " => " + touchXDiffLevel);
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
        Log.d(TAG, "videoPause");
        if (this.videoThread.isAlive()) {
            Log.d(TAG, "interrupt");
            this.videoThread.interrupt();
        }
        this.player.setStatus(VideoRunnable.STATUS.PAUSED);
        this.viewController.setVisibility(VideoRunnable.STATUS.PAUSED);
    }

    /**
     * videoSeek
     *
     * @param progress video progress
     */
    private void videoSeek(int progress) {
        Log.d(TAG, "videoSeek");
        if (this.player.getStatus() == VideoRunnable.STATUS.SEEKING) {
            return;
        }
        if (this.videoThread.isAlive()) {
            try {
                Log.d(TAG, "join");
                this.videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.player.seekTo(progress);
        this.player.setStatus(VideoRunnable.STATUS.SEEKING);
        this.viewController.setVisibility(VideoRunnable.STATUS.SEEKING);
        this.videoThread = new Thread(this.player);
        this.videoThread.start();
    }

    /**
     * logMetaData
     *
     * @param retriever media meta data retriever
     */
    private void logMetaData(MediaMetadataRetriever retriever) {
        Log.d(TAG, "logMetaData");

        Log.d(TAG, "==================================================");
        Log.d(TAG, "has audio  :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
        Log.d(TAG, "has video  :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
        Log.d(TAG, "date       :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
        Log.d(TAG, "width      :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        Log.d(TAG, "height     :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        Log.d(TAG, "duration   :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Log.d(TAG, "rotation   :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        Log.d(TAG, "num tracks :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
        Log.d(TAG, "title      :" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        Log.d(TAG, "==================================================");
    }

}
