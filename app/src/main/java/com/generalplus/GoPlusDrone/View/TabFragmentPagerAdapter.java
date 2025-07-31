package com.generalplus.GoPlusDrone.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.generalplus.GoPlusDrone.Fragment.BaseFragment;

import java.util.LinkedList;

/**
 * Pager adapter that holds a list of {@link BaseFragment} instances and
 * supplies them to a {@link androidx.viewpager.widget.ViewPager}.  Each
 * fragment provides its own title and icon resource via {@link BaseFragment#getTitle()}
 * and {@link BaseFragment#getIconResId()}.  The adapter will return the number
 * of fragments contained and provide titles for use by tab layouts.
 */
public class TabFragmentPagerAdapter extends FragmentPagerAdapter {

    private final LinkedList<BaseFragment> fragments;

    public TabFragmentPagerAdapter(@NonNull FragmentManager fm, LinkedList<BaseFragment> fragments) {
        // Use RESUME_ONLY_CURRENT_FRAGMENT to ensure only the current fragment is in the RESUMED state
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        if (fragments == null) {
            this.fragments = new LinkedList<>();
        } else {
            this.fragments = fragments;
        }
    }

    @Override
    @NonNull
    public BaseFragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    @NonNull
    public CharSequence getPageTitle(int position) {
        return fragments.get(position).getTitle();
    }

    /**
     * Return the icon resource ID for the fragment at the given position.
     */
    public int getIconResId(int position) {
        return fragments.get(position).getIconResId();
    }
}