package com.safevoice.app.models;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * A simple data model class (POJO) to represent an emergency contact.
 * It includes helper methods for converting the object to and from a JSONObject,
 * which is useful for storing it in SharedPreferences.
 */
public class Contact {

    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_PHONE = "phoneNumber";
    private static final String JSON_KEY_UID = "uid";

    private String name;
    private String phoneNumber;
    private String uid;

    public Contact(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.uid = null; // Ensure uid is null for the original constructor
    }

    public Contact(String name, String phoneNumber, String uid) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.uid = uid;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUid() {
        return uid;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * Converts this Contact object into a JSONObject.
     *
     * @return A JSONObject representation of the contact, or null on error.
     */
    @Nullable
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSON_KEY_NAME, this.name);
            jsonObject.put(JSON_KEY_PHONE, this.phoneNumber);
            // Use putOpt so it doesn't add the key if the value is null
            jsonObject.putOpt(JSON_KEY_UID, this.uid);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a Contact object from a JSONObject.
     *
     * @param jsonObject The JSONObject to parse.
     * @return A new Contact object, or null if parsing fails.
     */
    @Nullable
    public static Contact fromJSONObject(JSONObject jsonObject) {
        try {
            String name = jsonObject.getString(JSON_KEY_NAME);
            String phone = jsonObject.getString(JSON_KEY_PHONE);
            // Use optString to safely get the uid, returns an empty string or default if not found
            String uid = jsonObject.optString(JSON_KEY_UID, null);
            return new Contact(name, phone, uid);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Overriding equals and hashCode is important for managing lists of contacts,
    // for example, to correctly find and remove a specific contact.
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Contact contact = (Contact) obj;
        // If UIDs exist, they are the definitive source of truth for equality.
        if (uid != null && contact.uid != null) {
            return uid.equals(contact.uid);
        }
        // Fallback to name and phone for non-app contacts (like primary contact)
        return Objects.equals(name, contact.name) &&
               Objects.equals(phoneNumber, contact.phoneNumber);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for safe and correct hash code generation
        return Objects.hash(name, phoneNumber, uid);
    }
}