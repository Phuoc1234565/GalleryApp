package com.example.appanh;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.models.Photo;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FinalPolishTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Set<String> fixtures=new HashSet<>();
    private File input;
    private static final String PREFIX="FINAL_QA_";
    private void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private void waitFor(Callable<Boolean> check) throws Exception {
        long end=android.os.SystemClock.uptimeMillis()+20000;
        do{idle();if(check.call())return;Thread.sleep(100);}while(android.os.SystemClock.uptimeMillis()<end);
        fail("Timed out waiting for screen or import");
    }
    private Photo fixture(String name,boolean video,long date){
        Photo p=new Photo(null,PREFIX+name,null,100,64,64,video?"video/mp4":"image/jpeg");
        p.setId(UUID.randomUUID().toString());p.setDateTaken(new Date(date));fixtures.add(p.getId());return p;
    }
    private void append(List<Photo> photos) throws Exception {
        CountDownLatch done=new CountDownLatch(1);String[] failure={null};
        new GalleryRepository(context).appendLocal(photos,error->{failure[0]=error;done.countDown();});
        assertTrue(done.await(20,TimeUnit.SECONDS));assertNull(failure[0]);
    }
    @After public void cleanup() throws Exception {
        GalleryRepository repo=new GalleryRepository(context);CountDownLatch loaded=new CountDownLatch(1);
        repo.load((p,a,e)->loaded.countDown());assertTrue(loaded.await(20,TimeUnit.SECONDS));
        repo.photos.removeIf(p->{boolean test=fixtures.contains(p.getId())||(p.getFileName()!=null&&input!=null&&p.getFileName().equals(input.getName()));
            if(test&&p.getFilePath()!=null)new File(p.getFilePath()).delete();return test;});
        CountDownLatch done=new CountDownLatch(1);repo.persist(error->done.countDown());assertTrue(done.await(20,TimeUnit.SECONDS));
        if(input!=null)input.delete();
    }
    @Test public void renderPreservesGridAndShowsUsefulSearchEmptyState() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            waitFor(()->{boolean[] loaded={false};scenario.onActivity(a->loaded[0]=((RecyclerView)a.findViewById(R.id.grid)).getAdapter()!=null);return loaded[0];});
            scenario.onActivity(a->{
                a.repo.photos.clear();for(int i=0;i<90;i++)a.repo.photos.add(fixture("Ảnh "+i,false,i));a.render();
                RecyclerView grid=a.findViewById(R.id.grid);RecyclerView.Adapter<?> adapter=grid.getAdapter();RecyclerView.LayoutManager layout=grid.getLayoutManager();
                grid.scrollToPosition(50);a.selecting=true;a.selected.add(a.repo.photos.get(50).getId());a.render();
                assertSame(adapter,grid.getAdapter());assertSame(layout,grid.getLayoutManager());
                EditText search=a.findViewById(R.id.search);search.setText("khong co ket qua nay");
                assertEquals(0,grid.getAdapter().getItemCount());assertEquals(View.VISIBLE,a.findViewById(R.id.emptyState).getVisibility());
                android.view.ViewGroup empty=a.findViewById(R.id.emptyState);assertEquals("Không tìm thấy kết quả",((TextView)empty.getChildAt(1)).getText().toString());
                search.setText("anh 50");assertEquals(1,grid.getAdapter().getItemCount());
                a.getOnBackPressedDispatcher().onBackPressed();assertFalse(a.selecting);assertTrue(a.selected.isEmpty());
            });
        }
    }
    @Test public void viewerKeepsFilteredOrderAndCurrentPhotoAfterRecreation() throws Exception {
        Photo first=fixture("A.jpg",false,1),second=fixture("B.jpg",false,2),outside=fixture("Other.jpg",false,3);
        first.getAlbumIds().add("final-qa-album");second.getAlbumIds().add("final-qa-album");
        append(Arrays.asList(first,second,outside));
        Intent intent=new Intent(context,PhotoDetailActivity.class).putExtra("photoId",first.getId()).putExtra("albumId","final-qa-album").putExtra("sort","oldest");
        try(ActivityScenario<PhotoDetailActivity> scenario=ActivityScenario.launch(intent)){
            waitFor(()->{boolean[] ready={false};scenario.onActivity(a->ready[0]=((MaterialToolbar)a.findViewById(R.id.toolbar)).getTitle()!=null);return ready[0];});
            scenario.onActivity(a->{View photo=a.findViewById(R.id.photo);long t=android.os.SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,photo.getWidth()*.9f,50,0);
                MotionEvent up=MotionEvent.obtain(t,t+100,MotionEvent.ACTION_UP,photo.getWidth()*.1f,50,0);
                photo.dispatchTouchEvent(down);photo.dispatchTouchEvent(up);down.recycle();up.recycle();
                assertEquals(second.getId(),a.getIntent().getStringExtra("photoId"));});
            scenario.recreate();
            waitFor(()->{boolean[] ready={false};scenario.onActivity(a->ready[0]=second.getFileName().contentEquals(String.valueOf(((MaterialToolbar)a.findViewById(R.id.toolbar)).getTitle())));return ready[0];});
        }
    }
    @Test public void videoCannotOpenImageEditor() throws Exception {
        Photo video=fixture("video.mp4",true,1);append(Collections.singletonList(video));
        try(ActivityScenario<PhotoDetailActivity> scenario=ActivityScenario.launch(new Intent(context,PhotoDetailActivity.class).putExtra("photoId",video.getId()))){
            waitFor(()->{boolean[] ready={false};scenario.onActivity(a->ready[0]=!a.findViewById(R.id.edit).isEnabled());return ready[0];});
        }
    }
    @Test public void importSurvivesRecreationAndMergesExistingLibrary() throws Exception {
        File dir=new File(context.getFilesDir(),"media");dir.mkdirs();input=new File(dir,PREFIX+UUID.randomUUID()+".jpg");
        Bitmap bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);
        try(FileOutputStream out=new FileOutputStream(input)){bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);}bitmap.recycle();
        Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".files",input);
        List<Uri> uris=new ArrayList<>();for(int i=0;i<12;i++)uris.add(uri);
        Photo existing=fixture("before-import.jpg",false,1);append(Collections.singletonList(existing));
        ImportViewModel[] model={null};
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            waitFor(()->{boolean[] ready={false};scenario.onActivity(a->ready[0]=a.repo.find(existing.getId())!=null);return ready[0];});
            for(int round=1;round<=3;round++){
                scenario.onActivity(a->{model[0]=new ViewModelProvider(a).get(ImportViewModel.class);model[0].start(uris);assertTrue(model[0].busy);});
                scenario.recreate();scenario.onActivity(a->assertSame(model[0],new ViewModelProvider(a).get(ImportViewModel.class)));
                waitFor(()->!model[0].busy);assertEquals("All valid files must import; failures="+model[0].failedCount,12,model[0].importedCount);
                int expected=round*12;
                waitFor(()->{boolean[] ready={false};scenario.onActivity(a->{int count=0;for(Photo p:a.repo.photos)if(input.getName().equals(p.getFileName()))count++;ready[0]=count==expected&&a.repo.find(existing.getId())!=null;});return ready[0];});
            }
        }
    }
}
