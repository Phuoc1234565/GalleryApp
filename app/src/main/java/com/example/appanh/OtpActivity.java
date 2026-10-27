package com.example.appanh;
import android.os.Bundle;
import android.view.View;
import com.google.firebase.auth.*;
public class OtpActivity extends BaseActivity {
 private String verificationId; private long resendAt;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);screen(R.layout.activity_otp);verificationId=getIntent().getStringExtra("verificationId");
  if(verificationId==null){
   findViewById(R.id.otpBoxes).setVisibility(View.GONE);
   ((android.widget.Button)findViewById(R.id.verify)).setText("Tôi đã xác minh email");
   status("Liên kết xác minh đã được gửi đến email của bạn. Mở email, nhấn xác minh rồi quay lại đây.");
  }else{
   status("Nhập mã xác thực đã được gửi đến\n"+getIntent().getStringExtra("phone"));
   connectCodeBoxes();
  }
  click(R.id.verify,this::verify);click(R.id.resend,this::resend);
 }
 private boolean changingCode;
 private void connectCodeBoxes(){
  int[] ids={R.id.otp1,R.id.otp2,R.id.otp3,R.id.otp4,R.id.otp5,R.id.otp6};
  android.widget.EditText[] boxes=new android.widget.EditText[6];
  for(int i=0;i<6;i++)boxes[i]=findViewById(ids[i]);
  for(int i=0;i<6;i++){
   final int index=i;
   boxes[i].addTextChangedListener(new android.text.TextWatcher(){
    public void beforeTextChanged(CharSequence s,int start,int count,int after){}
    public void onTextChanged(CharSequence s,int start,int before,int count){}
    public void afterTextChanged(android.text.Editable value){
     if(changingCode)return;changingCode=true;String digits=value.toString();
     if(digits.length()>1){for(int j=0;j<6;j++)boxes[j].setText(j<digits.length()?String.valueOf(digits.charAt(j)):"");boxes[Math.min(5,digits.length()-1)].requestFocus();}
     else if(!digits.isEmpty()&&index<5)boxes[index+1].requestFocus();
     StringBuilder code=new StringBuilder();for(android.widget.EditText box:boxes)code.append(box.getText());
     ((android.widget.EditText)findViewById(R.id.otp)).setText(code);changingCode=false;
    }
   });
   boxes[i].setOnKeyListener((v,key,event)->{if(key==android.view.KeyEvent.KEYCODE_DEL&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&boxes[index].getText().length()==0&&index>0){boxes[index-1].setText("");boxes[index-1].requestFocus();return true;}return false;});
  }
 }
 private void verify(){if(verificationId!=null){String code=input(R.id.otp);if(!code.matches("[0-9]{6}")){toast("Nhập đủ 6 chữ số.");return;}FirebaseAuth.getInstance().signInWithCredential(PhoneAuthProvider.getCredential(verificationId,code)).addOnCompleteListener(this,t->{if(t.isSuccessful())enter();else status(t.getException().getLocalizedMessage());});return;}FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null){go(LoginActivity.class);finish();return;}u.reload().addOnCompleteListener(this,t->{if(t.isSuccessful()&&u.isEmailVerified())enter();else status("Email chưa được xác minh. Kiểm tra cả thư mục Spam.");});}
 private void enter(){AuthActivity.saveProfile(FirebaseAuth.getInstance().getCurrentUser(),null);go(MainActivity.class);finish();}
 private void resend(){if(System.currentTimeMillis()<resendAt){toast("Vui lòng chờ 60 giây giữa các lần gửi.");return;}if(verificationId!=null){toast("Quay lại đăng nhập và nhập lại số điện thoại để gửi SMS mới.");return;}FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null)return;resendAt=System.currentTimeMillis()+60000;u.sendEmailVerification().addOnCompleteListener(this,t->status(t.isSuccessful()?"Đã gửi lại email xác minh.":t.getException().getLocalizedMessage()));}
}
