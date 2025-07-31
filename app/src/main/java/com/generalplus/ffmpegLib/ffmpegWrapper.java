package com.generalplus.ffmpegLib;

import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * FFmpeg wrapper that exposes native methods for video playback and recording.  This
 * class implements {@link GLSurfaceView.Renderer} and forwards the GL callbacks to
 * native methods.  It also provides constants for various status codes and
 * configuration parameters.
 */
public class ffmpegWrapper implements GLSurfaceView.Renderer {
    private static final String TAG = "ffmpegWrapper";
    private static ffmpegWrapper mInstance;
    private static Handler mNowViewHandler;

    public static final String LOW_LOADING_TRANSCODE_OPTIONS = "qmin=15;qmax=35;b=400000;g=15;bf=0;refs=2;weightp=simple;level=2.2;" +
            "x264-params=lookahead-threads=3:subme=4:chroma_qp_offset=0";

    public static final int FFMPEG_STATUS_PLAYING = 0x00;
    public static final int FFMPEG_STATUS_STOPPED = 0x01;
    public static final int FFMPEG_STATUS_SAVESNAPSHOTCOMPLETE = 0x02;
    public static final int FFMPEG_STATUS_SAVEVIDEOCOMPLETE = 0x03;
    public static final int FFMPEG_STATUS_BUFFERING = 0x04;

    public static final int EXTRACTOR_OK = 0;
    public static final int EXTRACTOR_BUSY = 1;
    public static final int EXTRACTOR_READFILEFAILED = 2;
    public static final int EXTRACTOR_DECODEFAILED = 3;
    public static final int EXTRACTOR_NOSUCHFRAME = 4;

    public static final int CODEC_ID_NONE = 0;
    public static final int CODEC_ID_MJPEG = 8;
    public static final int CODEC_ID_H264 = 28;

    public enum eFFMPEG_ERRCODE {
        FFMPEGPLAYER_NOERROR,             //0
        FFMPEGPLAYER_INITMEDIAFAILED,     //1
        FFMPEGPLAYER_MEDIAISPLAYING,      //2
        FFMPEGPLAYER_CREATESAVESTREAMFAILED, //3
        FFMPEGPLAYER_SAVESNAPSHOTFAILED,  //4
        FFMPEGPLAYER_SAVEVIDEOFAILED      //5
    }

    public enum ePlayerStatus {
        E_PlayerStatus_Stoped,
        E_PlayerStatus_Playing,
        E_PlayerStatus_Stoping
    }

    public enum eDisplayScale {
        E_DisplayScale_Fit,
        E_DisplayScale_Fill,
        E_DisplayScale_Stretch
    }

    public enum eEncodeContainer {
        E_EncodeContainer_MP4,
        E_EncodeContainer_AVI
    }

    //----------------------------------------------------------------------
    static {
        try {
            Log.i(TAG, "Trying to load ffmpeg.so ...");
            System.loadLibrary("ffmpeg");
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "Cannot load ffmpeg.so ...");
            ule.printStackTrace();
        }
    }

    public ffmpegWrapper() {
        mInstance = this;
    }

    public static ffmpegWrapper getInstance() {
        return mInstance;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.e(TAG, "onSurfaceCreated ... ");
        naInitDrawFrame();
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        naDrawFrame();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.e(TAG, "onSurfaceChanged ... ");
        naSetup(width, height);
    }

    /**
     * Set the player status change notification handler.
     * @param viewHandler the handler to receive status messages
     */
    public void SetViewHandler(Handler viewHandler) {
        mNowViewHandler = viewHandler;
    }

    /**
     * Notify the status change to the registered handler.
     * @param status one of the FFMPEG_STATUS_* constants
     */
    void StatusChange(int status) {
        if (mNowViewHandler != null) {
            Message msg = new Message();
            msg.what = status;
            mNowViewHandler.sendMessage(msg);
        }
    }

    // Native method declarations
    public static native int naInitAndPlay(String pFileName, String pOptions);
    public static native int[] naGetVideoRes();
    public static native int naSetup(int pWidth, int pHeight);
    public static native int naPlay();
    public static native int naStop();
    public static native int naPause();
    public static native int naResume();
    public static native int naSeek(long lPos);
    public static native int naSetStreaming(boolean bEnable);
    public static native int naSetEncodeByLocalTime(boolean bEnable);
    public static native int naSetDebugMessage(boolean bRepeat);
    public static native int naSetRepeat(boolean bRepeat);
    public static native int naSetForceToTranscode(boolean bEnable);
    public static native long naGetDuration();
    public static native long naGetPosition();
    public static native int naInitDrawFrame();
    public static native int naDrawFrame();
    public static native int naStatus();
    public static native long naGetRevSizeCnt();
    public static native long naGetFrameCnt();
    public static native int naGetStreamCodecID();
    public static native int naSaveSnapshot(String pFileName);
    public static native int naSaveVideo(String pFileName, int eContainer, String pOptions);
    public static native int naSetViewAngle(int i32ViewAngle);
    public static native int naGetDecodeFrameReady();
    public static native ffDecodeFrame naGetDecodeFrame();
    public static native int naSetCovertDecodeFrameFormat(int i32Format);
    public static native int naSetScaleMode(int i32ScaleMode);
    public static native int naSetDecodeOptions(String pOptions);
    public static native int naSetDebugMessageAndSave(String sDir);
    public static native int naExtractFrame(String path, String sThumbnailPath, int iNum);
    public static native int naSetBrightnessContrast(int i32Brightness, int i32Contrast);
    public static native int naSetHueSaturation(int i32Hue, int i32Saturation);
    public static native int naSetEV(int i32Ev);
    public static native int naSetVolume(int i32Volume);
    public static native int naSetDecAudio(boolean bDec);

    // -------------------------------------------------------------------------
    // Additional native methods derived from the updated ffmpegWrapper interface.
    // These declarations provide compatibility with the GeneralPlus SDK.  If
    // corresponding native implementations are missing, they will be no‑ops at
    // runtime.

    /**
     * Set the amount of time (in milliseconds) that the decoder will buffer
     * frames when the display queue is empty during streaming.  Value must be
     * greater than zero.
     */
    public static native int naSetBufferingTime(long bufferTime);

    /**
     * Save the current stream to a file.  The correct file extension will be
     * appended automatically based on the codec.
     *
     * @param pFileName path to the output file (without extension)
     * @return zero on success, non‑zero on failure
     */
    public static native int naSaveVideo(String pFileName);

    /**
     * Stop saving the current video stream.  Returns a code indicating the
     * container format used (e.g. MP4 or AVI).
     */
    public static native int naStopSaveVideo();

    /**
     * Extract a frame from a video file and save it to disk.
     *
     * @param videoPath  input video file
     * @param savePath   output image file
     * @param frameIdx   frame index (zero‑based)
     * @return status code: EXTRACTOR_OK on success, or other EXTRACTOR_* value on error
     */
    public static native int naExtractFrame(String videoPath, String savePath, long frameIdx);

    /**
     * Set the display zoom ratio.  A value greater than one zooms in; a value
     * less than one zooms out.  Values must be positive.
     */
    public static native int naSetZoomInRatio(float fRatio);

    /**
     * Set transcoding options for saving the video stream.  Deprecated on
     * recent SDKs which no longer perform software transcoding by default.
     */
    public static native int naSetTransCodeOptions(String pOption);
}