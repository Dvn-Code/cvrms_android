package com.example.casavista.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.casavista.network.SupabaseClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class AvatarHelper {

    private static final String PREFS_NAME = "CasaVistaAvatarPrefs";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static String sanitizeKey(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return "default_guest";
        return identifier.trim().toLowerCase(Locale.US).replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public static String getCurrentUserEmail(Context context) {
        if (context == null) return "guest@casavista.com";
        SharedPreferences authPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String email = authPrefs.getString("user_email", null);
        if (email == null || email.trim().isEmpty()) {
            SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            email = userPrefs.getString("user_email", null);
        }
        if (email == null || email.trim().isEmpty()) {
            SharedPreferences staffPrefs = context.getSharedPreferences("StaffSessionPrefs", Context.MODE_PRIVATE);
            email = staffPrefs.getString("staff_email", null);
        }
        if (email == null || email.trim().isEmpty()) {
            SharedPreferences superPrefs = context.getSharedPreferences("SuperAdminProfilePrefs", Context.MODE_PRIVATE);
            email = superPrefs.getString("admin_email", "superadmin@casavista.com");
        }
        return email != null ? email.trim().toLowerCase(Locale.US) : "guest@casavista.com";
    }

    public static void saveProfileImage(Context context, Uri uri) {
        saveProfileImage(context, getCurrentUserEmail(context), uri, null);
    }

    public static void saveProfileImage(Context context, String userEmail, Uri uri, Runnable onUploaded) {
        if (context == null || uri == null) return;
        final String cleanEmail = (userEmail != null && !userEmail.trim().isEmpty())
                ? userEmail.trim().toLowerCase(Locale.US)
                : getCurrentUserEmail(context);

        new Thread(() -> {
            String base64Image = ImageBase64Helper.uriToBase64(context, uri, 512, 80);
            if (base64Image == null) return;

            // Cache locally for this specific account
            saveBase64ToCache(context, cleanEmail, base64Image);

            // Upload directly to Supabase as base64
            SupabaseClient.getInstance().uploadMedia(cleanEmail, "profile", base64Image, new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    if (onUploaded != null) {
                        mainHandler.post(onUploaded);
                    }
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }).start();
    }

    public static void saveBase64ToCache(Context context, String userEmail, String base64Image) {
        if (context == null || base64Image == null) return;
        String key = sanitizeKey(userEmail);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("avatar_b64_" + key, base64Image).apply();

        // Also save as local cache image file
        try {
            Bitmap bm = ImageBase64Helper.base64ToBitmap(base64Image);
            if (bm != null) {
                File file = new File(context.getFilesDir(), "avatar_" + key + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    bm.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                }
            }
        } catch (Exception ignored) {}
    }

    public static String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.US);
        } else {
            String firstInitial = parts[0].substring(0, 1).toUpperCase(Locale.US);
            String lastInitial = parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.US);
            return firstInitial + lastInitial;
        }
    }

    public static void loadAvatar(Context context, ImageView imageView, TextView tvInitials, String fullName) {
        loadAvatar(context, getCurrentUserEmail(context), imageView, tvInitials, fullName);
    }

    public static void loadAvatar(Context context, String userEmail, ImageView imageView, TextView tvInitials, String fullName) {
        if (context == null) return;
        final String cleanEmail = (userEmail != null && !userEmail.trim().isEmpty())
                ? userEmail.trim().toLowerCase(Locale.US)
                : getCurrentUserEmail(context);
        final String key = sanitizeKey(cleanEmail);

        // 1. Check local file cache for this specific email
        File file = new File(context.getFilesDir(), "avatar_" + key + ".jpg");
        if (file.exists()) {
            try {
                android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bm != null && imageView != null) {
                    imageView.setImageBitmap(bm);
                    imageView.setVisibility(View.VISIBLE);
                    if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {}
        } else {
            // Check SharedPreferences base64 cache
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String cachedB64 = prefs.getString("avatar_b64_" + key, null);
            if (cachedB64 != null && !cachedB64.isEmpty()) {
                Bitmap bm = ImageBase64Helper.base64ToBitmap(cachedB64);
                if (bm != null && imageView != null) {
                    imageView.setImageBitmap(bm);
                    imageView.setVisibility(View.VISIBLE);
                    if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                }
            } else {
                // Show initials default
                if (imageView != null) imageView.setVisibility(View.GONE);
                if (tvInitials != null) {
                    tvInitials.setText(getInitials(fullName));
                    tvInitials.setVisibility(View.VISIBLE);
                }
            }
        }

        // 2. Refresh from Supabase in background
        SupabaseClient.getInstance().fetchMedia(cleanEmail, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    String b64 = obj.optString("image_base64", null);
                    if (b64 != null && !b64.isEmpty()) {
                        saveBase64ToCache(context, cleanEmail, b64);
                        Bitmap remoteBm = ImageBase64Helper.base64ToBitmap(b64);
                        if (remoteBm != null && imageView != null) {
                            imageView.setImageBitmap(remoteBm);
                            imageView.setVisibility(View.VISIBLE);
                            if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }
}