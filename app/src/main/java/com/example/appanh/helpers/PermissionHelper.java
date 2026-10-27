package com.example.appanh.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility helper class for runtime permission checks, requests, and rationale dialogs.
 */
public class PermissionHelper {

    public static final int REQUEST_CODE_STORAGE = 1001;
    public static final int REQUEST_CODE_CAMERA = 1002;
    public static final int REQUEST_CODE_NOTIFICATIONS = 1003;
    public static final int REQUEST_CODE_ALL = 1004;

    // ------------------------------------------------------------------------
    // Storage Permissions
    // ------------------------------------------------------------------------

    /**
     * Returns the appropriate storage permissions array depending on Android OS version.
     */
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Checks if storage permissions are granted.
     */
    public static boolean hasStoragePermission(Context context) {
        if (context == null) return false;
        String[] permissions = getStoragePermissions();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requests storage permissions for the activity.
     */
    public static void requestStoragePermission(Activity activity, int requestCode) {
        if (activity == null) return;
        String[] permissions = getStoragePermissions();
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    // ------------------------------------------------------------------------
    // Camera Permission
    // ------------------------------------------------------------------------

    /**
     * Checks if camera permission is granted.
     */
    public static boolean hasCameraPermission(Context context) {
        if (context == null) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests camera permission for the activity.
     */
    public static void requestCameraPermission(Activity activity, int requestCode) {
        if (activity == null) return;
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, requestCode);
    }

    // ------------------------------------------------------------------------
    // Notification Permission
    // ------------------------------------------------------------------------

    /**
     * Checks if notification permission is granted (Android 13+).
     */
    public static boolean hasNotificationPermission(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Requests notification permission for the activity (Android 13+).
     */
    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, requestCode);
        }
    }

    // ------------------------------------------------------------------------
    // Combined Permissions
    // ------------------------------------------------------------------------

    /**
     * Checks if all required gallery permissions (storage and camera) are granted.
     */
    public static boolean hasAllPermissions(Context context) {
        return hasStoragePermission(context) && hasCameraPermission(context) && hasNotificationPermission(context);
    }

    /**
     * Requests all ungranted permissions in a single prompt.
     */
    public static void requestAllPermissions(Activity activity, int requestCode) {
        if (activity == null) return;

        List<String> list = new ArrayList<>();

        for (String perm : getStoragePermissions()) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                list.add(perm);
            }
        }

        if (!hasCameraPermission(activity)) {
            list.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(activity)) {
            list.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(activity, list.toArray(new String[0]), requestCode);
        }
    }

    /**
     * Utility to check whether all results in an onRequestPermissionsResult callback are GRANTED.
     */
    public static boolean isPermissionGranted(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) return false;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------------

    /**
     * Shows a rationale dialog explaining why a permission is needed before asking again.
     */
    public static void showPermissionRationaleDialog(Activity activity, String title, String message, Runnable onPositive) {
        if (activity == null || activity.isFinishing()) return;

        new MaterialAlertDialogBuilder(activity)
                .setTitle(title != null ? title : "Cấp quyền truy cập")
                .setMessage(message != null ? message : "Ứng dụng cần quyền này để hiển thị và quản lý ảnh/video trên thiết bị của bạn.")
                .setPositiveButton("Cho phép", (dialog, which) -> {
                    if (onPositive != null) {
                        onPositive.run();
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    /**
     * Shows a dialog directing the user to app settings if permission was permanently denied.
     */
    public static void showSettingsDialog(Activity activity, String message) {
        if (activity == null || activity.isFinishing()) return;

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Cần cấp quyền trong Cài đặt")
                .setMessage(message != null ? message : "Quyền đã bị từ chối trước đó. Vui lòng vào Cài đặt ứng dụng để bật lại quyền này.")
                .setPositiveButton("Mở Cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}
