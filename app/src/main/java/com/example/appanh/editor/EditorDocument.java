package com.example.appanh.editor;

import android.graphics.*;
import java.util.*;

/** Immutable edit snapshots; only transforms/export allocate a bitmap. */
public final class EditorDocument {
    public static final class Stroke {
        public final List<PointF> points;
        public final int color;
        public final float width;
        public final Path path=new Path();
        public Stroke(List<PointF> points,int color,float width){this.points=new ArrayList<>(points);this.color=color;this.width=width;if(!points.isEmpty()){PointF previous=points.get(0);path.moveTo(previous.x,previous.y);for(int i=1;i<points.size();i++){PointF point=points.get(i);path.quadTo(previous.x,previous.y,(previous.x+point.x)/2,(previous.y+point.y)/2);previous=point;}path.lineTo(previous.x,previous.y);}}
        public Stroke transform(Matrix matrix){float[] xy=new float[points.size()*2];for(int i=0;i<points.size();i++){xy[i*2]=points.get(i).x;xy[i*2+1]=points.get(i).y;}matrix.mapPoints(xy);List<PointF> transformed=new ArrayList<>();for(int i=0;i<points.size();i++)transformed.add(new PointF(xy[i*2],xy[i*2+1]));return new Stroke(transformed,color,width);}
    }
    public static final class State {
        public final Bitmap image;
        public final List<Stroke> strokes;
        public final int filter,brightness,contrast,version;
        State(Bitmap image,List<Stroke> strokes,int filter,int brightness,int contrast,int version){this.image=image;this.strokes=Collections.unmodifiableList(new ArrayList<>(strokes));this.filter=filter;this.brightness=brightness;this.contrast=contrast;this.version=version;}
    }
    private final State original;
    private State state;
    private int version;
    private final Deque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    public EditorDocument(Bitmap image){original=new State(image,Collections.emptyList(),0,0,0,0);state=original;}
    public State state(){return state;}
    public boolean dirty(){return state.version!=0;}
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean canRedo(){return !redo.isEmpty();}
    private void remember(){undo.addLast(state);while(undo.size()>4)undo.removeFirst();redo.clear();}
    public void addStroke(Stroke stroke){remember();List<Stroke> strokes=new ArrayList<>(state.strokes);strokes.add(stroke);state=new State(state.image,strokes,state.filter,state.brightness,state.contrast,++version);}
    public void clearStrokes(){if(state.strokes.isEmpty())return;remember();state=new State(state.image,Collections.emptyList(),state.filter,state.brightness,state.contrast,++version);}
    public void undo(){if(!canUndo())return;redo.addLast(state);state=undo.removeLast();}
    public void redo(){if(!canRedo())return;undo.addLast(state);state=redo.removeLast();}
    public void reset(){remember();state=original;}
    public void beginAdjustment(){remember();}
    public void adjust(int brightness,int contrast){state=new State(state.image,state.strokes,state.filter,brightness,contrast,++version);}
    public void filter(int value){if(value==state.filter)return;remember();state=new State(state.image,state.strokes,value,state.brightness,state.contrast,++version);}
    public void acceptTransform(State transformed){remember();state=new State(transformed.image,transformed.strokes,transformed.filter,transformed.brightness,transformed.contrast,++version);}
    public static State crop(State source,Rect rect){
        int left=Math.max(0,Math.min(source.image.getWidth()-1,rect.left)),top=Math.max(0,Math.min(source.image.getHeight()-1,rect.top));
        int width=Math.max(1,Math.min(source.image.getWidth()-left,rect.width())),height=Math.max(1,Math.min(source.image.getHeight()-top,rect.height()));
        Bitmap bitmap=Bitmap.createBitmap(source.image,left,top,width,height);Matrix matrix=new Matrix();matrix.setTranslate(-left,-top);
        List<Stroke> strokes=new ArrayList<>();for(Stroke stroke:source.strokes)strokes.add(stroke.transform(matrix));
        return new State(bitmap,strokes,source.filter,source.brightness,source.contrast,0);
    }
    public static State rotate(State source){
        Matrix rotation=new Matrix();rotation.setRotate(90);Bitmap bitmap=Bitmap.createBitmap(source.image,0,0,source.image.getWidth(),source.image.getHeight(),rotation,true);
        Matrix coordinates=new Matrix();coordinates.setValues(new float[]{0,-1,source.image.getHeight(),1,0,0,0,0,1});
        List<Stroke> strokes=new ArrayList<>();for(Stroke stroke:source.strokes)strokes.add(stroke.transform(coordinates));
        return new State(bitmap,strokes,source.filter,source.brightness,source.contrast,0);
    }
    public static ColorMatrixColorFilter effect(State state){
        ColorMatrix filter=new ColorMatrix();
        if(state.filter==1)filter.setSaturation(0);
        if(state.filter==2)filter.set(new float[]{.393f,.769f,.189f,0,0,.349f,.686f,.168f,0,0,.272f,.534f,.131f,0,0,0,0,0,1,0});
        if(state.filter==3)filter.set(new float[]{1.08f,0,0,0,7,0,1.02f,0,0,2,0,0,.91f,0,-3,0,0,0,1,0});
        if(state.filter==4)filter.set(new float[]{.92f,0,0,0,-2,0,1.02f,0,0,2,0,0,1.1f,0,8,0,0,0,1,0});
        float contrast=1+state.contrast/100f,offset=state.brightness+(1-contrast)*128;
        filter.postConcat(new ColorMatrix(new float[]{contrast,0,0,0,offset,0,contrast,0,0,offset,0,0,contrast,0,offset,0,0,0,1,0}));
        return new ColorMatrixColorFilter(filter);
    }
    public static void drawStroke(Canvas canvas,Stroke stroke,Paint paint){
        if(stroke.points.isEmpty())return;
        paint.reset();paint.setAntiAlias(true);paint.setColor(stroke.color);paint.setStrokeWidth(stroke.width);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);
        PointF start=stroke.points.get(0);if(stroke.points.size()==1){paint.setStyle(Paint.Style.FILL);canvas.drawCircle(start.x,start.y,stroke.width/2,paint);return;}
        canvas.drawPath(stroke.path,paint);
    }
    public static Bitmap render(State state){Bitmap output=Bitmap.createBitmap(state.image.getWidth(),state.image.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(output);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);paint.setColorFilter(effect(state));canvas.drawBitmap(state.image,0,0,paint);for(Stroke stroke:state.strokes)drawStroke(canvas,stroke,paint);return output;}
}
