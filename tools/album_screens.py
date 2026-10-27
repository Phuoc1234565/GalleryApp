from polish_screens import A, P, put, toolbar, label, row

def polish_albums():
    xmlns=f'xmlns:android="{A}" xmlns:app="{P}"'
    put('layout/item_album.xml',f'''<com.google.android.material.card.MaterialCardView {xmlns} android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_margin="5dp" app:cardCornerRadius="14dp" app:cardElevation="0dp" app:cardBackgroundColor="@color/surface" app:strokeWidth="0dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
    <FrameLayout android:layout_width="match_parent" android:layout_height="120dp">
    <ImageView android:id="@+id/thumbnail" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" android:contentDescription="Ảnh bìa album"/>
    <ImageView android:id="@+id/albumVideoBadge" android:layout_width="40dp" android:layout_height="40dp" android:layout_gravity="center" android:background="@drawable/bg_video_duration" android:src="@drawable/ic_play_circle" app:tint="@color/white" android:contentDescription="Album có video" android:visibility="gone"/>
    </FrameLayout>
    <FrameLayout android:layout_width="match_parent" android:layout_height="60dp">
    <TextView android:id="@+id/caption" android:layout_width="match_parent" android:layout_height="match_parent" android:layout_marginEnd="40dp" android:paddingStart="10dp" android:paddingEnd="0dp" android:gravity="center_vertical" android:textColor="@color/text_primary" android:textSize="15sp" android:lineSpacingExtra="4dp" android:maxLines="2" android:ellipsize="end"/>
    <ImageButton android:id="@+id/albumMore" android:layout_width="40dp" android:layout_height="48dp" android:layout_gravity="end|center_vertical" android:padding="10dp" android:background="?attr/selectableItemBackgroundBorderless" android:src="@drawable/ic_more_vert" app:tint="@color/text_secondary" android:contentDescription="Tùy chọn album"/>
    </FrameLayout>
    <ImageView android:id="@+id/check" android:layout_width="1dp" android:layout_height="1dp" android:visibility="gone" android:contentDescription="Đã chọn"/>
    </LinearLayout></com.google.android.material.card.MaterialCardView>''')
    put('menu/album_actions.xml',f'<menu {xmlns}><item android:id="@+id/addPhotos" android:title="Thêm ảnh" android:icon="@drawable/ic_add" app:showAsAction="always"/><item android:id="@+id/albumOptions" android:title="Tùy chọn album" android:icon="@drawable/ic_more_vert" app:showAsAction="always"/></menu>')
    put('layout/activity_album_detail.xml',f'<LinearLayout {xmlns} android:id="@+id/screenRoot" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="@color/background">{toolbar("Album")}<LinearLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="vertical" android:paddingHorizontal="12dp">'+label('',14,'@color/text_secondary','android:id="@+id/status" android:padding="8dp"')+'<androidx.recyclerview.widget.RecyclerView android:id="@+id/grid" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:paddingBottom="16dp" android:clipToPadding="false"/></LinearLayout></LinearLayout>')
    content=''.join(row(id,title,icon,red=id=='deleteAlbum') for id,title,icon in [('select','Chọn nhiều','ic_select_all'),('addPhotos','Thêm ảnh','ic_album'),('renameAlbum','Đổi tên','ic_edit'),('removeSelected','Bỏ ảnh đã chọn khỏi album','ic_delete'),('deleteAlbum','Xóa album','ic_delete')])
    put('layout/sheet_album_options.xml',f'<LinearLayout {xmlns} android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:background="@drawable/bg_bottom_sheet" android:padding="12dp"><View android:layout_width="36dp" android:layout_height="4dp" android:layout_gravity="center_horizontal" android:layout_marginVertical="8dp" android:background="@drawable/bg_handle_bar"/>'+label('Tùy chọn album',20,extra='android:padding="16dp"')+content+'</LinearLayout>')

if __name__=='__main__':polish_albums()
