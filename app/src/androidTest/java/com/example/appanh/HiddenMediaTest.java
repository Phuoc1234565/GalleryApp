package com.example.appanh;

import android.content.*;
import android.os.SystemClock;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.models.Photo;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;

@RunWith(AndroidJUnit4.class)
public class HiddenMediaTest {
    @Test public void hidePhotoAndVideoThenRestoreFromDedicatedScreen() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<String> ids=Arrays.asList("gallery-album-demo-food","gallery-demo-demo_video_01.mp4");
        Map<String,Boolean> previous=new HashMap<>();
        try{
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                await(scenario,ids);
                scenario.onActivity(a->{for(String id:ids){assertNotNull(a.repo.find(id));previous.put(id,a.repo.find(id).isHidden());assertFalse("Use unhidden demo fixtures",a.repo.find(id).isHidden());}});
                for(String id:ids){
                    scenario.onActivity(a->{((EditText)a.findViewById(R.id.search)).setText(a.repo.find(id).getFileName());});
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    scenario.onActivity(a->{RecyclerView grid=a.findViewById(R.id.grid);assertNotNull(grid.findViewHolderForAdapterPosition(0));grid.findViewHolderForAdapterPosition(0).itemView.performLongClick();});
                    onView(withText("Ẩn ảnh/video")).inRoot(isDialog()).check(matches(isDisplayed())).perform(click());
                    long end=SystemClock.uptimeMillis()+5000;boolean[] hidden={false};do{scenario.onActivity(a->hidden[0]=a.repo.find(id).isHidden()&&a.visible().isEmpty());if(!hidden[0])SystemClock.sleep(100);}while(!hidden[0]&&SystemClock.uptimeMillis()<end);assertTrue(hidden[0]);
                }
                scenario.onActivity(a->{((EditText)a.findViewById(R.id.search)).setText("");for(Photo photo:a.visible())assertFalse(ids.contains(photo.getId()));});
            }
            try(ActivityScenario<AlbumDetailActivity> album=ActivityScenario.launch(new Intent(context,AlbumDetailActivity.class).putExtra("albumId","demo-album-03-food"))){await(album,ids);album.onActivity(a->{for(Photo photo:a.visible())assertFalse(ids.contains(photo.getId()));});}
            try(ActivityScenario<HiddenMediaActivity> hidden=ActivityScenario.launch(HiddenMediaActivity.class)){
                await(hidden,ids);hidden.onActivity(a->{assertTrue(a.showHidden);Set<String> seen=new HashSet<>();for(Photo photo:a.visible())seen.add(photo.getId());assertTrue(seen.containsAll(ids));assertEquals("Ảnh và video đã ẩn",((com.google.android.material.appbar.MaterialToolbar)a.findViewById(R.id.toolbar)).getTitle().toString());});
                for(String id:ids){hidden.onActivity(a->((EditText)a.findViewById(R.id.search)).setText(a.repo.find(id).getFileName()));InstrumentationRegistry.getInstrumentation().waitForIdleSync();hidden.onActivity(a->((RecyclerView)a.findViewById(R.id.grid)).findViewHolderForAdapterPosition(0).itemView.performLongClick());InstrumentationRegistry.getInstrumentation().waitForIdleSync();onView(withText("Bỏ ẩn")).inRoot(isDialog()).check(matches(isDisplayed())).perform(click());long deadline=SystemClock.uptimeMillis()+5000;boolean[] restored={false};do{hidden.onActivity(a->restored[0]=!a.repo.find(id).isHidden()&&a.visible().isEmpty());if(!restored[0])SystemClock.sleep(100);}while(!restored[0]&&SystemClock.uptimeMillis()<deadline);assertTrue(restored[0]);}
            }
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){await(scenario,ids);scenario.onActivity(a->{Set<String> visible=new HashSet<>();for(Photo photo:a.visible())visible.add(photo.getId());assertTrue(visible.containsAll(ids));});}
        }finally{
            CountDownLatch done=new CountDownLatch(previous.size());InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{GalleryRepository repo=new GalleryRepository(context);repo.load((photos,albums,error)->{for(String id:ids)if(previous.containsKey(id))repo.setHidden(Collections.singletonList(id),previous.get(id),ignored->done.countDown());});});assertTrue(done.await(20,TimeUnit.SECONDS));
        }
    }
    private <T extends GalleryActivity> void await(ActivityScenario<T> scenario,List<String> ids){boolean[] ready={false};long end=SystemClock.uptimeMillis()+20000;do{scenario.onActivity(a->{ready[0]=true;for(String id:ids)if(a.repo.find(id)==null)ready[0]=false;});if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<end);assertTrue("Demo media must load",ready[0]);InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
}
