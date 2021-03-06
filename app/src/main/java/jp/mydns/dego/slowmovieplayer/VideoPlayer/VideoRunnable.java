package jp.mydns.dego.slowmovieplayer.VideoPlayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Message;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import jp.mydns.dego.slowmovieplayer.Util.DebugLog;

public class VideoRunnable implements Runnable {

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    private class VideoTimer {
        long startTime;
        long startTimeSys;
        long renderTime;
        long count;
        double speed;
        boolean isInterrupted;

        VideoTimer() {
            this.startTime = 0;
            this.startTimeSys = 0;
            this.renderTime = 0;
            this.count = 0;
            this.speed = 1.0;
            this.isInterrupted = false;
        }

        void start() {
            this.renderTime = 0;
            this.isInterrupted = false;

            this.startTime = VideoRunnable.this.extractor.getSampleTime();
            this.startTimeSys = System.nanoTime() / 1000;
            this.count = 0;
            this.speed = VideoRunnable.this.playSpeed;

            DebugLog.d(TAG_THREAD, "-------- start time --------");
            DebugLog.d(TAG_THREAD, "system : " + this.startTimeSys);
            DebugLog.d(TAG_THREAD, "video  : " + this.startTime);
            DebugLog.d(TAG_THREAD, "----------------------------");
        }

        void render() {
            this.renderTime = VideoRunnable.this.extractor.getSampleTime();
            this.count++;
        }

        void waitNext() {
            long elapsed;
            long waitTime = this.renderTime - this.startTime;
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

        long getCount() {
            return this.count;
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
    private double playSpeed;
    private VideoTimer videoTimer;

    private long lastKeyTime;
    private int frameRate;
    private long lastRenderTime;

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
        DebugLog.d(TAG_THREAD, "run");
        DebugLog.d(TAG_THREAD, "status :" + this.getStatus().name());

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
                this.toPreviousFrame();
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
        DebugLog.d(TAG, "setOnVideoStatusChangeListener");
        this.videoListener = listener;
    }

    /**
     * init
     *
     * @param filePath video file path
     * @param surface  video surface
     */
    void init(String filePath, Surface surface) {
        DebugLog.d(TAG, "init");

        this.setStatus(STATUS.INIT);
        this.surface = surface;
        this.videoTimer = new VideoTimer();

        this.prepare(filePath);

        this.setStatus(STATUS.VIDEO_SELECTED);
        this.playSpeed = VideoController.VIDEO_SPEED[VideoController.VIDEO_SPEED.length - 1];
    }

    /**
     * prepare
     *
     * @param filePath file path
     */
    void prepare(String filePath) {
        DebugLog.d(TAG, "prepare");
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

                this.frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);

                try {
                    DebugLog.d(TAG, "format: " + format);
                    this.decoder.configure(format, this.surface, null, 0);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    DebugLog.e(TAG, "codec '" + mime + "' failed configuration." + e);
                }
            }
        }
        if (this.decoder != null) {
            this.decoder.start();
        }

        this.lastKeyTime = 0;
    }

    /**
     * seekTo
     *
     * @param aProgress video progress
     */
    void seekTo(int aProgress) {
        DebugLog.d(TAG, "seekTo (" + aProgress + ")");
        this.decoder.flush();
        this.extractor.seekTo(aProgress * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        DebugLog.d(TAG_THREAD, "sample flag : " + this.extractor.getSampleFlags());
    }

    /**
     * toPreviousKeyFrame
     */
    void toPreviousKeyFrame() {
        DebugLog.d(TAG, "toPreviousKeyFrame");
        this.decoder.flush();

        // seek to previous key frame
        DebugLog.d(TAG, "last key time : " + this.lastKeyTime);
        DebugLog.d(TAG, "last render time : " + this.lastRenderTime);
        if (this.lastKeyTime < 1 || this.lastKeyTime < this.lastRenderTime) {
            this.extractor.seekTo(this.lastKeyTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        } else {
            this.extractor.seekTo(this.lastKeyTime - 1, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        }
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
        DebugLog.d(TAG, "setStatus (" + this.videoStatus.name() + " => " + status.name() + ")");
        this.videoStatus = status;

        if (status == STATUS.VIDEO_END) {
            this.setVisibility();
        }
    }

    /**
     * release
     */
    void release() {
        DebugLog.d(TAG, "release");
        this.decoder.stop();
        this.decoder.release();
        this.extractor.release();
    }

    /**
     * setSpeed
     *
     * @param speed play speed
     */
    void setSpeed(double speed) {
        DebugLog.d(TAG, "setSpeed(" + speed + ")");
        this.playSpeed = speed;
    }

    // ---------------------------------------------------------------------------------------------
    // private method
    // ---------------------------------------------------------------------------------------------

    /**
     * play
     */
    private void play() {
        DebugLog.d(TAG_THREAD, "play");
        this.setStatus(STATUS.PLAYING);

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEos = false;
        this.videoTimer.start();

        while (!Thread.currentThread().isInterrupted() && !this.videoTimer.isInterrupted) {
            if ((this.extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                this.lastKeyTime = this.extractor.getSampleTime();
                DebugLog.d(TAG_THREAD, "last key time : " + this.lastKeyTime);
            }

            if (!isEos) {
                isEos = queueInput();
            }

            boolean render = queueOutput(info);
            if (render) {
                this.lastRenderTime = info.presentationTimeUs;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                DebugLog.d(TAG_THREAD, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
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
        DebugLog.d(TAG_THREAD, "oneFrame");
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEos = false;
        this.videoTimer.start();

        while (!Thread.currentThread().isInterrupted() && !this.videoTimer.isInterrupted) {
            if ((this.extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                this.lastKeyTime = this.extractor.getSampleTime();
                DebugLog.d(TAG_THREAD, "last key time : " + this.lastKeyTime);
            }

            if (!isEos) {
                isEos = queueInput();
            }

            boolean render = queueOutput(info);
            if (render) {
                this.lastRenderTime = info.presentationTimeUs;
            }

            // 最後まで再生した場合
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                DebugLog.d(TAG_THREAD, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                this.setStatus(STATUS.VIDEO_END);
                return;
            }
            if (render) {
                this.setStatus(STATUS.PAUSED);
                return;
            }
        }
    }

    /**
     * toPreviousFrame
     */
    private void toPreviousFrame() {
        DebugLog.d(TAG_THREAD, "toPreviousFrame");
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEos = false;

        // 1.5フレーム前の時間を超過したら描画させる
        long targetTime = this.lastRenderTime - (long) (1500000 / this.frameRate);
        DebugLog.d(TAG_THREAD, "target time : " + targetTime);

        while (true) {
            if ((this.extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                this.lastKeyTime = this.extractor.getSampleTime();
                DebugLog.d(TAG_THREAD, "last key time : " + this.lastKeyTime);
            }

            if (!isEos) {   // EOSには到達しないはず
                isEos = queueInput();
            }

            if (this.queueOutput(info, targetTime)) {
                return;
            }

            // 最後まで再生した場合（到達しないはず）
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                this.setStatus(STATUS.PAUSED);
                return;
            }
        }
    }

    /**
     * queueInput
     *
     * @return is EOS
     */
    private boolean queueInput() {
        int inIndex = this.decoder.dequeueInputBuffer(10000);
        DebugLog.d(TAG_THREAD, "Input Buffer Index : " + inIndex);
        if (inIndex >= 0) {
            ByteBuffer buffer = this.decoder.getInputBuffer(inIndex);
            int sampleSize = (buffer != null) ? this.extractor.readSampleData(buffer, 0) : -1;
            if (sampleSize >= 0) {
                long sampleTime = (long) ((double) this.extractor.getSampleTime() / this.playSpeed);
                this.decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0);
                this.extractor.advance();
            } else {
                DebugLog.d(TAG_THREAD, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                this.decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return true;
            }
        }
        return false;
    }

    /**
     * queueOutput
     *
     * @param info Buffer Information
     * @return is rendered
     */
    private boolean queueOutput(MediaCodec.BufferInfo info) {
        boolean render = false;

        int outIndex = this.decoder.dequeueOutputBuffer(info, 10000);
        DebugLog.d(TAG_THREAD, "Output Buffer Index : " + outIndex);

        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            DebugLog.d(TAG_THREAD, "INFO_OUTPUT_FORMAT_CHANGED");
        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            DebugLog.d(TAG_THREAD, "INFO_TRY_AGAIN_LATER");
        } else {
            if (outIndex >= 0) {
                this.videoTimer.waitNext();

                render = (this.playSpeed <= 1.0 || (this.videoTimer.getCount() % (int) this.playSpeed) == 0);
                this.decoder.releaseOutputBuffer(outIndex, render);
                this.videoTimer.render();

                DebugLog.d(TAG_THREAD, "presentationTimeUs : " + info.presentationTimeUs);
                if (info.presentationTimeUs > 0 ||
                    ((this.getStatus() == STATUS.VIDEO_SELECTED || this.getStatus() == STATUS.SEEKING || this.getStatus() == STATUS.BACKWARD) && info.presentationTimeUs >= 0)) {
                    this.setProgress((long) ((double) info.presentationTimeUs * this.playSpeed));
                }
            }
        }
        return render;
    }

    /**
     * queueOutput
     *
     * @param info   Buffer Information
     * @param target target render time
     * @return is rendered
     */
    private boolean queueOutput(MediaCodec.BufferInfo info, long target) {
        int outIndex = this.decoder.dequeueOutputBuffer(info, 10000);
        DebugLog.d(TAG_THREAD, "Output Buffer Index : " + outIndex);

        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            DebugLog.d(TAG_THREAD, "INFO_OUTPUT_FORMAT_CHANGED");
        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            DebugLog.d(TAG_THREAD, "INFO_TRY_AGAIN_LATER");
        } else {
            if (outIndex >= 0) {
                DebugLog.d(TAG_THREAD, "presentation time : " + info.presentationTimeUs);
                if (info.presentationTimeUs < target) {
                    this.decoder.releaseOutputBuffer(outIndex, false);
                } else {
                    this.decoder.releaseOutputBuffer(outIndex, true);
                    DebugLog.d(TAG_THREAD, "previous frame rendered.");

                    this.lastRenderTime = info.presentationTimeUs;
                    DebugLog.d(TAG_THREAD, "last render time : " + this.lastRenderTime);
                    this.setProgress((long) ((double) info.presentationTimeUs * this.playSpeed));
                    this.setStatus(STATUS.PAUSED);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * setProgress
     *
     * @param time_us video progress in microseconds
     */
    private void setProgress(long time_us) {
        DebugLog.d(TAG_THREAD, "setProgress");
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
        DebugLog.d(TAG, "setVisibility");
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putLong(VideoPlayerHandler.MESSAGE_PROGRESS_US, -1);
        bundle.putSerializable(VideoPlayerHandler.MESSAGE_STATUS, this.getStatus());
        message.setData(bundle);
        this.handler.sendMessage(message);
    }
}