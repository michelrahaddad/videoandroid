package com.generalplus.GoPlusDrone.Activity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.generalplus.GoPlusDrone.Fragment.ControlCardFragment;
import com.generalplus.GoPlusDrone.Fragment.ControlFragment;
import com.generalplus.GoPlusDrone.Fragment.TabFragment;
import com.generalplus.GoPlusDrone.R;

import java.util.Locale;

/**
 * Main activity that hosts either the Control or Tab fragments depending on
 * whether the device is connected to the GoPlus card or not.  This class
 * migrates the original support library imports to AndroidX.
 */
public class MainActivity extends AppCompatActivity {
    private ControlFragment mControlFragment = null;
    private TabFragment mTabFragment = null;
    private boolean mIsCard = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.content_main);
            
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                mIsCard = bundle.getBoolean("IsCard");
            }
            
            // Verificar se o layout foi carregado corretamente
            if (findViewById(R.id.content_frame) != null || findViewById(R.id.frame_container) != null) {
                // Usar Handler para evitar travamento na inicialização
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        displayView(R.id.nav_camera);
                    }
                }, 200); // Delay de 200ms para garantir que tudo esteja carregado
            } else {
                // Fallback se o layout não carregar
                finish();
            }
            
        } catch (Exception e) {
            // Log do erro e fechamento gracioso
            e.printStackTrace();
            finish();
        }
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

    /**
     * Display the appropriate fragment based on the navigation item selected.
     */
    public void displayView(int viewId) {
        Fragment fragment = null;
        
        try {
            if (viewId == R.id.nav_camera) {
                if (mControlFragment == null) {
                    if (mIsCard) {
                        mControlFragment = new ControlCardFragment();
                        mControlFragment.setIsCard(true);
                    } else {
                        mControlFragment = new ControlFragment();
                        mControlFragment.setIsCard(false);
                    }
                }
                if (mControlFragment != null) {
                    mControlFragment.setControlLayout(false);
                    fragment = mControlFragment;
                }
            } else if (viewId == R.id.nav_gallery) {
                if (mControlFragment == null) {
                    mControlFragment = new ControlFragment();
                    mControlFragment.setIsCard(false);
                }
                if (mControlFragment != null) {
                    mControlFragment.setControlLayout(false);
                    fragment = mControlFragment;
                }
            } else if (viewId == R.id.nav_slideshow) {
                if (mTabFragment == null) {
                    mTabFragment = new TabFragment();
                }
                fragment = mTabFragment;
            }
            
            if (fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                
                // Tentar diferentes IDs de container
                if (findViewById(R.id.content_frame) != null) {
                    ft.replace(R.id.content_frame, fragment);
                } else if (findViewById(R.id.frame_container) != null) {
                    ft.replace(R.id.frame_container, fragment);
                } else {
                    // Fallback para qualquer container disponível
                    ft.replace(android.R.id.content, fragment);
                }
                
                ft.commitAllowingStateLoss(); // Usar commitAllowingStateLoss para evitar crashes
            }
            
            // update toolbar title if available
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("");
            }
            
        } catch (Exception e) {
            // Log do erro mas não fechar o app
            e.printStackTrace();
            
            // Fallback - tentar carregar fragment básico
            try {
                Fragment fallbackFragment = new ControlFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(android.R.id.content, fallbackFragment);
                ft.commitAllowingStateLoss();
            } catch (Exception fallbackError) {
                fallbackError.printStackTrace();
            }
        }
    }
}