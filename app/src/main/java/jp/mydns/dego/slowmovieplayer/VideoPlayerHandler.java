package jp.mydns.dego.slowmovieplayer;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class VideoPlayerHandler extends Handler {

//    private static final String TAG = "VideoPlayerHandler";

    private final WeakReference<ViewController> mViewControllerRef;
    static final String MESSAGE_PROGRESS_US = "MESSAGE_PROGRESS_US";
    static final String MESSAGE_STATUS = "MESSAGE_STATUS";

    VideoPlayerHandler(ViewController aViewController) {
        super();
        mViewControllerRef = new WeakReference<>(aViewController);
    }

    @Override
    public void handleMessage(Message aMessage) {

        long time_us = aMessage.getData().getLong(MESSAGE_PROGRESS_US);
        if (time_us >= 0) {
            mViewControllerRef.get().setProgress((int) (time_us / 1000));
        }

        VideoRunnable.STATUS status = (VideoRunnable.STATUS) aMessage.getData().getSerializable(MESSAGE_STATUS);
        if (status != null) {
            mViewControllerRef.get().setVisibility(status);
        }
    }
}
