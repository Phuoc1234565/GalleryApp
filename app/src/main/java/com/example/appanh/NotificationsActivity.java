package com.example.appanh;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import org.json.*;
import java.util.*;
public class NotificationsActivity extends BaseActivity {
 private final List<String> messages=new ArrayList<>();
 private static String key(){return "notifications-"+FirebaseAuth.getInstance().getUid();}
 public static void record(Context context,String message){String uid=FirebaseAuth.getInstance().getUid();SharedPreferences prefs=context.getSharedPreferences(key(),0);try{JSONArray old=new JSONArray(prefs.getString("items","[]")),all=new JSONArray();all.put(message+"\n"+new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("vi","VN")).format(new Date()));for(int i=0;i<Math.min(old.length(),99);i++)all.put(old.get(i));prefs.edit().putString("items",all.toString()).apply();}catch(JSONException ignored){}if(uid!=null){Map<String,Object> data=new HashMap<>();data.put("message",message);data.put("createdAt",FieldValue.serverTimestamp());data.put("read",false);FirebaseFirestore.getInstance().collection("users").document(uid).collection("notifications").add(data);}}
 @Override public void onCreate(Bundle state){super.onCreate(state);screen(R.layout.activity_notifications);try{JSONArray items=new JSONArray(getSharedPreferences(key(),0).getString("items","[]"));for(int i=0;i<items.length();i++)messages.add(items.getString(i));}catch(JSONException e){status("Không đọc được lịch sử trên máy.");}render();if(repo.uid()!=null)FirebaseFirestore.getInstance().collection("users").document(repo.uid()).collection("notifications").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).get().addOnSuccessListener(s->{messages.clear();for(DocumentSnapshot d:s)messages.add(String.valueOf(d.getString("message")));render();}).addOnFailureListener(e->status(e.getLocalizedMessage()));click(R.id.markRead,()->{if(repo.uid()==null){status("Đã đọc tất cả thông báo trên máy.");return;}FirebaseFirestore db=FirebaseFirestore.getInstance();db.collection("users").document(repo.uid()).collection("notifications").limit(100).get().addOnSuccessListener(s->{WriteBatch batch=db.batch();for(DocumentSnapshot d:s)batch.update(d.getReference(),"read",true);batch.commit().addOnSuccessListener(v->status("Đã đọc tất cả thông báo.")).addOnFailureListener(e->status(e.getLocalizedMessage()));});});}
 private void render(){status(messages.isEmpty()?"Chưa có thông báo.":messages.size()+" hoạt động gần đây");RecyclerView list=findViewById(R.id.grid);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(new RecyclerView.Adapter<Row>(){public Row onCreateViewHolder(ViewGroup parent,int type){return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification,parent,false));}public void onBindViewHolder(Row row,int index){((TextView)row.itemView.findViewById(R.id.notificationMessage)).setText(messages.get(index));}public int getItemCount(){return messages.size();}});}
 static class Row extends RecyclerView.ViewHolder {Row(View v){super(v);}}
}
