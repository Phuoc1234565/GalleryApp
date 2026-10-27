package com.example.appanh;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

/** The profile document is always users/{current Firebase Auth uid}. */
public final class AccountActivity extends BaseActivity {
    private FirebaseUser user;
    private DocumentReference profile;
    private ListenerRegistration subscription;
    private EditText name, bio;
    private boolean filling, dirty, saving;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable slowSave = () -> {
        if (saving && !isDestroyed()) status("Chưa nhận được xác nhận lưu từ máy chủ. Kiểm tra kết nối mạng; thay đổi đang chờ đồng bộ.");
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen(R.layout.activity_account);
        name = findViewById(R.id.profileName);
        bio = findViewById(R.id.profileBio);
        user = FirebaseAuth.getInstance().getCurrentUser();
        click(R.id.saveProfile, this::save);
        click(R.id.accountLogin, () -> go(LoginActivity.class));
        if (user == null) {
            findViewById(R.id.profileForm).setVisibility(View.GONE);
            findViewById(R.id.accountLogin).setVisibility(View.VISIBLE);
            text(R.id.profileHeading, "Bạn đang dùng chế độ khách");
            text(R.id.profileSubtitle, "Đăng nhập để lưu và quản lý hồ sơ cá nhân của bạn.");
            return;
        }
        profile = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        text(R.id.profileEmail, value(user.getEmail(), "Chưa liên kết email"));
        text(R.id.profilePhone, value(user.getPhoneNumber(), "Chưa liên kết số điện thoại"));
        text(R.id.profileVerification, user.getEmail() == null
                ? "Đăng nhập bằng số điện thoại" : user.isEmailVerified() ? "Email đã xác minh" : "Email chưa xác minh");
        text(R.id.profileSubtitle, value(user.getEmail(), user.getPhoneNumber()));
        fill(value(user.getDisplayName(), ""), "", user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString());
        TextWatcher changes = new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (!filling) { dirty = true; findViewById(R.id.saveProfile).setEnabled(!saving); }
            }
            public void afterTextChanged(Editable text) {}
        };
        name.addTextChangedListener(changes);
        bio.addTextChangedListener(changes);
        findViewById(R.id.saveProfile).setEnabled(false);
        status("Đang tải hồ sơ…");
        if (state != null && state.getBoolean("dirty")) {
            filling = true;
            name.setText(state.getString("name", ""));
            bio.setText(state.getString("bio", ""));
            filling = false;
            dirty = true;
            findViewById(R.id.saveProfile).setEnabled(true);
        }
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (saving) {
                    new androidx.appcompat.app.AlertDialog.Builder(AccountActivity.this)
                            .setTitle("Hồ sơ đang đồng bộ")
                            .setMessage("Máy chủ chưa xác nhận lưu. Bạn có thể ở lại để xem kết quả hoặc rời màn hình và kiểm tra lại sau.")
                            .setNegativeButton("Ở lại",null).setPositiveButton("Rời màn hình",(dialog,which)->finish()).show();
                    return;
                }
                if (!dirty) { finish(); return; }
                new androidx.appcompat.app.AlertDialog.Builder(AccountActivity.this)
                        .setTitle("Thay đổi chưa được lưu")
                        .setMessage("Bạn muốn tiếp tục chỉnh sửa hay bỏ các thay đổi?")
                        .setNegativeButton("Tiếp tục sửa", null)
                        .setPositiveButton("Bỏ thay đổi",(dialog,which)->finish()).show();
            }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        if (profile == null) return;
        subscription = profile.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot,error) -> {
            if (error != null) {
                status("Không tải được hồ sơ: " + error.getLocalizedMessage());
                return;
            }
            if (snapshot == null || saving) return;
            if (!dirty && snapshot.exists()) show(snapshot);
            if (!dirty) status(snapshot.getMetadata().isFromCache()
                    ? "Đang hiển thị dữ liệu trên thiết bị; chờ kết nối máy chủ."
                    : snapshot.exists() ? "Hồ sơ đã đồng bộ." : "Hoàn thiện thông tin rồi nhấn Lưu thay đổi để tạo hồ sơ.");
        });
    }

    private void show(DocumentSnapshot snapshot) {
        fill(value(snapshot.getString("displayName"), value(user.getDisplayName(), "")),
                value(snapshot.getString("bio"), ""),
                value(snapshot.getString("photoUrl"),user.getPhotoUrl()==null?null:user.getPhotoUrl().toString()));
    }

    private void fill(String displayName,String about,String photoUrl) {
        filling = true;
        name.setText(displayName);
        bio.setText(about);
        filling = false;
        text(R.id.profileHeading, value(displayName,"Hồ sơ của bạn"));
        text(R.id.profileInitial, displayName.isEmpty()?"Ả":displayName.substring(0,displayName.offsetByCodePoints(0,1)).toUpperCase(java.util.Locale.ROOT));
        if (photoUrl != null && !photoUrl.isEmpty()) {
            ImageView avatar = findViewById(R.id.profileAvatar);
            avatar.setVisibility(View.VISIBLE);
            Glide.with(this).load(photoUrl).circleCrop().into(avatar);
        }
    }

    private void save() {
        if (user == null || saving || !dirty) return;
        String displayName = name.getText().toString().trim();
        String about = bio.getText().toString().trim();
        if (displayName.isEmpty() || displayName.length() > 80) { name.setError("Họ tên cần từ 1 đến 80 ký tự."); name.requestFocus(); return; }
        if (about.length() > 240) { bio.setError("Giới thiệu tối đa 240 ký tự."); return; }
        // Prevent saving into a different account after an authentication change.
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null || !current.getUid().equals(user.getUid())) { status("Tài khoản đã thay đổi. Mở lại màn hồ sơ."); return; }
        saving = true;
        name.setEnabled(false); bio.setEnabled(false);
        findViewById(R.id.saveProfile).setEnabled(false);
        findViewById(R.id.profileProgress).setVisibility(View.VISIBLE);
        status("Đang lưu hồ sơ…");
        handler.postDelayed(slowSave,15000);
        Map<String,Object> data = new HashMap<>();
        data.put("displayName",displayName); data.put("bio",about);
        data.put("email",user.getEmail()); data.put("phoneNumber",user.getPhoneNumber());
        data.put("emailVerified",user.isEmailVerified()); data.put("updatedAt",FieldValue.serverTimestamp());
        // Merge preserves photos/albums and any other user preferences.
        profile.set(data, SetOptions.merge()).addOnCompleteListener(task -> {
            handler.removeCallbacks(slowSave);
            if (isDestroyed()) return;
            saving = false;
            name.setEnabled(true); bio.setEnabled(true);
            findViewById(R.id.profileProgress).setVisibility(View.GONE);
            if (!task.isSuccessful()) {
                findViewById(R.id.saveProfile).setEnabled(true);
                status("Chưa lưu được hồ sơ: " + (task.getException()==null?"Vui lòng thử lại.":task.getException().getLocalizedMessage()));
                return;
            }
            dirty = false;
            text(R.id.profileHeading,displayName);
            text(R.id.profileInitial,displayName.substring(0,displayName.offsetByCodePoints(0,1)).toUpperCase(java.util.Locale.ROOT));
            status("Đã lưu hồ sơ trên Firestore.");
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(displayName).build())
                    .addOnFailureListener(error -> { if(!isDestroyed())status("Hồ sơ đã lưu; tên trong phiên đăng nhập chưa đồng bộ. Mở lại ứng dụng để thử lại."); });
        });
    }

    private static String value(String text,String fallback) { return text==null||text.trim().isEmpty()?fallback:text; }
    private void text(int id,String value) { ((TextView)findViewById(id)).setText(value); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("dirty",dirty); state.putString("name",name.getText().toString()); state.putString("bio",bio.getText().toString());
        super.onSaveInstanceState(state);
    }
    @Override protected void onStop() { if(subscription!=null){subscription.remove();subscription=null;}super.onStop(); }
    @Override protected void onDestroy() { handler.removeCallbacks(slowSave);super.onDestroy(); }
}
