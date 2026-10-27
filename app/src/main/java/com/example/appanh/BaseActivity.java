package com.example.appanh;

import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {
    protected GalleryRepository repo;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Authentication screens do not read the media cache. In particular, the
        // welcome screen must draw before Firebase authentication is warmed up.
        if (!(this instanceof SplashActivity) && !(this instanceof AuthActivity)
                && !(this instanceof OtpActivity) && !(this instanceof GoogleSignInActivity)
                && !(this instanceof AccountActivity)) {
            repo = new GalleryRepository(this);
        }
    }
    protected void screen(int layout) {
        // Use one inset policy on Android 7–16. Otherwise pre-Android-15 windows
        // already inset the content and our listener would add the bars twice.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        boolean dark = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat barsController = WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());
        barsController.setAppearanceLightStatusBars(!dark);
        barsController.setAppearanceLightNavigationBars(!dark);
        setContentView(layout);
        View root=findViewById(R.id.screenRoot);
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
            // This root owns system/keyboard spacing. Material navigation must
            // not add the same bottom inset again inside its fixed-height bar.
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        MaterialToolbar toolbar=findViewById(R.id.toolbar);
        if(toolbar!=null) toolbar.setNavigationOnClickListener(v->getOnBackPressedDispatcher().onBackPressed());
        BottomNavigationView nav=findViewById(R.id.navigation);
        if(nav!=null) {
            nav.setItemActiveIndicatorEnabled(false);
            if(toolbar!=null && !(this instanceof SettingsActivity))toolbar.setNavigationIcon(null);
            int selected=this instanceof AlbumActivity ? R.id.nav_albums : this instanceof SettingsActivity ? R.id.nav_profile : this instanceof SearchActivity ? R.id.nav_search : R.id.nav_photos;
            nav.setSelectedItemId(selected);
            nav.setOnItemSelectedListener(item->{
                if(item.getItemId()==selected)return true;
                Class<?> target=item.getItemId()==R.id.nav_albums?AlbumActivity.class:item.getItemId()==R.id.nav_profile?SettingsActivity.class:item.getItemId()==R.id.nav_search?SearchActivity.class:MainActivity.class;
                Intent intent=new Intent(this,target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); startActivity(intent); return true;
            });
        }
    }
    protected void click(int id,Runnable action){View view=findViewById(id);if(view!=null)view.setOnClickListener(v->action.run());}
    @Override protected void onResume() {
        super.onResume();
        BottomNavigationView nav = findViewById(R.id.navigation);
        if (nav != null) {
            // REORDER_TO_FRONT reuses Activities; restore their own tab when
            // returning instead of keeping the tab tapped before leaving.
            nav.setSelectedItemId(this instanceof AlbumActivity ? R.id.nav_albums
                    : this instanceof SettingsActivity ? R.id.nav_profile
                    : this instanceof SearchActivity ? R.id.nav_search : R.id.nav_photos);
        }
    }
    protected String input(int id){return ((EditText)findViewById(id)).getText().toString().trim();}
    protected void go(Class<?> target){Intent intent=new Intent(this,target);if(target==MainActivity.class&&(this instanceof AuthActivity||this instanceof OtpActivity||this instanceof GoogleSignInActivity))intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(intent);}
    protected void status(String value){TextView text=findViewById(R.id.status);if(text!=null){text.setText(value);text.setVisibility(value==null||value.isEmpty()?View.GONE:View.VISIBLE);}}
    protected void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    protected void result(String error){if(error!=null)toast(error);}
    protected void confirm(String title,Runnable yes){new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setMessage("Bạn có chắc muốn tiếp tục?").setNegativeButton("Hủy",null).setPositiveButton("Đồng ý",(d,w)->yes.run()).show();}
    protected void ask(String title,String initial,java.util.function.Consumer<String> done){EditText text=new EditText(this);text.setText(initial);text.setSingleLine();androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setView(text).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();dialog.setOnShowListener(ignored->dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{String value=text.getText().toString().trim();if(value.isEmpty()||value.length()>80){text.setError("Nhập từ 1 đến 80 ký tự.");return;}done.accept(value);dialog.dismiss();}));dialog.show();text.setSelection(text.length());}
    @Override protected void onDestroy(){if(repo!=null)repo.close();super.onDestroy();}
}
