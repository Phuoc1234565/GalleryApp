package com.example.appanh.editor;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

/** Image-space crop handles and vector strokes: no bitmap allocation while dragging. */
public final class EditorCanvasView extends View {
    public enum Mode { VIEW, CROP, DRAW }
    private EditorDocument document;
    private EditorDocument.State paintedState;
    private Mode mode=Mode.VIEW;
    private final RectF imageBounds=new RectF(),crop=new RectF(),dragStart=new RectF(),cropBounds=new RectF();
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float scale=1,ratio,downX,downY,brushDp=6;
    private int handle,color=Color.WHITE;
    private final List<PointF> points=new ArrayList<>();
    private final Path livePath=new Path();
    private Runnable changed;
    private final float density;
    public EditorCanvasView(Context context,AttributeSet attrs){super(context,attrs);density=getResources().getDisplayMetrics().density;setContentDescription("Vùng chỉnh sửa ảnh. Chọn Cắt để kéo khung, hoặc Vẽ để thêm nét bút.");}
    public void bind(EditorDocument value){boolean newImage=paintedState==null||value!=null&&paintedState.image!=value.state().image;document=value;if(newImage)resetCrop();invalidate();}
    public void setOnChanged(Runnable callback){changed=callback;}
    public void setMode(Mode value){mode=value;points.clear();livePath.reset();if(value==Mode.CROP)resetCrop();invalidate();}
    public Mode mode(){return mode;}
    public void setBrush(int value,float widthDp){color=value;brushDp=widthDp;}
    public void setAspect(float value){ratio=value;resetCrop();invalidate();}
    public Rect cropPixels(){return new Rect(Math.round(crop.left),Math.round(crop.top),Math.round(crop.right),Math.round(crop.bottom));}
    public void restoreCrop(Rect rect){if(document==null||rect==null)return;Bitmap image=document.state().image;crop.set(Math.max(0,rect.left),Math.max(0,rect.top),Math.min(image.getWidth(),rect.right),Math.min(image.getHeight(),rect.bottom));if(crop.width()<1||crop.height()<1)resetCrop();invalidate();}
    public RectF imageBounds(){fit();return new RectF(imageBounds);}
    public void resetCrop(){if(document==null)return;Bitmap image=document.state().image;float width=image.getWidth(),height=image.getHeight();if(ratio>0){if(width/height>ratio)width=height*ratio;else height=width/ratio;}crop.set((image.getWidth()-width)/2,(image.getHeight()-height)/2,(image.getWidth()+width)/2,(image.getHeight()+height)/2);}
    private void fit(){if(document==null)return;Bitmap image=document.state().image;float areaWidth=Math.max(1,getWidth()-getPaddingLeft()-getPaddingRight()),areaHeight=Math.max(1,getHeight()-getPaddingTop()-getPaddingBottom());scale=Math.min(areaWidth/image.getWidth(),areaHeight/image.getHeight());float width=image.getWidth()*scale,height=image.getHeight()*scale;float left=getPaddingLeft()+(areaWidth-width)/2,top=getPaddingTop()+(areaHeight-height)/2;imageBounds.set(left,top,left+width,top+height);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(document==null)return;fit();EditorDocument.State state=document.state();
        if(state!=paintedState){imagePaint.setColorFilter(EditorDocument.effect(state));paintedState=state;}
        int saved=canvas.save();canvas.translate(imageBounds.left,imageBounds.top);canvas.scale(scale,scale);canvas.clipRect(0,0,state.image.getWidth(),state.image.getHeight());canvas.drawBitmap(state.image,0,0,imagePaint);
        for(EditorDocument.Stroke stroke:state.strokes)EditorDocument.drawStroke(canvas,stroke,paint);
        if(!points.isEmpty()){strokePaint(color,brushDp*density/scale);if(points.size()==1){paint.setStyle(Paint.Style.FILL);PointF point=points.get(0);canvas.drawCircle(point.x,point.y,paint.getStrokeWidth()/2,paint);}else canvas.drawPath(livePath,paint);}
        canvas.restoreToCount(saved);
        if(mode==Mode.CROP){
            RectF box=cropBounds;box.set(imageBounds.left+crop.left*scale,imageBounds.top+crop.top*scale,imageBounds.left+crop.right*scale,imageBounds.top+crop.bottom*scale);
            paint.reset();paint.setColor(0x99000000);canvas.drawRect(imageBounds.left,imageBounds.top,imageBounds.right,box.top,paint);canvas.drawRect(imageBounds.left,box.bottom,imageBounds.right,imageBounds.bottom,paint);canvas.drawRect(imageBounds.left,box.top,box.left,box.bottom,paint);canvas.drawRect(box.right,box.top,imageBounds.right,box.bottom,paint);
            paint.setAntiAlias(true);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);paint.setColor(Color.WHITE);canvas.drawRect(box,paint);paint.setStrokeWidth(density);paint.setColor(0xB3FFFFFF);
            for(int i=1;i<3;i++){float x=box.left+box.width()*i/3,y=box.top+box.height()*i/3;canvas.drawLine(x,box.top,x,box.bottom,paint);canvas.drawLine(box.left,y,box.right,y,paint);}
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawCircle(box.left,box.top,5*density,paint);canvas.drawCircle(box.right,box.top,5*density,paint);canvas.drawCircle(box.left,box.bottom,5*density,paint);canvas.drawCircle(box.right,box.bottom,5*density,paint);
        }
    }
    private void strokePaint(int color,float width){paint.reset();paint.setAntiAlias(true);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled()||document==null||mode==Mode.VIEW)return super.onTouchEvent(event);
        fit();float x=clamp((event.getX()-imageBounds.left)/scale,0,document.state().image.getWidth()),y=clamp((event.getY()-imageBounds.top)/scale,0,document.state().image.getHeight());
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(!imageBounds.contains(event.getX(),event.getY()))return false;
                getParent().requestDisallowInterceptTouchEvent(true);downX=x;downY=y;
                if(mode==Mode.CROP){dragStart.set(crop);float threshold=28*density/scale;handle=0;if(distance(x,y,crop.left,crop.top)<threshold)handle=1;else if(distance(x,y,crop.right,crop.top)<threshold)handle=2;else if(distance(x,y,crop.right,crop.bottom)<threshold)handle=3;else if(distance(x,y,crop.left,crop.bottom)<threshold)handle=4;else if(crop.contains(x,y))handle=5;}
                else {if(document.state().strokes.size()>=100)return false;points.clear();points.add(new PointF(x,y));livePath.reset();livePath.moveTo(x,y);}invalidate();return true;
            case MotionEvent.ACTION_MOVE:
                if(mode==Mode.CROP)moveCrop(x,y);else if(!points.isEmpty()&&points.size()<5000){PointF previous=points.get(points.size()-1);if(distance(x,y,previous.x,previous.y)>.75f/scale){livePath.quadTo(previous.x,previous.y,(previous.x+x)/2,(previous.y+y)/2);points.add(new PointF(x,y));}}invalidate();return true;
            case MotionEvent.ACTION_UP:
                if(mode==Mode.DRAW&&!points.isEmpty()){document.addStroke(new EditorDocument.Stroke(points,color,brushDp*density/scale));points.clear();livePath.reset();if(changed!=null)changed.run();}
                handle=0;getParent().requestDisallowInterceptTouchEvent(false);performClick();invalidate();return true;
            case MotionEvent.ACTION_CANCEL:points.clear();livePath.reset();handle=0;invalidate();return true;
            default:return true;
        }
    }
    private void moveCrop(float x,float y){
        float imageWidth=document.state().image.getWidth(),imageHeight=document.state().image.getHeight(),minimum=Math.min(32,Math.min(imageWidth,imageHeight)/4);
        if(handle==5){float left=clamp(dragStart.left+x-downX,0,imageWidth-dragStart.width()),top=clamp(dragStart.top+y-downY,0,imageHeight-dragStart.height());crop.set(left,top,left+dragStart.width(),top+dragStart.height());return;}
        if(handle==0)return;
        boolean left=handle==1||handle==4,top=handle==1||handle==2;float anchorX=left?dragStart.right:dragStart.left,anchorY=top?dragStart.bottom:dragStart.top;
        float width=Math.max(minimum,left?anchorX-x:x-anchorX),height=Math.max(minimum,top?anchorY-y:y-anchorY);
        float maxWidth=left?anchorX:imageWidth-anchorX,maxHeight=top?anchorY:imageHeight-anchorY;
        if(ratio>0){width=Math.min(Math.max(width,height*ratio),Math.min(maxWidth,maxHeight*ratio));height=width/ratio;}else{width=Math.min(width,maxWidth);height=Math.min(height,maxHeight);}
        crop.set(left?anchorX-width:anchorX,top?anchorY-height:anchorY,left?anchorX:anchorX+width,top?anchorY:anchorY+height);
    }
    private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
    private static float distance(float x,float y,float a,float b){return (float)Math.hypot(x-a,y-b);}
    @Override public boolean performClick(){super.performClick();return true;}
}
