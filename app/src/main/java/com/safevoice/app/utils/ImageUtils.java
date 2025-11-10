package com.safevoice.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * A utility class with static methods for image processing,
 * particularly for converting CameraX ImageProxy objects to Bitmaps.
 */
public class ImageUtils {

    // Private constructor to prevent instantiation of this utility class.
    private ImageUtils() {}

    /**
     * Converts an ImageProxy object (in YUV_420_888 format) to a Bitmap.
     * This is the most common format provided by CameraX's ImageAnalysis.
     *
     * @param imageProxy The ImageProxy from the camera.
     * @return A Bitmap representation of the image, or null if conversion fails.
     */
    public static Bitmap getBitmap(ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            return null;
        }

        Image image = imageProxy.getImage();
        
        // Ensure the image format is YUV_420_888, which is typical for camera previews.
        if (image.getFormat() != ImageFormat.YUV_420_888) {
             // Fallback for other formats like JPEG, though less common in analysis.
             return getBitmapFromPlanes(imageProxy);
        }

        // Conversion from YUV to JPEG bytes
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        // Convert the YUV byte array to a Bitmap
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    
    /**
     * A fallback method that attempts to create a bitmap from the first plane of an ImageProxy.
     * This works for formats like JPEG.
     */
    private static Bitmap getBitmapFromPlanes(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes.length == 0) {
            return null;
        }
        
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
                                              }
