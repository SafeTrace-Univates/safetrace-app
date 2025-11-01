package com.example.safetrace;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class ScanQRActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private PreviewView previewView;
    private ImageView imageViewVoltar;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        initializeViews();
        setupClickListeners();
        requestCameraPermission();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        imageViewVoltar = findViewById(R.id.imageViewVoltar);
    }

    private void setupClickListeners() {
        imageViewVoltar.setOnClickListener(v -> finish());
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissão de câmera necessária para escanear QR Code", 
                    Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                this.cameraProvider = cameraProvider;
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Erro ao inicializar câmera: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Camera selector
        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build();

        // Image analysis para detectar QR Code
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            processImageProxy(imageProxy);
        });

        // Unbind use cases before rebinding
        cameraProvider.unbindAll();

        // Bind use cases to camera
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void processImageProxy(androidx.camera.core.ImageProxy imageProxy) {
        InputImage image = InputImage.fromMediaImage(
            imageProxy.getImage(), 
            imageProxy.getImageInfo().getRotationDegrees()
        );

        com.google.mlkit.vision.barcode.BarcodeScanner scanner = 
            BarcodeScanning.getClient();

        scanner.process(image)
            .addOnSuccessListener(barcodes -> {
                if (barcodes.size() > 0) {
                    Barcode barcode = barcodes.get(0);
                    String qrCodeText = barcode.getRawValue();
                    
                    if (qrCodeText != null && !qrCodeText.isEmpty()) {
                        // Retornar o resultado e fechar a tela
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("qr_code_result", qrCodeText);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }
                imageProxy.close();
            })
            .addOnFailureListener(e -> {
                imageProxy.close();
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}

