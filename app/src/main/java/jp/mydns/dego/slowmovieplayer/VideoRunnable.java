package jp.mydns.dego.slowmovieplayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Message;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoRunnable implements Runnable {

//    private static final String TAG1 = "VideoRunnable";
//    private static final String TAG2 = "VideoThread";
    private static final String MIME_VIDEO = "video/";

    /* video status */
    enum STATUS {
        INIT,
        VIDEO_SELECTED,
        PAUSED,
        PLAYING,
        VIDEO_END,
        SEEKING,
        FORWARD,
        BACKWARD
    }

    private STATUS mVideoStatus;
    private OnVideoStatusChangeListener mVideoListener;
    private VideoPlayerHandler mHandler;

    private Surface mSurface;

    private MediaCodec mDecoder;
    private MediaExtractor mExtractor;
    private boolean mIsEOS;
    private int mFrameRate;
    private long mLastRenderTime;
    private int mOffsetFromKeyFrame;
    private int mOffsetTarget;
    private long mLastKeyFrameTime;

    /* playback speed config
     * -4 -> x1/4
     * -2 -> x1/2
     *  0 -> x1.0
     *  2 -> x2.0
     *  4 -> x4.0
     * others -> prohibit */
    private int mPlaybackSpeed;
    private long mFrameCount;

    /**
     * VideoRunnable
     *
     * @param aHandler handler
     */
    VideoRunnable(VideoPlayerHandler aHandler) {
        mHandler = aHandler;
        mVideoStatus = STATUS.INIT;
    }

    /**
     * setOnVideoStatusChangeListener
     *
     * @param aListener video status change listener
     */
    void setOnVideoStatusChangeListener(OnVideoStatusChangeListener aListener) {
        mVideoListener = aListener;
    }

    /**
     * init
     *
     * @param aFilePath video file path
     * @param aSurface  video surface
     */
    void init(String aFilePath, Surface aSurface) {

        mVideoStatus = STATUS.INIT;
        mSurface = aSurface;

        prepare(aFilePath);

        mVideoStatus = STATUS.VIDEO_SELECTED;
        mPlaybackSpeed = 0;
    }

    /**
     * prepare
     */
    void prepare(String aFilePath) {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(aFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int index = 0; index < mExtractor.getTrackCount(); index++) {
            MediaFormat format = mExtractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(MIME_VIDEO)) {
                mExtractor.selectTrack(index);
                try {
                    mDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long duration = format.getLong(MediaFormat.KEY_DURATION);
                mVideoListener.onDurationChanged((int) (duration / 1000));

                mFrameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);

                try {
                    mDecoder.configure(format, mSurface, null, 0);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
        mIsEOS = false;
        mLastRenderTime = 0;
        mOffsetFromKeyFrame = 0;
        mLastKeyFrameTime = 0;
        if (mDecoder != null) {
            mDecoder.start();
        }
    }

    /**
     * run
     */
    @Override
    synchronized public void run() {

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        mIsEOS = false;
        mFrameCount = 0;

        long videoTimeStart = mExtractor.getSampleTime();
        long systemTimeStart = System.nanoTime() / 1000;

        while (!Thread.currentThread().isInterrupted()) {
            if (!mIsEOS) {
                int inIndex = mDecoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = mDecoder.getInputBuffer(inIndex);
                    int sampleSize;
                    if (buffer != null) {
                        sampleSize = mExtractor.readSampleData(buffer, 0);
                    } else {
                        sampleSize = -1;
                    }
                    if (sampleSize >= 0) {
                        long sampleTime;
                        if (mPlaybackSpeed > 0) {
                            sampleTime = mExtractor.getSampleTime() / (long) mPlaybackSpeed;
                        } else {
                            sampleTime = mExtractor.getSampleTime();
                        }
                        mDecoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0);
                        mExtractor.advance();
                    } else {
                        mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mIsEOS = true;
                    }
                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            if (outIndex != MediaCodec.INFO_OUTPUT_FORMAT_CHANGED &&
                    outIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (outIndex >= 0) {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        mOffsetFromKeyFrame = 0;
                        mLastKeyFrameTime = mExtractor.getSampleTime();
                    } else {
                        mOffsetFromKeyFrame++;
                    }

                    releaseOutputBuffer(info, outIndex, systemTimeStart, videoTimeStart);

                    if (mVideoStatus == STATUS.VIDEO_SELECTED ||
                            mVideoStatus == STATUS.VIDEO_END ||
                            mVideoStatus == STATUS.SEEKING) {
                        mVideoStatus = STATUS.PAUSED;
                        return;
                    } else if (mVideoStatus == STATUS.FORWARD) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            mVideoStatus = STATUS.VIDEO_END;
                            setVisibility();
                        } else {
                            mVideoStatus = STATUS.PAUSED;
                        }
                        return;
                    }
                }
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mVideoStatus = STATUS.VIDEO_END;
                setVisibility();
                return;
            }
            if (mVideoStatus == STATUS.PAUSED) {
                return;
            }
        }
    }

    /**
     * releaseOutputBuffer
     *
     * @param aInfo            output buffer info
     * @param aOutputBufferId  output buffer id
     * @param aSystemTimeStart system time at video start
     * @param aVideoTimeStart  video time at video start
     */
    private void releaseOutputBuffer(MediaCodec.BufferInfo aInfo, int aOutputBufferId, long aSystemTimeStart, long aVideoTimeStart) {

        switch (mVideoStatus) {
            case PLAYING:
                mFrameCount++;
                if (mPlaybackSpeed > 0 && (mFrameCount % mPlaybackSpeed) != 0) {
                    mDecoder.releaseOutputBuffer(aOutputBufferId, false);
                    break;
                }
                long systemTime = System.nanoTime() / 1000 - aSystemTimeStart;
                long videoTime = mLastRenderTime - aVideoTimeStart;
                if (mPlaybackSpeed > 0) {
                    systemTime = systemTime * (long) mPlaybackSpeed;
                } else if (mPlaybackSpeed < 0) {
                    systemTime = systemTime / (-1 * (long) mPlaybackSpeed);
                }
                while (systemTime < videoTime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    systemTime = System.nanoTime() / 1000 - aSystemTimeStart;
                    if (mPlaybackSpeed > 0) {
                        systemTime = systemTime * (long) mPlaybackSpeed;
                    } else if (mPlaybackSpeed < 0) {
                        systemTime = systemTime / (-1 * (long) mPlaybackSpeed);
                    }
                }
            case VIDEO_SELECTED:
            case VIDEO_END:
            case SEEKING:
            case FORWARD:
                mDecoder.releaseOutputBuffer(aOutputBufferId, true);
                mLastRenderTime = mExtractor.getSampleTime();

                if (aInfo.presentationTimeUs > 0 || mLastRenderTime > 0) {
                    if (mPlaybackSpeed > 0) {
                        setProgress(aInfo.presentationTimeUs * mPlaybackSpeed);
                    } else {
                        setProgress(aInfo.presentationTimeUs);
                    }
                }
                break;
            case BACKWARD:
                if (mOffsetFromKeyFrame == mOffsetTarget) {
                    mDecoder.releaseOutputBuffer(aOutputBufferId, true);
                    mVideoStatus = STATUS.PAUSED;
                    if (mPlaybackSpeed > 0) {
                        setProgress(aInfo.presentationTimeUs * mPlaybackSpeed);
                    } else {
                        setProgress(aInfo.presentationTimeUs);
                    }
                } else {
                    mDecoder.releaseOutputBuffer(aOutputBufferId, false);
                }
                break;
            case INIT:
            case PAUSED:
            default:
                break;
        }
    }

    /**
     * seekTo
     *
     * @param aProgress video progress
     */
    void seekTo(int aProgress) {
        mDecoder.flush();
        mExtractor.seekTo(aProgress * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    /**
     * backward
     */
    void backward() {
        mDecoder.flush();
        if (mOffsetFromKeyFrame <= 0) {
            mExtractor.seekTo(mLastKeyFrameTime - 1000000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            mOffsetTarget = mFrameRate - 1;
        } else {
            mExtractor.seekTo(mLastKeyFrameTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            mOffsetTarget = mOffsetFromKeyFrame - 1;
        }
    }

    /**
     * getStatus
     *
     * @return video status
     */
    STATUS getStatus() {
        return mVideoStatus;
    }

    /**
     * setStatus
     *
     * @param aStatus status
     */
    void setStatus(STATUS aStatus) {
        mVideoStatus = aStatus;
    }

    /**
     * release
     */
    void release() {
        mDecoder.stop();
        mDecoder.release();
        mExtractor.release();
    }

    /**
     * getSpeed
     *
     * @return playback speed
     */
    int getSpeed() {
        return mPlaybackSpeed;
    }

    /**
     * setSpeed
     *
     * @param aSpeed playback speed (-4, -2, 0, 2, 4)
     */
    void setSpeed(int aSpeed) {
        if (aSpeed == -4 || aSpeed == -2 || aSpeed == 0 || aSpeed == 2 || aSpeed == 4) {
            mPlaybackSpeed = aSpeed;
        }
    }

    /**
     * setProgress
     *
     * @param aTime_us video progress in microseconds
     */
    private void setProgress(long aTime_us) {
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putLong(VideoPlayerHandler.MESSAGE_PROGRESS_US, aTime_us);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    /**
     * sendMessage
     */
    private void setVisibility() {
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putLong(VideoPlayerHandler.MESSAGE_PROGRESS_US, -1);
        bundle.putSerializable(VideoPlayerHandler.MESSAGE_STATUS, mVideoStatus);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }
}
