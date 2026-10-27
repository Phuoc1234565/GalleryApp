package com.example.appanh;

import com.example.appanh.models.Photo;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GalleryQueryTest {
    private Photo photo(String id,String name,long time){
        Photo p=new Photo(null,name,null,time,100,100,"image/jpeg");p.setId(id);p.setDateTaken(new Date(time));return p;
    }
    @Test public void vietnameseSearchIgnoresAccentsCaseAndOuterSpaces(){
        Photo p=photo("a","Biển Đà Nẵng.JPG",1);
        assertEquals(Collections.singletonList(p),GalleryQuery.apply(Collections.singletonList(p),false,false,null,"all","  BIEN da NANG  ","newest"));
        assertEquals("du lich",GalleryQuery.normalize("ĐU LỊCH"));
    }
    @Test public void privateDeletedAndOtherAlbumPhotosStayOutsideViewer(){
        Photo normal=photo("normal","Photo",1),hidden=photo("hidden","Photo",2),deleted=photo("deleted","Photo",3),other=photo("other","Photo",4);
        normal.getAlbumIds().add("album");hidden.getAlbumIds().add("album");deleted.getAlbumIds().add("album");
        hidden.setHidden(true);deleted.setDeleted(true);
        assertEquals(Collections.singletonList(normal),GalleryQuery.apply(Arrays.asList(normal,hidden,deleted,other),false,false,"album","all","","newest"));
    }
    @Test public void orderingIsSharedAndDeterministic(){
        Photo a=photo("a","Ánh.jpg",1),b=photo("b","Biển.jpg",2);
        List<Photo> photos=Arrays.asList(b,a);
        assertEquals(Arrays.asList(b,a),GalleryQuery.apply(photos,false,false,null,"all","","newest"));
        assertEquals(Arrays.asList(a,b),GalleryQuery.apply(photos,false,false,null,"all","","oldest"));
        assertEquals(Arrays.asList(a,b),GalleryQuery.apply(photos,false,false,null,"all","","type"));
        assertEquals(Arrays.asList(b,a),GalleryQuery.apply(photos,false,false,null,"all","","size"));
    }
    @Test public void videoAndFavoritesFiltersRespectHiddenCollection(){
        Photo video=photo("v","clip.mp4",1),image=photo("i","image.jpg",2);video.setMimeType("video/mp4");video.setFavorite(true);video.setHidden(true);
        List<Photo> photos=Arrays.asList(video,image);
        assertTrue(GalleryQuery.apply(photos,false,false,null,"videos","","newest").isEmpty());
        assertEquals(Collections.singletonList(video),GalleryQuery.apply(photos,false,true,null,"favorites","","newest"));
    }
    @Test public void missingNamesAndDatesDoNotCrash(){
        Photo photo=new Photo();photo.setId("empty");
        assertEquals(1,GalleryQuery.apply(Collections.singletonList(photo),false,false,null,"all",null,"newest").size());
    }
}
