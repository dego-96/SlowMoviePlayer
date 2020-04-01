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
    private String mVideoPath = null;
    private ViewStatusManager mViewStatusManager;

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
     * @param aView view
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
     * onActivityResult
     *
     * @param aRequestCode request code
     * @param aResultCode  result code
     * @param aData        data
     */
    @Override
    protected void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(aRequestCode, aResultCode, aData);
        if (aResultCode == Activity.RESULT_OK) {
            switch (aRequestCode) {
                case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
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
                    break;

                case REQUEST_GALLERY:
                    mVideoPath = getPathFromUri(aData);
                    Log.d(TAG, "video path :" + mVideoPath);
                    ((PlayerSurfaceView) findViewById(R.id.player_surface_view)).setVideoPath(mVideoPath);
                    mViewStatusManager.setButtonState(ViewStatusManager.VIEW_STATUS_VIDEO_SELECTED);
                    break;

                default:
                    Log.w(TAG, "unknown request code");
                    break;
            }
        }
    }

    /**
     * initialize
     */
    private void initialize() {
        Log.d(TAG, "initialize");
        mCanReadExternalStorage = false;
        mVideoPath = "";
        mViewStatusManager = new ViewStatusManager(this);
        mViewStatusManager.setButtonState(ViewStatusManager.VIEW_STATUS_INIT);
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
            return null;
        }

        Uri uri = aData.getData();
        if (uri == null) {
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
        }
        Log.d(TAG, "path: " + path);
        return path;
    }
}
