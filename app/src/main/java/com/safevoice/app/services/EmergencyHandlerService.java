package com.safevoice.app.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.safevoice.app.models.Contact;
import com.safevoice.app.utils.ContactsManager;
import com.safevoice.app.utils.LocationHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * This service is responsible for handling the emergency alert logic.
 * It is started by VoiceRecognitionService upon detecting the trigger phrase.
 * It fetches the location, checks network connectivity, and dispatches alerts.
 */
public class EmergencyHandlerService extends Service {

    private static final String TAG = "EmergencyHandlerService";

    private LocationHelper locationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        locationHelper = new LocationHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Emergency sequence initiated.");
        Toast.makeText(this, "Emergency Triggered! Sending alerts...", Toast.LENGTH_LONG).show();

        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Cannot proceed with emergency alerts. Missing permissions.");
            stopSelf();
            return START_NOT_STICKY;
        }

        locationHelper.getCurrentLocation(new LocationHelper.LocationResultCallback() {
            @Override
            public void onLocationResult(Location location) {
                if (location != null) {
                    Log.d(TAG, "Location acquired: " + location.getLatitude() + ", " + location.getLongitude());
                    executeEmergencyActions(location);
                } else {
                    Log.e(TAG, "Failed to acquire location. Sending alerts without it.");
                    executeEmergencyActions(null);
                }
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    /**
     * Executes the main emergency logic (calling, sending SMS) based on network state.
     *
     * @param location The user's current location. Can be null if fetching failed.
     */
    private void executeEmergencyActions(Location location) {
        // --- THIS IS THE FIX ---
        // Get the singleton instance of the ContactsManager.
        ContactsManager contactsManager = ContactsManager.getInstance(this);

        // Retrieve the saved contacts instead of using hardcoded placeholders.
        Contact primaryContact = contactsManager.getPrimaryContact();
        List<Contact> priorityContacts = contactsManager.getPriorityContacts();

        // Make the primary phone call.
        if (primaryContact != null) {
            makePhoneCall(primaryContact.getPhoneNumber());
        } else {
            Log.w(TAG, "No primary contact set. Cannot make emergency call.");
        }

        // Check for internet connectivity (though SMS doesn't strictly need it, it's good practice).
        if (isOnline()) {
            Log.d(TAG, "Device is online. Sending SMS alerts.");
            // Send SMS alerts to all priority contacts.
            if (priorityContacts != null && !priorityContacts.isEmpty()) {
                for (Contact contact : priorityContacts) {
                    sendSmsAlert(contact.getPhoneNumber(), location);
                }
            } else {
                Log.w(TAG, "No priority contacts set. Cannot send SMS alerts.");
            }
        } else {
            Log.d(TAG, "Device is offline. Only primary phone call was made.");
        }
    }

    /**
     * Initiates a direct phone call to the specified number.
     *
     * @param phoneNumber The number to call.
     */
    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.e(TAG, "Phone number is invalid. Cannot make call.");
            return;
        }

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            Log.i(TAG, "Attempting to call " + phoneNumber);
            startActivity(callIntent);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: CALL_PHONE permission might be missing or denied.", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initiate phone call.", e);
        }
    }

    /**
     * Sends an SMS alert to the specified number.
     *
     * @param phoneNumber The number to send the SMS to.
     * @param location    The user's location, used to generate a map link.
     */
    private void sendSmsAlert(String phoneNumber, Location location) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.e(TAG, "Priority contact phone number is invalid. Cannot send SMS.");
            return;
        }
        
        // --- THIS IS THE FIX ---
        // Get the user's name dynamically for a personalized message.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userName = "the user"; // Default name
        if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            // We use the Google display name. In a more advanced version,
            // you would fetch the "verifiedName" from Firestore.
            userName = currentUser.getDisplayName();
        }

        String message = "EMERGENCY: This is an automated alert from Safe Voice for " + userName + ". They may be in trouble.";

        if (location != null) {
            String mapLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
            message += "\n\nTheir last known location is:\n" + mapLink;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> messageParts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);
            Log.i(TAG, "SMS alert sent to " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS to " + phoneNumber, e);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}