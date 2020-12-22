package jp.mydns.dego.slowmovieplayer.VideoPlayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoRunnable implements Runnable {

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    private class VideoTimer {
        long startTime;
        long startTimeSys;
        long renderTime;
        double speed;
        boolean isInterrupted;

        VideoTimer(MediaExtractor extractor, double speed) {
            this.renderTime = 0;
            this.speed = speed;
            this.isInterrupted = false;

            this.start(extractor);
        }

        void start(MediaExtractor extractor) {
            this.startTime = extractor.getSampleTime();
            this.startTimeSys = System.nanoTime() / 1000;

            Log.d(TAG_THREAD, "-------- start time --------");
            Log.d(TAG_THREAD, "system : " + this.startTimeSys);
            Log.d(TAG_THREAD, "video  : " + this.startTime);
            Log.d(TAG_THREAD, "----------------------------");
        }

        void render(long renderTime) {
            this.renderTime = renderTime;
        }

        void waitNext() {
            long elapsed;
            long waitTime = this.renderTime - startTime;
            do {
                // 再生速度に合わせてシステム時間の経過スピードを変える
                elapsed = (long) ((System.nanoTime() / 1000.0 - this.startTimeSys) * this.speed);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.isInterrupted = true;
                }
            } while (elapsed < waitTime);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // public constant values
    // ---------------------------------------------------------------------------------------------
    /* video status */
    public enum STATUS {
        INIT,
        VIDEO_SELECTED,
        PAUSED,
        PLAYING,
        VIDEO_END,
        SEEKING,
        FORWARD,
        BACKWARD
    }

    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "VideoRunnable";
    private static final String TAG_THREAD = "VideoThread";
    private static final String MIME_VIDEO = "video/";

    // ---------------------------------------------------------------------------------------------
    // private fields
    // ---------------------------------------------------------------------------------------------
    private STATUS videoStatus;
    private OnVideoStatusChangeListener videoListener;
    private VideoPlayerHandler handler;

    private Surface surface;

    private MediaCodec decoder;
    private MediaExtractor extractor;
    //    private long lastRenderTime;
    private int offsetFromKeyFrame;
    //    private int offsetTarget;
    private long lastKeyFrameTime;

    private double playbackSpeed;
    private long frameCount;

    // ---------------------------------------------------------------------------------------------
    // static fields
    // ---------------------------------------------------------------------------------------------
    private static VideoRunnable instance = new VideoRunnable();

    // ---------------------------------------------------------------------------------------------
    // private static method
    // ---------------------------------------------------------------------------------------------

    /**
     * static VideoRunnable
     *
     * @return instance
     */
    static VideoRunnable getInstance() {
        return instance;
    }

    // ---------------------------------------------------------------------------------------------
    // constructor
    // ---------------------------------------------------------------------------------------------

    /**
     * VideoRunnable
     */
    private VideoRunnable() {
        this.handler = null;
        this.videoStatus = STATUS.INIT;
    }

    // ---------------------------------------------------------------------------------------------
    // public method
    // ---------------------------------------------------------------------------------------------

    /**
     * run
     */
    @Override
    synchronized public void run() {
        Log.d(TAG_THREAD, "run");
        Log.d(TAG_THREAD, "status :" + this.getStatus().name());

        switch (this.getStatus()) {
            case INIT:
            case PLAYING:
            default:
                // Nothing to do.
                break;
            case PAUSED:
            case VIDEO_END:
                this.play();
                break;
            case VIDEO_SELECTED:
            case FORWARD:
            case SEEKING:
                this.oneFrame();
                break;
            case BACKWARD:
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // private method (package private)
    // ---------------------------------------------------------------------------------------------

    /**
     * SetVideoHandler
     *
     * @param handler video player handler
     */
    void setVideoHandler(VideoPlayerHandler handler) {
        this.handler = handler;
    }

    /**
     * setOnVideoStatusChangeListener
     *
     * @param listener video status change listener
     */
    void setOnVideoStatusChangeListener(OnVideoStatusChangeListener listener) {
        Log.d(TAG, "setOnVideoStatusChangeListener");
        this.videoListener = listener;
    }

    /**
     * init
     *
     * @param filePath video file path
     * @param surface  video surface
     */
    void init(String filePath, Surface surface) {
        Log.d(TAG, "init");

        this.setStatus(STATUS.INIT);
        this.surface = surface;

        this.prepare(filePath);

        this.setStatus(STATUS.VIDEO_SELECTED);
        this.playbackSpeed = 1.0;
    }

    /**
     * prepare
     *
     * @param filePath file path
     */
    void prepare(String filePath) {
        Log.d(TAG, "prepare");
        this.extractor = new MediaExtractor();
        try {
            this.extractor.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int index = 0; index < this.extractor.getTrackCount(); index++) {
            MediaFormat format = this.extractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(MIME_VIDEO)) {
                this.extractor.selectTrack(index);
                try {
                    this.decoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long duration = format.getLong(MediaFormat.KEY_DURATION);
                this.videoListener.onDurationChanged((int) (duration / 1000));

//                this.frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
//                Log.d(TAG, "frame rate :" + this.frameRate);

                try {
                    Log.d(TAG, "format: " + format);
                    this.decoder.configure(format, this.surface, null, 0);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Log.e(TAG, "codec '" + mime + "' failed configuration." + e);
                }
            }
        }
        this.offsetFromKeyFrame = 0;
        this.lastKeyFrameTime = 0;
        if (this.decoder != null) {
            this.decoder.start();
        }
    }

    /**
     * seekTo
     *
     * @param aProgress video progress
     */
    void seekTo(int aProgress) {
        Log.d(TAG, "seekTo (" + aProgress + ")");
        this.decoder.flush();
        this.extractor.seekTo(aProgress * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    /**
     * backward
     */
    void backward() {
        Log.d(TAG, "backward");
        this.decoder.flush();
//        if (this.offsetFromKeyFrame <= 0) {
//            this.extractor.seekTo(this.lastKeyFrameTime - 1000000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//            this.offsetTarget = this.frameRate - 1;
//        } else {
//            this.extractor.seekTo(this.lastKeyFrameTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//            this.offsetTarget = this.offsetFromKeyFrame - 1;
//        }
    }

    /**
     * getStatus
     *
     * @return video status
     */
    STATUS getStatus() {
        return this.videoStatus;
    }

    /**
     * setStatus
     *
     * @param status status
     */
    void setStatus(STATUS status) {
        Log.d(TAG, "setStatus (" + this.videoStatus.name() + " => " + status.name() + ")");
        this.videoStatus = status;

        if (status == STATUS.VIDEO_END) {
            this.setVisibility();
        }
    }

    /**
     * release
     */
    void release() {
        Log.d(TAG, "release");
        this.decoder.stop();
        this.decoder.release();
        this.extractor.release();
    }

    /**
     * getSpeed
     *
     * @return playback speed
     */
    double getSpeed() {
        return this.playbackSpeed;
    }

    /**
     * setSpeed
     *
     * @param speed playback speed
     */
    void setSpeed(double speed) {
        Log.d(TAG, "setSpeed(" + speed + ")");
        this.playbackSpeed = speed;
    }

    // ---------------------------------------------------------------------------------------------
    // private method
    // ---------------------------------------------------------------------------------------------

    /**
     * play
     */
    private void play() {
        Log.d(TAG_THREAD, "play");
        this.setStatus(STATUS.PLAYING);

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEos = false;
        this.frameCount = 0;

        VideoTimer timer = new VideoTimer(this.extractor, this.playbackSpeed);

        while (!Thread.currentThread().isInterrupted() && !timer.isInterrupted) {
            if (!isEos) {
                isEos = queueInput();
            }

            queueOutput(info, timer);

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG_THREAD, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                this.setStatus(STATUS.VIDEO_END);
                return;
            }
        }
        this.setStatus(STATUS.PAUSED);
    }

    /**
     * oneFrame
     */
    private void oneFrame() {
        Log.d(TAG_THREAD, "oneFrame");
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEos = false;
        this.frameCount = 0;

        VideoTimer timer = new VideoTimer(this.extractor, this.playbackSpeed);

        while (!Thread.currentThread().isInterrupted() && !timer.isInterrupted) {
            if (!isEos) {
                isEos = queueInput();
            }

            boolean render = queueOutput(info, timer);

            // 最後まで再生した場合
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG_THREAD, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                this.setStatus(STATUS.VIDEO_END);
                return;
            }
            if (render) {
                this.setStatus(STATUS.PAUSED);
                return;
            }
        }
    }

    private boolean queueInput() {
        int inIndex = this.decoder.dequeueInputBuffer(10000);
        Log.d(TAG_THREAD, "Input Buffer Index : " + inIndex);
        if (inIndex >= 0) {
            ByteBuffer buffer = this.decoder.getInputBuffer(inIndex);
            int sampleSize = (buffer != null) ? this.extractor.readSampleData(buffer, 0) : -1;
            Log.d(TAG_THREAD, "sample size : " + sampleSize);
            if (sampleSize >= 0) {
                long sampleTime = (long) ((double) this.extractor.getSampleTime() / this.playbackSpeed);
                Log.d(TAG_THREAD, "sample time : " + sampleTime);
                this.decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0);
                this.extractor.advance();
            } else {
                Log.d(TAG_THREAD, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                this.decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return true;
            }
        }
        return false;
    }

    private boolean queueOutput(MediaCodec.BufferInfo info, VideoTimer timer) {
        boolean render = false;

        int outIndex = this.decoder.dequeueOutputBuffer(info, 10000);
        Log.d(TAG_THREAD, "Output Buffer Index : " + outIndex);

        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            Log.d(TAG_THREAD, "INFO_OUTPUT_FORMAT_CHANGED");
        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d(TAG_THREAD, "INFO_TRY_AGAIN_LATER");
        } else {
            if (outIndex >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    this.offsetFromKeyFrame = 0;
                    this.lastKeyFrameTime = this.extractor.getSampleTime();
                    Log.d(TAG_THREAD, "key frame !!!!!   (" + this.lastKeyFrameTime + " us)");
                } else {
                    this.offsetFromKeyFrame++;
                    Log.d(TAG_THREAD, "key frame offset : " + this.offsetFromKeyFrame
                            + "  (" + this.extractor.getSampleTime() + " us)");
                }

                this.frameCount++;

                timer.waitNext();

                if (this.playbackSpeed <= 1.0 || (this.frameCount % (int) this.playbackSpeed) == 0) {
                    this.decoder.releaseOutputBuffer(outIndex, true);
                    timer.render(this.extractor.getSampleTime());
                    render = true;
                }

                if (info.presentationTimeUs > 0 || timer.renderTime > 0) {
                    Log.d(TAG_THREAD, "presentationTimeUs : " + info.presentationTimeUs);
                    Log.d(TAG_THREAD, "render time        : " + timer.renderTime);
                    if (this.playbackSpeed > 1.0) {
                        this.setProgress((long) ((double) info.presentationTimeUs * this.playbackSpeed));
                    } else {
                        this.setProgress(info.presentationTimeUs);
                    }
                }
            }
        }
        return render;
    }

    /**
     * setProgress
     *
     * @param time_us video progress in microseconds
     */
    private void setProgress(long time_us) {
        Log.d(TAG_THREAD, "setProgress");
        Bundle bundle = new Bundle();
        bundle.putLong(VideoPlayerHandler.MESSAGE_PROGRESS_US, time_us);
        Message message = Message.obtain();
        message.setData(bundle);
        this.handler.sendMessage(message);
    }

    /**
     * setVisibility
     */
    private void setVisibility() {
        Log.d(TAG, "setVisibility");
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putLong(VideoPlayerHandler.MESSAGE_PROGRESS_US, -1);
        bundle.putSerializable(VideoPlayerHandler.MESSAGE_STATUS, this.getStatus());
        message.setData(bundle);
        this.handler.sendMessage(message);
    }
}
