package com.example.appanh;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.AtomicFile;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.models.Album;
import com.example.appanh.models.Photo;
import com.google.firebase.auth.FirebaseAuth;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Explicit demo preparation only. Normal test runs do not add sample data. */
@RunWith(AndroidJUnit4.class)
public class PrepareDemoMediaTest {
    @Test public void prepareRequestedDemoPack() throws Exception {
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("prepareDemo")));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String uid=FirebaseAuth.getInstance().getUid();
        File cache=new File(context.getFilesDir(),"gallery-"+(uid==null?"guest":uid)+".bin");
        ArrayList<Photo> photos=new ArrayList<>();ArrayList<Album> albums=new ArrayList<>();
        if(cache.exists())try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(cache))){
            for(Object item:(List<?>)in.readObject())if(item instanceof Photo)photos.add((Photo)item);
            for(Object item:(List<?>)in.readObject())if(item instanceof Album)albums.add((Album)item);
        }
        int added=0;
        for(int i=1;i<=13;i++){
            boolean video=i>10;int number=video?i-10:i;
            String filename=String.format(Locale.ROOT,"demo_%s_%02d.%s",video?"video":"photo",number,video?"mp4":"jpg");
            File file=new File(context.getFilesDir(),"media/"+filename);
            assertTrue("Missing demo file: "+filename,file.isFile()&&file.length()>1000);
            String id="gallery-demo-"+filename;
            boolean exists=false;for(Photo photo:photos)if(id.equals(photo.getId()))exists=true;
            if(exists)continue;
            int width,height;long duration=0;
            if(video){
                try(MediaMetadataRetriever metadata=new MediaMetadataRetriever()){
                    metadata.setDataSource(file.getPath());
                    width=Integer.parseInt(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    height=Integer.parseInt(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    duration=Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    assertNotNull("Video must decode: "+filename,metadata.getFrameAtTime(0));
                }
            }else{
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                BitmapFactory.decodeFile(file.getPath(),bounds);width=bounds.outWidth;height=bounds.outHeight;
            }
            assertTrue(width>0&&height>0);
            Photo photo=new Photo(uid,filename,file.getPath(),file.length(),width,height,video?"video/mp4":"image/jpeg");
            photo.setId(id);photo.setDuration(duration);photo.setDeviceName("Bộ ảnh demo");
            Date date=new Date(System.currentTimeMillis()-i*60000L);
            photo.setCreatedAt(date);photo.setDateTaken(date);photos.add(photo);added++;
        }
        AtomicFile atomic=new AtomicFile(cache);FileOutputStream stream=atomic.startWrite();
        try{
            ObjectOutputStream out=new ObjectOutputStream(stream);out.writeObject(photos);out.writeObject(albums);out.flush();atomic.finishWrite(stream);
        }catch(Exception error){atomic.failWrite(stream);throw error;}
        int count=0;for(Photo photo:photos)if(photo.getId().startsWith("gallery-demo-"))count++;
        assertEquals(13,count);
        System.out.println("Demo media ready: 10 photos, 3 videos; added "+added+". Existing library preserved.");
    }
}
