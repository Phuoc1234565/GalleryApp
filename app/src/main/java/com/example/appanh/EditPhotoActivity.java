package com.example.appanh;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.appanh.editor.EditorCanvasView;
import com.example.appanh.editor.EditorDocument;
import com.example.appanh.editor.EditorViewModel;
import com.example.appanh.models.Photo;
import com.google.android.material.button.MaterialButton;

public class EditPhotoActivity extends BaseActivity {
    EditorViewModel model;
    EditorCanvasView canvas;
    private String shownTool;
    private Rect restoredCrop;
    private static final int[] COLORS={Color.WHITE,Color.BLACK,0xFFEF4444,0xFF2563EB,0xFF16A34A,0xFFFACC15};
    private static final int[] COLOR_IDS={R.id.brushWhite,R.id.brushBlack,R.id.brushRed,R.id.brushBlue,R.id.brushGreen,R.id.brushYellow};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);screen(R.layout.activity_edit_photo);
        model=new ViewModelProvider(this).get(EditorViewModel.class);canvas=findViewById(R.id.photo);
        if(state!=null)restoredCrop=state.getParcelable("cropRect");
        canvas.setOnChanged(model::changed);
        click(R.id.crop,()->tool("crop"));click(R.id.draw,()->tool("draw"));click(R.id.filter,()->tool("filter"));click(R.id.adjust,()->tool("adjust"));
        click(R.id.rotate,()->model.rotate());
        click(R.id.applyCrop,()->{if(ready())model.crop(canvas.cropPixels(),false);});
        click(R.id.undo,()->{if(ready()){model.document.undo();model.changed();}});
        click(R.id.redo,()->{if(ready()){model.document.redo();model.changed();}});
        click(R.id.resetEditor,()->{if(ready()&&model.document.dirty())new AlertDialog.Builder(this).setTitle("Khôi phục ảnh gốc?").setMessage("Bỏ các thay đổi hiện tại. Bạn vẫn có thể nhấn Hoàn tác sau thao tác này.").setNegativeButton("Hủy",null).setPositiveButton("Khôi phục",(dialog,which)->{model.document.reset();model.tool="view";model.changed();}).show();});
        click(R.id.clearDrawing,()->{if(ready()){model.document.clearStrokes();model.changed();}});
        click(R.id.save,this::save);click(R.id.cancelEdit,this::leave);
        int[] ratios={R.id.ratioFree,R.id.ratioSquare,R.id.ratioPortrait,R.id.ratioWide};float[] values={0,1,4f/3,16f/9};
        for(int i=0;i<ratios.length;i++){final float value=values[i];click(ratios[i],()->{model.aspect=value;canvas.setAspect(value);ratios();});}
        int[] filters={R.id.filterOriginal,R.id.filterMono,R.id.filterSepia,R.id.filterWarm,R.id.filterCool};
        for(int i=0;i<filters.length;i++){final int value=i;click(filters[i],()->{if(ready()){model.document.filter(value);model.changed();}});}
        for(int i=0;i<COLOR_IDS.length;i++){final int color=COLORS[i];click(COLOR_IDS[i],()->{model.brushColor=color;brush();});}
        ((SeekBar)findViewById(R.id.brushWidth)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){model.brushWidth=value+1;brush();}}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});
        SeekBar.OnSeekBarChangeListener adjust=new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){if(ready())model.document.beginAdjustment();}
            public void onProgressChanged(SeekBar bar,int value,boolean user){if(user&&ready()){model.document.adjust(((SeekBar)findViewById(R.id.brightness)).getProgress()-100,((SeekBar)findViewById(R.id.contrast)).getProgress()-50);model.changed();}}
            public void onStopTrackingTouch(SeekBar bar){}
        };
        ((SeekBar)findViewById(R.id.brightness)).setOnSeekBarChangeListener(adjust);((SeekBar)findViewById(R.id.contrast)).setOnSeekBarChangeListener(adjust);
        model.updates.observe(this,ignored->refresh());
        model.message.observe(this,this::status);
        model.saved.observe(this,photo->{if(photo!=null){NotificationsActivity.record(this,"Đã lưu bản chỉnh sửa "+photo.getFileName());toast("Đã lưu bản sao. Ảnh gốc được giữ nguyên.");finish();}});
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){leave();}});
        if(model.document==null&&!model.busy)repo.load((photos,albums,error)->loadPhoto(true),()->loadPhoto(false));
    }
    private void loadPhoto(boolean finalResult){if(isDestroyed()||model.document!=null||model.busy)return;Photo photo=repo.find(getIntent().getStringExtra("photoId"));if(photo!=null&&!photo.isVideo())model.load(repo,photo);else if(finalResult)status(photo==null?"Không tìm thấy ảnh. Quay lại thư viện để chọn lại.":"Công cụ chỉnh sửa chỉ hỗ trợ ảnh.");}
    private boolean ready(){return model.document!=null&&!model.busy;}
    private void tool(String value){if(!ready())return;model.tool=value.equals(model.tool)?"view":value;status(model.tool.equals("crop")?"Kéo bốn góc để đổi khung, kéo bên trong để di chuyển.":model.tool.equals("draw")?"Chọn màu và nét bút, sau đó vẽ trực tiếp lên ảnh.":"Các thay đổi chỉ áp dụng lên bản sao khi lưu.");refresh();}
    private void refresh(){
        boolean enabled=ready();findViewById(R.id.editorBusy).setVisibility(model.busy?View.VISIBLE:View.GONE);
        for(int id:new int[]{R.id.crop,R.id.draw,R.id.filter,R.id.adjust,R.id.rotate,R.id.applyCrop,R.id.save,R.id.resetEditor,R.id.clearDrawing,R.id.brightness,R.id.contrast,R.id.brushWidth})findViewById(id).setEnabled(enabled);
        canvas.setEnabled(enabled);findViewById(R.id.undo).setEnabled(enabled&&model.document.canUndo());findViewById(R.id.redo).setEnabled(enabled&&model.document.canRedo());
        if(model.document==null)return;
        canvas.bind(model.document);
        if(!model.tool.equals(shownTool)){shownTool=model.tool;canvas.setMode(model.tool.equals("crop")?EditorCanvasView.Mode.CROP:model.tool.equals("draw")?EditorCanvasView.Mode.DRAW:EditorCanvasView.Mode.VIEW);if(model.tool.equals("crop"))canvas.setAspect(model.aspect);}
        if(restoredCrop!=null){canvas.restoreCrop(restoredCrop);restoredCrop=null;}
        findViewById(R.id.cropPanel).setVisibility(model.tool.equals("crop")?View.VISIBLE:View.GONE);findViewById(R.id.drawPanel).setVisibility(model.tool.equals("draw")?View.VISIBLE:View.GONE);findViewById(R.id.adjustPanel).setVisibility(model.tool.equals("adjust")?View.VISIBLE:View.GONE);findViewById(R.id.filterPanel).setVisibility(model.tool.equals("filter")?View.VISIBLE:View.GONE);
        ((SeekBar)findViewById(R.id.brightness)).setProgress(model.document.state().brightness+100);((SeekBar)findViewById(R.id.contrast)).setProgress(model.document.state().contrast+50);
        ((SeekBar)findViewById(R.id.brushWidth)).setProgress((int)model.brushWidth-1);brush();ratios();
    }
    private void ratios(){int[] ids={R.id.ratioFree,R.id.ratioSquare,R.id.ratioPortrait,R.id.ratioWide};float[] values={0,1,4f/3,16f/9};for(int i=0;i<ids.length;i++){MaterialButton button=findViewById(ids[i]);button.setBackgroundTintList(ColorStateList.valueOf(getColor(Math.abs(model.aspect-values[i])<.01?R.color.primary_light:R.color.viewer_controls)));}}
    private void brush(){canvas.setBrush(model.brushColor,model.brushWidth);for(int i=0;i<COLOR_IDS.length;i++){MaterialButton button=findViewById(COLOR_IDS[i]);button.setStrokeWidth((int)((model.brushColor==COLORS[i]?3:1)*getResources().getDisplayMetrics().density));button.setStrokeColor(ColorStateList.valueOf(getColor(model.brushColor==COLORS[i]?R.color.primary:R.color.field_border)));}}
    private boolean pendingCrop(){return ready()&&canvas.mode()==EditorCanvasView.Mode.CROP&&!canvas.cropPixels().equals(new Rect(0,0,model.document.state().image.getWidth(),model.document.state().image.getHeight()));}
    private void save(){if(!ready())return;if(pendingCrop()){model.crop(canvas.cropPixels(),true);return;}if(!model.document.dirty()){toast("Chưa có thay đổi để lưu.");return;}model.save();}
    private void leave(){if(model.busy){toast("Đang xử lý ảnh, vui lòng chờ một chút.");return;}if(model.document==null||!model.document.dirty()&&!pendingCrop()){finish();return;}new AlertDialog.Builder(this).setTitle("Bỏ các thay đổi?").setMessage("Ảnh gốc vẫn được giữ nguyên. Bản chỉnh sửa chưa được lưu.").setNegativeButton("Tiếp tục sửa",null).setPositiveButton("Bỏ thay đổi",(dialog,which)->finish()).show();}
    @Override protected void onSaveInstanceState(Bundle state){if(canvas!=null&&model.document!=null)state.putParcelable("cropRect",canvas.cropPixels());super.onSaveInstanceState(state);}
}
