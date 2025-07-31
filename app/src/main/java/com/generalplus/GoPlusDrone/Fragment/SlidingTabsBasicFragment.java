package com.generalplus.GoPlusDrone.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.generalplus.GoPlusDrone.R;
import com.generalplus.GoPlusDrone.View.SlidingTabLayout;

/**
 * A basic sample which shows how to use {@link SlidingTabLayout} to display a
 * custom {@link ViewPager} title strip which gives continuous feedback to the
 * user when scrolling.
 */
public class SlidingTabsBasicFragment extends Fragment {
    static final String LOG_TAG = "SlidingBasicFragment";
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set up the ViewPager with a simple adapter
        mViewPager = view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new SamplePagerAdapter());
        // Now give the SlidingTabLayout the ViewPager
        mSlidingTabLayout = view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    /**
     * A {@link PagerAdapter} used to display pages in this sample. The
     * individual pages are simple and just display two lines of text. The
     * important section of this class is the {@link #getPageTitle(int)}
     * method which controls what is displayed in the {@link SlidingTabLayout}.
     */
    class SamplePagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 2;
        }
        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return o == view;
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return "Item " + (position + 1);
        }
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.pager_item, container, false);
            container.addView(view);
            TextView title = view.findViewById(R.id.item_title);
            title.setText(String.valueOf(position + 1));
            Log.i(LOG_TAG, "instantiateItem() [position: " + position + "]");
            return view;
        }
        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
            Log.i(LOG_TAG, "destroyItem() [position: " + position + "]");
        }
    }
}