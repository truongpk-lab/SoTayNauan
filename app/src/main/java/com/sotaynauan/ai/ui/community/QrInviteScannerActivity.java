package com.sotaynauan.ai.ui.community;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.sotaynauan.ai.R;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrInviteScannerActivity extends ComponentActivity {
    public static final String EXTRA_QR_PAYLOAD = "extra_qr_payload";

    private static final int REQUEST_CAMERA_PERMISSION = 91;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor mainExecutor = command -> mainHandler.post(command);
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final MultiFormatReader qrReader = new MultiFormatReader();

    private PreviewView previewView;
    private TextView statusText;
    private EditText manualInput;
    private ProcessCameraProvider cameraProvider;
    private volatile boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_invite_scanner);

        previewView = findViewById(R.id.qrCameraPreview);
        statusText = findViewById(R.id.qrScanStatusText);
        manualInput = findViewById(R.id.manualQrPayloadInput);
        Button manualButton = findViewById(R.id.useManualQrPayloadButton);

        findViewById(R.id.cancelQrScanButton).setOnClickListener(view -> finish());
        manualButton.setOnClickListener(view -> returnPayload(manualInput.getText().toString()));

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            statusText.setText("Đang xin quyền camera...");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanExecutor.shutdownNow();
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
            statusText.setText("Không có quyền camera. Bạn vẫn có thể dán mã lời mời.");
        }
    }

    private void startCamera() {
        statusText.setText("Đưa mã QR vào khung hình");
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindCamera();
            } catch (ExecutionException exception) {
                statusText.setText("Camera chưa sẵn sàng. Bạn có thể dán mã lời mời.");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                statusText.setText("Camera chưa sẵn sàng. Bạn có thể dán mã lời mời.");
            }
        }, mainExecutor);
    }

    private void bindCamera() {
        if (cameraProvider == null) {
            return;
        }

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
        Preview preview = new Preview.Builder()
                .setTargetRotation(targetRotation())
                .build();
        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(targetRotation())
                .build();
        analysis.setAnalyzer(scanExecutor, this::analyzeQr);

        try {
            cameraProvider.unbindAll();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            cameraProvider.bindToLifecycle(this, selector, preview, analysis);
        } catch (Exception exception) {
            statusText.setText("Camera chưa sẵn sàng. Bạn có thể dán mã lời mời.");
        }
    }

    @ExperimentalGetImage
    private void analyzeQr(ImageProxy imageProxy) {
        if (finished) {
            imageProxy.close();
            return;
        }
        try {
            Image image = imageProxy.getImage();
            if (image == null || image.getPlanes().length == 0) {
                return;
            }
            byte[] data = copyLumaPlane(image.getPlanes()[0], image.getWidth(), image.getHeight());
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data,
                    image.getWidth(),
                    image.getHeight(),
                    0,
                    0,
                    image.getWidth(),
                    image.getHeight(),
                    false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = qrReader.decodeWithState(bitmap);
            if (result != null) {
                returnPayload(result.getText());
            }
        } catch (NotFoundException ignored) {
            qrReader.reset();
        } finally {
            imageProxy.close();
        }
    }

    private byte[] copyLumaPlane(Image.Plane plane, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        byte[] data = new byte[width * height];

        if (pixelStride == 1 && rowStride == width) {
            buffer.get(data);
            return data;
        }

        byte[] row = new byte[rowStride];
        for (int y = 0; y < height; y++) {
            int bytesToRead = Math.min(rowStride, buffer.remaining());
            if (bytesToRead <= 0) {
                break;
            }
            buffer.get(row, 0, bytesToRead);
            for (int x = 0; x < width; x++) {
                int sourceIndex = x * pixelStride;
                if (sourceIndex < bytesToRead) {
                    data[y * width + x] = row[sourceIndex];
                }
            }
        }
        return data;
    }

    private void returnPayload(String payload) {
        String cleanPayload = payload == null ? "" : payload.trim();
        if (cleanPayload.isEmpty()) {
            mainHandler.post(() -> statusText.setText("Mã lời mời đang trống."));
            return;
        }
        if (finished) {
            return;
        }
        finished = true;
        mainHandler.post(() -> completePayload(cleanPayload));
    }

    private void completePayload(String payload) {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        Intent result = new Intent().putExtra(EXTRA_QR_PAYLOAD, payload);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private int targetRotation() {
        return previewView.getDisplay() == null
                ? Surface.ROTATION_0
                : previewView.getDisplay().getRotation();
    }
}
