package com.example.appanh;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.appanh.models.Photo;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Copies files without retaining an Activity; work survives screen recreation. */
public final class ImportViewModel extends AndroidViewModel {
    public boolean busy;
    public int importedCount,failedCount;
    public final MutableLiveData<String> progress=new MutableLiveData<>();
    public final MutableLiveData<String> completed=new MutableLiveData<>();
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    public ImportViewModel(Application application){super(application);}

    public void start(List<Uri> uris){
        if(busy||uris.isEmpty())return;
        busy=true;importedCount=0;failedCount=0;progress.setValue("Đang nhập "+uris.size()+" mục…");
        GalleryRepository repository=new GalleryRepository(getApplication());
        io.execute(()->{
            List<Photo> imported=new ArrayList<>();int failures=0;
            ContentResolver resolver=getApplication().getContentResolver();
            for(Uri uri:uris){
                File target=null;
                try{
                    String name="Media";
                    try(Cursor cursor=resolver.query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                        if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);
                    }
                    String mime=resolver.getType(uri);
                    if(mime==null||mime.equals("application/octet-stream")){
                        int dot=name.lastIndexOf('.');
                        if(dot>=0)mime=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot+1).toLowerCase(Locale.ROOT));
                    }
                    if(mime==null||!(mime.startsWith("image/")||mime.startsWith("video/")))throw new IOException("Định dạng không hỗ trợ");
                    File dir=new File(getApplication().getFilesDir(),"media");
                    if(!dir.exists()&&!dir.mkdirs())throw new IOException("Không tạo được thư mục ảnh");
                    String id=UUID.randomUUID().toString();target=new File(dir,id);
                    try(InputStream in=resolver.openInputStream(uri);OutputStream out=new FileOutputStream(target)){
                        if(in==null)throw new IOException("Không mở được tệp");
                        byte[] buffer=new byte[65536];int count;
                        while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
                    }
                    if(target.length()==0)throw new IOException("Tệp trống");
                    Photo photo=new Photo(repository.uid(),name,target.getPath(),target.length(),0,0,mime);
                    photo.setId(id);photo.setCreatedAt(new Date());photo.setDateTaken(new Date());photo.setDeviceName(android.os.Build.MODEL);
                    if(!photo.isVideo()){
                        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                        BitmapFactory.decodeFile(target.getPath(),options);
                        if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Ảnh không hợp lệ");
                        photo.setWidth(options.outWidth);photo.setHeight(options.outHeight);
                    }else{
                        android.media.MediaMetadataRetriever metadata=new android.media.MediaMetadataRetriever();
                        try{
                            metadata.setDataSource(target.getPath());
                            String width=metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            String height=metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            String duration=metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                            if(width==null||height==null||duration==null)throw new IOException("Video không hợp lệ");
                            photo.setWidth(Integer.parseInt(width));photo.setHeight(Integer.parseInt(height));photo.setDuration(Long.parseLong(duration));
                            if(photo.getWidth()<=0||photo.getHeight()<=0||photo.getDuration()<=0)throw new IOException("Video không hợp lệ");
                            android.graphics.Bitmap frame=metadata.getFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                            if(frame==null)throw new IOException("Không giải mã được video");
                            frame.recycle();
                        }finally{metadata.release();}
                    }
                    imported.add(photo);
                }catch(Exception error){if(target!=null)target.delete();failures++;android.util.Log.w("GalleryImport","Không đọc được tệp đã chọn",error);}
                int processed=imported.size()+failures;
                main.post(()->progress.setValue("Đang nhập "+processed+"/"+uris.size()+" mục…"));
            }
            int skipped=failures;
            // Merge with the latest disk data so edits in another screen are retained.
            main.post(()->repository.appendLocal(imported,error->{
                busy=false;importedCount=error==null?imported.size():0;failedCount=skipped;progress.setValue("");
                completed.setValue(error!=null?"Chưa lưu được ảnh đã nhập. Kiểm tra dung lượng và thử lại.":
                        "Đã nhập "+imported.size()+" mục"+(skipped==0?".":(" · "+skipped+" tệp không đọc được.")));
                // Auto-upload imported photos to Firebase Storage in the background
                if(error==null && repository.uid()!=null){
                    uploadImported(repository, imported, 0);
                } else {
                    repository.close();
                }
            }));
        });
    }

    /** Sequentially upload each imported photo to Firebase Storage. */
    private void uploadImported(GalleryRepository repository, List<Photo> photos, int index){
        if(index >= photos.size()){ repository.close(); return; }
        Photo photo = photos.get(index);
        if(photo.getFirebaseUrl() != null && !photo.getFirebaseUrl().isEmpty()){
            uploadImported(repository, photos, index + 1); return;
        }
        repository.upload(photo, error -> {
            if(error != null) android.util.Log.w("GalleryImport", "Upload failed for " + photo.getFileName() + ": " + error);
            uploadImported(repository, photos, index + 1);
        });
    }

    @Override protected void onCleared(){ io.shutdown(); }
}
