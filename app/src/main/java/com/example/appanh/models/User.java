package com.example.appanh.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Model class representing a registered user and preferences.
 */
public class User implements Serializable {

    @DocumentId
    private String id;
    private String email;
    private String displayName;
    private String photoUrl;
    private String phoneNumber;
    private boolean isEmailVerified;
    private String provider; // "email", "google"
    private long storageUsed; // in bytes
    private boolean autoBackup;
    private boolean wifiOnlyBackup;
    private boolean darkMode;
    private String language; // "vi", "en"
    private String sortOrder; // "date_desc", "date_asc", "name", "size"

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date lastLoginAt;

    // Empty constructor for Firestore
    public User() {}

    public User(String id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.provider = "email";
        this.storageUsed = 0L;
        this.autoBackup = false;
        this.wifiOnlyBackup = true;
        this.darkMode = false;
        this.language = "vi";
        this.sortOrder = "date_desc";
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getStorageUsed() {
        return storageUsed;
    }

    public void setStorageUsed(long storageUsed) {
        this.storageUsed = storageUsed;
    }

    public boolean isAutoBackup() {
        return autoBackup;
    }

    public void setAutoBackup(boolean autoBackup) {
        this.autoBackup = autoBackup;
    }

    public boolean isWifiOnlyBackup() {
        return wifiOnlyBackup;
    }

    public void setWifiOnlyBackup(boolean wifiOnlyBackup) {
        this.wifiOnlyBackup = wifiOnlyBackup;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", provider='" + provider + '\'' +
                ", autoBackup=" + autoBackup +
                '}';
    }
}
