package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.generalplus.GoPlusDrone.R;
import com.generalplus.ffmpegLib.ffmpegWrapper;

import generalplus.com.GPCamLib.CamWrapper;

/**
 * Activity for playing back video or picture streaming from the camera or a local file.
 * Uses {@link ffmpegWrapper} to decode the stream into an OpenGL surface.
 */
public class FileViewController extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "FileViewController";

    private LinearLayout vlcContainer;
    private GLSurfaceView mSurfaceView;
    private FrameLayout vlcOverlay;
    private ImageButton imgbtnPlaypause;
    private Handler handlerOverlay;
    private Runnable runnableOverlay;
    private Context mContext;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;

    // media player parameters
    private static int mVideoWidth;
    private static int mVideoHeight;
    private static int mVideoVisibleWidth;
    private static int mVideoVisibleHeight;
    private static int mSarNum;
    private static int mSarDen;
    private String mUrlToStream;
    private int mFileFlag;
    private int mFileIndex;
    private static final int VideoSizeChanged = -1;
    private static final long timeToDisappear = 10 * 1000;
    private static boolean mRunVLC = false;
    private static boolean mIsPause = false;
    private long mLastClickTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_vlc_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        mContext = this;

        vlcContainer = findViewById(R.id.vlc_container);
        mSurfaceView = findViewById(R.id.vlc_surface);
        vlcOverlay = findViewById(R.id.vlc_overlay);
        imgbtnPlaypause = findViewById(R.id.imgbtn_playpause);

        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(ffmpegWrapper.getInstance());
        mSurfaceView.setKeepScreenOn(true);
    }

    private void setupControls() {
        // Hide system action bar if present
        vlcContainer.setVisibility(View.VISIBLE);
        // OVERLAY
        handlerOverlay = new Handler();
        runnableOverlay = new Runnable() {
            @Override
            public void run() {
                vlcOverlay.setVisibility(View.GONE);
            }
        };
        handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
        vlcOverlay.setVisibility(View.GONE);
        toggleFullscreen(true);
        vlcContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vlcOverlay.getVisibility() == View.VISIBLE) {
                    vlcOverlay.setVisibility(View.GONE);
                } else {
                    vlcOverlay.setVisibility(View.VISIBLE);
                }
                handlerOverlay.removeCallbacks(runnableOverlay);
                handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
            }
        });
        imgbtnPlaypause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopStreaming();
            }
        });
    }

    private void playLocalFile() {
        ffmpegWrapper.getInstance().naSetStreaming(false);
        ffmpegWrapper.getInstance().naInitAndPlay(mUrlToStream, "");
    }

    private void playVideoStreaming() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ffmpegWrapper.getInstance().naStop();
                CamWrapper.getComWrapperInstance().GPCamSendRestartStreaming();
                CamWrapper.getComWrapperInstance().GPCamSendStartPlayback(mFileIndex);
                ffmpegWrapper.getInstance().naInitAndPlay(CamWrapper.STREAMING_URL, "");
            }
        }).start();
    }

    private void playPictureStreaming() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ffmpegWrapper.getInstance().naStop();
                CamWrapper.getComWrapperInstance().GPCamClearCommandQueue();
                CamWrapper.getComWrapperInstance().GPCamSendRestartStreaming();
                ffmpegWrapper.getInstance().naInitAndPlay(CamWrapper.STREAMING_URL, "");
                for (int i = 0; i < 5; i++) {
                    CamWrapper.getComWrapperInstance().GPCamSendStartPlayback(mFileIndex);
                }
            }
        }).start();
    }

    private void stopStreaming() {
        if (mFileFlag == CamWrapper.GPFILEFLAG_AVISTREAMING && mUrlToStream.isEmpty()) {
            // Play streaming
            if (ffmpegWrapper.getInstance().naStatus() == ffmpegWrapper.ePlayerStatus.E_PlayerStatus_Playing.ordinal()) {
                CamWrapper.getComWrapperInstance().GPCamSendPausePlayback();
            } else {
                playVideoStreaming();
            }
        } else if (mFileFlag == CamWrapper.GPFILEFLAG_AVISTREAMING && !mUrlToStream.isEmpty()) {
            // Play local file
            if (!mIsPause) {
                ffmpegWrapper.getInstance().naPause();
            } else {
                ffmpegWrapper.getInstance().naResume();
            }
            mIsPause = !mIsPause;
        }
    }

    private void toggleFullscreen(boolean fullscreen) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            // Use immersive mode if required
        } else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG, "onConfigurationChanged ...");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle b = getIntent().getExtras();
        mUrlToStream = b.getString(CamWrapper.GPFILECALLBACKTYPE_FILEURL, null);
        mFileFlag = b.getInt(CamWrapper.GPFILECALLBACKTYPE_FILEFLAG, 0);
        mFileIndex = b.getInt(CamWrapper.GPFILECALLBACKTYPE_FILEINDEX, 0);
        setupControls();
        initStreaming();
        imgbtnPlaypause.setVisibility(View.VISIBLE);
        if (mUrlToStream.isEmpty()) {
            // Streaming
            ffmpegWrapper.getInstance().naSetStreaming(true);
            if (mFileFlag == CamWrapper.GPFILEFLAG_JPGSTREAMING) {
                playPictureStreaming();
                imgbtnPlaypause.setVisibility(View.INVISIBLE);
            } else {
                playVideoStreaming();
            }
        } else {
            // Local file
            playLocalFile();
        }
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause ...");
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void finishPlayback() {
        Log.e(TAG, "Finish ...");
        ffmpegWrapper.getInstance().naStop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceholder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
    }

    public void initStreaming() {
        if (ffmpegWrapper.getInstance().naStatus() == ffmpegWrapper.ePlayerStatus.E_PlayerStatus_Playing.ordinal()) {
            return;
        }
    }
}