package com.generalplus.ffmpegLib;

/**
 * Simple container class that holds a decoded frame from FFmpeg.  The frame data
 * is stored in planar YUV format, with separate planes for Y, U and V.  The
 * {@code linesize} array describes the number of bytes per row for each plane.
 */
public class ffDecodeFrame {
    public static final int FFDECODE_FORMAT_YUV420P = 0;
    public static final int FFDECODE_FORMAT_YUV422P = 1;
    public static final int FFDECODE_FORMAT_YUV444P = 2;
    public static final int FFDECODE_FORMAT_YUVJ420P = 3;
    public static final int FFDECODE_FORMAT_YUVJ422P = 4;
    public static final int FFDECODE_FORMAT_YUVJ444P = 5;

    public byte[][] data;
    public int[] linesize;
    public int width;
    public int height;
    public int format;

    public ffDecodeFrame(byte[][] data, int[] linesize, int width, int height, int format) {
        this.data = data;
        this.linesize = linesize;
        this.width = width;
        this.height = height;
        this.format = format;
    }
}