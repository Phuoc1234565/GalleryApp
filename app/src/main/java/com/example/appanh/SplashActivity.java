package com.example.appanh;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
public class SplashActivity extends BaseActivity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  screen(R.layout.activity_splash);
  ((com.google.android.material.appbar.MaterialToolbar)findViewById(R.id.toolbar)).setNavigationIcon(null);
  click(R.id.start, () -> ready(() -> {
   com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
   go(user == null ? LoginActivity.class : user.getEmail() != null && !user.isEmailVerified() ? OtpActivity.class : MainActivity.class);
   finish();
  }));
  click(R.id.guest, () -> ready(() -> {
   FirebaseAuth.getInstance().signOut();
   startActivity(new android.content.Intent(this,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
   finish();
  }));
 }
 private void ready(Runnable action) {
  findViewById(R.id.start).setEnabled(false);
  findViewById(R.id.guest).setEnabled(false);
  ((GalleryApplication)getApplication()).whenReady(() -> {
   if(isFinishing() || isDestroyed()) return;
   findViewById(R.id.start).setEnabled(true);
   findViewById(R.id.guest).setEnabled(true);
   RuntimeException error = ((GalleryApplication)getApplication()).getInitializationError();
   if(error != null) { toast("Không khởi tạo được Firebase. Kiểm tra google-services.json."); return; }
   action.run();
  });
 }
}
