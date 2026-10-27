package com.example.appanh;

import android.os.Bundle;
import android.view.View;
import com.google.android.material.appbar.MaterialToolbar;

/** A separate, discoverable collection for hidden photos and videos. */
public class HiddenMediaActivity extends GalleryActivity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);showHidden=true;
        MaterialToolbar toolbar=findViewById(R.id.toolbar);
        toolbar.setTitle("Ảnh và video đã ẩn");toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        findViewById(R.id.navigation).setVisibility(View.GONE);
        findViewById(R.id.importMedia).setVisibility(View.GONE);
        View empty=findViewById(R.id.emptyState);
        if(empty instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)empty;((android.widget.TextView)group.getChildAt(1)).setText("Chưa có ảnh hoặc video đã ẩn");((android.widget.TextView)group.getChildAt(2)).setText("Nhấn giữ một mục trong thư viện rồi chọn Ẩn. Nhấn giữ mục tại đây để bỏ ẩn và đưa về thư viện.");}
    }
}
