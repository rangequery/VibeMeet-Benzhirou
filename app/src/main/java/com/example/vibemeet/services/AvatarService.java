package com.example.vibemeet.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages avatar photo storage and processing.
 * Crops to square, applies circular mask, saves to internal storage.
 */
public class AvatarService {

    private static final String AVATAR_FILENAME = "user_avatar.png";
    private static final String AVATAR_DIR = "avatars";
    private static final int AVATAR_SIZE = 512;

    private final Context context;

    public AvatarService(Context context) {
        this.context = context.getApplicationContext();
    }

    public File getAvatarFile() {
        File dir = new File(context.getFilesDir(), AVATAR_DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, AVATAR_FILENAME);
    }

    public boolean hasAvatar() {
        return getAvatarFile().exists();
    }

    public Bitmap loadAvatar() {
        File file = getAvatarFile();
        if (!file.exists()) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public Bitmap loadCircularAvatar() {
        Bitmap raw = loadAvatar();
        return raw != null ? toCircular(raw) : null;
    }

    /**
     * Processes a camera-captured image:
     * - Reads EXIF orientation and rotates correctly
     * - Crops to square (centered)
     * - Resizes to 512x512
     * - Saves as PNG to internal storage
     */
    public boolean saveAvatarFromUri(Uri photoUri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(photoUri);
            if (is == null) return false;
            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();

            if (original == null) return false;

            // Handle rotation from EXIF
            int rotation = readExifRotation(photoUri);
            if (rotation != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotation);
                original = Bitmap.createBitmap(original, 0, 0,
                        original.getWidth(), original.getHeight(), m, true);
            }

            // Crop to square
            Bitmap squared = cropToSquare(original);
            // Resize to standard size
            Bitmap sized = Bitmap.createScaledBitmap(squared, AVATAR_SIZE, AVATAR_SIZE, true);

            // Save
            File out = getAvatarFile();
            FileOutputStream fos = new FileOutputStream(out);
            sized.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveAvatarFromBitmap(Bitmap bitmap) {
        try {
            Bitmap squared = cropToSquare(bitmap);
            Bitmap sized = Bitmap.createScaledBitmap(squared, AVATAR_SIZE, AVATAR_SIZE, true);

            File out = getAvatarFile();
            FileOutputStream fos = new FileOutputStream(out);
            sized.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteAvatar() {
        File file = getAvatarFile();
        if (file.exists()) file.delete();
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        return Bitmap.createBitmap(bitmap, x, y, size, size);
    }

    private Bitmap toCircular(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);

        // Circle clip
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Draw bitmap centered
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;
        Rect src = new Rect(x, y, x + size, y + size);
        RectF dst = new RectF(0, 0, size, size);
        canvas.drawBitmap(bitmap, src, dst, paint);

        return output;
    }

    private int readExifRotation(Uri photoUri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(photoUri);
            if (is == null) return 0;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            is.close();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
