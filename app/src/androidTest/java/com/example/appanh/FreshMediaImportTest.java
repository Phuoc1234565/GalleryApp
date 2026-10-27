package com.example.appanh;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.models.Photo;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

@RunWith(AndroidJUnit4.class)
public class FreshMediaImportTest {
 private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
 private final Context context=instrumentation.getTargetContext();
 private void waitFor(Callable<Boolean> check) throws Exception {long end=SystemClock.uptimeMillis()+30000;do{if(check.call())return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);dump(instrumentation.getUiAutomation().getRootInActiveWindow(),0);fail("Timed out waiting for import UI");}
 private void dump(AccessibilityNodeInfo node,int level){if(node==null||level>16)return;System.out.println("PICKER "+node.getViewIdResourceName()+" text="+node.getText()+" desc="+node.getContentDescription()+" clickable="+node.isClickable());for(int i=0;i<node.getChildCount();i++)dump(node.getChild(i),level+1);}
 private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String value){if(node==null)return null;if(value.contentEquals(String.valueOf(node.getText()))||value.contentEquals(String.valueOf(node.getContentDescription())))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=find(node.getChild(i),value);if(child!=null)return child;}return null;}
 private boolean action(String value,int action){AccessibilityNodeInfo node=find(instrumentation.getUiAutomation().getRootInActiveWindow(),value);while(node!=null){if(node.performAction(action))return true;node=node.getParent();}return false;}
 private boolean any(String... labels){for(String label:labels)if(action(label,AccessibilityNodeInfo.ACTION_CLICK))return true;return false;}
 private boolean scroll(AccessibilityNodeInfo node){if(node==null)return false;if(node.isScrollable()&&node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true;for(int i=0;i<node.getChildCount();i++)if(scroll(node.getChild(i)))return true;return false;}
 private void selectFile(String name,boolean first) throws Exception {
  for(int attempt=0;attempt<10;attempt++){
   if(action(name,first?AccessibilityNodeInfo.ACTION_LONG_CLICK:AccessibilityNodeInfo.ACTION_CLICK)){SystemClock.sleep(200);return;}
   scroll(instrumentation.getUiAutomation().getRootInActiveWindow());SystemClock.sleep(300);
  }
  dump(instrumentation.getUiAutomation().getRootInActiveWindow(),0);fail("Could not select "+name);
 }
 private void capture(String name) throws Exception {SystemClock.sleep(1500);Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}

 @Test public void invalidMediaDoesNotAddBrokenItems() throws Exception {
  File dir=new File(context.getCacheDir(),"shared");dir.mkdirs();List<File> files=new ArrayList<>();List<android.net.Uri> uris=new ArrayList<>();
  try{
   for(String suffix:new String[]{".png",".mp4",".jpg",".txt"}){File file=new File(dir,"INVALID_"+UUID.randomUUID()+suffix);try(FileOutputStream out=new FileOutputStream(file)){if(!suffix.equals(".jpg"))out.write("not valid media".getBytes(java.nio.charset.StandardCharsets.UTF_8));}files.add(file);uris.add(FileProvider.getUriForFile(context,context.getPackageName()+".files",file));}
   ImportViewModel[] model={null};instrumentation.runOnMainSync(()->{model[0]=new ImportViewModel((Application)context.getApplicationContext());model[0].start(uris);});
   waitFor(()->{boolean[] finished={false};instrumentation.runOnMainSync(()->finished[0]=!model[0].busy);return finished[0];});
   assertEquals(0,model[0].importedCount);assertEquals(4,model[0].failedCount);
  }finally{for(File file:files)file.delete();}
 }

 @Test public void addFreshPackThroughSystemPicker() throws Exception {
  assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("addFreshMedia")));
  List<String> names=new ArrayList<>();try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir(),"fresh-import-manifest.txt")),java.nio.charset.StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)if(!line.trim().isEmpty())names.add(line.trim());}
  String[] paths=new String[names.size()];for(int i=0;i<paths.length;i++)paths[i]="/sdcard/Download/GalleryFresh/"+names.get(i);
  java.util.concurrent.CountDownLatch scanned=new java.util.concurrent.CountDownLatch(paths.length);
  android.media.MediaScannerConnection.scanFile(context,paths,null,(path,uri)->{System.out.println("SCANNED "+path+" => "+uri);scanned.countDown();});
  assertTrue(scanned.await(30,java.util.concurrent.TimeUnit.SECONDS));
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   waitFor(()->{boolean[] ready={false};scenario.onActivity(a->ready[0]=!a.repo.photos.isEmpty());return ready[0];});
   Set<String> existing=new HashSet<>();List<String> pending=new ArrayList<>(names);String[] owner={null};
   scenario.onActivity(a->{owner[0]=a.repo.uid();for(Photo p:a.repo.photos){existing.add(p.getId());pending.remove(p.getFileName());}});
   if(!pending.isEmpty()){
    scenario.onActivity(a->a.findViewById(R.id.importMedia).performClick());
    waitFor(()->{AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();return root!=null&&String.valueOf(root.getPackageName()).contains("documentsui");});
    waitFor(()->any("Show roots","Open navigation drawer","Hiện gốc"));
    waitFor(()->any("Downloads","Tải xuống"));
    waitFor(()->any("GalleryFresh"));
    for(int i=0;i<pending.size();i++)selectFile(pending.get(i),i==0);
    capture("fresh-picker-selection.png");
    waitFor(()->any("Open","OPEN","Mở","MỞ","Select","SELECT","Chọn","CHỌN"));
   }
   waitFor(()->{boolean[] ready={false};scenario.onActivity(a->{Set<String> found=new HashSet<>();for(Photo p:a.repo.photos)if(names.contains(p.getFileName()))found.add(p.getFileName());ready[0]=found.size()==names.size();});return ready[0];});
   scenario.onActivity(a->{assertEquals(owner[0],a.repo.uid());for(String id:existing)assertNotNull("Existing media must remain",a.repo.find(id));for(Photo p:a.repo.photos)if(names.contains(p.getFileName())){assertTrue(a.repo.source(p) instanceof File);assertTrue(p.getWidth()>0&&p.getHeight()>0);if(p.isVideo())assertTrue(p.getDuration()>0);}});
   scenario.recreate();
   waitFor(()->{boolean[] ready={false};scenario.onActivity(a->{int found=0;for(Photo p:a.repo.photos)if(names.contains(p.getFileName()))found++;ready[0]=found==names.size();});return ready[0];});
   capture("fresh-library.png");
   scenario.onActivity(a->a.findViewById(R.id.videos).performClick());capture("fresh-videos.png");
   scenario.onActivity(a->a.findViewById(R.id.all).performClick());
  }
 }
 @Test public void indexDemoAndReplacePhotoBasedVideos() throws Exception {
  assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("replaceFreshVideos")));
  List<String> scanPaths=new ArrayList<>();
  for(int i=1;i<=10;i++)scanPaths.add(String.format(Locale.ROOT,"/sdcard/Download/GalleryDemo/demo_photo_%02d.jpg",i));
  for(int i=1;i<=3;i++)scanPaths.add(String.format(Locale.ROOT,"/sdcard/Download/GalleryDemo/demo_video_%02d.mp4",i));
  scanPaths.add("/sdcard/Download/GalleryDemo/album_family.jpg");scanPaths.add("/sdcard/Download/GalleryDemo/album_food.jpg");
  String[] oldNames={"Moi_Video_Meo.mp4","Moi_Video_Pho_dem.mp4","Moi_Video_Bien_binh_minh.mp4"};
  String[] newNames={"Video_Buom_canh_trong.mp4","Video_Ong_tren_hoa.mp4","Video_Thac_nuoc.mp4"};
  for(String name:newNames)scanPaths.add("/sdcard/Download/GalleryFresh/"+name);
  java.util.concurrent.CountDownLatch removedScan=new java.util.concurrent.CountDownLatch(oldNames.length);
  String[] removedPaths=new String[oldNames.length];for(int i=0;i<oldNames.length;i++)removedPaths[i]="/sdcard/Download/GalleryFresh/"+oldNames[i];
  android.media.MediaScannerConnection.scanFile(context,removedPaths,null,(path,uri)->removedScan.countDown());
  assertTrue(removedScan.await(30,java.util.concurrent.TimeUnit.SECONDS));
  java.util.concurrent.CountDownLatch scanned=new java.util.concurrent.CountDownLatch(scanPaths.size());
  Set<String> indexed=java.util.Collections.synchronizedSet(new HashSet<>());
  android.media.MediaScannerConnection.scanFile(context,scanPaths.toArray(new String[0]),null,(path,uri)->{if(uri!=null)indexed.add(path);scanned.countDown();});
  assertTrue(scanned.await(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals("Every shared file must be indexed",scanPaths.size(),indexed.size());
  GalleryRepository repo=new GalleryRepository(context);java.util.concurrent.CountDownLatch loaded=new java.util.concurrent.CountDownLatch(1);
  instrumentation.runOnMainSync(()->repo.load((photos,albums,error)->loaded.countDown()));assertTrue(loaded.await(30,java.util.concurrent.TimeUnit.SECONDS));
  List<Photo> targets=new ArrayList<>();
  for(int i=0;i<oldNames.length;i++){Photo match=null;for(Photo p:repo.photos)if(oldNames[i].equals(p.getFileName())||newNames[i].equals(p.getFileName()))match=p;assertNotNull("Expected the recently added video",match);targets.add(match);}
  for(int i=0;i<targets.size();i++){
   Photo photo=targets.get(i);File incoming=new File(context.getCacheDir(),"shared/"+newNames[i]);assertTrue(incoming.isFile());
   android.media.MediaMetadataRetriever metadata=new android.media.MediaMetadataRetriever();int width,height;long duration;
   try{metadata.setDataSource(incoming.getPath());width=Integer.parseInt(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));height=Integer.parseInt(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));duration=Long.parseLong(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION));Bitmap first=metadata.getFrameAtTime(0);Bitmap later=metadata.getFrameAtTime(2000000);assertNotNull(first);assertNotNull(later);assertFalse("Video must contain real changing frames",first.sameAs(later));first.recycle();later.recycle();}finally{metadata.release();}
   assertTrue(width>0&&height>0&&duration>=5900);
   Object source=repo.source(photo);assertTrue(source instanceof File);File target=(File)source;
   assertTrue(target.getCanonicalPath().startsWith(new File(context.getFilesDir(),"media").getCanonicalPath()+File.separator));
   android.util.AtomicFile atomic=new android.util.AtomicFile(target);FileOutputStream output=atomic.startWrite();
   try(InputStream input=new FileInputStream(incoming)){byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);atomic.finishWrite(output);}catch(Exception e){atomic.failWrite(output);throw e;}
   String name=newNames[i];java.util.concurrent.CountDownLatch saved=new java.util.concurrent.CountDownLatch(1);String[] failure={null};
   instrumentation.runOnMainSync(()->{photo.setFileName(name);photo.setFilePath(target.getPath());photo.setFileSize(target.length());photo.setMimeType("video/mp4");photo.setWidth(width);photo.setHeight(height);photo.setDuration(duration);repo.save(photo,error->{failure[0]=error;saved.countDown();});});
   assertTrue(saved.await(30,java.util.concurrent.TimeUnit.SECONDS));assertNull(failure[0]);
  }
  java.util.concurrent.CountDownLatch reloaded=new java.util.concurrent.CountDownLatch(1);GalleryRepository fresh=new GalleryRepository(context);
  instrumentation.runOnMainSync(()->fresh.load((photos,albums,error)->reloaded.countDown()));assertTrue(reloaded.await(30,java.util.concurrent.TimeUnit.SECONDS));
  for(int i=0;i<targets.size();i++){Photo after=fresh.find(targets.get(i).getId());assertNotNull(after);assertEquals(newNames[i],after.getFileName());assertTrue(fresh.source(after) instanceof File);assertEquals(targets.get(i).isFavorite(),after.isFavorite());assertEquals(targets.get(i).isHidden(),after.isHidden());assertEquals(targets.get(i).getAlbumIds(),after.getAlbumIds());}
  repo.close();fresh.close();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   waitFor(()->{boolean[] ready={false};scenario.onActivity(a->ready[0]=a.repo.find(targets.get(0).getId())!=null);return ready[0];});
   scenario.onActivity(a->a.findViewById(R.id.videos).performClick());capture("distinct-videos.png");scenario.onActivity(a->a.findViewById(R.id.all).performClick());
   scenario.onActivity(a->a.findViewById(R.id.importMedia).performClick());
   waitFor(()->{AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();return root!=null&&String.valueOf(root.getPackageName()).contains("documentsui");});
   waitFor(()->any("Show roots","Open navigation drawer","Hiện gốc"));waitFor(()->any("Downloads","Tải xuống"));waitFor(()->any("GalleryDemo"));
   waitFor(()->find(instrumentation.getUiAutomation().getRootInActiveWindow(),"demo_photo_01.jpg")!=null||find(instrumentation.getUiAutomation().getRootInActiveWindow(),"album_family.jpg")!=null);
   capture("gallery-demo-visible.png");
   Set<String> seen=new HashSet<>();
   for(int page=0;page<10;page++){AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();for(String path:scanPaths){String name=new File(path).getName();if(path.contains("GalleryDemo")&&find(root,name)!=null)seen.add(name);}if(!scroll(root))break;SystemClock.sleep(400);}
   assertEquals("All 15 original demo files must be selectable",15,seen.size());
   long time=SystemClock.uptimeMillis();instrumentation.getUiAutomation().injectInputEvent(new android.view.KeyEvent(time,time,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_BACK,0),true);instrumentation.getUiAutomation().injectInputEvent(new android.view.KeyEvent(time,time,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_BACK,0),true);
  }
 }

}
