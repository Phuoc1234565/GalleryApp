package com.example.appanh.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Model class representing a photo or video item in the gallery.
 */
public class Photo implements Serializable {

    @DocumentId
    private String id;
    private String userId;
    private String fileName;
    private String filePath;
    private String thumbnailPath;
    private String firebaseUrl;
    private long fileSize;
    private int width;
    private int height;
    private String mimeType; // image/jpeg, image/png, video/mp4
    private boolean isVideo;
    private long duration; // for videos, in milliseconds
    private String deviceName;
    private String location;
    private boolean isFavorite;
    private boolean isDeleted;
    private boolean hidden;
    private List<String> albumIds;

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    private Date deletedAt;
    private Date dateTaken;

    // Empty constructor for Firestore
    public Photo() {
        this.albumIds = new ArrayList<>();
    }

    public Photo(String userId, String fileName, String filePath, long fileSize, int width, int height, String mimeType) {
        this.userId = userId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.mimeType = mimeType;
        this.isVideo = mimeType != null && mimeType.startsWith("video");
        this.isFavorite = false;
        this.isDeleted = false;
        this.albumIds = new ArrayList<>();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getFirebaseUrl() {
        return firebaseUrl;
    }

    public void setFirebaseUrl(String firebaseUrl) {
        this.firebaseUrl = firebaseUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
        this.isVideo = mimeType != null && mimeType.startsWith("video");
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public List<String> getAlbumIds() {
        if (albumIds == null) {
            albumIds = new ArrayList<>();
        }
        return albumIds;
    }

    public void setAlbumIds(List<String> albumIds) {
        this.albumIds = albumIds != null ? albumIds : new ArrayList<>();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Photo photo = (Photo) o;
        return Objects.equals(id, photo.id) || 
               (filePath != null && Objects.equals(filePath, photo.filePath));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filePath);
    }

    @Override
    public String toString() {
        return "Photo{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", isVideo=" + isVideo +
                ", isFavorite=" + isFavorite +
                ", isDeleted=" + isDeleted +
                '}';
    }
}
