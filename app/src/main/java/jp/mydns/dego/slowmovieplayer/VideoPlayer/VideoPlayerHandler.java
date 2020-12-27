package jp.mydns.dego.slowmovieplayer.VideoPlayer;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

import jp.mydns.dego.slowmovieplayer.ViewController;

public class VideoPlayerHandler extends Handler {

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // public constant values
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "VideoPlayerHandler";

    static final String MESSAGE_PROGRESS_US = "MESSAGE_PROGRESS_US";
    static final String MESSAGE_STATUS = "MESSAGE_STATUS";

    // ---------------------------------------------------------------------------------------------
    // private fields
    // ---------------------------------------------------------------------------------------------
    private final WeakReference<ViewController> viewControllerRef;

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
     * VideoPlayerHandler
     *
     * @param viewController view controller
     */
    VideoPlayerHandler(ViewController viewController) {
        super();
        Log.d(TAG, "VideoPlayerHandler");
        this.viewControllerRef = new WeakReference<>(viewController);
    }

    // ---------------------------------------------------------------------------------------------
    // public method
    // ---------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    // private method (package private)
    // ---------------------------------------------------------------------------------------------

    /**
     * handleMessage
     *
     * @param message message
     */
    @Override
    public void handleMessage(Message message) {
        Log.d(TAG, "handleMessage");

        long time_us = message.getData().getLong(MESSAGE_PROGRESS_US);
        Log.d(TAG, "progress time (us) : " + time_us);
        if (time_us >= 0) {
            this.viewControllerRef.get().setProgress((int) (time_us / 1000));
        }

        VideoRunnable.STATUS status = (VideoRunnable.STATUS) message.getData().getSerializable(MESSAGE_STATUS);
        if (status != null) {
            Log.d(TAG, "status : " + status.name() + " (" + status + ")");
            this.viewControllerRef.get().setVisibility(status);
        }
    }
}
