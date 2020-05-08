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
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 10;
    private static final int REQUEST_GALLERY = 20;

    private boolean mCanReadExternalStorage;

    private VideoController mVideoController;

    /**
     * onCreate
     *
     * @param aSavedInstanceState savedInstanceState
     */
    @Override
    protected void onCreate(Bundle aSavedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(aSavedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    /**
     * onResume
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        checkExternalStorageAccess();
    }

    /**
     * onVideoSelectButtonClicked
     *
     * @param aView button view
     */
    public void onVideoSelectButtonClicked(View aView) {
        Log.d(TAG, "onVideoSelectButtonClicked");
        if (mCanReadExternalStorage) {
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
     * @param aView button view
     */
    public void onPlayButtonClicked(View aView) {
        Log.d(TAG, "onPlayButtonClicked");
        mVideoController.videoPlay();
    }

    /**
     * onStopButtonClicked
     *
     * @param aView button view
     */
    public void onStopButtonClicked(View aView) {
        Log.d(TAG, "onStopButtonClicked");
        mVideoController.videoStop();
    }

    /**
     * onForwardButtonClicked
     *
     * @param aView button view
     */
    public void onForwardButtonClicked(View aView) {
        Log.d(TAG, "onForwardButtonClicked");
        mVideoController.videoForward();
    }

    /**
     * onBackwardButtonClicked
     *
     * @param aView button view
     */
    public void onBackwardButtonClicked(View aView) {
        Log.d(TAG, "onBackwardButtonClicked");
        mVideoController.videoBackward();
    }

    /**
     * onSpeedUpButtonClicked
     *
     * @param aView button view
     */
    public void onSpeedUpButtonClicked(View aView) {
        Log.d(TAG, "onSpeedUpButtonClicked");
    }

    /**
     * onSpeedDownButtonClicked
     *
     * @param aView button view
     */
    public void onSpeedDownButtonClicked(View aView) {
        Log.d(TAG, "onSpeedDownButtonClicked");
    }

    /**
     * onActivityResult
     *
     * @param aRequestCode request code
     * @param aResultCode  result code
     * @param aData        received data
     */
    @Override
    protected void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(aRequestCode, aResultCode, aData);
        switch (aRequestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                requestPermission(aResultCode);
                break;

            case REQUEST_GALLERY:
                requestGalleryResult(aResultCode, aData);
                break;

            default:
                Log.w(TAG, "unknown request code");
                break;
        }
    }

    /**
     * requestPermission
     *
     * @param aResultCode intent request code
     */
    private void requestPermission(int aResultCode) {
        Log.d(TAG, "requestPermission");
        if (aResultCode != Activity.RESULT_OK) {
            return;
        }
        int permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (permission != PackageManager.PERMISSION_GRANTED) {
            mCanReadExternalStorage = false;
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
     * @param aResultCode intent result code
     * @param aData       intent received data
     */
    private void requestGalleryResult(int aResultCode, Intent aData) {
        Log.d(TAG, "requestGalleryResult");
        String video_path = "";
        if (aResultCode == Activity.RESULT_OK) {
            video_path = getPathFromUri(aData);
            mVideoController.setVideoPath(video_path);
            Log.d(TAG, "video path :" + video_path);
        }
        if ("".equals(video_path)) {
            Toast.makeText(getApplication(), getString(R.string.toast_no_video), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * initialize
     */
    private void initialize() {
        Log.d(TAG, "initialize");
        mVideoController = new VideoController(this);
        mCanReadExternalStorage = false;
    }

    /**
     * checkExternalStorageAccess
     */
    private void checkExternalStorageAccess() {
        Log.d(TAG, "checkExternalStorageAccess");
        int permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if (permission != PackageManager.PERMISSION_GRANTED) {
            mCanReadExternalStorage = false;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            mCanReadExternalStorage = true;
        }
    }

    /**
     * getPathFromUri
     *
     * @param aData data
     * @return path
     */
    private String getPathFromUri(Intent aData) {
        Log.d(TAG, "getPathFromUri");
        if (!mCanReadExternalStorage) {
            Log.e(TAG, "can not read external storage");
            return null;
        }

        Uri uri = aData.getData();
        if (uri == null) {
            Log.e(TAG, "uri is null");
            return null;
        }

        String path = null;
        int takeFlags = aData.getFlags();
        takeFlags &= Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        getContentResolver().takePersistableUriPermission(uri, takeFlags);
        String wholeID = DocumentsContract.getDocumentId(uri);
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
        } else {
            Log.e(TAG, "cursor is null");
        }
        Log.d(TAG, "path: " + path);
        return path;
    }
}
