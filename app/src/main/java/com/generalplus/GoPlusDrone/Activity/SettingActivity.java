package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.generalplus.GoPlusDrone.Fragment.SettingsFragment;

/**
 * Activity that hosts the {@link SettingsFragment}.  This uses the legacy
 * {@link android.app.Fragment} API since the settings fragment extends
 * {@link android.preference.PreferenceFragment}.  If you wish to migrate to
 * AndroidX preference support libraries, consider using
 * {@link androidx.preference.PreferenceFragmentCompat} instead.
 */
public class SettingActivity extends Activity {
    private static final String TAG = "SettingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: ");
    }
}