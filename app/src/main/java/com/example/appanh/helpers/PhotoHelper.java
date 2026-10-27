package com.example.appanh.helpers;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.example.appanh.models.Photo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility helper class for local device media loading, EXIF extraction,
 * thumbnail generation, image formatting, and compression.
 */
public class PhotoHelper {

    private static final String TAG = "PhotoHelper";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface OnMediaLoadedListener {
        void onMediaLoaded(List<Photo> mediaList);
        void onError(String error);
    }

    // ------------------------------------------------------------------------
    // MediaStore Loading Operations
    // ------------------------------------------------------------------------

    /**
     * Loads local photos and videos synchronously using MediaStore.
     */
    public static List<Photo> loadLocalMedia(Context context) {
        List<Photo> mediaList = new ArrayList<>();
        if (context == null) return mediaList;

        ContentResolver contentResolver = context.getContentResolver();

        // 1. Load Images
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] imageProjection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = contentResolver.query(imagesUri, imageProjection, null, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                int widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
                int heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
                int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
                int dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                int dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    String path = cursor.getString(dataCol);
                    long size = cursor.getLong(sizeCol);
                    int width = cursor.getInt(widthCol);
                    int height = cursor.getInt(heightCol);
                    String mimeType = cursor.getString(mimeCol);

                    if (mimeType == null || mimeType.isEmpty()) {
                        mimeType = "image/jpeg";
                    }

                    Photo photo = new Photo();
                    photo.setId(String.valueOf(id));
                    photo.setFileName(name != null ? name : "IMG_" + id);
                    photo.setFilePath(path);
                    photo.setFileSize(size);
                    photo.setWidth(width);
                    photo.setHeight(height);
                    photo.setMimeType(mimeType);
                    photo.setVideo(false);

                    long dateTakenMs = 0;
                    if (dateTakenCol != -1) {
                        dateTakenMs = cursor.getLong(dateTakenCol);
                    }
                    if (dateTakenMs <= 0 && dateAddedCol != -1) {
                        dateTakenMs = cursor.getLong(dateAddedCol) * 1000L;
                    }
                    if (dateTakenMs > 0) {
                        photo.setDateTaken(new Date(dateTakenMs));
                        photo.setCreatedAt(new Date(dateTakenMs));
                    } else {
                        photo.setCreatedAt(new Date());
                    }

                    // Content URI for thumbnail or loading
                    Uri contentUri = ContentUris.withAppendedId(imagesUri, id);
                    photo.setThumbnailPath(contentUri.toString());

                    mediaList.add(photo);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore images", e);
        }

        // 2. Load Videos
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] videoProjection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATE_ADDED
        };

        String videoSortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = contentResolver.query(videoUri, videoProjection, null, null, videoSortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
                int heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
                int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
                int durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
                int dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
                int dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    String path = cursor.getString(dataCol);
                    long size = cursor.getLong(sizeCol);
                    int width = cursor.getInt(widthCol);
                    int height = cursor.getInt(heightCol);
                    String mimeType = cursor.getString(mimeCol);
                    long duration = durationCol != -1 ? cursor.getLong(durationCol) : 0;

                    if (mimeType == null || mimeType.isEmpty()) {
                        mimeType = "video/mp4";
                    }

                    Photo video = new Photo();
                    video.setId(String.valueOf(id));
                    video.setFileName(name != null ? name : "VID_" + id);
                    video.setFilePath(path);
                    video.setFileSize(size);
                    video.setWidth(width);
                    video.setHeight(height);
                    video.setMimeType(mimeType);
                    video.setVideo(true);
                    video.setDuration(duration);

                    long dateTakenMs = 0;
                    if (dateTakenCol != -1) {
                        dateTakenMs = cursor.getLong(dateTakenCol);
                    }
                    if (dateTakenMs <= 0 && dateAddedCol != -1) {
                        dateTakenMs = cursor.getLong(dateAddedCol) * 1000L;
                    }
                    if (dateTakenMs > 0) {
                        video.setDateTaken(new Date(dateTakenMs));
                        video.setCreatedAt(new Date(dateTakenMs));
                    } else {
                        video.setCreatedAt(new Date());
                    }

                    Uri contentUri = ContentUris.withAppendedId(videoUri, id);
                    video.setThumbnailPath(contentUri.toString());

                    mediaList.add(video);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore videos", e);
        }

        // Sort unified list descending by date
        mediaList.sort((p1, p2) -> {
            Date d1 = p1.getDateTaken() != null ? p1.getDateTaken() : p1.getCreatedAt();
            Date d2 = p2.getDateTaken() != null ? p2.getDateTaken() : p2.getCreatedAt();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });

        return mediaList;
    }

    /**
     * Loads local photos and videos asynchronously.
     */
    public static void loadLocalMediaAsync(Context context, OnMediaLoadedListener listener) {
        EXECUTOR.execute(() -> {
            try {
                List<Photo> list = loadLocalMedia(context);
                MAIN_HANDLER.post(() -> {
                    if (listener != null) listener.onMediaLoaded(list);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading media asynchronously", e);
                MAIN_HANDLER.post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        });
    }

    // ------------------------------------------------------------------------
    // EXIF Metadata Operations
    // ------------------------------------------------------------------------

    /**
     * Retrieves an ExifInterface object for the given file path.
     */
    public static ExifInterface getExifData(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        try {
            return new ExifInterface(filePath);
        } catch (IOException e) {
            Log.e(TAG, "Failed to get EXIF data from: " + filePath, e);
            return null;
        }
    }

    /**
     * Extracts readable EXIF metadata details into a Map.
     */
    public static Map<String, String> getExifDetails(String filePath) {
        Map<String, String> details = new LinkedHashMap<>();
        ExifInterface exif = getExifData(filePath);
        if (exif == null) return details;

        String make = exif.getAttribute(ExifInterface.TAG_MAKE);
        String model = exif.getAttribute(ExifInterface.TAG_MODEL);
        String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
        String fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
        String expTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        String iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
        String focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
        String flash = exif.getAttribute(ExifInterface.TAG_FLASH);
        String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

        if (make != null || model != null) {
            String device = (make != null ? make : "") + " " + (model != null ? model : "");
            details.put("Thiết bị", device.trim());
        }
        if (date != null) {
            details.put("Thời gian chụp", date);
        }
        if (width != null && length != null && !width.equals("0") && !length.equals("0")) {
            details.put("Độ phân giải", width + " × " + length);
        }
        if (fNumber != null) {
            details.put("Khẩu độ", "f/" + fNumber);
        }
        if (expTime != null) {
            try {
                double sec = Double.parseDouble(expTime);
                if (sec < 1.0 && sec > 0) {
                    details.put("Tốc độ màn trập", "1/" + Math.round(1.0 / sec) + " s");
                } else {
                    details.put("Tốc độ màn trập", expTime + " s");
                }
            } catch (NumberFormatException e) {
                details.put("Tốc độ màn trập", expTime);
            }
        }
        if (iso != null) {
            details.put("ISO", "ISO-" + iso);
        }
        if (focalLength != null) {
            details.put("Tiêu cự", focalLength + " mm");
        }
        if (flash != null) {
            details.put("Flash", flash.equals("0") ? "Tắt" : "Bật");
        }

        // GPS coordinates
        float[] latLong = new float[2];
        if (exif.getLatLong(latLong)) {
            details.put("Vị trí (GPS)", String.format(Locale.getDefault(), "%.5f, %.5f", latLong[0], latLong[1]));
        }

        return details;
    }

    /**
     * Extracts latitude and longitude from EXIF data.
     * Returns float[]{latitude, longitude} or null if not available.
     */
    public static float[] getExifGpsCoordinates(String filePath) {
        ExifInterface exif = getExifData(filePath);
        if (exif == null) return null;

        float[] latLong = new float[2];
        if (exif.getLatLong(latLong)) {
            return latLong;
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Thumbnail Generation
    // ------------------------------------------------------------------------

    /**
     * Generates a thumbnail Bitmap from a file path with specified max dimensions.
     */
    public static Bitmap generateThumbnail(String filePath, int width, int height) {
        if (filePath == null || filePath.isEmpty()) return null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File file = new File(filePath);
                if (file.exists()) {
                    return ThumbnailUtils.createImageThumbnail(file, new Size(width, height), null);
                }
            }

            // Fallback for older APIs or if file thumbnail failed
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            options.inSampleSize = calculateInSampleSize(options, width, height);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

            // Handle EXIF orientation rotation
            return rotateBitmapIfRequired(bitmap, filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error generating thumbnail for: " + filePath, e);
            return null;
        }
    }

    /**
     * Generates a thumbnail Bitmap from a Content URI.
     */
    public static Bitmap generateThumbnail(Context context, Uri uri, int width, int height) {
        if (context == null || uri == null) return null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return context.getContentResolver().loadThumbnail(uri, new Size(width, height), null);
            } else {
                InputStream input = context.getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                if (input != null) input.close();

                options.inSampleSize = calculateInSampleSize(options, width, height);
                options.inJustDecodeBounds = false;

                InputStream input2 = context.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(input2, null, options);
                if (input2 != null) input2.close();
                return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating thumbnail from Uri", e);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Formatting Utilities
    // ------------------------------------------------------------------------

    /**
     * Formats byte size into human readable string (B, KB, MB, GB).
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Formats Date to dd/MM/yyyy.
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Formats timestamp in milliseconds to dd/MM/yyyy.
     */
    public static String formatDate(long timestamp) {
        return formatDate(new Date(timestamp));
    }

    /**
     * Formats Date to dd/MM/yyyy HH:mm.
     */
    public static String formatDateTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Formats video duration in milliseconds to mm:ss or hh:mm:ss.
     */
    public static String formatDuration(long durationMs) {
        if (durationMs <= 0) return "00:00";
        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats Date to a friendly relative representation in Vietnamese.
     */
    public static String formatRelativeTime(Date date) {
        if (date == null) return "";

        long now = System.currentTimeMillis();
        long diff = now - date.getTime();

        if (diff < 60 * 1000) {
            return "Vừa xong";
        } else if (diff < 60 * 60 * 1000) {
            long minutes = diff / (60 * 1000);
            return minutes + " phút trước";
        }

        Calendar nowCal = Calendar.getInstance();
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (nowCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)) {
            return "Hôm nay, " + timeFormat.format(date);
        }

        nowCal.add(Calendar.DAY_OF_YEAR, -1);
        if (nowCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)) {
            return "Hôm qua, " + timeFormat.format(date);
        }

        return formatDateTime(date);
    }

    // ------------------------------------------------------------------------
    // Image Dimension and Compression Utilities
    // ------------------------------------------------------------------------

    /**
     * Returns dimensions of an image without loading full bitmap into memory.
     * @return int[]{width, height}
     */
    public static int[] getImageDimensions(String filePath) {
        if (filePath == null || filePath.isEmpty()) return new int[]{0, 0};

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        return new int[]{options.outWidth, options.outHeight};
    }

    /**
     * Returns dimensions of an image from a Content URI.
     */
    public static int[] getImageDimensions(Context context, Uri uri) {
        if (context == null || uri == null) return new int[]{0, 0};

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            return new int[]{options.outWidth, options.outHeight};
        } catch (Exception e) {
            Log.e(TAG, "Error getting image dimensions from Uri", e);
            return new int[]{0, 0};
        }
    }

    /**
     * Compresses an image file down to a JPEG byte array with maximum dimension and quality.
     */
    public static byte[] compressImage(String filePath, int quality, int maxDimension) {
        if (filePath == null) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension);
        options.inJustDecodeBounds = false;

        Bitmap scaledBitmap = BitmapFactory.decodeFile(filePath, options);
        if (scaledBitmap == null) return null;

        Bitmap rotatedBitmap = rotateBitmapIfRequired(scaledBitmap, filePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int safeQuality = Math.max(10, Math.min(quality, 100));
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, safeQuality, baos);

        if (rotatedBitmap != scaledBitmap) {
            scaledBitmap.recycle();
        }

        return baos.toByteArray();
    }

    /**
     * Compresses an image file and saves the result to a cache file.
     */
    public static File compressImageToFile(Context context, String sourcePath, int quality) {
        if (context == null || sourcePath == null) return null;

        byte[] compressedData = compressImage(sourcePath, quality, 1920);
        if (compressedData == null) return null;

        try {
            File cacheDir = new File(context.getCacheDir(), "compressed_photos");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String fileName = "compressed_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(cacheDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(compressedData);
                fos.flush();
            }
            return destFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving compressed file", e);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Helper calculation methods
    // ------------------------------------------------------------------------

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static Bitmap rotateBitmapIfRequired(Bitmap img, String selectedImage) {
        if (img == null || selectedImage == null) return img;

        try {
            ExifInterface ei = new ExifInterface(selectedImage);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not check EXIF orientation", e);
            return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        if (rotatedImg != img) {
            img.recycle();
        }
        return rotatedImg;
    }
}
