package com.example.appanh;

import android.os.Bundle;
import android.util.Log;
import com.example.appanh.models.Photo;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;

/** Cloud backup: uploads local photos to Firebase Storage and saves firebaseUrl to Firestore. */
public class BackupActivity extends BaseActivity {
    private static final String TAG = "BackupActivity";

    @Override public void onCreate(Bundle state){
        super.onCreate(state);screen(R.layout.activity_backup);
        android.content.SharedPreferences prefs=getSharedPreferences("backup-"+repo.uid(),0);
        SwitchMaterial auto=findViewById(R.id.autoBackup),wifi=findViewById(R.id.wifiOnly);
        auto.setChecked(prefs.getBoolean("auto",false));wifi.setChecked(prefs.getBoolean("wifi",true));
        auto.setOnCheckedChangeListener((view,checked)->prefs.edit().putBoolean("auto",checked).apply());
        wifi.setOnCheckedChangeListener((view,checked)->prefs.edit().putBoolean("wifi",checked).apply());
        status("Nhấn Sao lưu ngay để tải ảnh lên cloud.");
        click(R.id.backupNow,()->{
            status("Đang sao lưu…");
            backup(this, repo, true, ()->{
                if(!isFinishing()&&!isDestroyed()) status("Sao lưu hoàn tất.");
            });
        });
    }

    /**
     * Uploads all local photos that don't yet have a firebaseUrl to Firebase Storage,
     * then persists the download URL back into each Photo's Firestore document.
     */
    public static void backup(BaseActivity activity, GalleryRepository repo, boolean manual, Runnable done){
        if(repo.uid()==null){
            if(manual) activity.status("Hãy đăng nhập để sao lưu.");
            done.run();
            return;
        }
        List<Photo> pending = new ArrayList<>();
        for(Photo p : repo.photos){
            if(!p.isDeleted() && (p.getFirebaseUrl()==null || p.getFirebaseUrl().isEmpty())
                    && p.getFilePath()!=null && new java.io.File(p.getFilePath()).exists()){
                pending.add(p);
            }
        }
        if(pending.isEmpty()){
            if(manual) activity.status("Tất cả ảnh đã được sao lưu.");
            done.run();
            return;
        }
        if(manual) activity.status("Đang sao lưu "+pending.size()+" ảnh…");
        uploadNext(activity, repo, pending, 0, manual, done);
    }

    private static void uploadNext(BaseActivity activity, GalleryRepository repo,
                                   List<Photo> pending, int index, boolean manual, Runnable done){
        if(index >= pending.size()){
            if(manual && !activity.isFinishing() && !activity.isDestroyed())
                activity.status("Đã sao lưu "+pending.size()+" ảnh thành công.");
            done.run();
            return;
        }
        Photo photo = pending.get(index);
        if(manual && !activity.isFinishing() && !activity.isDestroyed())
            activity.status("Đang sao lưu "+(index+1)+"/"+pending.size()+"…");

        repo.upload(photo, error -> {
            if(error != null){
                Log.w(TAG, "Backup failed for "+photo.getFileName()+": "+error);
            }
            // Continue with the next photo regardless of individual failures
            uploadNext(activity, repo, pending, index+1, manual, done);
        });
    }
}
