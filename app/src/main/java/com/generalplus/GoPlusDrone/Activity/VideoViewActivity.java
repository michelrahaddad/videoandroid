package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import com.generalplus.GoPlusDrone.R;

import java.util.ArrayList;

/**
 * Simple activity that plays a video using {@link VideoView} and a
 * {@link MediaController}.  The activity expects to be started with an
 * Intent containing a list of file paths under the key {@code "FilePath"}
 * and an integer position indicating which item in the list to play first.
 */
public class VideoViewActivity extends Activity {
    private VideoView mVideoView;
    private MediaController mMediaController;
    private ArrayList<String> mFilePaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoview);

        // Initialise UI elements
        mVideoView = findViewById(R.id.videoView);
        mMediaController = new MediaController(this);

        // Retrieve the selected video index and file list from the Intent
        Intent intent = getIntent();
        int position = intent.getExtras().getInt("position");
        mFilePaths = intent.getStringArrayListExtra("FilePath");

        // Configure the VideoView to play the selected file
        mVideoView.setVideoPath(mFilePaths.get(position));
        mVideoView.setMediaController(mMediaController);
        mVideoView.start();
        mVideoView.requestFocus();
    }
}