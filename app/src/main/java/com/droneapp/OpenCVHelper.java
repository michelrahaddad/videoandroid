package com.generalplus.GoPlusDrone;
import com.generalplus.GoPlusDrone.R;

import android.util.Log;

/**
 * A lightweight helper wrapper around the native OpenCV panorama stitcher.
 *
 * <p>The original project depended on a native helper class named
 * <code>OpenCVHelper</code> to stitch multiple JPEG images into a single
 * panorama.  That native implementation is not available in this
 * source tree, so this stub provides a minimal API to satisfy the
 * compiler and allow the application to build.  The default
 * implementation simply logs the request and returns {@code -1} to
 * indicate failure.  You can integrate a proper OpenCV based
 * implementation later by replacing the body of
 * {@link #getStitcImageStr(String[], String)} with real stitching
 * logic.</p>
 */
public final class OpenCVHelper {

    private static final String TAG = "OpenCVHelper";

    private OpenCVHelper() {
        // Prevent instantiation
    }

    /**
     * Stitches an array of image file paths into a single panorama saved at
     * the destination path.
     *
     * @param src an array of absolute paths to the source JPEG images
     * @param dst the absolute path where the stitched panorama should be
     *            written
     * @return zero on success, non‑zero on error
     */
    public static int getStitcImageStr(String[] src, String dst) {
        Log.w(TAG, "OpenCV stitching requested but no native implementation is available.");
        Log.w(TAG, "Source images: " + java.util.Arrays.toString(src));
        Log.w(TAG, "Destination: " + dst);
        // Returning non‑zero indicates failure; callers of this method in
        // AlogInterface.java interpret zero as success and non‑zero as
        // failure.  Modify this return value as appropriate when you
        // implement real stitching logic.
        return -1;
    }
}