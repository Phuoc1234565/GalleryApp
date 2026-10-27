package com.example.appanh;

import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.appanh.editor.*;
import com.example.appanh.models.Photo;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EditorInteractionTest {
    @Test public void transformsPreserveInkAndUndo(){
        Bitmap bitmap=Bitmap.createBitmap(100,80,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);
        EditorDocument doc=new EditorDocument(bitmap);
        doc.addStroke(new EditorDocument.Stroke(Arrays.asList(new PointF(30,30),new PointF(60,30)),Color.RED,6));
        doc.filter(1);Bitmap rendered=EditorDocument.render(doc.state());assertEquals(Color.RED,rendered.getPixel(40,30));rendered.recycle();
        doc.acceptTransform(EditorDocument.crop(doc.state(),new Rect(20,10,80,70)));
        assertEquals(60,doc.state().image.getWidth());assertEquals(10,doc.state().strokes.get(0).points.get(0).x,.01);
        doc.acceptTransform(EditorDocument.rotate(doc.state()));assertEquals(40,doc.state().strokes.get(0).points.get(0).x,.01);assertEquals(10,doc.state().strokes.get(0).points.get(0).y,.01);
        doc.undo();assertEquals(10,doc.state().strokes.get(0).points.get(0).x,.01);doc.redo();assertEquals(40,doc.state().strokes.get(0).points.get(0).x,.01);
        doc.reset();assertFalse(doc.dirty());assertTrue(doc.state().strokes.isEmpty());doc.undo();assertTrue(doc.dirty());
    }
    @Test public void gesturesRecreationAndExportKeepOriginal() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent=new Intent(context,EditPhotoActivity.class).putExtra("photoId","gallery-album-demo-food");
        try(ActivityScenario<EditPhotoActivity> scenario=ActivityScenario.launch(intent)){
            awaitReady(scenario);
            File[] original={null};byte[][] digest={null};int[] dimensions=new int[2];List<String> albums=new ArrayList<>();
            scenario.onActivity(a->{original[0]=new File(a.model.original.getFilePath());albums.addAll(a.model.original.getAlbumIds());a.findViewById(R.id.crop).performClick();});
            digest[0]=hash(original[0]);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{RectF b=a.canvas.imageBounds();Rect before=a.canvas.cropPixels();drag(a.canvas,b.left+2,b.top+2,b.left+b.width()*.2f,b.top+b.height()*.2f);Rect after=a.canvas.cropPixels();assertTrue(after.left>before.left);assertTrue(after.top>before.top);assertTrue(after.right<=a.model.document.state().image.getWidth());});
            capture("editor-crop.png");
            scenario.onActivity(a->a.findViewById(R.id.applyCrop).performClick());awaitReady(scenario);
            scenario.onActivity(a->{dimensions[0]=a.model.document.state().image.getWidth();dimensions[1]=a.model.document.state().image.getHeight();a.findViewById(R.id.draw).performClick();a.findViewById(R.id.brushRed).performClick();});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{RectF b=a.canvas.imageBounds();drag(a.canvas,b.left+b.width()*.2f,b.centerY(),b.left+b.width()*.8f,b.centerY());assertEquals(1,a.model.document.state().strokes.size());a.findViewById(R.id.undo).performClick();assertTrue(a.model.document.state().strokes.isEmpty());a.findViewById(R.id.redo).performClick();assertEquals(1,a.model.document.state().strokes.size());});
            scenario.recreate();awaitReady(scenario);scenario.onActivity(a->{assertEquals(1,a.model.document.state().strokes.size());assertEquals("draw",a.model.tool);});capture("editor-draw.png");
            com.example.appanh.editor.EditorViewModel[] vm={null};scenario.onActivity(a->{vm[0]=a.model;a.findViewById(R.id.save).performClick();});
            long end=SystemClock.uptimeMillis()+20000;Photo saved;do{SystemClock.sleep(100);saved=vm[0].saved.getValue();}while(saved==null&&SystemClock.uptimeMillis()<end);
            assertNotNull("Edited copy must be saved",saved);assertNotEquals(original[0].getPath(),saved.getFilePath());assertEquals(albums,saved.getAlbumIds());
            Bitmap output=BitmapFactory.decodeFile(saved.getFilePath());assertNotNull(output);assertEquals(dimensions[0],output.getWidth());assertEquals(dimensions[1],output.getHeight());int ink=output.getPixel(output.getWidth()/2,output.getHeight()/2);assertTrue(Color.red(ink)>Color.green(ink)+60);output.recycle();assertArrayEquals(digest[0],hash(original[0]));
        }
    }
    private void awaitReady(ActivityScenario<EditPhotoActivity> scenario){boolean[] ready={false};long end=SystemClock.uptimeMillis()+20000;do{scenario.onActivity(a->ready[0]=a.model.document!=null&&!a.model.busy);if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<end);assertTrue("Editor must finish loading",ready[0]);InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private static void drag(EditorCanvasView view,float x,float y,float endX,float endY){long time=SystemClock.uptimeMillis();for(int i=0;i<=10;i++){int action=i==0?MotionEvent.ACTION_DOWN:i==10?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;MotionEvent e=MotionEvent.obtain(time,time+i*16,action,x+(endX-x)*i/10,y+(endY-y)*i/10,0);view.dispatchTouchEvent(e);e.recycle();}}
    private static byte[] hash(File file)throws Exception{MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[8192];int read;while((read=in.read(buffer))!=-1)hash.update(buffer,0,read);}return hash.digest();}
    private void capture(String name)throws Exception{SystemClock.sleep(300);Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=new FileOutputStream(new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
}
