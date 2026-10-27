package com.example.appanh;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class AlbumDemoUiTest {
    @Test public void demoAlbumsHaveRealContentsAndMenus() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(ActivityScenario<AlbumActivity> scenario=ActivityScenario.launch(AlbumActivity.class)){
            waitReady(scenario,"demo-album-01-family");
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            // Allow the local-first render and the following cloud refresh to
            // settle before asserting the four image requests are complete.
            SystemClock.sleep(1000);
            long deadline=SystemClock.uptimeMillis()+15000;boolean[] covers={false};
            do{scenario.onActivity(activity->{RecyclerView grid=activity.findViewById(R.id.grid);covers[0]=true;for(int i=0;i<4;i++){RecyclerView.ViewHolder card=grid.findViewHolderForAdapterPosition(i);if(card==null||!(((android.widget.ImageView)card.itemView.findViewById(R.id.thumbnail)).getDrawable() instanceof android.graphics.drawable.BitmapDrawable))covers[0]=false;}});if(!covers[0])SystemClock.sleep(100);}while(!covers[0]&&SystemClock.uptimeMillis()<deadline);
            assertTrue("All four covers must show real media",covers[0]);capture("albums-demo.png");
            scenario.onActivity(activity->{RecyclerView grid=activity.findViewById(R.id.grid);assertTrue(grid.getAdapter().getItemCount()>=4);RecyclerView.ViewHolder first=grid.findViewHolderForAdapterPosition(0);assertNotNull(first);assertNotNull(first.itemView.findViewById(R.id.albumMore));first.itemView.findViewById(R.id.albumMore).performClick();});
            onView(withText("Đổi tên")).check(matches(isDisplayed()));
            onView(withText("Thêm ảnh")).check(matches(isDisplayed()));
            capture("album-menu-demo.png");pressBack();
        }
        for(String key:new String[]{"01-family","02-travel","03-food","04-video"}){
            String id="demo-album-"+key;
            try(ActivityScenario<AlbumDetailActivity> scenario=ActivityScenario.launch(new Intent(context,AlbumDetailActivity.class).putExtra("albumId",id))){
                waitReady(scenario,id);
                scenario.onActivity(activity->{assertFalse(activity.visible().isEmpty());for(com.example.appanh.models.Photo photo:activity.visible())assertTrue(photo.getAlbumIds().contains(id));assertNotNull(((com.google.android.material.appbar.MaterialToolbar)activity.findViewById(R.id.toolbar)).getMenu().findItem(R.id.addPhotos));});
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();capture("album-"+key+".png");
            }
        }
    }
    private <T extends GalleryActivity> void waitReady(ActivityScenario<T> scenario,String id){
        long deadline=SystemClock.uptimeMillis()+15000;boolean[] ready={false};
        do{scenario.onActivity(activity->{RecyclerView grid=activity.findViewById(R.id.grid);ready[0]=activity.repo.album(id)!=null&&grid.getAdapter()!=null&&grid.getAdapter().getItemCount()>0;});if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Demo album must load",ready[0]);
    }
    private void capture(String name)throws Exception{
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
}
