package com.photos48.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Utility for loading image thumbnails efficiently without third-party libraries.
 * Uses Android's ContentResolver and BitmapFactory with downsampling.
 */
public class ImageLoader {
    
    private static final String TAG = "ImageLoader";
    private static final int THUMBNAIL_SIZE = 300; // Target size in pixels
    
    /**
     * Load a thumbnail bitmap from a content URI.
     * Efficiently downsamples to avoid OutOfMemoryError.
     * 
     * @param context Application context
     * @param uriString Content URI string
     * @param targetSize Target size in pixels (width/height)
     * @return Bitmap or null if loading fails
     */
    public static Bitmap loadThumbnail(Context context, String uriString, int targetSize) {
        try {
            Uri uri = Uri.parse(uriString);
            
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for: " + uriString);
                return null;
            }
            
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize);
            
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for: " + uriString);
                return null;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            return bitmap;
            
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + uriString, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + uriString, e);
            return null;
        }
    }
    
    /**
     * Load thumbnail with default size.
     */
    public static Bitmap loadThumbnail(Context context, String uriString) {
        return loadThumbnail(context, uriString, THUMBNAIL_SIZE);
    }
    
    /**
     * Calculate sample size for efficient bitmap loading.
     * Public access to calculate optimal downsampling.
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, 
                                           int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
}
