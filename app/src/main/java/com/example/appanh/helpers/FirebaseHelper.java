package com.example.appanh.helpers;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.appanh.models.Album;
import com.example.appanh.models.Notification;
import com.example.appanh.models.Photo;
import com.example.appanh.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton helper class managing all Firebase Authentication, Firestore, and Storage operations.
 */
public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";

    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private static final String USERS_COLLECTION = "users";
    private static final String PHOTOS_COLLECTION = "photos";
    private static final String ALBUMS_COLLECTION = "albums";
    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    private static final String TRASH_COLLECTION = "trash";

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ------------------------------------------------------------------------
    // Auth methods
    // ------------------------------------------------------------------------

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ------------------------------------------------------------------------
    // Firestore references
    // ------------------------------------------------------------------------

    public CollectionReference getUsersRef() {
        return db.collection(USERS_COLLECTION);
    }

    public CollectionReference getPhotosRef() {
        return db.collection(PHOTOS_COLLECTION);
    }

    public CollectionReference getAlbumsRef() {
        return db.collection(ALBUMS_COLLECTION);
    }

    public CollectionReference getNotificationsRef() {
        return db.collection(NOTIFICATIONS_COLLECTION);
    }

    public CollectionReference getTrashRef() {
        return db.collection(TRASH_COLLECTION);
    }

    // ------------------------------------------------------------------------
    // User operations
    // ------------------------------------------------------------------------

    /**
     * Saves or overwrites user information in Firestore.
     */
    public void saveUser(User user, OnCompleteListener listener) {
        String uid = user.getId();
        if (uid == null || uid.trim().isEmpty()) {
            uid = getCurrentUserId();
            user.setId(uid);
        }

        if (uid == null) {
            if (listener != null) listener.onFailure("Không tìm thấy User ID");
            return;
        }

        getUsersRef().document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Retrieves user profile from Firestore by userId.
     */
    public void getUser(String userId, OnDataListener<User> listener) {
        if (userId == null || userId.trim().isEmpty()) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        getUsersRef().document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(snapshot.getId());
                        }
                        if (listener != null) listener.onSuccess(user);
                    } else {
                        if (listener != null) listener.onFailure("Không tìm thấy người dùng");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Updates specific fields of a user document in Firestore.
     */
    public void updateUser(String userId, Map<String, Object> updates, OnCompleteListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        Map<String, Object> finalUpdates = new HashMap<>(updates);
        finalUpdates.put("updatedAt", FieldValue.serverTimestamp());

        getUsersRef().document(userId)
                .update(finalUpdates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    // ------------------------------------------------------------------------
    // Photo operations
    // ------------------------------------------------------------------------

    /**
     * Saves a new photo or updates an existing photo record in Firestore.
     */
    public void savePhoto(Photo photo, OnCompleteListener listener) {
        DocumentReference docRef;
        if (photo.getId() == null || photo.getId().trim().isEmpty()) {
            docRef = getPhotosRef().document();
            photo.setId(docRef.getId());
        } else {
            docRef = getPhotosRef().document(photo.getId());
        }

        docRef.set(photo)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving photo", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Retrieves all active (non-deleted) photos for the given user, sorted descending by date.
     */
    public void getPhotos(String userId, OnDataListener<List<Photo>> listener) {
        if (userId == null) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        getPhotosRef()
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Photo> photoList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Photo p = doc.toObject(Photo.class);
                        p.setId(doc.getId());
                        if (!p.isDeleted()) {
                            photoList.add(p);
                        }
                    }
                    sortPhotosByDateDesc(photoList);
                    if (listener != null) listener.onSuccess(photoList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting photos", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Retrieves all photos belonging to a specific album.
     */
    public void getPhotosByAlbum(String albumId, OnDataListener<List<Photo>> listener) {
        if (albumId == null) {
            if (listener != null) listener.onFailure("Album ID không hợp lệ");
            return;
        }

        getPhotosRef()
                .whereArrayContains("albumIds", albumId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Photo> photoList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Photo p = doc.toObject(Photo.class);
                        p.setId(doc.getId());
                        if (!p.isDeleted()) {
                            photoList.add(p);
                        }
                    }
                    sortPhotosByDateDesc(photoList);
                    if (listener != null) listener.onSuccess(photoList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting photos by album", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Updates specific fields of a photo document.
     */
    public void updatePhoto(String photoId, Map<String, Object> updates, OnCompleteListener listener) {
        if (photoId == null) {
            if (listener != null) listener.onFailure("Photo ID không hợp lệ");
            return;
        }

        Map<String, Object> finalUpdates = new HashMap<>(updates);
        finalUpdates.put("updatedAt", FieldValue.serverTimestamp());

        getPhotosRef().document(photoId)
                .update(finalUpdates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating photo", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Permanently deletes a photo document from Firestore and trash.
     */
    public void deletePhoto(String photoId, OnCompleteListener listener) {
        if (photoId == null) {
            if (listener != null) listener.onFailure("Photo ID không hợp lệ");
            return;
        }

        WriteBatch batch = db.batch();
        batch.delete(getPhotosRef().document(photoId));
        batch.delete(getTrashRef().document(photoId));

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting photo", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Moves a photo to trash (soft-delete).
     */
    public void moveToTrash(Photo photo, OnCompleteListener listener) {
        if (photo == null || photo.getId() == null) {
            if (listener != null) listener.onFailure("Ảnh không hợp lệ");
            return;
        }

        photo.setDeleted(true);
        photo.setDeletedAt(new Date());

        WriteBatch batch = db.batch();

        DocumentReference photoRef = getPhotosRef().document(photo.getId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("deleted", true);
        updates.put("deletedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        batch.update(photoRef, updates);

        DocumentReference trashRef = getTrashRef().document(photo.getId());
        batch.set(trashRef, photo);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error moving photo to trash", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Restores a photo from trash.
     */
    public void restoreFromTrash(String photoId, OnCompleteListener listener) {
        if (photoId == null) {
            if (listener != null) listener.onFailure("Photo ID không hợp lệ");
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference photoRef = getPhotosRef().document(photoId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("deleted", false);
        updates.put("deletedAt", null);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        batch.update(photoRef, updates);

        DocumentReference trashRef = getTrashRef().document(photoId);
        batch.delete(trashRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error restoring photo from trash", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Gets all photos in the trash for a given user.
     */
    public void getTrashPhotos(String userId, OnDataListener<List<Photo>> listener) {
        if (userId == null) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        getTrashRef()
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Photo> trashList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Photo p = doc.toObject(Photo.class);
                        p.setId(doc.getId());
                        trashList.add(p);
                    }
                    if (listener != null) listener.onSuccess(trashList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting trash photos", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Permanently deletes all photos in the trash for a given user.
     */
    public void emptyTrash(String userId, OnCompleteListener listener) {
        if (userId == null) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        getTrashRef()
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        if (listener != null) listener.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        batch.delete(getTrashRef().document(id));
                        batch.delete(getPhotosRef().document(id));
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (listener != null) listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error committing empty trash batch", e);
                                if (listener != null) listener.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying trash to empty", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Toggles favorite status for a photo.
     */
    public void toggleFavorite(String photoId, boolean isFavorite, OnCompleteListener listener) {
        if (photoId == null) {
            if (listener != null) listener.onFailure("Photo ID không hợp lệ");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("favorite", isFavorite);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        getPhotosRef().document(photoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error toggling favorite", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Searches photos for a user matching file name or location query.
     */
    public void searchPhotos(String userId, String query, OnDataListener<List<Photo>> listener) {
        if (userId == null) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        final String cleanQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";

        getPhotos(userId, new OnDataListener<List<Photo>>() {
            @Override
            public void onSuccess(List<Photo> allPhotos) {
                if (cleanQuery.isEmpty()) {
                    if (listener != null) listener.onSuccess(allPhotos);
                    return;
                }

                List<Photo> filtered = new ArrayList<>();
                for (Photo photo : allPhotos) {
                    boolean matchName = photo.getFileName() != null && 
                            photo.getFileName().toLowerCase(Locale.getDefault()).contains(cleanQuery);
                    boolean matchLocation = photo.getLocation() != null && 
                            photo.getLocation().toLowerCase(Locale.getDefault()).contains(cleanQuery);

                    if (matchName || matchLocation) {
                        filtered.add(photo);
                    }
                }
                if (listener != null) listener.onSuccess(filtered);
            }

            @Override
            public void onFailure(String error) {
                if (listener != null) listener.onFailure(error);
            }
        });
    }

    /**
     * Retrieves filtered photos based on filterType: "favorites", "videos", "images", "recent".
     */
    public void getFilteredPhotos(String userId, String filterType, OnDataListener<List<Photo>> listener) {
        getPhotos(userId, new OnDataListener<List<Photo>>() {
            @Override
            public void onSuccess(List<Photo> allPhotos) {
                if (filterType == null || filterType.isEmpty()) {
                    if (listener != null) listener.onSuccess(allPhotos);
                    return;
                }

                List<Photo> filtered = new ArrayList<>();
                String type = filterType.toLowerCase(Locale.getDefault());

                for (Photo p : allPhotos) {
                    switch (type) {
                        case "favorites":
                        case "favorite":
                            if (p.isFavorite()) filtered.add(p);
                            break;
                        case "videos":
                        case "video":
                            if (p.isVideo()) filtered.add(p);
                            break;
                        case "images":
                        case "image":
                        case "photo":
                            if (!p.isVideo()) filtered.add(p);
                            break;
                        case "recent":
                            // Recent last 30 items or non-deleted
                            filtered.add(p);
                            break;
                        default:
                            filtered.add(p);
                            break;
                    }
                }

                if ("recent".equals(type) && filtered.size() > 50) {
                    filtered = new ArrayList<>(filtered.subList(0, 50));
                }

                if (listener != null) listener.onSuccess(filtered);
            }

            @Override
            public void onFailure(String error) {
                if (listener != null) listener.onFailure(error);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Album operations
    // ------------------------------------------------------------------------

    /**
     * Creates or saves an album in Firestore.
     */
    public void createAlbum(Album album, OnCompleteListener listener) {
        DocumentReference docRef;
        if (album.getId() == null || album.getId().trim().isEmpty()) {
            docRef = getAlbumsRef().document();
            album.setId(docRef.getId());
        } else {
            docRef = getAlbumsRef().document(album.getId());
        }

        docRef.set(album)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating album", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Retrieves all albums created by the user.
     */
    public void getAlbums(String userId, OnDataListener<List<Album>> listener) {
        if (userId == null) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        getAlbumsRef()
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Album> albums = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Album album = doc.toObject(Album.class);
                        album.setId(doc.getId());
                        albums.add(album);
                    }
                    if (listener != null) listener.onSuccess(albums);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting albums", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Updates specific fields of an album document.
     */
    public void updateAlbum(String albumId, Map<String, Object> updates, OnCompleteListener listener) {
        if (albumId == null) {
            if (listener != null) listener.onFailure("Album ID không hợp lệ");
            return;
        }

        Map<String, Object> finalUpdates = new HashMap<>(updates);
        finalUpdates.put("updatedAt", FieldValue.serverTimestamp());

        getAlbumsRef().document(albumId)
                .update(finalUpdates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating album", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Deletes an album from Firestore.
     */
    public void deleteAlbum(String albumId, OnCompleteListener listener) {
        if (albumId == null) {
            if (listener != null) listener.onFailure("Album ID không hợp lệ");
            return;
        }

        getAlbumsRef().document(albumId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting album", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Adds a photo to an album and updates both references.
     */
    public void addPhotoToAlbum(String albumId, String photoId, OnCompleteListener listener) {
        if (albumId == null || photoId == null) {
            if (listener != null) listener.onFailure("Album ID hoặc Photo ID không hợp lệ");
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference albumRef = getAlbumsRef().document(albumId);
        batch.update(albumRef, 
                "photoIds", FieldValue.arrayUnion(photoId),
                "photoCount", FieldValue.increment(1),
                "updatedAt", FieldValue.serverTimestamp());

        DocumentReference photoRef = getPhotosRef().document(photoId);
        batch.update(photoRef, 
                "albumIds", FieldValue.arrayUnion(albumId),
                "updatedAt", FieldValue.serverTimestamp());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding photo to album", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Removes a photo from an album and updates both references.
     */
    public void removePhotoFromAlbum(String albumId, String photoId, OnCompleteListener listener) {
        if (albumId == null || photoId == null) {
            if (listener != null) listener.onFailure("Album ID hoặc Photo ID không hợp lệ");
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference albumRef = getAlbumsRef().document(albumId);
        batch.update(albumRef, 
                "photoIds", FieldValue.arrayRemove(photoId),
                "photoCount", FieldValue.increment(-1),
                "updatedAt", FieldValue.serverTimestamp());

        DocumentReference photoRef = getPhotosRef().document(photoId);
        batch.update(photoRef, 
                "albumIds", FieldValue.arrayRemove(albumId),
                "updatedAt", FieldValue.serverTimestamp());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing photo from album", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    // ------------------------------------------------------------------------
    // Notification operations
    // ------------------------------------------------------------------------

    /**
     * Creates and stores a notification.
     */
    public void createNotification(Notification notification, OnCompleteListener listener) {
        DocumentReference docRef;
        if (notification.getId() == null || notification.getId().trim().isEmpty()) {
            docRef = getNotificationsRef().document();
            notification.setId(docRef.getId());
        } else {
            docRef = getNotificationsRef().document(notification.getId());
        }

        docRef.set(notification)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating notification", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Retrieves all notifications for the given user, sorted newest first.
     */
    public void getNotifications(String userId, OnDataListener<List<Notification>> listener) {
        if (userId == null) {
            if (listener != null) listener.onFailure("User ID không hợp lệ");
            return;
        }

        getNotificationsRef()
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Notification n = doc.toObject(Notification.class);
                        n.setId(doc.getId());
                        list.add(n);
                    }
                    // Sort descending by createdAt
                    Collections.sort(list, (n1, n2) -> {
                        Date d1 = n1.getCreatedAt();
                        Date d2 = n2.getCreatedAt();
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    });
                    if (listener != null) listener.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting notifications", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Marks a notification as read.
     */
    public void markNotificationRead(String notificationId, OnCompleteListener listener) {
        if (notificationId == null) {
            if (listener != null) listener.onFailure("Notification ID không hợp lệ");
            return;
        }

        getNotificationsRef().document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as read", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    // ------------------------------------------------------------------------
    // Storage operations
    // ------------------------------------------------------------------------

    /**
     * Uploads a photo to Firebase Storage with progress tracking.
     */
    public void uploadPhoto(Uri fileUri, String userId, OnUploadListener listener) {
        if (fileUri == null || userId == null) {
            if (listener != null) listener.onFailure("File URI hoặc User ID không hợp lệ");
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference()
                .child("users")
                .child(userId)
                .child("photos")
                .child(fileName);

        UploadTask uploadTask = storageRef.putFile(fileUri);

        uploadTask.addOnProgressListener(snapshot -> {
            long totalBytes = snapshot.getTotalByteCount();
            if (totalBytes > 0) {
                int progress = (int) ((100.0 * snapshot.getBytesTransferred()) / totalBytes);
                if (listener != null) {
                    listener.onProgress(progress);
                }
            }
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null ? task.getException() : new Exception("Upload failed");
            }
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            if (listener != null) {
                listener.onSuccess(uri.toString());
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading photo", e);
            if (listener != null) {
                listener.onFailure(e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------------
    // Internal Utilities
    // ------------------------------------------------------------------------

    private void sortPhotosByDateDesc(List<Photo> photos) {
        Collections.sort(photos, (p1, p2) -> {
            Date d1 = p1.getDateTaken() != null ? p1.getDateTaken() : p1.getCreatedAt();
            Date d2 = p2.getDateTaken() != null ? p2.getDateTaken() : p2.getCreatedAt();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });
    }

    // ------------------------------------------------------------------------
    // Callback interfaces
    // ------------------------------------------------------------------------

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnDataListener<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    public interface OnUploadListener {
        void onProgress(int progress);
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }
}
