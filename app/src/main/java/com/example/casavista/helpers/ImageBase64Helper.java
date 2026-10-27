package com.example.casavista.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageBase64Helper {

    private static final int DEFAULT_MAX_DIMENSION = 800;
    private static final int DEFAULT_JPEG_QUALITY = 80;

    /**
     * Efficiently reads an image Uri, scales down to prevent memory issues,
     * compresses to JPEG, and returns a Base64 encoded string.
     */
    public static String uriToBase64(Context context, Uri imageUri) {
        return uriToBase64(context, imageUri, DEFAULT_MAX_DIMENSION, DEFAULT_JPEG_QUALITY);
    }

    public static String uriToBase64(Context context, Uri imageUri, int maxDimension, int quality) {
        if (context == null || imageUri == null) return null;
        try {
            // First decode bounds only to calculate inSampleSize
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, boundsOptions);
            }

            int origWidth = boundsOptions.outWidth;
            int origHeight = boundsOptions.outHeight;
            if (origWidth <= 0 || origHeight <= 0) return null;

            int sampleSize = 1;
            while ((origWidth / sampleSize) > maxDimension * 1.5 || (origHeight / sampleSize) > maxDimension * 1.5) {
                sampleSize *= 2;
            }

            // Decode the actual bitmap with sample size
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                if (is == null) return null;
                bitmap = BitmapFactory.decodeStream(is, null, decodeOptions);
            }

            if (bitmap == null) return null;

            // Scale down to exact maxDimension if still larger
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w > maxDimension || h > maxDimension) {
                float ratio = Math.min((float) maxDimension / w, (float) maxDimension / h);
                int targetW = Math.round(w * ratio);
                int targetH = Math.round(h * ratio);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                    bitmap = scaled;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] bytes = baos.toByteArray();
            bitmap.recycle();

            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decodes a Base64 string (with or without 'data:image/...;base64,' prefix) into a Bitmap.
     */
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) return null;
        try {
            String clean = base64String.trim();
            if (clean.contains(",")) {
                clean = clean.substring(clean.indexOf(",") + 1);
            }
            byte[] decoded = Base64.decode(clean, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Compresses a Bitmap and converts it into a Base64 string.
     */
    public static String bitmapToBase64(Bitmap bitmap, int quality) {
        if (bitmap == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encodeImageToBase64(Context context, Uri imageUri) {
        return uriToBase64(context, imageUri);
    }

    public static Bitmap decodeBase64ToBitmap(String base64String) {
        return base64ToBitmap(base64String);
    }
}
