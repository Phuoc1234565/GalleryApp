package com.example.appanh;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.models.Album;
import com.example.appanh.models.Photo;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class PrepareDemoAlbumsTest {
    @Test public void prepareFourAlbums() throws Exception {
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("prepareDemo")));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String uid=FirebaseAuth.getInstance().getUid();
        File cache=new File(context.getFilesDir(),"gallery-"+(uid==null?"guest":uid)+".bin");
        ArrayList<Photo> photos=new ArrayList<>();ArrayList<Album> albums=new ArrayList<>();
        try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(cache))){
            for(Object item:(List<?>)in.readObject())if(item instanceof Photo)photos.add((Photo)item);
            for(Object item:(List<?>)in.readObject())if(item instanceof Album)albums.add((Album)item);
        }
        for(String kind:new String[]{"family","food"}) {
            String id="gallery-album-demo-"+kind;
            if(photos.stream().anyMatch(photo->id.equals(photo.getId())))continue;
            File file=new File(context.getFilesDir(),"media/album_"+kind+".jpg");
            assertTrue(file.isFile());BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),bounds);assertTrue(bounds.outWidth>0);
            Photo photo=new Photo(uid,"album_"+kind+".jpg",file.getPath(),file.length(),bounds.outWidth,bounds.outHeight,"image/jpeg");
            photo.setId(id);photo.setCreatedAt(new Date());photo.setDateTaken(new Date());photo.setDeviceName("Bộ album demo");photos.add(photo);
        }
        String[] keys={"01-family","02-travel","03-food","04-video"};
        String[] names={"Gia đình","Du lịch","Đồ ăn","Video"};
        Set<String> pending=new HashSet<>(context.getSharedPreferences("pending-albums-"+uid,0).getStringSet("ids",Collections.emptySet()));
        for(int i=0;i<4;i++) {
            String id="demo-album-"+keys[i];Album album=null;
            for(Album candidate:albums)if(id.equals(candidate.getId()))album=candidate;
            if(album==null){album=new Album(uid,names[i],"Album mẫu để trình bày chức năng");album.setId(id);album.setCreatedAt(new Date());albums.add(album);}
            album.setCreatedAt(new Date(System.currentTimeMillis()-i*1000L));
            for(Photo photo:photos) {
                String filename=photo.getFileName();
                boolean member=i==0&&(filename.equals("album_family.jpg")||filename.equals("demo_photo_09.jpg"))
                        ||i==1&&filename.startsWith("demo_photo_")&&!filename.equals("demo_photo_09.jpg")
                        ||i==2&&filename.equals("album_food.jpg")||i==3&&filename.startsWith("demo_video_");
                if(member&&!photo.getAlbumIds().contains(id))photo.getAlbumIds().add(id);
            }
            String cover=i==0?"album_family.jpg":i==1?"demo_photo_07.jpg":i==2?"album_food.jpg":"demo_video_01.mp4";
            album.setCoverPhotoPath(new File(context.getFilesDir(),"media/"+cover).getPath());
            if(i==0)album.setCoverPhotoUrl("https://images.pexels.com/photos/1682497/pexels-photo-1682497.jpeg?w=960");
            if(i==2)album.setCoverPhotoUrl("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=960&q=85");
            pending.add(id);
        }
        context.getSharedPreferences("pending-albums-"+uid,0).edit().putStringSet("ids",pending).commit();
        AtomicFile atomic=new AtomicFile(cache);FileOutputStream stream=atomic.startWrite();
        try{ObjectOutputStream out=new ObjectOutputStream(stream);out.writeObject(photos);out.writeObject(albums);out.flush();atomic.finishWrite(stream);}catch(Exception error){atomic.failWrite(stream);throw error;}
        if(uid!=null) {
            FirebaseFirestore db=FirebaseFirestore.getInstance();WriteBatch batch=db.batch();DocumentReference owner=db.collection("users").document(uid);
            for(Album album:albums)if(album.getId().startsWith("demo-album-")){
                Map<String,Object> data=new HashMap<>();data.put("userId",uid);data.put("name",album.getName());data.put("description",album.getDescription());data.put("createdAt",album.getCreatedAt());data.put("coverPhotoUrl",album.getCoverPhotoUrl());data.put("updatedAt",FieldValue.serverTimestamp());
                batch.set(owner.collection("albums").document(album.getId()),data,SetOptions.merge());
            }
            for(Photo photo:photos)if(photo.getId().startsWith("gallery-demo-")||photo.getId().startsWith("gallery-album-demo-")){
                Map<String,Object> data=new HashMap<>();data.put("userId",uid);data.put("fileName",photo.getFileName());data.put("fileSize",photo.getFileSize());data.put("width",photo.getWidth());data.put("height",photo.getHeight());data.put("mimeType",photo.getMimeType());data.put("video",photo.isVideo());data.put("favorite",photo.isFavorite());data.put("hidden",photo.isHidden());data.put("deleted",photo.isDeleted());data.put("albumIds",photo.getAlbumIds());data.put("firebaseUrl",photo.getFirebaseUrl());data.put("dateTaken",photo.getDateTaken());data.put("createdAt",photo.getCreatedAt());data.put("updatedAt",FieldValue.serverTimestamp());
                batch.set(owner.collection("photos").document(photo.getId()),data,SetOptions.merge());
            }
            try{Tasks.await(batch.commit(),25,TimeUnit.SECONDS);pending.removeIf(id->id.startsWith("demo-album-"));context.getSharedPreferences("pending-albums-"+uid,0).edit().putStringSet("ids",pending).commit();System.out.println("Four demo albums and membership metadata confirmed on Firestore.");}
            catch(Exception error){System.out.println("Albums ready locally; cloud metadata pending: "+error.getMessage());}
        }
        for(String key:keys){String id="demo-album-"+key;assertTrue(albums.stream().anyMatch(album->id.equals(album.getId())));assertTrue(photos.stream().anyMatch(photo->photo.getAlbumIds().contains(id)));}
    }
}
