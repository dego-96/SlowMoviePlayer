package jp.mydns.dego.slowmovieplayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

class VideoPlayer extends MediaCodec.Callback {

    private static final String TAG = "VideoPlayer";
    private static final String MIME_VIDEO = "video/";
    private static final long SEEK_RENDER_TIMEOUT = 50;

    /* player status */
    enum PLAYER_STATUS {
        INITIALIZED,
        PAUSED,
        PLAYING,
        STOPPED,
        FORWARD,
        BACKWARD,
        SEEK_RENDER_START,
        SEEK_RENDER_FINISH
    }

    private PLAYER_STATUS mPlayerStatus;

    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private Surface mSurface;
    private String mFilePath;
    private Queue<DecodeEvent> mQueue;
    private boolean mPlayToEnd;
    private long mLastSeekTime;
    private long mLastRenderTime;
    private int mFrameRate;

    private OnVideoStatusChangeListener mVideoListener;

    /**
     * onError
     *
     * @param aCodec     media codec
     * @param aException codec exception
     */
    @Override
    public void onError(@NonNull MediaCodec aCodec, @NonNull MediaCodec.CodecException aException) {
        Log.d(TAG, "onError");
    }

    /**
     * onInputBufferAvailable
     *
     * @param aCodec         media codec
     * @param aInputBufferId input buffer id
     */
    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec aCodec, int aInputBufferId) {
        Log.d(TAG, "onInputBufferAvailable");
        Log.d(TAG, "Input Buffer ID: " + aInputBufferId);

        if (mPlayerStatus == PLAYER_STATUS.INITIALIZED ||
                mPlayerStatus == PLAYER_STATUS.PLAYING ||
                mPlayerStatus == PLAYER_STATUS.STOPPED ||
                mPlayerStatus == PLAYER_STATUS.SEEK_RENDER_START ||
                mPlayerStatus == PLAYER_STATUS.BACKWARD) {
            queueInputBuffer(aCodec, aInputBufferId);
        } else {
            mQueue.add(new DecodeEvent(true, aInputBufferId));
        }

    }

    /**
     * onOutputBufferAvailable
     *
     * @param aCodec          media codec
     * @param aOutputBufferId output buffer id
     * @param aInfo           buffer info
     */
    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec aCodec, int aOutputBufferId, @NonNull MediaCodec.BufferInfo aInfo) {
        Log.d(TAG, "onOutputBufferAvailable");
        Log.d(TAG, "Output Buffer ID: " + aOutputBufferId);

        if (mPlayerStatus == PLAYER_STATUS.INITIALIZED ||
                mPlayerStatus == PLAYER_STATUS.PLAYING ||
                mPlayerStatus == PLAYER_STATUS.STOPPED ||
                mPlayerStatus == PLAYER_STATUS.SEEK_RENDER_START ||
                mPlayerStatus == PLAYER_STATUS.BACKWARD) {
            if (aOutputBufferId >= 0) {
                releaseOutputBuffer(aCodec, aOutputBufferId);
            }
        } else {
            mQueue.add(new DecodeEvent(false, aOutputBufferId));
        }
    }

    /**
     * onOutputFormatChanged
     *
     * @param aCodec  media codec
     * @param aFormat media format
     */
    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec aCodec, @NonNull MediaFormat aFormat) {
        Log.d(TAG, "onOutputFormatChanged");
    }

    /**
     * setProgressChangeListener
     *
     * @param aListener progress change listener
     */
    void setVideoStatusChangeListener(OnVideoStatusChangeListener aListener) {
        Log.d(TAG, "setProgressChangeListener");
        this.mVideoListener = aListener;
    }

    /**
     * init
     *
     * @param aSurface  video play surface
     * @param aFilePath video file path
     */
    void init(Surface aSurface, String aFilePath) {
        Log.d(TAG, "init");
        mFilePath = aFilePath;
        mSurface = aSurface;
        mPlayerStatus = PLAYER_STATUS.INITIALIZED;
        /* ちらつき防止のためViewは更新しない */
        init();
    }

    /**
     * init
     */
    private void init() {
        Log.d(TAG, "init");
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mFilePath);

            for (int index = 0; index < mExtractor.getTrackCount(); index++) {
                MediaFormat format = mExtractor.getTrackFormat(index);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith(MIME_VIDEO)) {
                    mExtractor.selectTrack(index);
                    mDecoder = MediaCodec.createDecoderByType(mime);

                    long duration = format.getLong(MediaFormat.KEY_DURATION);
                    mVideoListener.onDurationChanged((int) (duration / 1000));

                    mFrameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    Log.d(TAG, "frame rate :" + mFrameRate);

                    try {
                        Log.d(TAG, "format: " + format);
                        mDecoder.configure(format, mSurface, null, 0);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        Log.e(TAG, "codec '" + mime + "' failed configuration." + e);
                    }
                }
            }
            mDecoder.setCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayToEnd = false;
        if (mQueue == null) {
            mQueue = new ArrayDeque<>();
        } else {
            mQueue.clear();
        }
        mDecoder.start();
    }

    /**
     * play
     */
    void play() {
        Log.d(TAG, "play");
        mPlayerStatus = PLAYER_STATUS.PLAYING;
        mVideoListener.onPlayerStatusChanged(this);
        if (mPlayToEnd) {
            mPlayToEnd = false;
            mDecoder.stop();
            mDecoder.release();
            init();
        } else if (mQueue.isEmpty()) {
            mDecoder.start();
        } else {
            runQueuedProcess(false);
        }
    }

    /**
     * pause
     */
    void pause() {
        Log.d(TAG, "pause");
        mPlayerStatus = PLAYER_STATUS.PAUSED;
        mVideoListener.onPlayerStatusChanged(this);
    }

    /**
     * stop
     */
    void stop() {
        Log.d(TAG, "stop");
        mPlayerStatus = PLAYER_STATUS.STOPPED;
        mVideoListener.onPlayerStatusChanged(this);
        mDecoder.stop();
        mDecoder.release();
        init();
    }

    /**
     * forward
     */
    void forward() {
        Log.d(TAG, "forward");
        mPlayerStatus = PLAYER_STATUS.FORWARD;
        mVideoListener.onPlayerStatusChanged(this);
        if (mPlayerStatus != PLAYER_STATUS.PLAYING) {
            mPlayerStatus = PLAYER_STATUS.PAUSED;
            runQueuedProcess(true);
        }
    }

    /**
     * backward
     */
    void backward() {
        Log.d(TAG, "backward");
        mPlayerStatus = PLAYER_STATUS.BACKWARD;
        mVideoListener.onPlayerStatusChanged(this);

        long seek_time = mExtractor.getSampleTime() - 10 * (1000 * 1000 / mFrameRate);
        if (seek_time < 0) {
            seek_time = 0;
        }
        mQueue.clear();
        mDecoder.flush();
        mExtractor.seekTo(seek_time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        mDecoder.start();
    }

    /**
     * seek
     *
     * @param aProgress video seek time in milliseconds
     */
    void seek(int aProgress) {
        Log.d(TAG, "seek" + "  (from " + mPlayerStatus.name() + ")");

        if (mPlayerStatus == PLAYER_STATUS.SEEK_RENDER_START ||
                System.currentTimeMillis() - mLastSeekTime < SEEK_RENDER_TIMEOUT) {
            Log.d(TAG, "nothing to do.");
            return;
        }
        mPlayerStatus = PLAYER_STATUS.SEEK_RENDER_START;
        mVideoListener.onPlayerStatusChanged(this);

        Log.d(TAG, "seekTo : " + aProgress * 1000);
        mQueue.clear();
        mDecoder.flush();
        mExtractor.seekTo(aProgress * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        mDecoder.start();
    }

    /**
     * getPlayerStatus
     *
     * @return video player status
     */
    PLAYER_STATUS getPlayerStatus() {
        return mPlayerStatus;
    }

    /**
     * queueInputBuffer
     *
     * @param aCodec         media codec
     * @param aInputBufferId input buffer id
     */
    private void queueInputBuffer(@NonNull MediaCodec aCodec, int aInputBufferId) {
        Log.d(TAG, "queueInputBuffer");
        ByteBuffer inputBuffer = aCodec.getInputBuffer(aInputBufferId);
        int sampleSize = 0;
        if (inputBuffer != null) {
            sampleSize = mExtractor.readSampleData(inputBuffer, 0);
        }
        mPlayToEnd = !mExtractor.advance();
        if (!mPlayToEnd && sampleSize > 0) {
            aCodec.queueInputBuffer(aInputBufferId, 0, sampleSize, mExtractor.getSampleTime(), 0);
        } else {
            Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
            aCodec.queueInputBuffer(aInputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mPlayToEnd = true;
        }
    }

    /**
     * releaseOutputBuffer
     *
     * @param aCodec          media codec
     * @param aOutputBufferId output buffer id
     */
    private void releaseOutputBuffer(@NonNull MediaCodec aCodec, int aOutputBufferId) {
        Log.d(TAG, "releaseOutputBuffer");
        switch (mPlayerStatus) {
            case INITIALIZED:
            case STOPPED:
                mPlayerStatus = PLAYER_STATUS.PAUSED;
                mVideoListener.onPlayerStatusChanged(this);
            case PAUSED:
            case PLAYING:
            case FORWARD:
                aCodec.releaseOutputBuffer(aOutputBufferId, true);
                mLastRenderTime = mExtractor.getSampleTime();
                Log.d(TAG, "sample time :" + mLastRenderTime);
                if (mLastRenderTime > 0) {
                    mVideoListener.onProgressChanged((int) (mLastRenderTime / 1000));
                }
                break;
            case BACKWARD:
                long sample_time = mExtractor.getSampleTime();
                Log.d(TAG, "================================");
                Log.d(TAG, "last render time :" + mLastRenderTime);
                Log.d(TAG, "sample time      :" + sample_time);
                if (sample_time > (mLastRenderTime - 2 * (1000 * 1000 / mFrameRate))) {
                    aCodec.releaseOutputBuffer(aOutputBufferId, true);
                    mLastRenderTime = sample_time;
                    Log.d(TAG, "render           : true");
                    if (sample_time > 0) {
                        mVideoListener.onProgressChanged((int) (sample_time / 1000));
                    }
                    mPlayerStatus = PLAYER_STATUS.PAUSED;
                    mVideoListener.onPlayerStatusChanged(this);
                } else {
                    aCodec.releaseOutputBuffer(aOutputBufferId, false);
                    Log.d(TAG, "render           : false");
                }
                Log.d(TAG, "================================");
                break;
            case SEEK_RENDER_START:
                mPlayerStatus = PLAYER_STATUS.SEEK_RENDER_FINISH;
                mVideoListener.onPlayerStatusChanged(this);
                mLastSeekTime = System.currentTimeMillis();
                aCodec.releaseOutputBuffer(aOutputBufferId, true);
                mLastRenderTime = mExtractor.getSampleTime();
                Log.d(TAG, "sample time :" + mLastRenderTime);
                break;
            case SEEK_RENDER_FINISH:
                return;
            default:
                Log.e(TAG, "video player status error");
                break;
        }

        if (mPlayToEnd) {
            mPlayerStatus = PLAYER_STATUS.PAUSED;
            mVideoListener.onPlayToEnd();
        }
    }

    /**
     * runQueuedProcess
     */
    private void runQueuedProcess(boolean aOneFrame) {
        Log.d(TAG, "runQueuedProcess");
        while (mQueue != null && !mQueue.isEmpty()) {
            DecodeEvent content = mQueue.poll();
            if (content == null) {
                break;
            } else if (content.isInput()) {
                queueInputBuffer(mDecoder, content.getBufferId());
            } else {
                releaseOutputBuffer(mDecoder, content.getBufferId());
                if (aOneFrame) {
                    break;
                }
            }
        }
    }
}
