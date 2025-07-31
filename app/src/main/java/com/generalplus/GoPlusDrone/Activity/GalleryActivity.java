package com.generalplus.GoPlusDrone.Activity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTabHost;

import com.generalplus.GoPlusDrone.Fragment.PhotoListFragment;
import com.generalplus.GoPlusDrone.Fragment.VideoListFragment;
import com.generalplus.GoPlusDrone.R;

import java.util.Locale;

/**
 * Gallery activity that hosts tabs for photos and videos using a {@link FragmentTabHost}.
 * Migrated from support library to AndroidX.
 */
public class GalleryActivity extends AppCompatActivity {
    public static boolean mEdit = false;
    public static boolean m_bEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        mEdit = false;
        final FragmentTabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
        // Add photo tab
        tabHost.addTab(tabHost.newTabSpec(getResources().getString(R.string.tab_photo)).setIndicator("Photo"),
                PhotoListFragment.class, null);
        // Add video tab
        tabHost.addTab(tabHost.newTabSpec(getResources().getString(R.string.tab_video)).setIndicator("Video"),
                VideoListFragment.class, null);
        tabHost.getTabWidget().getChildTabViewAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEdit && 0 != tabHost.getCurrentTab()) {
                    Toast.makeText(GalleryActivity.this, "Please press DONE button.", Toast.LENGTH_SHORT).show();
                    return;
                }
                tabHost.setCurrentTab(0);
            }
        });
        tabHost.getTabWidget().getChildTabViewAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEdit && 1 != tabHost.getCurrentTab()) {
                    Toast.makeText(GalleryActivity.this, "Please press DONE button.", Toast.LENGTH_SHORT).show();
                    return;
                }
                tabHost.setCurrentTab(1);
            }
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Back");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLanguage();
    }

    private void updateLanguage() {
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        config.locale = Locale.ENGLISH;
        resources.updateConfiguration(config, dm);
    }
}