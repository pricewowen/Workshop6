package com.example.workshop6.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;

public final class ImageUtils {

    /** Soft limit for gallery picks. */
    public static final long MAX_PHOTO_BYTES = 2L * 1024L * 1024L;
    /** Max size for gallery picks; camera captures skip this (we compress on save). */
    public static final long MAX_PHOTO_BYTES_GALLERY = 16L * 1024L * 1024L;
    public static final int MAX_DIMENSION_PX = 1024;

    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_JPG = "image/jpg";
    private static final String MIME_PNG = "image/png";

    private ImageUtils() { }

    /**
     * Create a content Uri for the camera to write into (cache/images/).
     */
    public static Uri createCameraImageUri(Context context) {
        File dir = new File(context.getCacheDir(), "images");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        File file = new File(dir, "camera_profile.jpg");
        String authority = context.getPackageName() + ".fileprovider";
        return FileProvider.getUriForFile(context, authority, file);
    }

    /**
     * Allowed MIME types: JPEG, JPG, PNG (and default Android camera type is image/jpeg).
     */
    private static boolean isAllowedMimeType(Context context, Uri uri) {
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) {
            String path = uri.getPath();
            if (path != null) {
                String lower = path.toLowerCase(Locale.US);
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            }
            return false;
        }
        mime = mime.toLowerCase(Locale.US);
        return MIME_JPEG.equals(mime) || MIME_JPG.equals(mime) || MIME_PNG.equals(mime);
    }

    public static String validateProfilePhoto(Context context, Uri uri) {
        if (uri == null) return "Please select a photo.";

        // Format: only standard photo types (JPEG, JPG, PNG; Android camera uses image/jpeg)
        if (!isAllowedMimeType(context, uri)) {
            return context.getString(com.example.workshop6.R.string.error_photo_format);
        }

        // Skip size check for our own camera capture (we compress on save); apply limit for gallery picks
        boolean isOurCameraFile = uri.getAuthority() != null
                && uri.getAuthority().equals(context.getPackageName() + ".fileprovider");
        if (!isOurCameraFile) {
            try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
                if (pfd != null) {
                    long size = pfd.getStatSize();
                    if (size > 0 && size > MAX_PHOTO_BYTES_GALLERY) {
                        return context.getString(com.example.workshop6.R.string.error_photo_too_large_gallery);
                    }
                }
            } catch (Exception e) {
                return "Could not read the selected image.";
            }
        }

        // Ensure the selected file decodes as an image; large dimensions are resized on upload.
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return "Could not read the selected image.";
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                return "Selected file is not a valid image.";
            }
        } catch (Exception e) {
            return "Could not read the selected image.";
        }

        return null;
    }

    /** Returns a bitmap scaled so neither dimension exceeds maxSize; recycles the input if scaled. */
    private static Bitmap scaleDownToMaxSize(Bitmap bitmap, int maxSize) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= maxSize && h <= maxSize) return bitmap;
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
        if (scaled != bitmap) bitmap.recycle();
        return scaled;
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    public static Bitmap decodeForUpload(Context context, Uri uri) {
        if (uri == null) return null;
        try (InputStream boundsStream = context.getContentResolver().openInputStream(uri)) {
            if (boundsStream == null) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsStream, null, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = calculateInSampleSize(
                    bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX, MAX_DIMENSION_PX
            );
            try (InputStream imageStream = context.getContentResolver().openInputStream(uri)) {
                if (imageStream == null) return null;
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream, null, decode);
                if (bitmap == null) return null;
                return scaleDownToMaxSize(bitmap, MAX_DIMENSION_PX);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] compressBitmapJpeg(Bitmap bitmap, long maxBytes) {
        if (bitmap == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int quality = 90;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        while (out.size() > maxBytes && quality > 45) {
            out.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        }
        if (out.size() > maxBytes) return null;
        return out.toByteArray();
    }

    public static Bitmap decodeForPreview(Context context, Uri uri) {
        if (uri == null) return null;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) return null;
            return scaleDownToMaxSize(bitmap, 512);
        } catch (Exception e) {
            return null;
        }
    }

}