package jp.mydns.dego.slowmovieplayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class VideoDecoderCallback extends MediaCodec.Callback {

    private static final String TAG = "VideoDecoder";

    private MediaExtractor mExtractor;

    VideoDecoderCallback(MediaExtractor aExtractor) {
        mExtractor = aExtractor;
    }

    @Override
    public void onError(@NonNull MediaCodec aCodec, @NonNull MediaCodec.CodecException aException) {
        Log.d(TAG, "onError");
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec aCodec, int aInputBufferId) {
        Log.d(TAG, "onInputBufferAvailable");

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

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec aCodec, int aOutputBufferId, @NonNull MediaCodec.BufferInfo aInfo) {
        Log.d(TAG, "onOutputBufferAvailable");
        if (aOutputBufferId >= 0) {
            aCodec.releaseOutputBuffer(aOutputBufferId, true);
        }
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec aCodec, @NonNull MediaFormat aFormat) {
        Log.d(TAG, "onOutputFormatChanged");
    }
}
