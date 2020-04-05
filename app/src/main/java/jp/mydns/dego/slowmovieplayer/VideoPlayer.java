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

    private static final int PLAYER_STATUS_INITIALIZED = 0;
    private static final int PLAYER_STATUS_PLAYING = 1;
    private static final int PLAYER_STATUS_PAUSED = 2;

    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private Surface mSurface;
    private String mFilePath;
    private int mPlayerStatus;
    private Queue<DecodeEvent> mQueue;

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

        if (mPlayerStatus == PLAYER_STATUS_INITIALIZED ||
                mPlayerStatus == PLAYER_STATUS_PLAYING) {
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

        if (mPlayerStatus == PLAYER_STATUS_INITIALIZED ||
                mPlayerStatus == PLAYER_STATUS_PLAYING) {
            releaseOutputBuffer(aCodec, aOutputBufferId);
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
     * init
     *
     * @param aSurface  video play surface
     * @param aFilePath video file path
     * @return initialization result
     */
    boolean init(Surface aSurface, String aFilePath) {
        Log.d(TAG, "init");
        mFilePath = aFilePath;
        mSurface = aSurface;
        return init();
    }

    /**
     * init
     *
     * @return initialization result
     */
    private boolean init() {
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
                    try {
                        Log.d(TAG, "format: " + format);
                        mDecoder.configure(format, mSurface, null, 0);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        Log.e(TAG, "codec '" + mime + "' failed configuration." + e);
                        return false;
                    }
                }
            }
            mDecoder.setCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayerStatus = PLAYER_STATUS_INITIALIZED;
        if (mQueue == null) {
            mQueue = new ArrayDeque<>();
        } else {
            mQueue.clear();
        }
        return true;
    }

    /**
     * start
     */
    void start() {
        Log.d(TAG, "start");
        if (mPlayerStatus == PLAYER_STATUS_INITIALIZED) {
            mDecoder.start();
        } else {
            mPlayerStatus = PLAYER_STATUS_PLAYING;
            while (mQueue != null && !mQueue.isEmpty()) {
                DecodeEvent content = mQueue.poll();
                if (content == null) {
                    break;
                } else if (content.isInput()) {
                    queueInputBuffer(mDecoder, content.getBufferId());
                } else {
                    releaseOutputBuffer(mDecoder, content.getBufferId());
                }
            }
        }
    }

    /**
     * pause
     */
    void pause() {
        Log.d(TAG, "pause");
        mPlayerStatus = PLAYER_STATUS_PAUSED;
    }

    /**
     * stop
     */
    void stop() {
        Log.d(TAG, "stop");
        mDecoder.stop();
        mDecoder.release();
        init();
        mPlayerStatus = PLAYER_STATUS_INITIALIZED;
        mDecoder.start();
    }

    /**
     * forward
     */
    void forward() {
        Log.d(TAG, "forward");
        if (mPlayerStatus != PLAYER_STATUS_PLAYING) {
            mPlayerStatus = PLAYER_STATUS_PAUSED;
            while (mQueue != null && !mQueue.isEmpty()) {
                DecodeEvent content = mQueue.poll();
                if (content == null) {
                    break;
                } else if (content.isInput()) {
                    queueInputBuffer(mDecoder, content.getBufferId());
                } else {
                    releaseOutputBuffer(mDecoder, content.getBufferId());
                    break;
                }
            }
        }
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
        if (mExtractor.advance() && sampleSize > 0) {
            aCodec.queueInputBuffer(aInputBufferId, 0, sampleSize, mExtractor.getSampleTime(), 0);
        } else {
            Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
            aCodec.queueInputBuffer(aInputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
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
        if (aOutputBufferId >= 0) {
            aCodec.releaseOutputBuffer(aOutputBufferId, true);
            if (mPlayerStatus == PLAYER_STATUS_INITIALIZED) {
                mPlayerStatus = PLAYER_STATUS_PAUSED;
            }
        }
    }
}
