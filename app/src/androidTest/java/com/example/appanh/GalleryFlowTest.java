package com.example.appanh;

import android.content.*;
import android.graphics.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appanh.models.*;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GalleryFlowTest {
 private Context context;
 @Before public void setup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();FirebaseAuth.getInstance().signOut();}
 @Test public void allScreenLayoutsInflate(){
  try(ActivityScenario<SplashActivity> scenario=ActivityScenario.launch(SplashActivity.class)){
   scenario.onActivity(activity->{int[] layouts={R.layout.activity_splash,R.layout.activity_login,R.layout.activity_register,R.layout.activity_otp,R.layout.activity_main,R.layout.activity_search,R.layout.sheet_filter,R.layout.sheet_options,R.layout.sheet_photo_context,R.layout.activity_photo_detail,R.layout.activity_edit_photo,R.layout.dialog_photo_info,R.layout.activity_album,R.layout.activity_create_album,R.layout.activity_album_detail,R.layout.activity_trash,R.layout.activity_settings,R.layout.activity_google_sign_in,R.layout.activity_backup,R.layout.activity_dark_gallery,R.layout.activity_notifications,R.layout.sheet_share};for(int layout:layouts)assertNotNull(activity.getLayoutInflater().inflate(layout,null));});
   screenshot("preview-splash.png");
  }
 }
 @Test public void importedPhotoPersistsAndAlbumCanBeCreated() throws Exception {
  GalleryRepository repository=new GalleryRepository(context);CountDownLatch loaded=new CountDownLatch(1);repository.load((p,a,e)->loaded.countDown());assertTrue(loaded.await(10,TimeUnit.SECONDS));
  File dir=new File(context.getFilesDir(),"media");dir.mkdirs();String id=UUID.randomUUID().toString();File file=new File(dir,id);Bitmap bitmap=Bitmap.createBitmap(300,200,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.rgb(45,100,230));try(OutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);}
  Photo p=new Photo(null,"test_gallery.jpg",file.getPath(),file.length(),300,200,"image/jpeg");p.setId(id);p.setDateTaken(new Date());repository.photos.add(p);CountDownLatch saved=new CountDownLatch(1);AtomicReference<String> error=new AtomicReference<>();repository.persist(e->{error.set(e);saved.countDown();});assertTrue(saved.await(10,TimeUnit.SECONDS));assertNull(error.get());
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   waitUntil(()->{final boolean[] found={false};scenario.onActivity(a->{RecyclerView grid=a.findViewById(R.id.grid);found[0]=grid.getAdapter()!=null&&grid.getAdapter().getItemCount()>0;});return found[0];});
   screenshot("preview-gallery.png");
  }
  try(ActivityScenario<CreateAlbumActivity> scenario=ActivityScenario.launch(CreateAlbumActivity.class)){
   waitUntil(()->{final boolean[] found={false};scenario.onActivity(a->found[0]=a.repo.find(id)!=null);return found[0];});
   scenario.onActivity(a->{((EditText)a.findViewById(R.id.albumName)).setText("Album kiểm thử");a.selected.add(id);a.findViewById(R.id.create).performClick();});
   waitUntil(()->scenario.getState()==androidx.lifecycle.Lifecycle.State.DESTROYED);
  }
  GalleryRepository fresh=new GalleryRepository(context);CountDownLatch reloaded=new CountDownLatch(1);fresh.load((photos,albums,e)->reloaded.countDown());assertTrue(reloaded.await(10,TimeUnit.SECONDS));Photo restored=fresh.find(id);assertNotNull(restored);assertEquals(1,restored.getAlbumIds().size());assertNotNull(fresh.album(restored.getAlbumIds().get(0)));
  Intent detail=new Intent(context,PhotoDetailActivity.class).putExtra("photoId",id);
  try(ActivityScenario<PhotoDetailActivity> scenario=ActivityScenario.launch(detail)){
   waitUntil(()->{final boolean[] ready={false};scenario.onActivity(a->ready[0]=a.repo.find(id)!=null&&((ImageView)a.findViewById(R.id.photo)).getDrawable()!=null);return ready[0];});
   scenario.onActivity(a->a.findViewById(R.id.favorite).performClick());
   waitUntil(()->{final boolean[] favorite={false};scenario.onActivity(a->favorite[0]=((Button)a.findViewById(R.id.favorite)).getText().toString().contains("Đã yêu thích"));return favorite[0];});
  }
 }
 @Test public void welcomeAndFormsRemainInteractive() throws Exception {
  try(ActivityScenario<LoginActivity> scenario=ActivityScenario.launch(LoginActivity.class)){
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   screenshot("preview-login.png");
   scenario.onActivity(a->{EditText email=a.findViewById(R.id.email);email.setText("invalid-email");((EditText)a.findViewById(R.id.password)).setText("123456");a.findViewById(R.id.submit).performClick();assertTrue(a.findViewById(R.id.submit).isEnabled());email.setText("");});
  }
  try(ActivityScenario<RegisterActivity> scenario=ActivityScenario.launch(RegisterActivity.class)){
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();screenshot("preview-register.png");
   scenario.onActivity(a->assertNotNull(a.findViewById(R.id.name)));
  }
  try(ActivityScenario<OtpActivity> scenario=ActivityScenario.launch(new Intent(context,OtpActivity.class).putExtra("verificationId","test-layout-only").putExtra("phone","+84 ••• ••• 123"))){
   scenario.onActivity(a->{((EditText)a.findViewById(R.id.otp1)).setText("123456");assertEquals("123456",((EditText)a.findViewById(R.id.otp)).getText().toString());});
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();screenshot("preview-otp.png");
  }
 }
 private void waitUntil(Callable<Boolean> condition) throws Exception {long deadline=System.currentTimeMillis()+10000;while(System.currentTimeMillis()<deadline){InstrumentationRegistry.getInstrumentation().waitForIdleSync();if(condition.call())return;Thread.sleep(100);}fail("Điều kiện không đạt trong 10 giây");}
 private void screenshot(String name){Bitmap capture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();if(capture!=null)try(OutputStream out=context.openFileOutput(name,Context.MODE_PRIVATE)){capture.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new AssertionError(e);}}
}
