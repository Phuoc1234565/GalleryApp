package com.example.appanh.editor;

import android.app.Application;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.example.appanh.GalleryRepository;
import com.example.appanh.models.Photo;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Retains edits across Activity recreation; decode, crop, rotate and export run off the UI thread. */
public final class EditorViewModel extends AndroidViewModel {
    public EditorDocument document;
    public Photo original;
    public boolean busy;
    public String tool="view";
    public int brushColor=Color.WHITE;
    public float brushWidth=6,aspect;
    public final MutableLiveData<Integer> updates=new MutableLiveData<>(0);
    public final MutableLiveData<String> message=new MutableLiveData<>();
    public final MutableLiveData<Photo> saved=new MutableLiveData<>();
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private GalleryRepository repository;
    private int tick;
    public EditorViewModel(Application app){super(app);}
    public void changed(){updates.setValue(++tick);}
    public void load(GalleryRepository repo,Photo photo){
        if(document!=null||busy)return;repository=repo;original=photo;busy=true;changed();
        io.execute(()->{
            FutureTarget<Bitmap> request=null;
            try{
                request=Glide.with(getApplication()).asBitmap().load(repo.source(photo)).fitCenter().submit(1600,1600);
                Bitmap bitmap=request.get().copy(Bitmap.Config.ARGB_8888,false);
                main.post(()->{document=new EditorDocument(bitmap);busy=false;message.setValue("Chọn công cụ để chỉnh sửa ảnh");changed();});
            }catch(Exception error){failure("Không đọc được ảnh. Kiểm tra tệp hoặc kết nối mạng.");}
            finally{if(request!=null)Glide.with(getApplication()).clear(request);}
        });
    }
    public void crop(Rect crop,boolean saveAfter){transform(crop,false,saveAfter);}
    public void rotate(){transform(null,true,false);}
    private void transform(Rect crop,boolean rotate,boolean saveAfter){
        if(document==null||busy)return;EditorDocument.State source=document.state();busy=true;message.setValue(rotate?"Đang xoay ảnh…":"Đang cắt ảnh…");changed();
        io.execute(()->{
            try{EditorDocument.State output=rotate?EditorDocument.rotate(source):EditorDocument.crop(source,crop);
                main.post(()->{document.acceptTransform(output);busy=false;tool="view";message.setValue(rotate?"Đã xoay ảnh":"Đã áp dụng vùng cắt");changed();if(saveAfter)save();});
            }catch(Exception|OutOfMemoryError error){failure("Không xử lý được ảnh. Hãy hoàn tác và thử vùng cắt nhỏ hơn.");}
        });
    }
    public void save(){
        if(document==null||busy)return;EditorDocument.State source=document.state();busy=true;message.setValue("Đang lưu bản sao…");changed();
        io.execute(()->{
            File target=null;Bitmap output=null;
            try{
                output=EditorDocument.render(source);String id=UUID.randomUUID().toString();File dir=new File(getApplication().getFilesDir(),"media");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Không tạo được thư mục ảnh");
                target=new File(dir,id+".jpg");try(OutputStream out=new FileOutputStream(target)){if(!output.compress(Bitmap.CompressFormat.JPEG,94,out))throw new IOException("Không mã hóa được ảnh");}
                Photo photo=new Photo(repository.uid(),"EDIT_"+System.currentTimeMillis()+".jpg",target.getPath(),target.length(),output.getWidth(),output.getHeight(),"image/jpeg");
                photo.setId(id);photo.setCreatedAt(new Date());photo.setDateTaken(new Date());photo.setDeviceName(android.os.Build.MODEL);photo.setAlbumIds(new ArrayList<>(original.getAlbumIds()));photo.setHidden(original.isHidden());
                main.post(()->repository.appendLocal(photo,error->{busy=false;if(error!=null){message.setValue("Chưa lưu được bản sao: "+error);changed();}else{saved.setValue(photo);changed();
                    // Auto-upload edited photo to Firebase Storage in the background
                    if(repository.uid()!=null){repository.upload(photo,uploadError->{if(uploadError!=null)android.util.Log.w("EditorViewModel","Upload failed: "+uploadError);});}
                }}));
            }catch(Exception|OutOfMemoryError error){if(target!=null)target.delete();failure("Chưa lưu được ảnh. Kiểm tra dung lượng bộ nhớ và thử lại.");}
            finally{if(output!=null)output.recycle();}
        });
    }
    private void failure(String text){main.post(()->{busy=false;message.setValue(text);changed();});}
    @Override protected void onCleared(){io.shutdown();}
}
