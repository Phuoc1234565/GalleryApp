package com.example.appanh;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.example.appanh.models.Photo;
import com.example.appanh.models.Album;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;

/** Account-scoped local cache; Firestore metadata and Storage originals are separate. */
public final class GalleryRepository {
    public interface Result { void done(String error); }
    public interface Loaded { void done(List<Photo> photos, List<Album> albums, String error); }
    private final Context context;
    public final ArrayList<Photo> photos = new ArrayList<>();
    public final ArrayList<Album> albums = new ArrayList<>();
    private final String owner;
    private static final java.util.concurrent.ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    public GalleryRepository(Context context) {
        this.context = context.getApplicationContext();
        owner = FirebaseAuth.getInstance().getUid();
    }
    public String uid() { return owner; }
    private CollectionReference collection(String name) {
        return FirebaseFirestore.getInstance().collection("users").document(owner).collection(name);
    }
    private File cache() { return new File(context.getFilesDir(), "gallery-" + (owner == null ? "guest" : owner) + ".bin"); }
    public void load(Loaded result) {
        load(result, null);
    }
    public void load(Loaded result, Runnable localReady) {
        io.execute(() -> {
            ArrayList<Photo> localPhotos = new ArrayList<>(); ArrayList<Album> localAlbums = new ArrayList<>();
            String failure = null;
            if (cache().exists()) try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(cache()))) {
                for (Object item : (List<?>) in.readObject()) if (item instanceof Photo) localPhotos.add((Photo)item);
                for (Object item : (List<?>) in.readObject()) if (item instanceof Album) localAlbums.add((Album)item);
            } catch (Exception e) { failure = "Không đọc được bộ nhớ cục bộ: " + e.getMessage(); }
            String localError = failure;
            main.post(() -> {
                for(Photo photo:localPhotos)applyPendingHidden(photo);
                photos.clear(); photos.addAll(localPhotos); albums.clear(); albums.addAll(localAlbums);
                // Gallery screens can draw their local content while cloud reads
                // are pending; detail screens still wait for the final result.
                if (localReady != null) localReady.run();
                if (owner == null) { result.done(photos, albums, localError); return; }
                collection("photos").get().addOnSuccessListener(snapshot -> {
                    Map<String, Photo> merged = new LinkedHashMap<>();
                    // Retain imports not yet backed up. Synced records removed on another device
                    // must disappear instead of being resurrected from this device's cache.
                    for (Photo p : photos) if(p.getFirebaseUrl()==null||p.getFirebaseUrl().isEmpty()) merged.put(p.getId(), p);
                    for (DocumentSnapshot doc : snapshot) {
                        Photo p = doc.toObject(Photo.class); if (p == null) continue; p.setId(doc.getId());
                        Photo local = find(p.getId());
                        File original=localFile(local==null?p:local);
                        if(original!=null)p.setFilePath(original.getPath());
                        applyPendingHidden(p);merged.put(p.getId(), p);
                    }
                    photos.clear(); photos.addAll(merged.values());
                    collection("albums").get().addOnSuccessListener(a -> {
                        Set<String> pending=context.getSharedPreferences("pending-albums-"+owner,0).getStringSet("ids",Collections.emptySet());
                        Map<String,Album> mergedAlbums=new LinkedHashMap<>();
                        for(Album local:albums)if(pending.contains(local.getId()))mergedAlbums.put(local.getId(),local);
                        for (DocumentSnapshot doc : a) { Album album = doc.toObject(Album.class); if (album != null) { album.setId(doc.getId());Album local=album(album.getId());if(local!=null&&local.getCoverPhotoPath()!=null&&new File(local.getCoverPhotoPath()).exists())album.setCoverPhotoPath(local.getCoverPhotoPath());mergedAlbums.put(album.getId(),album); } }
                        albums.clear();albums.addAll(mergedAlbums.values());
                        persist(error -> result.done(photos, albums, error));
                    }).addOnFailureListener(e -> result.done(photos, albums, e.getMessage()));
                }).addOnFailureListener(e -> result.done(photos, albums, "Dùng dữ liệu trên máy. Firebase: " + e.getMessage()));
            });
        });
    }
    public void persist(Result result) {
        ArrayList<Photo> p = new ArrayList<>(photos); ArrayList<Album> a = new ArrayList<>(albums);
        io.execute(() -> {
            String error = null; android.util.AtomicFile atomic = new android.util.AtomicFile(cache()); FileOutputStream stream = null;
            try { stream=atomic.startWrite(); ObjectOutputStream out=new ObjectOutputStream(stream); out.writeObject(p); out.writeObject(a); out.flush(); atomic.finishWrite(stream); }
            catch (Exception e) { if(stream!=null)atomic.failWrite(stream);error=e.getMessage(); }
            String finalError = error; main.post(() -> result.done(finalError));
        });
    }
    /** Append an editor export against the latest disk snapshot, preserving concurrent backup updates. */
    public void appendLocal(Photo photo,Result result){appendLocal(Collections.singletonList(photo),result);}
    public void appendLocal(List<Photo> imported,Result result){
        io.execute(()->{
            ArrayList<Photo> latestPhotos=new ArrayList<>();ArrayList<Album> latestAlbums=new ArrayList<>();
            try{
                if(cache().exists())try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(cache()))){for(Object item:(List<?>)in.readObject())if(item instanceof Photo)latestPhotos.add((Photo)item);for(Object item:(List<?>)in.readObject())if(item instanceof Album)latestAlbums.add((Album)item);}
                for(Photo photo:imported){latestPhotos.removeIf(existing->Objects.equals(existing.getId(),photo.getId()));latestPhotos.add(photo);}
                android.util.AtomicFile atomic=new android.util.AtomicFile(cache());FileOutputStream stream=atomic.startWrite();
                try{ObjectOutputStream out=new ObjectOutputStream(stream);out.writeObject(latestPhotos);out.writeObject(latestAlbums);out.flush();atomic.finishWrite(stream);}catch(Exception error){atomic.failWrite(stream);throw error;}
                main.post(()->{photos.clear();photos.addAll(latestPhotos);albums.clear();albums.addAll(latestAlbums);result.done(null);});
            }catch(Exception error){main.post(()->result.done(error.getLocalizedMessage()));}
        });
    }
    public Photo find(String id) { for (Photo p : photos) if (Objects.equals(p.getId(),id)) return p; return null; }
    private android.content.SharedPreferences hiddenChanges(){return context.getSharedPreferences("pending-hidden-"+(owner==null?"guest":owner),0);}
    private void applyPendingHidden(Photo photo){android.content.SharedPreferences prefs=hiddenChanges();if(prefs.contains(photo.getId()))photo.setHidden(prefs.getBoolean(photo.getId(),false));}
    /** Persist visibility locally first; a pending cloud write must never block hiding. */
    public void setHidden(List<String> ids,boolean hidden,Result result){
        android.content.SharedPreferences.Editor prefs=hiddenChanges().edit();List<Photo> changed=new ArrayList<>();
        for(String id:ids){Photo photo=find(id);if(photo==null||photo.isDeleted())continue;photo.setHidden(hidden);prefs.putBoolean(id,hidden);changed.add(photo);}
        prefs.apply();persist(error->{result.done(error);if(error!=null||owner==null)return;for(Photo photo:changed)save(photo,syncError->{if(syncError==null&&hiddenChanges().contains(photo.getId())&&hiddenChanges().getBoolean(photo.getId(),!hidden)==hidden)hiddenChanges().edit().remove(photo.getId()).apply();});});
    }
    public Album album(String id) { for (Album a : albums) if (Objects.equals(a.getId(),id)) return a; return null; }
    public void save(Photo p, Result result) {
        p.setUserId(owner); p.setUpdatedAt(new Date());
        if (find(p.getId()) == null) photos.add(p);
        if (owner == null) { persist(result); return; }
        // Device-local paths must not be copied to another device through Firestore.
        Map<String,Object> data = new HashMap<>();
        data.put("fileName",p.getFileName()); data.put("firebaseUrl",p.getFirebaseUrl()); data.put("userId",owner);
        data.put("fileSize",p.getFileSize()); data.put("width",p.getWidth()); data.put("height",p.getHeight());
        data.put("mimeType",p.getMimeType()); data.put("video",p.isVideo()); data.put("duration",p.getDuration()); data.put("favorite",p.isFavorite());
        data.put("deleted",p.isDeleted()); data.put("deletedAt",p.getDeletedAt()); data.put("albumIds",p.getAlbumIds());
        data.put("hidden",p.isHidden());
        data.put("dateTaken",p.getDateTaken()); data.put("createdAt",p.getCreatedAt()); data.put("updatedAt",FieldValue.serverTimestamp());
        persist(error -> {
            if (error != null) { result.done(error); return; }
            collection("photos").document(p.getId()).set(data).addOnSuccessListener(v -> result.done(null)).addOnFailureListener(e -> result.done(e.getMessage()));
        });
    }
    public void saveAlbum(Album a, Result result) {
        a.setUserId(owner); if (a.getId() == null) a.setId(UUID.randomUUID().toString());
        if (album(a.getId()) == null) albums.add(a);
        if (owner == null) { persist(result); return; }
        collection("albums").document(a.getId()).set(a).addOnSuccessListener(v -> {clearPendingAlbum(a.getId());persist(result);}).addOnFailureListener(e -> result.done(e.getMessage()));
    }
    public void deleteAlbum(Album a, Result result) {
        // Membership lives on the photo; an orphan ID is harmless and excluded by the UI.
        if (owner == null) { albums.remove(a); persist(result); return; }
        collection("albums").document(a.getId()).delete().addOnSuccessListener(v -> { clearPendingAlbum(a.getId());albums.remove(a); persist(result); }).addOnFailureListener(e -> result.done(e.getMessage()));
    }
    private void clearPendingAlbum(String id){android.content.SharedPreferences prefs=context.getSharedPreferences("pending-albums-"+owner,0);Set<String> ids=new HashSet<>(prefs.getStringSet("ids",Collections.emptySet()));ids.remove(id);prefs.edit().putStringSet("ids",ids).apply();}
    public void upload(Photo p, Result result) {
        if (owner == null) { result.done("Hãy đăng nhập để sao lưu."); return; }
        if (p.getFirebaseUrl() != null && !p.getFirebaseUrl().isEmpty()) { result.done(null); return; }
        if (p.getFilePath() == null || !new File(p.getFilePath()).exists()) { result.done("Không tìm thấy file gốc: " + p.getFileName()); return; }
        com.google.firebase.storage.StorageReference ref = FirebaseStorage.getInstance().getReference().child("users/"+owner+"/media/"+p.getId());
        com.google.firebase.storage.StorageMetadata metadata = new com.google.firebase.storage.StorageMetadata.Builder().setContentType(p.getMimeType()).build();
        ref.putFile(Uri.fromFile(new File(p.getFilePath())),metadata).continueWithTask(task -> { if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException()); return ref.getDownloadUrl(); })
            .addOnSuccessListener(uri -> { p.setFirebaseUrl(uri.toString()); save(p,error -> {
                if(error==null)result.done(null);
                else {p.setFirebaseUrl(null);persist(cacheError->result.done(error));}
            }); }).addOnFailureListener(e -> result.done(e.getMessage()));
    }
    public void remove(Photo p, Result result) {
        Runnable removeMetadata = () -> {
            Runnable removeLocal = () -> { photos.remove(p); if (p.getFilePath()!=null) { File f=new File(p.getFilePath()); if (f.getAbsolutePath().startsWith(new File(context.getFilesDir(),"media").getAbsolutePath()+File.separator)) f.delete(); } persist(result); };
            if (owner == null) removeLocal.run();
            else collection("photos").document(p.getId()).delete().addOnSuccessListener(v -> removeLocal.run()).addOnFailureListener(e -> result.done(e.getMessage()));
        };
        if (owner != null && p.getFirebaseUrl()!=null) FirebaseStorage.getInstance().getReference().child("users/"+owner+"/media/"+p.getId()).delete()
            .addOnSuccessListener(v -> removeMetadata.run()).addOnFailureListener(e -> {
                if (e instanceof com.google.firebase.storage.StorageException && ((com.google.firebase.storage.StorageException)e).getErrorCode()==com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) removeMetadata.run(); else result.done(e.getMessage());
            });
        else removeMetadata.run();
    }
    /** Local imports use the photo ID as filename; recover their path after metadata reload. */
    private File localFile(Photo photo) {
        if(photo.getFilePath()!=null){File file=new File(photo.getFilePath());if(file.isFile())return file;}
        String id=photo.getId();
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,128}"))return null;
        File directory=new File(context.getFilesDir(),"media");
        for(String name:new String[]{id,id+".jpg"}){File file=new File(directory,name);if(file.isFile())return file;}
        return null;
    }
    public Object source(Photo p) { File file=localFile(p);return file!=null?file:p.getFirebaseUrl(); }
    public void close() { /* Shared I/O executor also finishes in-flight uploads after Activity changes. */ }
}
