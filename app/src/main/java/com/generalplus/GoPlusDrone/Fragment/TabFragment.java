package com.generalplus.GoPlusDrone.Fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.generalplus.GoPlusDrone.R;
import com.generalplus.GoPlusDrone.View.SlidingTabLayout;
import com.generalplus.GoPlusDrone.View.TabFragmentPagerAdapter;

import java.util.LinkedList;

/**
 * A fragment that hosts a ViewPager and SlidingTabLayout. This class uses
 * AndroidX components throughout. The ViewPager displays two fragments: one for
 * photos and one for videos.
 */
public class TabFragment extends Fragment {
    private SlidingTabLayout tabs;
    private ViewPager pager;
    private FragmentPagerAdapter adapter;
    private int m_iSelect = 0;

    /**
     * Create a new instance of TabFragment.
     */
    public static Fragment newInstance() {
        return new TabFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final LinkedList<BaseFragment> fragments = getFragments();
        adapter = new TabFragmentPagerAdapter(getChildFragmentManager(), fragments);
        pager = view.findViewById(R.id.viewpager);
        pager.setAdapter(adapter);
        tabs = view.findViewById(R.id.sliding_tabs);
        tabs.setViewPager(pager);

        AppCompatActivity mAppCompatActivity = (AppCompatActivity) requireActivity();
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        mAppCompatActivity.setSupportActionBar(toolbar);
        ActionBar actionBar = mAppCompatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                requireActivity().finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private LinkedList<BaseFragment> getFragments() {
        LinkedList<BaseFragment> fragments = new LinkedList<>();
        fragments.add(PhotoListFragment.newInstance("Photo"));
        fragments.add(VideoListFragment.newInstance("Video"));
        return fragments;
    }
}