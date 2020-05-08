package jp.mydns.dego.slowmovieplayer;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class VideoPlayerHandler extends Handler {

    private static final String TAG = "VideoPlayerHandler";

    private final WeakReference<ViewController> mViewControllerRef;
    static final String MESSAGE_PROGRESS_US = "MESSAGE_PROGRESS_US";
    static final String MESSAGE_STATUS = "MESSAGE_STATUS";

    VideoPlayerHandler(ViewController aViewController) {
        super();
        Log.d(TAG, "VideoPlayerHandler");
        mViewControllerRef = new WeakReference<>(aViewController);
    }

    @Override
    public void handleMessage(Message aMessage) {
        Log.d(TAG, "handleMessage");

        long time_us = aMessage.getData().getLong(MESSAGE_PROGRESS_US);
        Log.d(TAG, "progress time (us) : " + time_us);
        if (time_us >= 0) {
            mViewControllerRef.get().setProgress((int) (time_us / 1000));
        }

        VideoRunnable.STATUS status = (VideoRunnable.STATUS) aMessage.getData().getSerializable(MESSAGE_STATUS);
        if (status != null) {
            Log.d(TAG, "status : " + status.name() + " (" + status + ")");
            mViewControllerRef.get().setVisibility(status);
        }
    }
}
