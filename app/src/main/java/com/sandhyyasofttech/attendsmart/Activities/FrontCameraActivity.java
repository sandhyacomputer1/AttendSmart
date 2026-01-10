package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.sandhyyasofttech.attendsmart.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FrontCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Button btnCapture, btnCancel;
    private String action;

    // UI elements
    private RelativeLayout cameraContainer;
    private TextView tvAction, txtLoading;
    private ImageView focusIndicator, flashIcon;
    private RelativeLayout loadingLayout;
    private ProgressBar progressBar;

    private boolean isFlashOn = false;
    private boolean isFocusing = false;
    private static final int IMAGE_MAX_SIZE = 1024; // Max dimension for the image
    private static final int IMAGE_QUALITY = 85; // Optimal quality for attendance photos
    private static final String TAG = "FrontCameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_camera);

        action = getIntent().getStringExtra("action");

        // Initialize all views
        initViews();

        // Set up camera surface
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Set action text based on check-in/check-out
        updateActionText();

        // Set up button click listeners
        setupButtonListeners();

        // Set up touch listener for focus
        setupTouchFocus();

        // Initially hide loading layout
        loadingLayout.setVisibility(View.GONE);
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.surfaceView);
        btnCapture = findViewById(R.id.btnCapture);
        btnCancel = findViewById(R.id.btnCancel);
        cameraContainer = findViewById(R.id.cameraContainer);
        tvAction = findViewById(R.id.tvAction);
        focusIndicator = findViewById(R.id.focusIndicator);
        flashIcon = findViewById(R.id.flashIcon);
        loadingLayout = findViewById(R.id.loadingLayout);
        progressBar = findViewById(R.id.progressBar);
        txtLoading = findViewById(R.id.txtLoading);
    }

    private void updateActionText() {
        if ("checkIn".equals(action)) {
            tvAction.setText("Check In Photo");
        } else if ("checkOut".equals(action)) {
            tvAction.setText("Check Out Photo");
        }
    }

    private void setupButtonListeners() {
        btnCapture.setOnClickListener(v -> captureImage());

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Flash toggle
        flashIcon.setOnClickListener(v -> toggleFlash());
    }

    private void setupTouchFocus() {
        cameraContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && mCamera != null && !isFocusing) {
                    focusAtPoint(event.getX(), event.getY());
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // Open front camera
            int cameraId = findFrontFacingCamera();
            if (cameraId == -1) {
                Toast.makeText(this, "Front camera not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            mCamera = Camera.open(cameraId);
            mCamera.setDisplayOrientation(90); // Fix orientation

            // Set camera parameters for optimal image quality
            Camera.Parameters parameters = mCamera.getParameters();

            // Get supported picture sizes and choose the best one
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            Camera.Size optimalPictureSize = getOptimalPictureSize(pictureSizes, 1200, 1600);

            if (optimalPictureSize != null) {
                parameters.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
                Log.d(TAG, "Picture size set to: " + optimalPictureSize.width + "x" + optimalPictureSize.height);
            }

            // Get optimal preview size for circular container
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalPreviewSize = getOptimalPreviewSize(previewSizes,
                    cameraContainer.getWidth(), cameraContainer.getHeight());

            if (optimalPreviewSize != null) {
                parameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
                Log.d(TAG, "Preview size set to: " + optimalPreviewSize.width + "x" + optimalPreviewSize.height);
            }

            // Set focus mode
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // Set JPEG quality
            parameters.setJpegQuality(IMAGE_QUALITY);

            // Enable face detection if supported

            // Check and setup flash
            setupFlash(parameters);

            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            // Auto-focus when camera starts
            if (parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mCamera.autoFocus(null);
            }

        } catch (Exception e) {
            Log.e(TAG, "Camera error: " + e.getMessage(), e);
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Camera.Size getOptimalPictureSize(List<Camera.Size> sizes, int minWidth, int maxWidth) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = 4.0 / 3.0; // Standard camera aspect ratio

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find a size with matching aspect ratio and within limits
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;

            // Skip if aspect ratio is too different
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            // Check if width is within our desired range
            if (size.width >= minWidth && size.width <= maxWidth) {
                // Choose the size closest to max width
                if (Math.abs(size.width - maxWidth) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.width - maxWidth);
                }
            }
        }

        // If no match found in range, get the smallest size above minWidth
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (size.width >= minWidth && size.width < minDiff) {
                    optimalSize = size;
                    minDiff = size.width;
                }
            }
        }

        // Last resort: use the largest available size
        if (optimalSize == null && !sizes.isEmpty()) {
            optimalSize = sizes.get(0);
            for (Camera.Size size : sizes) {
                if (size.width * size.height > optimalSize.width * optimalSize.height) {
                    optimalSize = size;
                }
            }
        }

        return optimalSize;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of the orientation, swap width/height
        int targetHeight = Math.min(width, height);

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // If no match, find the closest size
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    private void setupFlash(Camera.Parameters parameters) {
        if (parameters.getSupportedFlashModes() != null &&
                parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            flashIcon.setVisibility(View.VISIBLE);
            // Set initial flash state to off
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            flashIcon.setImageResource(R.drawable.ic_flash_off);
            isFlashOn = false;
        } else {
            flashIcon.setVisibility(View.GONE);
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Surface changed error: " + e.getMessage(), e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    private void focusAtPoint(float x, float y) {
        if (mCamera == null || isFocusing) return;

        isFocusing = true;

        // Calculate position relative to camera container
        float relativeX = x / cameraContainer.getWidth();
        float relativeY = y / cameraContainer.getHeight();

        // Show focus indicator
        focusIndicator.setX(x - focusIndicator.getWidth() / 2);
        focusIndicator.setY(y - focusIndicator.getHeight() / 2);
        focusIndicator.setVisibility(View.VISIBLE);

        // Animate focus indicator
        focusIndicator.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(200)
                .withEndAction(() -> {
                    focusIndicator.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                });

        try {
            Camera.Parameters params = mCamera.getParameters();
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // Convert touch position to camera focus area
                int focusX = (int) (relativeX * 2000 - 1000);
                int focusY = (int) (relativeY * 2000 - 1000);

                // Ensure coordinates are within bounds
                focusX = Math.max(-1000, Math.min(focusX, 1000));
                focusY = Math.max(-1000, Math.min(focusY, 1000));

                Rect focusRect = new Rect(
                        focusX - 100, focusY - 100,
                        focusX + 100, focusY + 100
                );

                Camera.Area focusArea = new Camera.Area(focusRect, 1000);
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(focusArea);

                params.setFocusAreas(focusAreas);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);

                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        isFocusing = false;
                        // Hide focus indicator after delay
                        new Handler().postDelayed(() -> {
                            focusIndicator.setVisibility(View.INVISIBLE);
                        }, 1000);
                    }
                });
            }
        } catch (Exception e) {
            isFocusing = false;
            Log.e(TAG, "Focus error: " + e.getMessage(), e);
        }
    }

    private void toggleFlash() {
        if (mCamera == null) return;

        try {
            Camera.Parameters params = mCamera.getParameters();
            List<String> flashModes = params.getSupportedFlashModes();

            if (flashModes == null || flashModes.isEmpty()) {
                return;
            }

            String newMode;
            int flashIconRes;

            if (isFlashOn) {
                // Turn flash off
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    newMode = Camera.Parameters.FLASH_MODE_OFF;
                    flashIconRes = R.drawable.ic_flash_off;
                    isFlashOn = false;
                } else {
                    return;
                }
            } else {
                // Turn flash on
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    newMode = Camera.Parameters.FLASH_MODE_TORCH;
                    flashIconRes = R.drawable.ic_flash_on;
                    isFlashOn = true;
                } else if (flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                    newMode = Camera.Parameters.FLASH_MODE_ON;
                    flashIconRes = R.drawable.ic_flash_on;
                    isFlashOn = true;
                } else {
                    return;
                }
            }

            params.setFlashMode(newMode);
            mCamera.setParameters(params);
            flashIcon.setImageResource(flashIconRes);

        } catch (Exception e) {
            Log.e(TAG, "Flash toggle error: " + e.getMessage(), e);
            Toast.makeText(this, "Flash not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImage() {
        if (mCamera != null) {
            // Disable buttons during capture
            btnCapture.setEnabled(false);
            btnCancel.setEnabled(false);

            // Show loading animation
            showLoading("Capturing photo...");

            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    updateLoadingText("Processing image...");

                    // Small delay to show processing text
                    new Handler().postDelayed(() -> {
                        try {
                            processCapturedImage(data);
                        } catch (Exception e) {
                            Log.e(TAG, "Image processing error: " + e.getMessage(), e);
                            hideLoading();
                            Toast.makeText(FrontCameraActivity.this, "Error processing image", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }, 300);
                }
            });
        }
    }

    private void processCapturedImage(byte[] data) {
        // Convert byte array to bitmap
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        if (originalBitmap == null) {
            hideLoading();
            Toast.makeText(FrontCameraActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateLoadingText("Optimizing image...");

        // Calculate optimal scale to reduce image size while maintaining quality
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        float scale;
        if (width > height) {
            scale = (float) IMAGE_MAX_SIZE / width;
        } else {
            scale = (float) IMAGE_MAX_SIZE / height;
        }

        // Don't upscale if image is smaller than max size
        if (scale > 1) {
            scale = 1;
        }

        // Flip horizontally for front camera mirror effect and rotate
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1); // Mirror effect for front camera
        matrix.postRotate(90); // Rotate to correct orientation
        matrix.postScale(scale, scale); // Scale down if needed

        Bitmap processedBitmap = Bitmap.createBitmap(originalBitmap,
                0, 0,
                width,
                height,
                matrix,
                true);

        updateLoadingText("Compressing image...");

        // Compress bitmap with optimal quality
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, stream);
        byte[] compressedData = stream.toByteArray();

        // Log the image size
        Log.d(TAG, "Original image size: " + (data.length / 1024) + " KB");
        Log.d(TAG, "Compressed image size: " + (compressedData.length / 1024) + " KB");
        Log.d(TAG, "Image dimensions: " + processedBitmap.getWidth() + "x" + processedBitmap.getHeight());

        updateLoadingText("Finalizing...");

        // Recycle bitmaps to free memory
        originalBitmap.recycle();
        processedBitmap.recycle();

        // Small delay before returning result
        new Handler().postDelayed(() -> {
            // Return result to main activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("image_data", compressedData);
            resultIntent.putExtra("action", action);
            setResult(RESULT_OK, resultIntent);

            hideLoading();

            Toast.makeText(FrontCameraActivity.this, "✓ Photo captured successfully!", Toast.LENGTH_SHORT).show();
            finish();

        }, 500);
    }

    private void showLoading(String message) {
        runOnUiThread(() -> {
            loadingLayout.setVisibility(View.VISIBLE);
            loadingLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            txtLoading.setText(message);
            btnCapture.setEnabled(false);
            btnCancel.setEnabled(false);
        });
    }

    private void updateLoadingText(String message) {
        runOnUiThread(() -> {
            txtLoading.setText(message);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            loadingLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            loadingLayout.setVisibility(View.GONE);
            btnCapture.setEnabled(true);
            btnCancel.setEnabled(true);
        });
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                Log.e(TAG, "Camera release error: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (loadingLayout.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null && mHolder != null) {
            surfaceCreated(mHolder);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }
}