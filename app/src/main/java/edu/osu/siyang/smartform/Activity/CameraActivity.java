package edu.osu.siyang.smartform.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.ContentValues;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

public class CameraActivity extends Activity implements PictureCallback, SurfaceHolder.Callback {

    public static final String EXTRA_CAMERA_DATA = "camera_data";

    private static final String KEY_IS_CAPTURING = "is_capturing";

    private static final String TAG = "CameraActivity";

    private Camera mCamera;
    private Bitmap mCameraBitmap;
    private ImageView mCameraImage;
    private ImageView mCameraLayer;
    private SurfaceView mCameraPreview;
    private Button mCaptureImageButton;
    private Button mDoneImageButton;
    private byte[] mCameraData;
    private boolean mIsCapturing;
    private Uri imageUri;

    private OnClickListener mCaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            captureImage();
        }
    };

    private OnClickListener mRecaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setupImageCapture();
        }
    };

    private OnClickListener mDoneImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            File saveFile = openFileForImage();
            boolean success = true;

            if (saveFile != null) {
                saveImageToFile(saveFile);
            } else {
                success = false;
                Toast.makeText(CameraActivity.this, "Unable to open file for saving image.",
                        Toast.LENGTH_SHORT).show();
            }
            // Set the photo filename on the result intent
            if (success) {
                Intent i = new Intent();
                i.putExtra(EXTRA_CAMERA_DATA, imageUri.toString());
                setResult(Activity.RESULT_OK, i);
            }
            else {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(edu.osu.siyang.smartform.R.layout.activity_camera);

        mCameraImage = (ImageView) findViewById(edu.osu.siyang.smartform.R.id.camera_image_view);
        mCameraImage.setVisibility(View.INVISIBLE);

        mCameraLayer = (ImageView) findViewById(edu.osu.siyang.smartform.R.id.camera_layer);
        mCameraLayer.setVisibility(View.VISIBLE);

        mCameraPreview = (SurfaceView) findViewById(edu.osu.siyang.smartform.R.id.preview_view);
        final SurfaceHolder surfaceHolder = mCameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mCaptureImageButton = (Button) findViewById(edu.osu.siyang.smartform.R.id.capture_image_button);
        mCaptureImageButton.setOnClickListener(mCaptureImageButtonClickListener);

        mDoneImageButton = (Button) findViewById(edu.osu.siyang.smartform.R.id.done_image_button);
        mDoneImageButton.setOnClickListener(mDoneImageButtonClickListener);
        mDoneImageButton.setEnabled(false);

        mIsCapturing = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(KEY_IS_CAPTURING, mIsCapturing);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mIsCapturing = savedInstanceState.getBoolean(KEY_IS_CAPTURING, mCameraData == null);
        if (mCameraData != null) {
            setupImageDisplay();
        } else {
            setupImageCapture();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCamera == null) {
            try {
                mCamera = Camera.open();
                mCamera.setPreviewDisplay(mCameraPreview.getHolder());
                if (mIsCapturing) {
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();
                }
            } catch (Exception e) {
                Toast.makeText(CameraActivity.this, "Unable to open camera. Please go to settings for camera permission", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        mCameraData = data;
        setupImageDisplay();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            Camera.Parameters camParams = mCamera.getParameters();
            Camera.Size sc = getOptimalPreviewSize(camParams.getSupportedPreviewSizes(), width, height);
            camParams.setPreviewSize(sc.width, sc.height);

            // Flatten camera parameters
            String flattened = camParams.flatten();
            StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
            Log.d(TAG, "Dump all camera parameters:");
            while (tokenizer.hasMoreElements()) {
                Log.d(TAG, tokenizer.nextToken());
            }


            // Customize camera parameters
            camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camParams.set("ISO", "200");
            camParams.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
            camParams.setPreviewSize(sc.width, sc.height);
            camParams.setExposureCompensation(0);
            mCamera.setParameters(camParams);

            // Check camera parameters
            Log.i(TAG, "Focus setting = " + camParams.getFocusMode());
            Log.i(TAG, "ISO setting = " + camParams.get("iso"));
            Log.i(TAG, "Exposure setting = " + camParams.getExposureCompensation());
            Log.i(TAG, "White Balance setting = " + camParams.getWhiteBalance());
            Log.i(TAG, "Preview Size setting = " + camParams.getPreviewSize());


            Toast.makeText(CameraActivity.this, "Camera active", Toast.LENGTH_LONG).show();
            try {
                mCamera.setPreviewDisplay(holder);
                if (mIsCapturing) {
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();
                }
            } catch (IOException e) {
                Toast.makeText(CameraActivity.this, "Unable to start camera preview.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    private void captureImage() {
        mCamera.takePicture(null, null, this);
    }

    /**
     * Init camera
     */
    private void setupImageCapture() {
        mCameraBitmap.recycle();
        mCameraBitmap = null;
        mCameraImage.setVisibility(View.INVISIBLE);
        mCameraLayer.setVisibility(View.VISIBLE);
        mCameraPreview.setVisibility(View.VISIBLE);
        mDoneImageButton.setEnabled(false);

        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
        mCaptureImageButton.setText(edu.osu.siyang.smartform.R.string.capture_image);
        mCaptureImageButton.setOnClickListener(mCaptureImageButtonClickListener);
    }

    /**
     * Check available memory
     * @return
     */
    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Scale sample bitmap for better performance
     * @param data
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap getSampleBitmap(byte[] data, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        int scale = calculateInSampleSize(options, reqWidth, reqHeight);
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o2);
    }

    /**
     * Draw bitmap image in app
     */
    private void setupImageDisplay() {
        Bitmap rotateBitmap = getSampleBitmap(mCameraData, 500, 500);
        Bitmap cropBitmap = RotateBitmap(rotateBitmap, 90);
        rotateBitmap.recycle();
        mCameraBitmap = Bitmap.createBitmap(cropBitmap, cropBitmap.getWidth()/4, cropBitmap.getHeight()/2 - cropBitmap.getWidth()/4,
                cropBitmap.getWidth()/2, cropBitmap.getWidth()/2);
        cropBitmap.recycle();
        ActivityManager.MemoryInfo memoryInfo = getAvailableMemory();
        Log.i(TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
        Log.i(TAG, " memoryInfo.lowMemory " + memoryInfo.lowMemory + "\n" );
        Log.i(TAG, " memoryInfo.threshold " + memoryInfo.threshold + "\n" );

        Drawable drawable = new BitmapDrawable(getResources(), mCameraBitmap);

        if(mCameraImage.getDrawable() != null) ((BitmapDrawable)mCameraImage.getDrawable()).getBitmap().recycle();

        mCameraImage.setImageDrawable(drawable);
        mCamera.stopPreview();
        mCameraImage.setVisibility(View.VISIBLE);
        mCameraLayer.setVisibility(View.INVISIBLE);
        mCameraPreview.setVisibility(View.INVISIBLE);
        mDoneImageButton.setEnabled(true);
        mCaptureImageButton.setText(edu.osu.siyang.smartform.R.string.recapture_image);
        mCaptureImageButton.setOnClickListener(mRecaptureImageButtonClickListener);
    }

    private File openFileForImage() {
        File imageDirectory = null;
        String filename = UUID.randomUUID().toString() + ".png";
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            imageDirectory = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "SmART-Form");
            if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
                imageDirectory = null;
            } else {
                return new File(imageDirectory.getPath() + File.separator + filename);
            }
        }
        return null;
    }

    private void saveImageToFile(File file) {
        if (mCameraBitmap != null) {
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(file);
                if (!mCameraBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)) {
                    Toast.makeText(CameraActivity.this, "Unable to save image to file.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Saved bitmap = "+ mCameraBitmap.getByteCount());

                    Toast.makeText(CameraActivity.this, "Saved image to the device",
                            Toast.LENGTH_SHORT).show();
                    imageUri = getImageContentUri(getApplicationContext(), file);
                }
                outStream.close();
            } catch (Exception e) {
                Toast.makeText(CameraActivity.this, "Unable to save image to file.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Get image uri link in media store
     */
    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns rotated bitmap by angle
     */
    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Calculates and returns optimal preview size from supported by each device.
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;
        for (Camera.Size size : sizes) {
            Log.d("CamSettings", "size "+ size.width +"-"+ size.height);
        }
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        Log.d("CamSettings", "optimalSize "+ optimalSize.width +"-"+ optimalSize.height);

        return optimalSize;
    }
}