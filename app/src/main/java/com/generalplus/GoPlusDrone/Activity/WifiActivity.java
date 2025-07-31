package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.generalplus.GoPlusDrone.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import generalplus.com.GPCamLib.CamWrapper;

/**
 * Activity allowing the user to scan nearby Wi‑Fi networks and configure the
 * GoPlus device with a selected SSID and password.  A list of available
 * networks is displayed using an {@link AlertDialog}, and when the user
 * confirms their choice the corresponding SSID/password is sent to the
 * camera via {@link CamWrapper#GPCamSendVendorCmd(byte[], int)}.
 */
public class WifiActivity extends Activity {
    private static final String TAG = "WifiActivity";

    private WifiManager wifiManager;
    private List<ScanResult> results;
    private final ArrayList<HashMap<String, String>> mArrayList = new ArrayList<>();
    private BroadcastReceiver mBroadcastReceiver;
    private EditText mEtSSID;
    private EditText mEtPW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        mEtSSID = findViewById(R.id.etSSID);
        mEtPW = findViewById(R.id.etPW);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Remind");
            dialog.setMessage("Your Wi-Fi is disabled, enable it?");
            dialog.setIcon(android.R.drawable.ic_dialog_info);
            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wifiManager.setWifiEnabled(true);
                }
            });
            dialog.show();
        }

        initBroadcastReceiver();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume ...");
        super.onResume();
        CamWrapper.getComWrapperInstance().SetViewHandler(mFromWrapperHandler, CamWrapper.GPVIEW_STREAMING);
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause ...");
        super.onPause();
        CamWrapper.getComWrapperInstance().SetViewHandler(null, CamWrapper.GPVIEW_FILELIST);
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy ...");
        super.onDestroy();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        finishConnection();
    }

    /**
     * Initialise a {@link BroadcastReceiver} that listens for Wi-Fi scan
     * results.  When new results are available the available SSIDs are
     * displayed in a list for the user to select.
     */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                results = wifiManager.getScanResults();
                Log.e(TAG, "Acquire wifi " + results.size());
                mArrayList.clear();

                // Remove duplicate SSIDs and ignore empty names
                for (int i = 0; i < results.size(); i++) {
                    ScanResult scanResult = results.get(i);
                    if (scanResult == null || scanResult.SSID == null || scanResult.SSID.isEmpty()) {
                        continue;
                    }
                    boolean bSame = false;
                    for (int j = 0; j < mArrayList.size(); j++) {
                        if (scanResult.SSID.equalsIgnoreCase(mArrayList.get(j).get("ssid"))) {
                            bSame = true;
                            break;
                        }
                    }
                    if (bSame) {
                        continue;
                    }
                    HashMap<String, String> item = new HashMap<>();
                    item.put("ssid", scanResult.SSID);
                    item.put("power", scanResult.level + " dBm");
                    mArrayList.add(item);
                }

                // Sort by signal strength ascending
                Collections.sort(mArrayList, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {
                        return lhs.get("power").compareTo(rhs.get("power"));
                    }
                });

                String[] names = new String[mArrayList.size()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = mArrayList.get(i).get("ssid");
                }
                final String[] fdinner = names;
                AlertDialog.Builder dialogList = new AlertDialog.Builder(WifiActivity.this);
                dialogList.setTitle("Please select the Wifi SSID");
                dialogList.setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mEtSSID.setText(fdinner[which]);
                    }
                });
                dialogList.show();
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void pressSet(View view) {
        setVendorSSIDPW();
    }

    /**
     * Send the selected SSID and password to the device via a vendor command.
     * This encodes the strings into a byte array according to the camera's
     * protocol and invokes {@link CamWrapper#GPCamSendVendorCmd(byte[], int)}.
     */
    private void setVendorSSIDPW() {
        String ssid = mEtSSID.getText().toString();
        String password = mEtPW.getText().toString();
        if (ssid == null || password == null) {
            Toast.makeText(WifiActivity.this, "SSID or password cannot be null", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ssid.length() > 32) {
            Toast.makeText(WifiActivity.this, "Sorry! The maximum SSID length is 32", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() > 32) {
            Toast.makeText(WifiActivity.this, "Sorry! The maximum password length is 32", Toast.LENGTH_SHORT).show();
            return;
        }

        int size = 12 + ssid.length() + password.length();
        byte[] byStringData = new byte[size];
        byStringData[0] = 0x47;
        byStringData[1] = 0x50;
        byStringData[2] = 0x56;
        byStringData[3] = 0x45;
        byStringData[4] = 0x4E;
        byStringData[5] = 0x44;
        byStringData[6] = 0x4F;
        byStringData[7] = 0x52;
        byStringData[8] = 0x1;
        byStringData[9] = 0x0;

        byStringData[10] = (byte) ssid.length();
        byte[] bySSID = ssid.getBytes();
        for (int i = 0; i < bySSID.length; i++) {
            byStringData[i + 11] = bySSID[i];
        }

        byStringData[11 + ssid.length()] = (byte) password.length();
        byte[] byPW = password.getBytes();
        for (int i = 0; i < byPW.length; i++) {
            byStringData[11 + ssid.length() + 1 + i] = byPW[i];
        }
        CamWrapper.getComWrapperInstance().GPCamSendVendorCmd(byStringData, size);
    }

    public void pressWifilist(View view) {
        Toast.makeText(WifiActivity.this, "Scanning...", Toast.LENGTH_SHORT).show();
        wifiManager.startScan();
    }

    /**
     * Handler that receives status and data callbacks from the camera.  Only
     * vendor commands relevant to the Wi‑Fi configuration are processed here.
     */
    private final Handler mFromWrapperHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CamWrapper.GPCALLBACKTYPE_CAMSTATUS:
                    Bundle data = msg.getData();
                    parseGPCamStatus(data);
                    break;
                case CamWrapper.GPCALLBACKTYPE_CAMDATA:
                    break;
            }
        }
    };

    private void parseGPCamStatus(Bundle statusBundle) {
        int cmdType = statusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_CMDTYPE);
        int mode = statusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_CMDMODE);
        int dataSize = statusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_DATASIZE);
        byte[] pbyData = statusBundle.getByteArray(CamWrapper.GPCALLBACKSTATUSTYPE_DATA);

        if (cmdType == CamWrapper.GP_SOCK_TYPE_ACK) {
            if (mode == CamWrapper.GPSOCK_MODE_Vendor) {
                Log.e(TAG, "GPSOCK_MODE_Vendor ... ");
                try {
                    String str = new String(pbyData, "UTF-8");
                    if (str != null && str.length() >= 10) {
                        if (str.contains("GPVENDOR")) {
                            if (pbyData[8] == 0x1 && pbyData[9] == 0x0) {
                                Toast.makeText(WifiActivity.this, "Success", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }
                    Toast.makeText(WifiActivity.this, "Fail", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (cmdType == CamWrapper.GP_SOCK_TYPE_NAK) {
            int errorCode = (pbyData[0] & 0xFF) + ((pbyData[1] & 0xFF) << 8);
            switch (errorCode) {
                case CamWrapper.Error_ServerIsBusy:
                    Log.e(TAG, "Error_ServerIsBusy ... ");
                    break;
                case CamWrapper.Error_InvalidCommand:
                    Log.e(TAG, "Error_InvalidCommand ... ");
                    break;
                case CamWrapper.Error_RequestTimeOut:
                    Log.e(TAG, "Error_RequestTimeOut ... ");
                    break;
                case CamWrapper.Error_ModeError:
                    Log.e(TAG, "Error_ModeError ... ");
                    break;
                case CamWrapper.Error_NoStorage:
                    Log.e(TAG, "Error_NoStorage ... ");
                    break;
                case CamWrapper.Error_WriteFail:
                    Log.e(TAG, "Error_WriteFail ... ");
                    break;
                case CamWrapper.Error_GetFileListFail:
                    Log.e(TAG, "Error_GetFileListFail ... ");
                    break;
                case CamWrapper.Error_GetThumbnailFail:
                    Log.e(TAG, "Error_GetThumbnailFail ... ");
                    break;
                case CamWrapper.Error_FullStorage:
                    Log.e(TAG, "Error_FullStorage ... ");
                    break;
                case CamWrapper.Error_SocketClosed:
                    Log.e(TAG, "Error_SocketClosed ... ");
                    finishConnection();
                    break;
                case CamWrapper.Error_LostConnection:
                    Log.e(TAG, "Error_LostConnection ...");
                    finishConnection();
                    break;
            }
        }
    }

    /**
     * Disconnect from the camera and finish the activity.
     */
    private void finishConnection() {
        CamWrapper.getComWrapperInstance().GPCamDisconnect();
        finish();
    }
}