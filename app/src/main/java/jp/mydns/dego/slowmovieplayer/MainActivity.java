package jp.mydns.dego.slowmovieplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import jp.mydns.dego.slowmovieplayer.VideoPlayer.VideoController;

public class MainActivity extends AppCompatActivity {

    // ---------------------------------------------------------------------------------------------
    // inner class
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // public constant values
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private constant values
    // ---------------------------------------------------------------------------------------------
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 10;
    private static final int REQUEST_GALLERY = 20;

    // ---------------------------------------------------------------------------------------------
    // private fields
    // ---------------------------------------------------------------------------------------------
    private boolean canReadExternalStorage;
    private VideoController videoController;

    // ---------------------------------------------------------------------------------------------
    // static fields
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // private static method
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // constructor
    // ---------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    // Android Life Cycle
    // ---------------------------------------------------------------------------------------------

    /**
     * onCreate
     *
     * @param savedInstanceState savedInstanceState
     */
    @Override
    protected void onCreate(Bundle aSavedInstanceState) {
        super.onCreate(aSavedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    /**
     * onResume
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkExternalStorageAccess();
    }

    /**
     * onActivityResult
     *
     * @param requestCode request code
     * @param resultCode  result code
     * @param data        received data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                requestPermission(resultCode);
                break;

            case REQUEST_GALLERY:
                requestGalleryResult(resultCode, data);
                break;

            default:
                Log.w(TAG, "unknown request code");
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // public method
    // ---------------------------------------------------------------------------------------------

    /**
     * onVideoSelectButtonClicked
     *
     * @param view button view
     */
    public void onVideoSelectButtonClicked(View view) {
        Log.d(TAG, "onVideoSelectButtonClicked");
        if (this.canReadExternalStorage) {
            Intent intentGallery;
            intentGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
            intentGallery.setType("video/*");
            startActivityForResult(intentGallery, REQUEST_GALLERY);
        } else {
            checkExternalStorageAccess();
        }
    }

    /**
     * onPlayButtonClicked
     *
     * @param view button view
     */
    public void onPlayButtonClicked(View view) {
        Log.d(TAG, "onPlayButtonClicked");
        this.videoController.videoPlay();
    }

    /**
     * onStopButtonClicked
     *
     * @param view button view
     */
    public void onStopButtonClicked(View view) {
        Log.d(TAG, "onStopButtonClicked");
        this.videoController.videoStop();
    }

    /**
     * onForwardButtonClicked
     *
     * @param view button view
     */
    public void onForwardButtonClicked(View view) {
        Log.d(TAG, "onForwardButtonClicked");
        this.videoController.videoForward();
    }

    /**
     * onBackwardButtonClicked
     *
     * @param view button view
     */
    public void onBackwardButtonClicked(View view) {
        Log.d(TAG, "onBackwardButtonClicked");
        this.videoController.videoBackward();
    }

    /**
     * onSpeedUpButtonClicked
     *
     * @param view button view
     */
    public void onSpeedUpButtonClicked(View view) {
        Log.d(TAG, "onSpeedUpButtonClicked");
        this.videoController.videoSpeedUp();
    }

    /**
     * onSpeedDownButtonClicked
     *
     * @param view button view
     */
    public void onSpeedDownButtonClicked(View view) {
        Log.d(TAG, "onSpeedDownButtonClicked");
        this.videoController.videoSpeedDown();
    }

    /**
     * onExpandButtonClicked
     *
     * @param view button view
     */
    public void onExpandButtonClicked(View view) {
        Log.d(TAG, "onExpandButtonClicked");
        this.videoController.setManipulation(VideoController.MANIPULATION.EXPAND_CONTRACT);
    }

    /**
     * onFrameControlButtonClicked
     *
     * @param view button view
     */
    public void onFrameControlButtonClicked(View view) {
        Log.d(TAG, "onFrameControlButtonClicked");
        this.videoController.setManipulation(VideoController.MANIPULATION.FRAME_CONTROL);
    }

    // ---------------------------------------------------------------------------------------------
    // private method (package private)
    // ---------------------------------------------------------------------------------------------

    /**
     * requestPermission
     *
     * @param resultCode intent request code
     */
    private void requestPermission(int resultCode) {
        Log.d(TAG, "requestPermission");
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        int permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (permission != PackageManager.PERMISSION_GRANTED) {
            this.canReadExternalStorage = false;
            Toast.makeText(
                    this,
                    getString(R.string.toast_no_permission),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * requestGalleryResult
     *
     * @param resultCode intent result code
     * @param data       intent received data
     */
    private void requestGalleryResult(int resultCode, Intent data) {
        Log.d(TAG, "requestGalleryResult");
        if (resultCode == Activity.RESULT_OK) {
            String video_path = getPathFromUri(data);
            if (video_path == null || "".equals(video_path)) {
                Toast.makeText(getApplication(), getString(R.string.toast_no_video), Toast.LENGTH_SHORT).show();
            } else {
                this.videoController.setVideoPath(video_path);
                Log.d(TAG, "video path :" + video_path);
            }
        }
    }

    /**
     * initialize
     */
    private void initialize() {
        Log.d(TAG, "initialize");
        this.videoController = new VideoController(this);
        this.canReadExternalStorage = false;
    }

    /**
     * checkExternalStorageAccess
     */
    private void checkExternalStorageAccess() {
        int permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (permission != PackageManager.PERMISSION_GRANTED) {
            this.canReadExternalStorage = false;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            this.canReadExternalStorage = true;
        }
    }

    /**
     * getPathFromUri
     *
     * @param data data
     * @return path
     */
    private String getPathFromUri(Intent data) {
        Log.d(TAG, "getPathFromUri");
        if (!this.canReadExternalStorage) {
            Log.e(TAG, "can not read external storage");
            return null;
        }

        Uri uri = data.getData();
        if (uri == null) {
            return null;
        }

        String path = null;
        int takeFlags = data.getFlags();
        takeFlags &= Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        getContentResolver().takePersistableUriPermission(uri, takeFlags);
        String wholeID = DocumentsContract.getDocumentId(uri);
        if (wholeID.contains(":")) {
            String id = wholeID.split(":")[1];
            String[] column = {MediaStore.Video.Media.DATA};
            String sel = MediaStore.Video.Media._ID + "=?";
            Cursor cursor = getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    column,
                    sel,
                    new String[]{id},
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(column[0]));
                cursor.close();
            }
            return path;
        } else if (wholeID.contains("/")) {
            return wholeID;
        } else {
            return null;
        }
    }
}
