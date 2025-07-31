/*
 * A horizontally scrolling tab layout that can be bound to a {@link androidx.viewpager.widget.ViewPager}
 * to display a strip of tabs.  This layout handles drawing the selected indicator
 * and manages scrolling as the pager is swiped.  Tabs can be customized via
 * {@link #setCustomTabView(int, int, int)} or the default tab view will be used.
 *
 * This implementation is adapted from the Android Open Source Project's
 * SlidingTabLayout but updated to use AndroidX classes such as
 * {@link androidx.viewpager.widget.ViewPager} and
 * {@link androidx.fragment.app.FragmentPagerAdapter}.
 */
package com.generalplus.GoPlusDrone.View;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

/**
 * A custom horizontal scroll view that provides a scrolling tab bar. To use, add
 * this view to your layout and then call {@link #setViewPager(ViewPager)}
 * supplying the associated pager.  Tabs will be created with titles from
 * {@link TabFragmentPagerAdapter#getPageTitle(int)} and will stay in sync with
 * the pager's scroll state.
 */
public class SlidingTabLayout extends HorizontalScrollView {

    /**
     * Allows complete control over the colors drawn in the tab layout.  Implement
     * this interface and call {@link #setCustomTabColorizer(TabColorizer)} to
     * customize the indicator and divider colors for each tab.
     */
    public interface TabColorizer {
        /**
         * @return the color of the indicator used when {@code position} is selected.
         */
        int getIndicatorColor(int position);

        /**
         * @return the color of the divider drawn to the right of {@code position}.
         */
        int getDividerColor(int position);
    }

    private static final int TITLE_OFFSET_DIPS = 24;
    private static final int TAB_VIEW_PADDING_DIPS = 16;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;

    private int mTitleOffset;

    private int mTabViewLayoutId;
    private int mTabViewTextViewId;
    private int mTabViewIconId;

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;

    private final SlidingTabStrip mTabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Disable the scroll bar as the tab strip implements its own indicator
        setHorizontalScrollBarEnabled(false);
        // Ensure the tab strip fills the width of this view
        setFillViewport(true);
        mTitleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);
        mTabStrip = new SlidingTabStrip(context);
        addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * Set the custom {@link TabColorizer} to be used to color the indicator and
     * dividers. If you only require simple customization then you can use
     * {@link #setSelectedIndicatorColors(int...)} and {@link #setDividerColors(int...)}.
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        mTabStrip.setCustomTabColorizer(tabColorizer);
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are
     * treated as a circular array. Providing one color will mean that all tabs are
     * indicated with the same color.
     */
    public void setSelectedIndicatorColors(int... colors) {
        mTabStrip.setSelectedIndicatorColors(colors);
    }

    /**
     * Sets the colors to be used for tab dividers. These colors are treated as a
     * circular array. Providing one color will mean that all tabs use the same
     * divider color.
     */
    public void setDividerColors(int... colors) {
        mTabStrip.setDividerColors(colors);
    }

    /**
     * Set the {@link ViewPager.OnPageChangeListener}. When using
     * {@link SlidingTabLayout} you are required to set any
     * {@link ViewPager.OnPageChangeListener} through this method. This is so that
     * the layout can update its scroll position correctly.
     *
     * @see ViewPager#setOnPageChangeListener(ViewPager.OnPageChangeListener)
     */
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mViewPagerPageChangeListener = listener;
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId layout id to be inflated
     * @param textViewId  id of the {@link TextView} in the inflated view
     */
    public void setCustomTabView(int layoutResId, int textViewId, int iconViewId) {
        mTabViewLayoutId = layoutResId;
        mTabViewTextViewId = textViewId;
        mTabViewIconId = iconViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager
     * content (number of tabs and tab titles) does not change after this call has been
     * made.
     */
    public void setViewPager(@NonNull ViewPager viewPager) {
        mTabStrip.removeAllViews();
        mViewPager = viewPager;
        if (viewPager != null) {
            viewPager.addOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not
     * set via {@link #setCustomTabView(int, int, int)}.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Use the theme's selectableItemBackground for pressed state feedback
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                    outValue, true);
            textView.setBackgroundResource(outValue.resourceId);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Use all-caps styling on API 14+ to match the Action Bar tab style
            textView.setAllCaps(true);
        }
        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);
        return textView;
    }

    private void populateTabStrip() {
        final TabFragmentPagerAdapter adapter = (TabFragmentPagerAdapter) mViewPager.getAdapter();
        if (adapter == null) {
            return;
        }
        final OnClickListener tabClickListener = new TabClickListener();
        final int tabCount = adapter.getCount();
        // Divide the screen width equally among the tabs
        final int tabWidth = getResources().getDisplayMetrics().widthPixels / Math.max(tabCount, 1);
        for (int i = 0; i < tabCount; i++) {
            View tabView = null;
            TextView tabTitleView = null;
            if (mTabViewLayoutId != 0) {
                // Inflate custom tab view
                tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip, false);
                tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
            }
            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
            }
            if (tabTitleView == null && tabView instanceof TextView) {
                tabTitleView = (TextView) tabView;
            }
            // Set the title on the tab
            tabTitleView.setText(adapter.getPageTitle(i));
            tabView.setOnClickListener(tabClickListener);
            LayoutParams params = new LayoutParams(tabWidth, LayoutParams.WRAP_CONTENT);
            tabView.setLayoutParams(params);
            mTabStrip.addView(tabView);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mViewPager != null) {
            scrollToTab(mViewPager.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = mTabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }
        View selectedChild = mTabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            int targetScrollX = selectedChild.getLeft() + positionOffset;
            if (tabIndex > 0 || positionOffset > 0) {
                // Apply offset so that selected tab isn't flush with the scrollview's left edge
                targetScrollX -= mTitleOffset;
            }
            scrollTo(targetScrollX, 0);
        }
    }

    /**
     * Internal {@link ViewPager.OnPageChangeListener} that keeps the tab indicator and scroll
     * position in sync with the pager.
     */
    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int mScrollState;
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int tabStripChildCount = mTabStrip.getChildCount();
            if (tabStripChildCount == 0 || position < 0 || position >= tabStripChildCount) {
                return;
            }
            mTabStrip.onViewPagerPageChanged(position, positionOffset);
            View selectedTitle = mTabStrip.getChildAt(position);
            int extraOffset = selectedTitle != null
                    ? (int) (positionOffset * selectedTitle.getWidth())
                    : 0;
            scrollToTab(position, extraOffset);
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageScrolled(position, positionOffset,
                        positionOffsetPixels);
            }
        }
        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }
        @Override
        public void onPageSelected(int position) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                mTabStrip.onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageSelected(position);
            }
        }
    }

    /**
     * Simple click listener for tab views that forwards the click to the pager.
     */
    private class TabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < mTabStrip.getChildCount(); i++) {
                if (v == mTabStrip.getChildAt(i)) {
                    mViewPager.setCurrentItem(i);
                    return;
                }
            }
        }
    }
}