package com.lunartag.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.lunartag.app.databinding.ActivityMainBinding;
import com.lunartag.app.firebase.RemoteConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The main screen of the application, which hosts the bottom navigation and various fragments.
 * It is responsible for requesting all necessary runtime permissions upon creation.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    private ActivityResultLauncher<String[]> permissionLauncher;

    // We will populate this dynamically based on Android Version to prevent crashes
    private String[] requiredPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the permissions list based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+): Needs READ_MEDIA_IMAGES
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            // Android 12 and below: Needs WRITE_EXTERNAL_STORAGE
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        // NEW: Trigger the Remote Config fetch immediately when the app starts.
        // This replaces the passive FCM listener with an active check.
        RemoteConfigManager.fetchRemoteConfig(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.navView, navController);
        }

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> results) {
                        boolean allGranted = true;
                        for (Boolean granted : results.values()) {
                            if (!granted) {
                                allGranted = false;
                                break;
                            }
                        }

                        if (allGranted) {
                            Toast.makeText(MainActivity.this, "All permissions granted. Lunar Tag is ready.", Toast.LENGTH_SHORT).show();
                            // This is the action to take after permissions are successfully granted.
                            onPermissionsGranted();
                        } else {
                            Toast.makeText(MainActivity.this, "Some permissions were denied. Core features may not work.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        checkAndRequestPermissions();
    }

    /**
     * Checks which of the required permissions are not yet granted and requests them.
     */
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        boolean allPermissionsAlreadyGranted = true;
        for (String permission : requiredPermissions) {
            if (permission != null && ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
                allPermissionsAlreadyGranted = false;
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }

        // If all permissions were already granted from a previous launch,
        // perform the success action immediately.
        if (allPermissionsAlreadyGranted) {
            onPermissionsGranted();
        }
    }

    /**
     * Helper method to be called once all necessary permissions have been granted.
     */
    private void onPermissionsGranted() {
        // This is a placeholder for any logic that needs to run after permissions are confirmed.
        // For example, initializing location services or other components.
        // This structure matches your reference file exactly.
    }
}