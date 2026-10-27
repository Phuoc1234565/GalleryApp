package com.example.appanh;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ThemeSwitchTest {
    @Test public void bothThemesReachAllLayoutsAndExistingActivities() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean original=ThemeManager.isDark(context);
        ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class);
        try {
            ui(()->{MainActivity activity=(MainActivity)foreground();activity.findViewById(R.id.videos).performClick();((EditText)activity.findViewById(R.id.search)).setText("demo");((com.google.android.material.bottomnavigation.BottomNavigationView)activity.findViewById(R.id.navigation)).setSelectedItemId(R.id.nav_profile);});
            await(SettingsActivity.class,original);
            for(boolean dark:new boolean[]{false,true}) {
                ui(()->ThemeManager.setDark(foreground(),dark));
                await(SettingsActivity.class,dark);
                ui(()->{
                    SettingsActivity activity=(SettingsActivity)foreground();assertAllLayouts(activity);
                    int background=activity.getColor(R.color.background);
                    assertEquals(background,((ColorDrawable)activity.findViewById(R.id.screenRoot).getBackground()).getColor());
                    assertTrue(ColorUtils.calculateContrast(activity.getColor(R.color.text_primary),background)>=4.5);
                    assertTrue(ColorUtils.calculateContrast(activity.getColor(R.color.text_secondary),background)>=4.5);
                    AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Giao diện").setMessage("Nội dung dễ đọc").setPositiveButton("Đóng",null).create();dialog.show();
                    android.widget.TextView message=dialog.findViewById(android.R.id.message);
                    assertTrue(ColorUtils.calculateContrast(message.getCurrentTextColor(),activity.getColor(R.color.surface))>=4.5);dialog.dismiss();
                    BottomSheetDialog sheet=new BottomSheetDialog(activity);sheet.setContentView(R.layout.sheet_filter);sheet.show();assertEquals(dark,night(sheet.getContext()));sheet.dismiss();
                });
                capture("settings-"+(dark?"dark":"light")+".png");
                ui(()->((com.google.android.material.bottomnavigation.BottomNavigationView)foreground().findViewById(R.id.navigation)).setSelectedItemId(R.id.nav_photos));
                await(MainActivity.class,dark);
                ui(()->{MainActivity activity=(MainActivity)foreground();assertEquals("videos",activity.type);assertEquals("demo",activity.query);assertEquals(activity.getColor(R.color.text_primary),((EditText)activity.findViewById(R.id.search)).getCurrentTextColor());});
                capture("gallery-"+(dark?"dark":"light")+".png");
                ui(()->((com.google.android.material.bottomnavigation.BottomNavigationView)foreground().findViewById(R.id.navigation)).setSelectedItemId(R.id.nav_profile));
                await(SettingsActivity.class,dark);
            }
        } finally {
            ui(()->{java.util.HashSet<android.app.Activity> all=new java.util.HashSet<>();for(androidx.test.runner.lifecycle.Stage stage:new androidx.test.runner.lifecycle.Stage[]{androidx.test.runner.lifecycle.Stage.RESUMED,androidx.test.runner.lifecycle.Stage.STARTED,androidx.test.runner.lifecycle.Stage.STOPPED})all.addAll(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage));for(android.app.Activity activity:all)activity.finish();ThemeManager.setDark(context,original);});
            scenario.close();
        }
    }

    private void ui(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private android.app.Activity foreground(){return androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).iterator().next();}
    private void await(Class<?> expected,boolean dark){
        long deadline=SystemClock.uptimeMillis()+30000;boolean[] ready={false};
        do {ui(()->{java.util.Collection<android.app.Activity> list=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);ready[0]=!list.isEmpty()&&expected.isInstance(list.iterator().next())&&night(list.iterator().next())==dark;});if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Expected theme on "+expected,ready[0]);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private boolean night(Context context) {return (context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
    private void assertAllLayouts(SettingsActivity activity) {
        int[] layouts={R.layout.activity_account,R.layout.activity_album_detail,R.layout.activity_album,
                R.layout.activity_backup,R.layout.activity_create_album,R.layout.activity_dark_gallery,
                R.layout.activity_edit_photo,R.layout.activity_google_sign_in,R.layout.activity_login,
                R.layout.activity_main,R.layout.activity_notifications,R.layout.activity_otp,
                R.layout.activity_photo_detail,R.layout.activity_register,R.layout.activity_search,
                R.layout.activity_settings,R.layout.activity_splash,R.layout.activity_trash,
                R.layout.dialog_photo_info,R.layout.item_album,R.layout.item_media_row,R.layout.item_media,
                R.layout.item_notification,R.layout.item_photo_grid,R.layout.sheet_filter,R.layout.sheet_options,
                R.layout.sheet_photo_context,R.layout.sheet_share,R.layout.sheet_album_options};
        for(int layout:layouts) {
            View view=activity.getLayoutInflater().inflate(layout,null);
            assertNotNull(view);
            if(layout==R.layout.activity_photo_detail||layout==R.layout.activity_edit_photo)
                assertEquals(activity.getColor(R.color.viewer_background),((ColorDrawable)view.getBackground()).getColor());
        }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(350);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
}
