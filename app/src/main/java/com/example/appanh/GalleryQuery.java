package com.example.appanh;

import com.example.appanh.models.Photo;
import java.text.Normalizer;
import java.util.*;

/** Shared ordering and filtering for the grid and the photo viewer. */
final class GalleryQuery {
    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('đ', 'd').trim();
    }

    static List<Photo> apply(List<Photo> photos, boolean deleted, boolean hidden,
                             String albumId, String type, String query, String sort) {
        String search = normalize(query);
        List<Photo> result = new ArrayList<>();
        for (Photo photo : photos) {
            if (photo.isDeleted() != deleted || (!deleted && photo.isHidden() != hidden)) continue;
            if (albumId != null && !photo.getAlbumIds().contains(albumId)) continue;
            if ("images".equals(type) && photo.isVideo()
                    || "videos".equals(type) && !photo.isVideo()
                    || "favorites".equals(type) && !photo.isFavorite()) continue;
            if (normalize(photo.getFileName()).contains(search)) result.add(photo);
        }
        Comparator<Photo> order = Comparator.comparingLong(p -> p.getDateTaken() == null ? 0 : p.getDateTaken().getTime());
        if ("newest".equals(sort)) order = order.reversed();
        else if ("size".equals(sort)) order = Comparator.comparingLong(Photo::getFileSize).reversed();
        else if ("type".equals(sort)) order = Comparator.comparing(p -> normalize(p.getFileName()));
        result.sort(order.thenComparing(p -> String.valueOf(p.getId())));
        return result;
    }
}
