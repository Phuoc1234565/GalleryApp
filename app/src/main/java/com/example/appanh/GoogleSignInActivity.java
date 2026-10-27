package com.example.appanh;
import android.os.Bundle;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;
public class GoogleSignInActivity extends BaseActivity {
 private final ActivityResultLauncher<android.content.Intent> launcher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{try{GoogleSignInAccount a=GoogleSignIn.getSignedInAccountFromIntent(r.getData()).getResult(ApiException.class);if(a==null||a.getIdToken()==null){status("Không nhận được token Google.");return;}FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(a.getIdToken(),null)).addOnCompleteListener(this,t->{if(t.isSuccessful()){AuthActivity.saveProfile(FirebaseAuth.getInstance().getCurrentUser(),a.getDisplayName());go(MainActivity.class);finish();}else status(t.getException().getLocalizedMessage());});}catch(ApiException e){status("Đăng nhập Google chưa hoàn tất ("+e.getStatusCode()+"). Kiểm tra SHA-1/SHA-256 và google-services.json.");}});
 @Override public void onCreate(Bundle state){super.onCreate(state);screen(R.layout.activity_google_sign_in);click(R.id.google,()->{int id=getResources().getIdentifier("default_web_client_id","string",getPackageName());if(id==0){status("Bật Google provider trong Firebase, thêm SHA và tải lại google-services.json.");return;}GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(id)).requestEmail().build();launcher.launch(GoogleSignIn.getClient(this,options).getSignInIntent());});}
}
