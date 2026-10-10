package com.example.casavista.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AvatarHelper {

    private static final String PREFS_NAME = "CasaVistaPrefs";
    private static final String KEY_PROFILE_URI = "global_profile_image_uri";

    public static void saveProfileImage(Context context, Uri uri) {
        if (context == null || uri == null) return;
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PROFILE_URI, uri.toString()).apply();
    }

    // Takes first word (First Name) initial and last word (Last Name) initial -> "Juan Dela Cruz" = "JC"
    public static String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        } else {
            String firstInitial = parts[0].substring(0, 1).toUpperCase();
            String lastInitial = parts[parts.length - 1].substring(0, 1).toUpperCase();
            return firstInitial + lastInitial;
        }
    }

    public static void loadAvatar(Context context, ImageView imageView, TextView tvInitials, String fullName) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_PROFILE_URI, null);

        if (uriString != null && !uriString.isEmpty()) {
            try {
                if (imageView != null) {
                    imageView.setImageURI(Uri.parse(uriString));
                    imageView.setVisibility(View.VISIBLE);
                }
                if (tvInitials != null) {
                    tvInitials.setVisibility(View.GONE);
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Show First + Last Name initials when no image exists
        if (imageView != null) {
            imageView.setVisibility(View.GONE);
        }
        if (tvInitials != null) {
            tvInitials.setText(getInitials(fullName));
            tvInitials.setVisibility(View.VISIBLE);
        }
    }
}