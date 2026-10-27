package com.example.appanh;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.appanh.helpers.PhotoHelper;
import com.example.appanh.models.Photo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;

public class SettingsActivity extends BaseActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen(R.layout.activity_settings);
        click(R.id.account,()->go(AccountActivity.class));
        click(R.id.backup,()->go(BackupActivity.class));
        click(R.id.notifications,()->go(NotificationsActivity.class));
        click(R.id.trash,()->go(TrashActivity.class));
        click(R.id.hiddenMedia,()->go(HiddenMediaActivity.class));
        click(R.id.storage,this::showStorage);
        click(R.id.theme,this::chooseTheme);
        click(R.id.language,()->new AlertDialog.Builder(this).setTitle("Ngôn ngữ")
                .setSingleChoiceItems(new String[]{"Tiếng Việt"},0,(dialog,which)->dialog.dismiss())
                .setNegativeButton("Đóng",null).show());
        click(R.id.about,()->new AlertDialog.Builder(this).setTitle("Ảnh 1.0")
                .setMessage("Lưu giữ khoảnh khắc đẹp\n\nQuản lý ảnh và video, tạo album và chỉnh sửa những kỷ niệm của bạn.")
                .setPositiveButton("Đóng",null).show());
        click(R.id.help,()->new AlertDialog.Builder(this).setTitle("Trợ giúp")
                .setMessage("1. Tab Ảnh → dấu + để thêm ảnh/video.\n2. Nhấn giữ ảnh/video → Ẩn để đưa vào mục đã ẩn.\n3. Hồ sơ → Ảnh và video đã ẩn → nhấn giữ → Bỏ ẩn.\n4. Tạo album ở tab Album.\n5. Hồ sơ → Tài khoản để sửa thông tin.\n6. Thùng rác cho phép khôi phục ảnh đã xóa.\n\nDữ liệu khách và từng tài khoản được lưu riêng. Sao lưu chỉ minh họa giao diện. Ẩn là bộ lọc trong app, chưa có khóa mật khẩu.")
                .setPositiveButton("Đã hiểu",null).show());
        click(R.id.logout,()->{
            if(FirebaseAuth.getInstance().getCurrentUser()==null){go(LoginActivity.class);return;}
            confirm("Đăng xuất tài khoản?",()->{
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        detail(R.id.accountDetail,user==null?"Chế độ khách · Đăng nhập":user.getEmail()==null?user.getPhoneNumber():user.getEmail());
        detail(R.id.themeDetail,getSharedPreferences("settings",0).getBoolean("dark",false)?"Tối":"Sáng");
        repo.load((photos,albums,error)->{if(!isDestroyed())summary();},()->{if(!isDestroyed())summary();});
    }

    private void summary() {
        long bytes=0;int active=0,trash=0,synced=0;
        for(Photo photo:repo.photos){bytes+=photo.getFileSize();if(photo.isDeleted())trash++;else{active++;if(photo.getFirebaseUrl()!=null&&!photo.getFirebaseUrl().isEmpty())synced++;}}
        detail(R.id.storageDetail,PhotoHelper.formatFileSize(bytes)+" · "+repo.photos.size()+" mục");
        detail(R.id.trashDetail,trash==0?"Không có ảnh đã xóa":trash+" mục · Có thể khôi phục");
        int hidden=0;for(Photo photo:repo.photos)if(!photo.isDeleted()&&photo.isHidden())hidden++;
        detail(R.id.hiddenMediaDetail,hidden==0?"Quản lý các mục đã ẩn":hidden+" mục · Nhấn để xem và bỏ ẩn");
        boolean auto=getSharedPreferences("backup-"+repo.uid(),0).getBoolean("auto",false);
        detail(R.id.backupDetail,"Giao diện minh họa · Chưa kích hoạt");
        try {
            int count=new JSONArray(getSharedPreferences("notifications-"+repo.uid(),0).getString("items","[]")).length();
            detail(R.id.notificationsDetail,count==0?"Chưa có hoạt động mới":count+" hoạt động gần đây");
        }catch(org.json.JSONException ignored){detail(R.id.notificationsDetail,"Lịch sử hoạt động");}
    }

    private void showStorage() {
        long bytes=0;int images=0,videos=0,trash=0;
        for(Photo photo:repo.photos){bytes+=photo.getFileSize();if(photo.isDeleted())trash++;else if(photo.isVideo())videos++;else images++;}
        new AlertDialog.Builder(this).setTitle("Dung lượng thư viện")
                .setMessage(PhotoHelper.formatFileSize(bytes)+"\n\n"+images+" ảnh · "+videos+" video\n"+trash+" mục trong thùng rác\n\nDung lượng tính theo các tệp trong thư viện của tài khoản hiện tại.")
                .setPositiveButton("Đóng",null).setNeutralButton("Mở thùng rác",(dialog,which)->go(TrashActivity.class)).show();
    }

    private void chooseTheme() {
        boolean dark=getSharedPreferences("settings",0).getBoolean("dark",false);
        new AlertDialog.Builder(this).setTitle("Giao diện")
                .setSingleChoiceItems(new String[]{"Sáng","Tối"},dark?1:0,(dialog,index)->{
                    dialog.dismiss();boolean next=index==1;if(next==dark)return;
                    ThemeManager.setDark(this,next);
                }).setNegativeButton("Đóng",null).show();
    }

    private void detail(int id,String value){((TextView)findViewById(id)).setText(value);}
}
