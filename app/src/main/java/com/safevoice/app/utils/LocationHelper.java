package com.safevoice.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * A helper class to simplify the process of getting the device's current location.
 * It uses the FusedLocationProviderClient for efficient location fetching.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;

    /**
     * Interface to provide the location result asynchronously.
     */
    public interface LocationResultCallback {
        void onLocationResult(Location location);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Fetches the current location of the device.
     * This method requests a single, high-accuracy update.
     *
     * @param callback The callback to be invoked with the location result.
     */
    public void getCurrentLocation(final LocationResultCallback callback) {
        // First, check if location permissions have been granted.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            Log.e(TAG, "Location permission not granted. Cannot fetch location.");
            // Immediately call the callback with a null location.
            callback.onLocationResult(null);
            return;
        }

        // Use the modern getCurrentLocation API for a one-time location request.
        // This is more efficient than requesting continuous updates.
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // This is the success case. The location object can be null if a location is not available.
                    if (location != null) {
                        Log.d(TAG, "Successfully retrieved location using getCurrentLocation.");
                        callback.onLocationResult(location);
                    } else {
                        Log.w(TAG, "getCurrentLocation returned a null location. This can happen if location is turned off.");
                        // If it fails, we fall back to requesting a location update.
                        requestLocationUpdate(callback);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "getCurrentLocation failed.", e);
                    // If it fails, we fall back to requesting a location update.
                    requestLocationUpdate(callback);
                }
            });
    }

    /**
     * A fallback method to request location updates if getCurrentLocation fails.
     * It sets up a request and waits for one update before removing the listener.
     */
    private void requestLocationUpdate(final LocationResultCallback callback) {
        // Define the parameters for the location request.
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 second interval
                .setMinUpdateIntervalMillis(5000) // 5 second minimum interval
                .setMaxUpdates(1) // We only need one update.
                .build();

        // Create a LocationCallback to handle the received location.
        final LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Remove the callback immediately to stop further updates and save battery.
                fusedLocationClient.removeLocationUpdates(this);
                
                if (locationResult.getLastLocation() != null) {
                    Log.d(TAG, "Successfully retrieved location using requestLocationUpdates.");
                    callback.onLocationResult(locationResult.getLastLocation());
                } else {
                    Log.e(TAG, "LocationResult was null after fallback request.");
                    callback.onLocationResult(null);
                }
            }
        };

        // Check for permissions again, as this is a security requirement.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
             Log.e(TAG, "Permission check failed before requesting location updates.");
             callback.onLocationResult(null);
        }
    }
               }
