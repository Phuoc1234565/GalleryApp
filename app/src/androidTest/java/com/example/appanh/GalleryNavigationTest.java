package com.example.appanh;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Does not sign out or change the user's photo library. */
@RunWith(AndroidJUnit4.class)
public class GalleryNavigationTest {
    @Test public void searchAndAllFourDestinationsAreVisible() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                assertNavigation(activity, R.id.nav_photos);
                EditText search = activity.findViewById(R.id.search);
                assertTrue(search.getHeight() - search.getCompoundPaddingTop()
                        - search.getCompoundPaddingBottom() >= search.getLineHeight());
                search.setText("Biển Đà Nẵng");
                assertEquals("biển đà nẵng", activity.query);
                search.setText("");
            });
            capture("gallery-fixed.png");
            navigate(R.id.nav_albums, AlbumActivity.class);
            capture("albums-fixed.png");
            navigate(R.id.nav_search, SearchActivity.class);
            capture("search-fixed.png");
            navigate(R.id.nav_profile, SettingsActivity.class);
            capture("settings-fixed.png");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> resumed().findViewById(R.id.account).performClick());
            waitForDestination(AccountActivity.class);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Activity account = resumed();
                assertNotNull(account.findViewById(R.id.profileName));
                if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser()!=null) {
                    EditText name = account.findViewById(R.id.profileName);
                    String before = name.getText().toString();
                    name.setText("");
                    account.findViewById(R.id.saveProfile).performClick();
                    assertNotNull("Empty name must not reach Firestore",name.getError());
                    name.setText(before);
                    name.setError(null);
                }
            });
            capture("account-fixed.png");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> resumed().finish());
            waitForDestination(SettingsActivity.class);
            navigate(R.id.nav_photos, MainActivity.class);
        }
    }

    private void navigate(int id, Class<?> destination) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            BottomNavigationView navigation = resumed().findViewById(R.id.navigation);
            assertNotNull(navigation);
            navigation.setSelectedItemId(id);
        });
        waitForDestination(destination);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Activity current = resumed();
            assertTrue("Wrong destination: " + current.getClass(), destination.isInstance(current));
            assertNavigation(current, id);
        });
    }

    private void waitForDestination(Class<?> destination) {
        long deadline = android.os.SystemClock.uptimeMillis() + 30000;
        boolean[] ready = {false};
        do {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                ready[0] = !activities.isEmpty() && destination.isInstance(activities.iterator().next());
            });
            if (!ready[0]) android.os.SystemClock.sleep(100);
        } while (!ready[0] && android.os.SystemClock.uptimeMillis() < deadline);
        assertTrue("Destination did not resume: " + destination, ready[0]);
    }

    private Activity resumed() {
        Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED);
        assertFalse("No foreground activity", activities.isEmpty());
        return activities.iterator().next();
    }

    private void assertNavigation(Activity activity, int selected) {
        BottomNavigationView navigation = activity.findViewById(R.id.navigation);
        assertEquals(selected, navigation.getSelectedItemId());
        Rect bounds = new Rect();
        for (int id : new int[]{R.id.nav_photos,R.id.nav_albums,R.id.nav_search,R.id.nav_profile}) {
            View item = navigation.findViewById(id);
            assertNotNull(item);
            assertTrue("Tab must be on screen: " + id, item.getGlobalVisibleRect(bounds));
            assertTrue("Tab must have touchable height", bounds.height() >= 48 * activity.getResources().getDisplayMetrics().density);
        }
    }

    private void capture(String name) throws Exception {
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File file = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(), name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            screenshot.compress(Bitmap.CompressFormat.PNG,100,out);
        }
    }
}
