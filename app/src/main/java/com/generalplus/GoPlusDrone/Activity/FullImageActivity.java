package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.generalplus.GoPlusDrone.R;
import com.generalplus.GoPlusDrone.View.TouchImageView;

import java.util.ArrayList;

/**
 * Activity that displays a list of images in full screen using a {@link ViewPager}.
 * The activity expects an Intent extra containing a list of file paths under
 * the key {@code "FilePath"} and a position indicating the initial image.
 */
public class FullImageActivity extends Activity {
    private FullImageAdapter mAdapter;
    private ViewPager mViewPager;
    private ArrayList<String> mFilePaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_view);
        mViewPager = findViewById(R.id.pager);
        Intent intent = getIntent();
        int position = intent.getExtras().getInt("position");
        mFilePaths = intent.getStringArrayListExtra("FilePath");
        mAdapter = new FullImageAdapter(this, mFilePaths);
        mViewPager.setAdapter(mAdapter);
        // Display the selected image first
        mViewPager.setCurrentItem(position);
    }

    /**
     * Adapter that wraps a list of image file paths into pages for the ViewPager.
     */
    public static class FullImageAdapter extends PagerAdapter {
        private final Activity mActivity;
        private final ArrayList<String> mImagePaths;
        private LayoutInflater mInflater;

        public FullImageAdapter(Activity activity, ArrayList<String> imagePaths) {
            this.mActivity = activity;
            this.mImagePaths = imagePaths;
        }

        @Override
        public int getCount() {
            return mImagePaths.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TouchImageView imgDisplay;
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View viewLayout = mInflater.inflate(R.layout.layout_fullscreen_image, container, false);
            imgDisplay = viewLayout.findViewById(R.id.imgDisplay);
            Bitmap bitmap = BitmapFactory.decodeFile(mImagePaths.get(position));
            imgDisplay.setImageBitmap(bitmap);
            ((ViewPager) container).addView(viewLayout);
            return viewLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }
    }
}