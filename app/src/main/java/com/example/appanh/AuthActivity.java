package com.example.appanh;
import android.content.*;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class AuthActivity extends BaseActivity {
 protected boolean register;
 private boolean busy;
 @Override public void onCreate(Bundle state){super.onCreate(state);register=this instanceof RegisterActivity;screen(register?R.layout.activity_register:R.layout.activity_login);click(R.id.submit,this::submit);click(R.id.switchAuth,()->{go(register?LoginActivity.class:RegisterActivity.class);finish();});click(R.id.google,()->go(GoogleSignInActivity.class));click(R.id.forgot,this::reset);}
 private void setBusy(boolean value){busy=value;findViewById(R.id.submit).setEnabled(!value);android.view.View progress=findViewById(R.id.authProgress);if(progress!=null)progress.setVisibility(value?View.VISIBLE:View.GONE);}
 private void submit(){
  if(busy)return;String email=input(R.id.email),password=((android.widget.EditText)findViewById(R.id.password)).getText().toString();
  if(email.startsWith("+")){phone(email);return;}
  if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){toast("Nhập email hợp lệ hoặc số điện thoại dạng +84…");return;}
  if(password.length()<6){toast("Mật khẩu cần ít nhất 6 ký tự.");return;}
  if(register&&input(R.id.name).isEmpty()){toast("Vui lòng nhập họ tên.");return;}
  setBusy(true);status("Đang kết nối…");
  com.google.android.gms.tasks.Task<AuthResult> task=register?FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password):FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password);
  task.addOnCompleteListener(this,t->{setBusy(false);if(!t.isSuccessful()){status(t.getException()==null?"Đăng nhập thất bại":t.getException().getLocalizedMessage());return;}
   FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();if(user==null)return;
   if(register){user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(input(R.id.name)).build());saveProfile(user,input(R.id.name));user.sendEmailVerification().addOnCompleteListener(this,sent->{if(!sent.isSuccessful())toast("Chưa gửi được email. Bạn có thể gửi lại ở màn hình tiếp theo.");go(OtpActivity.class);finish();});}
   else {saveProfile(user,user.getDisplayName());go(user.isEmailVerified()?MainActivity.class:OtpActivity.class);finish();}
  });
 }
 private void reset(){String email=input(R.id.email);if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){toast("Nhập email trước khi đặt lại mật khẩu.");return;}FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(this,t->status(t.isSuccessful()?"Đã gửi email đặt lại mật khẩu.":String.valueOf(t.getException().getLocalizedMessage())));}
 private void phone(String number){
  if(!number.matches("\\+[1-9][0-9]{7,14}")){toast("Số điện thoại cần có mã quốc gia, ví dụ +84901234567.");return;}
  setBusy(true);status("Đang gửi SMS…");
  PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance()).setPhoneNumber(number).setTimeout(60L,TimeUnit.SECONDS).setActivity(this).setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
   @Override public void onVerificationCompleted(PhoneAuthCredential credential){FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(AuthActivity.this,t->{setBusy(false);if(t.isSuccessful()){saveProfile(FirebaseAuth.getInstance().getCurrentUser(),null);go(MainActivity.class);finish();}else status(t.getException().getLocalizedMessage());});}
   @Override public void onVerificationFailed(com.google.firebase.FirebaseException e){setBusy(false);status(e.getLocalizedMessage());}
   @Override public void onCodeSent(String id,PhoneAuthProvider.ForceResendingToken token){setBusy(false);startActivity(new Intent(AuthActivity.this,OtpActivity.class).putExtra("verificationId",id).putExtra("phone",number));}
  }).build());
 }
 public static void saveProfile(FirebaseUser user,String name){
  if(user==null)return;
  FirebaseFirestore db=FirebaseFirestore.getInstance();
  DocumentReference ref=db.collection("users").document(user.getUid());
  db.runTransaction(transaction->{
   DocumentSnapshot old=transaction.get(ref);
   Map<String,Object> data=new HashMap<>();
   data.put("email",user.getEmail());data.put("phoneNumber",user.getPhoneNumber());
   data.put("emailVerified",user.isEmailVerified());data.put("lastLoginAt",FieldValue.serverTimestamp());
   if(!old.exists())data.put("createdAt",FieldValue.serverTimestamp());
   // Keep the name edited in the user's Firestore profile across logins.
   String display=name==null?user.getDisplayName():name;
   if(display!=null&&(!old.contains("displayName")||old.getString("displayName")==null||old.getString("displayName").trim().isEmpty()))data.put("displayName",display);
   transaction.set(ref,data,SetOptions.merge());return null;
  });
 }
}
