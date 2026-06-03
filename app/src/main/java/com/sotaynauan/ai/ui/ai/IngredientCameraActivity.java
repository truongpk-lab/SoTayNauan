package com.sotaynauan.ai.ui.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;
import com.sotaynauan.ai.R;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class IngredientCameraActivity extends ComponentActivity {
    public static final String EXTRA_IMAGE_PATH = "ingredient_image_path";

    private static final int REQUEST_CAMERA_PERMISSION = 71;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor mainExecutor = command -> mainHandler.post(command);

    private PreviewView previewView;
    private TextView statusText;
    private TextView switchButton;
    private Button captureButton;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean hasBackCamera;
    private boolean hasFrontCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_camera);

        previewView = findViewById(R.id.cameraPreview);
        statusText = findViewById(R.id.cameraStatus);
        switchButton = findViewById(R.id.switchCameraButton);
        captureButton = findViewById(R.id.captureIngredientButton);

        findViewById(R.id.cancelCameraButton).setOnClickListener(view -> finish());
        switchButton.setOnClickListener(view -> switchCamera());
        captureButton.setOnClickListener(view -> takePhoto());

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            statusText.setText(R.string.ingredient_camera_starting);
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            statusText.setText(R.string.ingredient_camera_permission_denied);
            captureButton.setEnabled(false);
            switchButton.setVisibility(View.INVISIBLE);
        }
    }

    private void startCamera() {
        statusText.setText(R.string.ingredient_camera_starting);
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                if (!hasBackCamera && !hasFrontCamera) {
                    showNoCamera();
                    return;
                }
                lensFacing = hasBackCamera
                        ? CameraSelector.LENS_FACING_BACK
                        : CameraSelector.LENS_FACING_FRONT;
                bindCameraUseCases();
            } catch (CameraInfoUnavailableException | ExecutionException exception) {
                showNoCamera();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                showNoCamera();
            }
        }, mainExecutor);
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        Preview preview = new Preview.Builder()
                .setTargetRotation(targetRotation())
                .build();
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(targetRotation())
                .build();

        try {
            cameraProvider.unbindAll();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            statusText.setText(R.string.ingredient_camera_ready);
            captureButton.setEnabled(true);
            boolean canSwitch = hasBackCamera && hasFrontCamera;
            switchButton.setVisibility(canSwitch ? View.VISIBLE : View.INVISIBLE);
        } catch (Exception exception) {
            showNoCamera();
        }
    }

    private void switchCamera() {
        if (!hasBackCamera || !hasFrontCamera) {
            return;
        }
        lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    private void takePhoto() {
        ImageCapture activeImageCapture = imageCapture;
        if (activeImageCapture == null) {
            statusText.setText(R.string.ingredient_camera_unavailable);
            return;
        }

        captureButton.setEnabled(false);
        statusText.setText(R.string.ingredient_camera_capturing);
        File photoFile = createImageFile();
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        activeImageCapture.takePicture(outputOptions, mainExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Intent result = new Intent()
                                .putExtra(EXTRA_IMAGE_PATH, photoFile.getAbsolutePath());
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        captureButton.setEnabled(true);
                        statusText.setText(R.string.ingredient_camera_capture_failed);
                    }
                });
    }

    private File createImageFile() {
        File directory = new File(getCacheDir(), "ingredient-scans");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = String.format(Locale.US, "ingredient_%d.jpg",
                System.currentTimeMillis());
        return new File(directory, fileName);
    }

    private int targetRotation() {
        return previewView.getDisplay() == null
                ? Surface.ROTATION_0
                : previewView.getDisplay().getRotation();
    }

    private void showNoCamera() {
        statusText.setText(R.string.ingredient_camera_unavailable);
        captureButton.setEnabled(false);
        switchButton.setVisibility(View.INVISIBLE);
    }
}
