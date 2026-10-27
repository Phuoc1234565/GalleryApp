package com.example.appanh;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.models.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(AndroidJUnit4.class)
public class MediaRecoveryTest {
 @Test public void inspectLocalReferences() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  for(File cache:context.getFilesDir().listFiles())if(cache.getName().startsWith("gallery-")&&cache.getName().endsWith(".bin")){
   System.out.println("CACHE guest="+cache.getName().equals("gallery-guest.bin"));
   try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(cache))){
    for(Object item:(List<?>)in.readObject())if(item instanceof Photo){Photo p=(Photo)item;System.out.println("PHOTO "+p.getId()+" name="+p.getFileName()+" path="+p.getFilePath()+" exists="+(p.getFilePath()!=null&&new File(p.getFilePath()).isFile())+" bytes="+p.getFileSize()+" dimensions="+p.getWidth()+"x"+p.getHeight()+" deleted="+p.isDeleted()+" hidden="+p.isHidden()+" cloud="+(p.getFirebaseUrl()!=null&&!p.getFirebaseUrl().isEmpty()));}
    for(Object item:(List<?>)in.readObject())if(item instanceof Album){Album a=(Album)item;System.out.println("ALBUM "+a.getName()+" cover="+a.getCoverPhotoPath());}
   }
  }
 }
 @Test public void repairExistingReferencesAndVerify() throws Exception {
  org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("repairMediaReferences")));
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  Map<String,Photo> originals=new HashMap<>();Map<String,Album> covers=new HashMap<>();
  File guest=new File(context.getFilesDir(),"gallery-guest.bin");
  try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(guest))){
   for(Object item:(List<?>)in.readObject())if(item instanceof Photo){Photo p=(Photo)item;if(p.getFilePath()!=null&&new File(p.getFilePath()).isFile())originals.put(p.getId(),p);}
   for(Object item:(List<?>)in.readObject())if(item instanceof Album){Album a=(Album)item;if(a.getCoverPhotoPath()!=null&&new File(a.getCoverPhotoPath()).isFile())covers.put(a.getId(),a);}
  }
  for(File cache:context.getFilesDir().listFiles())if(cache.getName().startsWith("gallery-")&&cache.getName().endsWith(".bin")&&!cache.equals(guest)){
   List<Photo> photos=new ArrayList<>();List<Album> albums=new ArrayList<>();
   try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(cache))){
    for(Object item:(List<?>)in.readObject())if(item instanceof Photo)photos.add((Photo)item);
    for(Object item:(List<?>)in.readObject())if(item instanceof Album)albums.add((Album)item);
   }
   int fixed=0,missingActive=0,missingTrash=0;
   for(Photo photo:photos){
    if(photo.getFilePath()==null||!new File(photo.getFilePath()).isFile()){
     Photo original=originals.get(photo.getId());
     File recovered=photo.getId()!=null&&photo.getId().matches("[A-Za-z0-9_-]{1,128}")?new File(context.getFilesDir(),"media/"+photo.getId()):null;
     if(original==null&&recovered!=null&&recovered.isFile()&&recovered.length()==photo.getFileSize()){photo.setFilePath(recovered.getPath());fixed++;continue;}
     if(original!=null){photo.setFilePath(original.getFilePath());fixed++;}
     else {if(photo.isDeleted())missingTrash++;else missingActive++;System.out.println("UNRECOVERED name="+photo.getFileName()+" deleted="+photo.isDeleted()+" hidden="+photo.isHidden());}
    }
   }
   for(Album album:albums){Album original=covers.get(album.getId());if(original!=null&&(album.getCoverPhotoPath()==null||!new File(album.getCoverPhotoPath()).isFile()))album.setCoverPhotoPath(original.getCoverPhotoPath());}
   android.util.AtomicFile atomic=new android.util.AtomicFile(cache);FileOutputStream stream=atomic.startWrite();
   try{ObjectOutputStream out=new ObjectOutputStream(stream);out.writeObject(photos);out.writeObject(albums);out.flush();atomic.finishWrite(stream);}catch(Exception e){atomic.failWrite(stream);throw e;}
   System.out.println("REPAIRED="+fixed+" MISSING_ACTIVE="+missingActive+" MISSING_TRASH="+missingTrash);
  }
  try(androidx.test.core.app.ActivityScenario<MainActivity> scenario=androidx.test.core.app.ActivityScenario.launch(MainActivity.class)){
   long end=android.os.SystemClock.uptimeMillis()+20000;boolean[] ready={false};
   do{scenario.onActivity(a->{int count=0;for(Photo p:a.repo.photos)if(originals.containsKey(p.getId())&&a.repo.source(p) instanceof File)count++;ready[0]=count>=15;});if(!ready[0])android.os.SystemClock.sleep(100);}while(!ready[0]&&android.os.SystemClock.uptimeMillis()<end);
   org.junit.Assert.assertTrue("Recovered photos must remain readable after account reload",ready[0]);
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(1500);
   android.graphics.Bitmap capture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
   try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"gallery-recovered.png"))){capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}capture.recycle();
   scenario.onActivity(a->a.startActivity(new android.content.Intent(a,AlbumActivity.class)));
   android.os.SystemClock.sleep(2000);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   capture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
   try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"albums-recovered.png"))){capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}capture.recycle();
   InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
    java.util.Collection<android.app.Activity> resumed=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
    for(android.app.Activity activity:resumed)if(activity instanceof AlbumActivity)activity.finish();
   });

  }
 }

 @Test public void sourceRecoversLocalIdWithoutMetadataPath() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String id=UUID.randomUUID().toString();File file=new File(context.getFilesDir(),"media/"+id+".jpg");
  android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(8,8,android.graphics.Bitmap.Config.ARGB_8888);
  try{
   try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,out);}
   Photo photo=new Photo();photo.setId(id);GalleryRepository repo=new GalleryRepository(context);
   org.junit.Assert.assertEquals(file,repo.source(photo));
   photo.setFilePath("/missing/stale-path.jpg");org.junit.Assert.assertEquals(file,repo.source(photo));
   photo.setId("../gallery-guest.bin");org.junit.Assert.assertNull(repo.source(photo));repo.close();
  }finally{bitmap.recycle();file.delete();}
 }

}
