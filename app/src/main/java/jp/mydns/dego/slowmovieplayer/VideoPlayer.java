package jp.mydns.dego.slowmovieplayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

class VideoPlayer {

    private static final String TAG = "VideoPlayer";
    private static final String MIME_VIDEO = "video/";

    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;

    /**
     * init
     *
     * @param aSurface  video play surface
     * @param aFilePath video file path
     * @return initialization result
     */
    boolean init(Surface aSurface, String aFilePath) {
        Log.d(TAG, "init");
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(aFilePath);
            for (int index = 0; index < mExtractor.getTrackCount(); index++) {
                MediaFormat format = mExtractor.getTrackFormat(index);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith(MIME_VIDEO)) {
                    mExtractor.selectTrack(index);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    try {
                        Log.d(TAG, "format: " + format);
                        mDecoder.configure(format, aSurface, null, 0);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        Log.e(TAG, "codec '" + mime + "' failed configuration." + e);
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDecoder.setCallback(new VideoDecoderCallback(mExtractor));
        return true;
    }

    /**
     * start
     */
    void start() {
        mDecoder.start();
    }
}
