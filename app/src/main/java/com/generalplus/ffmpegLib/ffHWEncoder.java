package com.generalplus.ffmpegLib;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Hardware encoder wrapper used to capture frames from the FFmpeg decoder and
 * encode them to an H.264 elementary stream.  This version removes the
 * dependency on {@link android.test.AndroidTestCase} and instead throws
 * {@link RuntimeException}s when invalid conditions are encountered.  It also
 * relies on {@link ffmpegWrapper} and {@link ffDecodeFrame} from the
 * {@code com.generalplus.ffmpegLib} package.
 */
public class ffHWEncoder {
    private static final String TAG = "ffHWEncoder";
    private static final boolean VERBOSE = false;           // lots of logging
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding

    public static final int HWENCODE_QUALITY_LOW = 0x01;
    public static final int HWENCODE_QUALITY_MEDIUM = 0x02;
    public static final int HWENCODE_QUALITY_HIGH = 0x04;

    private int mFPS = 15;
    private int motionfactor = HWENCODE_QUALITY_HIGH;
    private int mBitRate = 6000000;
    private MediaMuxer mMuxer = null;
    private MediaCodec mEncoder = null;
    private int mColorFormat = -1;
    private boolean mMuxerStarted = false;
    private int mTrackIndex = -1;
    private long mFirstFrameTime = 0;
    private boolean mStop = false;
    private Timer mEncodetimer = null;

    //--------------------------------------------------------------------------------
    public void SetFPS(int i32FPS) {
        mFPS = i32FPS;
        if (mFPS <= 0) {
            mFPS = 15;
        }
    }

    //--------------------------------------------------------------------------------
    public void SetQuality(int i32Q) {
        motionfactor = i32Q;
        if (motionfactor > HWENCODE_QUALITY_HIGH) {
            motionfactor = HWENCODE_QUALITY_HIGH;
        }
    }

    //--------------------------------------------------------------------------------
    public void Start(String outputPath) throws Exception {
        try {
            int[] res = ffmpegWrapper.getInstance().naGetVideoRes();
            int width = res[0];
            int height = res[1];
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null) {
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            // pixel count  x motion factor(1, 2 or 4)  x 0.07 = bit rate in bps
            mBitRate = (int) (width * height * mFPS * motionfactor * 0.07);
            Log.d(TAG, "found codec: " + codecInfo.getName());
            mColorFormat = selectColorFormat(codecInfo, MIME_TYPE);
            Log.d(TAG, "found colorFormat: " + mColorFormat);
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFPS);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            if (Build.VERSION.SDK_INT > 23) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            }
            if (VERBOSE) Log.d(TAG, "format: " + format);
            mEncoder = MediaCodec.createByCodecName(codecInfo.getName());
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();
            try {
                mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException ioe) {
                throw new RuntimeException("MediaMuxer creation failed", ioe);
            }
            ffmpegWrapper.getInstance().naSetCovertDecodeFrameFormat(ffDecodeFrame.FFDECODE_FORMAT_YUV420P);
            mStop = false;
            mMuxerStarted = false;
            mTrackIndex = -1;
            mFirstFrameTime = System.nanoTime();
            mEncodetimer = new Timer(true);
            mEncodetimer.schedule(new EncodeTask(), 0, 40);
        } catch (Exception e) {
            Release();
        }
    }

    //--------------------------------------------------------------------------------
    public void Stop() {
        mStop = true;
    }

    //--------------------------------------------------------------------------------
    private void Release() {
        if (VERBOSE) Log.d(TAG, "releasing codecs");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    //--------------------------------------------------------------------------------
    private class EncodeTask extends TimerTask {
        @Override
        public void run() {
            if (mStop) {
                EncodeData(null);
                if (mEncodetimer != null) {
                    mEncodetimer.cancel();
                    mEncodetimer = null;
                }
                Release();
            } else {
                ffDecodeFrame decodeFrame = ffmpegWrapper.getInstance().naGetDecodeFrame();
                int i32Y = decodeFrame.width * decodeFrame.height;
                int i32U = decodeFrame.width * decodeFrame.height / 4;
                int i32V = decodeFrame.width * decodeFrame.height / 4;
                byte[] yuv420Data = new byte[i32Y + i32U + i32V];
                switch (mColorFormat) {
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: {
                        System.arraycopy(decodeFrame.data[0], 0, yuv420Data, 0, i32Y);
                        System.arraycopy(decodeFrame.data[1], 0, yuv420Data, i32Y, i32U);
                        System.arraycopy(decodeFrame.data[2], 0, yuv420Data, i32Y + i32U, i32V);
                        break;
                    }
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                        // Unsupported formats for this encoder
                        return;
                    default:
                        return;
                }
                EncodeData(yuv420Data);
            }
        }
    }

    //--------------------------------------------------------------------------------
    private void EncodeData(byte[] frameData) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderInputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        boolean encoderDone = false;
        while (!encoderDone) {
            if (!inputDone) {
                int inputBufIndex = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    long ptsUsec = computePresentationTimeMicros();
                    if (mStop) {
                        mEncoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];
                        if (inputBuf.capacity() < frameData.length) {
                            throw new RuntimeException("input buffer capacity " + inputBuf.capacity() + " is less than frameData length " + frameData.length);
                        }
                        inputBuf.clear();
                        inputBuf.put(frameData);
                        mEncoder.queueInputBuffer(inputBufIndex, 0, frameData.length, ptsUsec, 0);
                        inputDone = true;
                    }
                }
            }
            if (!encoderDone) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    encoderDone = true;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    if (mMuxerStarted) {
                        throw new RuntimeException("format changed twice");
                    }
                    mTrackIndex = mMuxer.addTrack(newFormat);
                    mMuxer.start();
                    mMuxerStarted = true;
                } else if (encoderStatus < 0) {
                    throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        info.size = 0;
                    } else {
                        encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    }
                    if (info.size != 0) {
                        if (!mMuxerStarted) {
                            throw new RuntimeException("muxer hasn't started");
                        }
                        encodedData.position(info.offset);
                        encodedData.limit(info.offset + info.size);
                        mMuxer.writeSampleData(mTrackIndex, encodedData, info);
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                }
            }
        }
    }

    //--------------------------------------------------------------------------------
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    //--------------------------------------------------------------------------------
    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int colorFormat : capabilities.colorFormats) {
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        throw new RuntimeException("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
    }

    //--------------------------------------------------------------------------------
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    //--------------------------------------------------------------------------------
    private long computePresentationTimeMicros() {
        return (System.nanoTime() - mFirstFrameTime) / 1000;
    }
}