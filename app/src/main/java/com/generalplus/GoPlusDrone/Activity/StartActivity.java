package com.generalplus.GoPlusDrone.Activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.generalplus.GoPlusDrone.Fragment.ControlFragment;
import com.generalplus.GoPlusDrone.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * Entry point activity that presents the user with options to start the app in
 * different modes (low‑delay, normal, RTP, settings, etc.) and handles
 * permission requests.  This class extends {@link StartCardActivity} to
 * inherit the connection logic.
 */
public class StartActivity extends StartCardActivity {
    private SharedPreferences mSharedPreferences = null;
    private static final String SharedPreferences_KEY = "SharedPreferences_KEY";
    private static final String Language_KEY = "Language_KEY";
    private int mLocaleIndex = 0;
    private final Locale[] mLocales = {Locale.ENGLISH, Locale.TRADITIONAL_CHINESE, Locale.CHINA, Locale.getDefault()};
    private Button mBnStart = null;
    private boolean mIsCard = false;
    private Thread mGetIpThread = null;
    private DatagramSocket service = null;
    private Button mBnConnect = null;
    public static int gStationIP = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mBnStart = findViewById(R.id.bnStart);
        mBnConnect = findViewById(R.id.bnConnect);
        mSharedPreferences = getSharedPreferences(SharedPreferences_KEY, 0);
        TextView tvName = findViewById(R.id.tvName);
        tvName.setText(getResources().getString(R.string.main_name));

        // Hide the status bar
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }

        // Request Wi‑Fi network capabilities on Lollipop and above
        if (Build.VERSION.SDK_INT >= 21) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            final ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest request = builder.build();
            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.i("StartActivity", "Wi‑Fi network is available");
                    if (Build.VERSION.SDK_INT >= 23) {
                        connectivityManager.bindProcessToNetwork(network);
                    } else {
                        ConnectivityManager.setProcessDefaultNetwork(network);
                    }
                }
            };
            connectivityManager.registerNetworkCallback(request, callback);
        }

        // Request permissions if necessary
        if (shouldAskPermission()) {
            int writePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            writePermission += ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                int permsRequestCode = 200;
                ActivityCompat.requestPermissions(this, perms, permsRequestCode);
            }
        }

        try {
            if (service == null) {
                service = new DatagramSocket(null);
                service.setReuseAddress(true);
                service.bind(new InetSocketAddress(8080));
            }
            mGetIpThread = new Thread(new GetIpRunnable());
            mGetIpThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Optionally update language here
    }

    private void updateLanguage() {
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        config.locale = mLocales[mLocaleIndex];
        resources.updateConfiguration(config, dm);
        mBnStart.setText(getResources().getString(R.string.start_button));
    }

    private void showLanDialog() {
        String[] language = {getString(R.string.item_Language1), getString(R.string.item_Language2), getString(R.string.item_Language3), getString(R.string.item_Language0)};
        new AlertDialog.Builder(StartActivity.this).setItems(language, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mLocaleIndex == which) {
                    return;
                }
                mLocaleIndex = which;
                updateLanguage();
                mSharedPreferences.edit().putInt(Language_KEY, mLocaleIndex).commit();
            }
        }).show();
    }

    public void pressLanguage(View view) {
        showLanDialog();
    }

    private void startApp() {
        if (mIsCard) {
            if (mConnectGPWifiDeviceThread == null) {
                if (mDialog == null) {
                    mDialog = new ProgressDialog(this);
                    mDialog.setCanceledOnTouchOutside(false);
                    mDialog.setMessage("Please wait ...");
                }
                mDialog.show();
                mConnectGPWifiDeviceThread = new Thread(new ConnectGPWifiDeviceRunnable());
                mConnectGPWifiDeviceThread.start();
            }
        } else {
            Intent intent = new Intent();
            Bundle b = new Bundle();
            b.putBoolean("IsCard", false);
            intent.putExtras(b);
            intent.setClass(StartActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    public void pressSetting(View view) {
        Intent intent = new Intent(StartActivity.this, SettingActivity.class);
        startActivity(intent);
    }

    public void pressWifi(View view) {
        if (mConnectGPWifiDeviceThread == null) {
            if (mDialog == null) {
                mDialog = new ProgressDialog(this);
                mDialog.setCanceledOnTouchOutside(false);
                mDialog.setMessage("Please wait ...");
            }
            mDialog.show();
            mConnectGPWifiDeviceThread = new Thread(new ConnectGPWifiDeviceRunnable());
            mConnectGPWifiDeviceThread.start();
        }
    }

    public void pressLowDelayStart(View view) {
        ControlFragment.g_iConnectType = 1;
        startApp();
    }

    public void pressStart(View view) {
        ControlFragment.g_iConnectType = 0;
        startApp();
    }

    public void pressRTPStart(View view) {
        ControlFragment.g_iConnectType = 2;
        startApp();
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                boolean writeAccepted = true;
                for (int grantResult : grantResults) {
                    if (PackageManager.PERMISSION_GRANTED != grantResult) {
                        writeAccepted = false;
                        break;
                    }
                }
                if (!writeAccepted) {
                    if (shouldAskPermission()) {
                        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        ActivityCompat.requestPermissions(this, perms, permsRequestCode);
                    }
                }
                break;
        }
    }

    private boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    class GetIpRunnable implements Runnable {
        @Override
        public void run() {
            byte[] buf = new byte[1024];
            DatagramPacket dPacket = new DatagramPacket(buf, buf.length);
            try {
                Log.d("UdpService", "UdpService is running");
                while (true) {
                    service.receive(dPacket);
                    byte[] data = dPacket.getData();
                    String strData = new String(data, "US-ASCII");
                    if (strData.contains("GPLUS")) {
                        gStationIP = data[8] & 0xFF;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mBnConnect.setText("Connect (station mode)");
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}