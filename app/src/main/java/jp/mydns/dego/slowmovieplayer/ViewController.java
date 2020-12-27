package jp.mydns.dego.slowmovieplayer;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.mydns.dego.slowmovieplayer.VideoPlayer.VideoController;
import jp.mydns.dego.slowmovieplayer.VideoPlayer.VideoRunnable;

public class ViewController {

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // public constant values
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "ViewController";
    private static final int ANIMATOR_DURATION = 300;

    // ---------------------------------------------------------------------------------------------
    // private fields
    // ---------------------------------------------------------------------------------------------
    private final static SimpleDateFormat TimerFormat = new SimpleDateFormat("mm:ss:SSS", Locale.JAPAN);

    private Activity activity;
    private boolean frameOuted;

    /* Views */
    private VideoSurfaceView surfaceView;
    private ImageView noVideoImageView;
    private ImageView galleryImageView;
    private ImageView backwardImageView;
    private ImageView forwardImageView;
    private ImageView playImageView;
    private ImageView stopImageView;
    private ImageView speedUpImageView;
    private ImageView speedDownImageView;
    private ImageView expandImageView;
    private ImageView frameControlImageView;
    private SeekBar seekBar;
    private TextView playSpeedTextView;
    private TextView currentTimeTextView;
    private TextView remainTimeTextView;

    /* Layout */
    private LinearLayout controlButtonsLayout;
    private LinearLayout seekBarLayout;

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
     * ViewController
     *
     * @param activity main activity
     */
    public ViewController(Activity activity) {
        Log.d(TAG, "ViewController");

        this.activity = activity;

        this.surfaceView = activity.findViewById(R.id.player_surface_view);
        this.noVideoImageView = activity.findViewById(R.id.image_no_video);
        this.galleryImageView = activity.findViewById(R.id.button_gallery);
        this.backwardImageView = activity.findViewById(R.id.button_backward);
        this.forwardImageView = activity.findViewById(R.id.button_forward);
        this.playImageView = activity.findViewById(R.id.button_play);
        this.stopImageView = activity.findViewById(R.id.button_stop);
        this.speedUpImageView = activity.findViewById(R.id.button_speed_up);
        this.speedDownImageView = activity.findViewById(R.id.button_speed_down);
        this.expandImageView = activity.findViewById(R.id.button_expand_contract);
        this.frameControlImageView = activity.findViewById(R.id.button_frame_control);
        this.seekBar = activity.findViewById(R.id.seek_bar_progress);
        this.playSpeedTextView = activity.findViewById(R.id.text_view_speed);
        this.currentTimeTextView = activity.findViewById(R.id.text_view_current_time);
        this.remainTimeTextView = activity.findViewById(R.id.text_view_remain_time);

        this.controlButtonsLayout = activity.findViewById(R.id.layout_control_buttons);
        this.seekBarLayout = activity.findViewById(R.id.layout_seek_bar);

        this.frameOuted = false;
    }

    // ---------------------------------------------------------------------------------------------
    // public method
    // ---------------------------------------------------------------------------------------------

    /**
     * getView
     *
     * @param id view id
     * @return view
     */
    public View getView(int id) {
        return this.activity.findViewById(id);
    }

    /**
     * setSurfaceViewSize
     *
     * @param videoWidth  width
     * @param videoHeight height
     * @param rotation    rotation
     */
    @SuppressWarnings("noinspection")
    public void setSurfaceViewSize(int videoWidth, int videoHeight, int rotation) {
        Log.d(TAG, "setSurfaceViewSize");

        Point point = new Point();
        Display display = this.activity.getWindowManager().getDefaultDisplay();
        display.getSize(point);
        int displayWidth = point.x;
        int displayHeight = point.y;

        int width;
        int height;
        if ((rotation % 180) == 0) {
            // 横画面映像
            if (((float) displayWidth / (float) displayHeight) > ((float) videoWidth / (float) videoHeight)) {
                // 画面より縦長
                width = (int) ((float) videoWidth * ((float) displayHeight / (float) videoHeight));
                height = displayHeight;
            } else {
                // 画面より横長
                width = displayWidth;
                height = (int) ((float) videoHeight * ((float) displayWidth / (float) videoWidth));
            }
        } else {
            // 縦画面映像
            if (((float) displayWidth / (float) displayHeight) > ((float) videoHeight / (float) videoWidth)) {
                // 画面より縦長
                width = (int) ((float) videoHeight * ((float) displayHeight / (float) videoWidth));
                height = displayHeight;
            } else {
                // 画面より横長
                width = displayWidth;
                height = (int) ((float) videoWidth * ((float) displayWidth / (float) videoHeight));
            }
        }
        this.surfaceView.setInitialSize(width, height);
    }

    /**
     * setVisibility
     *
     * @param aStatus video status
     */
    public void setVisibility(VideoRunnable.STATUS aStatus) {
        Log.d(TAG, "setVisibility( " + aStatus.name() + " )");
        switch (aStatus) {
            case INIT:
                this.surfaceView.setVisibility(View.GONE);
                this.noVideoImageView.setVisibility(View.VISIBLE);
                this.galleryImageView.setVisibility(View.VISIBLE);
                this.backwardImageView.setVisibility(View.GONE);
                this.forwardImageView.setVisibility(View.GONE);
                this.playImageView.setVisibility(View.GONE);
                this.stopImageView.setVisibility(View.GONE);
                this.speedUpImageView.setVisibility(View.GONE);
                this.speedDownImageView.setVisibility(View.GONE);
                this.expandImageView.setVisibility(View.GONE);
                this.frameControlImageView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                this.playSpeedTextView.setVisibility(View.GONE);
                this.currentTimeTextView.setVisibility(View.GONE);
                this.remainTimeTextView.setVisibility(View.GONE);
                break;
            case PLAYING:
                this.surfaceView.setVisibility(View.VISIBLE);
                this.noVideoImageView.setVisibility(View.GONE);
                this.galleryImageView.setVisibility(View.INVISIBLE);
                this.backwardImageView.setVisibility(View.INVISIBLE);
                this.forwardImageView.setVisibility(View.INVISIBLE);
                this.playImageView.setVisibility(View.VISIBLE);
                this.playImageView.setImageResource(R.drawable.pause);
                this.stopImageView.setVisibility(View.VISIBLE);
                this.speedUpImageView.setVisibility(View.INVISIBLE);
                this.speedDownImageView.setVisibility(View.INVISIBLE);
                this.expandImageView.setVisibility(View.INVISIBLE);
                this.frameControlImageView.setVisibility(View.INVISIBLE);
                this.seekBar.setVisibility(View.VISIBLE);
                this.playSpeedTextView.setVisibility(View.VISIBLE);
                this.currentTimeTextView.setVisibility(View.VISIBLE);
                this.remainTimeTextView.setVisibility(View.VISIBLE);
                break;
            case VIDEO_SELECTED:
            case PAUSED:
            case VIDEO_END:
            case FORWARD:
            case BACKWARD:
            case SEEKING:
                this.surfaceView.setVisibility(View.VISIBLE);
                this.noVideoImageView.setVisibility(View.GONE);
                this.galleryImageView.setVisibility(View.VISIBLE);
                this.backwardImageView.setVisibility(View.VISIBLE);
                this.forwardImageView.setVisibility(View.VISIBLE);
                this.playImageView.setVisibility(View.VISIBLE);
                this.playImageView.setImageResource(R.drawable.play);
                this.stopImageView.setVisibility(View.VISIBLE);
                this.speedUpImageView.setVisibility(View.VISIBLE);
                this.speedDownImageView.setVisibility(View.VISIBLE);
                this.expandImageView.setVisibility(View.VISIBLE);
                this.frameControlImageView.setVisibility(View.VISIBLE);
                this.seekBar.setVisibility(View.VISIBLE);
                this.playSpeedTextView.setVisibility(View.VISIBLE);
                this.currentTimeTextView.setVisibility(View.VISIBLE);
                this.remainTimeTextView.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * setProgress
     *
     * @param progress progress
     */
    public void setProgress(int progress) {
        Log.d(TAG, "setProgress (" + progress + ")");
        this.seekBar.setProgress(progress);

        this.currentTimeTextView.setText(TimerFormat.format(new Date(progress)));
        int duration = this.seekBar.getMax();
        if (progress < duration) {
            this.remainTimeTextView.setText(TimerFormat.format(new Date(duration - progress)));
        } else {
            this.remainTimeTextView.setText(this.activity.getString(R.string.remain_time_init));
        }
    }

    /**
     * setDuration
     *
     * @param aDuration video duration
     */
    public void setDuration(int aDuration) {
        Log.d(TAG, "setDuration (" + aDuration + ")");
        this.seekBar.setMax(aDuration);

        this.remainTimeTextView.setText(TimerFormat.format(new Date(aDuration)));
    }

    /**
     * setPlaySpeed
     *
     * @param speed play speed
     */
    public void setPlaySpeedText(double speed) {
        Log.d(TAG, "setPlaySpeed");

        if (3.0 < speed) {
            this.playSpeedTextView.setText(this.activity.getString(R.string.play_speed_x4_0));
        } else if (1.5 < speed) {
            this.playSpeedTextView.setText(this.activity.getString(R.string.play_speed_x2_0));
        } else if (0.8 < speed) {
            this.playSpeedTextView.setText(this.activity.getString(R.string.play_speed_init));
        } else if (0.4 < speed) {
            this.playSpeedTextView.setText(this.activity.getString(R.string.play_speed_x1_2));
        } else {
            this.playSpeedTextView.setText(this.activity.getString(R.string.play_speed_x1_4));
        }
    }

    /**
     * setManipulation
     *
     * @param manipulation 操作モード
     */
    public void setManipulation(VideoController.MANIPULATION manipulation) {
        if (manipulation == VideoController.MANIPULATION.EXPAND_CONTRACT) {
            this.expandImageView.setImageResource(R.drawable.expand_en);
            this.frameControlImageView.setImageResource(R.drawable.frame_control);
        } else if (manipulation == VideoController.MANIPULATION.FRAME_CONTROL) {
            this.expandImageView.setImageResource(R.drawable.expand);
            this.frameControlImageView.setImageResource(R.drawable.frame_control_en);
        }
    }

    /**
     * animFullscreenPreview
     */
    public void animFullscreenPreview() {
        Log.d(TAG, "fullScreenAnimationStart");

        float toX, fromX, toY, fromY;
        List<Animator> animatorList = new ArrayList<>();

        if (this.frameOuted) {
            // animation (frame in)
            fromX = 280.0f;
            toX = 0.0f;
            fromY = 280.0f;
            toY = 0.0f;
        } else {
            // animation (frame out)
            fromX = 0.0f;
            toX = 280.0f;
            fromY = 0.0f;
            toY = 280.0f;
        }
        this.frameOuted = !this.frameOuted;

        ObjectAnimator animatorTrans_Buttons = ObjectAnimator.ofFloat(this.controlButtonsLayout, "translationY", fromY, toY);
        ObjectAnimator animatorTrans_SeekBar = ObjectAnimator.ofFloat(this.seekBarLayout, "translationY", fromY, toY);
        ObjectAnimator animatorTrans_Forward = ObjectAnimator.ofFloat(this.forwardImageView, "translationX", fromX, toX);
        ObjectAnimator animatorTrans_Backward = ObjectAnimator.ofFloat(this.backwardImageView, "translationX", -1.0f * fromX, -1.0f * toX);
        ObjectAnimator animatorTrans_Gallery = ObjectAnimator.ofFloat(this.galleryImageView, "translationY", -1.0f * fromY, -1.0f * toY);
        ObjectAnimator animatorTrans_ExpandContract = ObjectAnimator.ofFloat(this.expandImageView, "translationX", fromX, toX);
        ObjectAnimator animatorTrans_FrameControl = ObjectAnimator.ofFloat(this.frameControlImageView, "translationX", fromX, toX);

        animatorTrans_Buttons.setDuration(ANIMATOR_DURATION);
        animatorTrans_SeekBar.setDuration(ANIMATOR_DURATION);
        animatorTrans_Forward.setDuration(ANIMATOR_DURATION);
        animatorTrans_Backward.setDuration(ANIMATOR_DURATION);
        animatorTrans_Gallery.setDuration(ANIMATOR_DURATION);
        animatorTrans_ExpandContract.setDuration(ANIMATOR_DURATION);
        animatorTrans_FrameControl.setDuration(ANIMATOR_DURATION);
        animatorList.add(animatorTrans_Buttons);
        animatorList.add(animatorTrans_SeekBar);
        animatorList.add(animatorTrans_Forward);
        animatorList.add(animatorTrans_Backward);
        animatorList.add(animatorTrans_Gallery);
        animatorList.add(animatorTrans_ExpandContract);
        animatorList.add(animatorTrans_FrameControl);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorList);
        animatorSet.start();
    }

    // ---------------------------------------------------------------------------------------------
    // private method (package private)
    // ---------------------------------------------------------------------------------------------

}
