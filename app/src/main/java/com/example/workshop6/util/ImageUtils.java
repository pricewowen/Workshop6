package com.example.workshop6.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
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

        // Dimension check – skip for camera capture (we resize on save)
        if (!isOurCameraFile) {
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return "Could not read the selected image.";

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, opts);

                if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                    return "Selected file is not a valid image.";
                }
                if (opts.outWidth > MAX_DIMENSION_PX || opts.outHeight > MAX_DIMENSION_PX) {
                    return context.getString(com.example.workshop6.R.string.error_photo_dimensions_too_large);
                }
            } catch (Exception e) {
                return "Could not read the selected image.";
            }
        }

        return null;
    }

    /**
     * Saves a profile image to app storage. When {@code userId} is positive, uses {@code user_{id}.jpg};
     * otherwise uses {@code profile_local.jpg} (API-backed sessions without a local numeric id).
     */
    public static String saveProfilePhoto(Context context, Uri uri, int userId) {
        if (uri == null) return null;

        Bitmap bitmap;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) return null;
        } catch (Exception e) {
            return null;
        }

        // Keep source framing and resize for storage.
        bitmap = scaleDownToMaxSize(bitmap, MAX_DIMENSION_PX);

        File dir = new File(context.getFilesDir(), "profile_photos");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        String fileName = userId > 0 ? ("user_" + userId + ".jpg") : "profile_local.jpg";
        File outFile = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            boolean ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            return ok ? outFile.getAbsolutePath() : null;
        } catch (Exception e) {
            return null;
        } finally {
            bitmap.recycle();
        }
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

    /**
     * Decodes a local file path with sampling to avoid OOM on large images.
     */
    public static Bitmap decodeFileForPreview(String path, int maxSizePx) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calculateInSampleSize(bounds, maxSizePx, maxSizePx);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap sampled = BitmapFactory.decodeFile(path, opts);
            if (sampled == null) {
                return null;
            }
            return scaleDownToMaxSize(sampled, maxSizePx);
        } catch (OutOfMemoryError oom) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }
}