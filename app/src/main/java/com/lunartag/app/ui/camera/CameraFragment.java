package com.lunartag.app.ui.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.lunartag.app.R;
import com.lunartag.app.databinding.FragmentCameraBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private FragmentCameraBinding binding;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Check for camera permissions and start the camera if granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            // Permissions are requested in MainActivity, so we can just show a message here.
            Toast.makeText(getContext(), "Camera permissions not granted.", Toast.LENGTH_SHORT).show();
        }

        // Set up the listener for the capture button
        binding.buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        // Placeholder for initializing the OSMDroid map view
        initializeMap();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // Camera provider is now available.
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Set up the Preview use case
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                    // Set up the ImageCapture use case
                    imageCapture = new ImageCapture.Builder().build();

                    // Select the back camera as the default
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    // Unbind everything before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Use case binding failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // Create a file to store the image
        File photoFile = new File(
                getOutputDirectory(),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                        // This is where the main logic happens after a photo is saved:
                        // 1. Get the current location from the location provider.
                        // 2. Read the image file into a Bitmap.
                        // 3. Render the watermark (map image and text) onto the Bitmap.
                        // 4. Save the modified Bitmap back to the file.
                        // 5. Write all required EXIF data (real capture time, custom time, etc.).
                        // 6. Save the photo's metadata to the local Room database.
                        // 7. Schedule the send operation using WorkManager/AlarmManager.
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                }
        );
    }

    private void initializeMap() {
        // Placeholder for OSMDroid map initialization logic.
        // This will involve setting the tile source, map center, zoom level,
        // and adding an overlay for the user's current location.
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getContext().getExternalMediaDirs()[0], getString(R.string.app_name));
        mediaDir.mkdirs();
        return mediaDir;
    }

    private boolean allPermissionsGranted() {
        // Check if all permissions from the required list are granted.
        String[] requiredPermissions = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
          }
