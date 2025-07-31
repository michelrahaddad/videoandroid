package generalplus.com.GPCamLib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Broadcast receiver used by the GeneralPlus camera library to monitor
 * incoming phone calls.  When a call state change is detected, the
 * library can pause recording or take other appropriate action.  If
 * you choose to register this receiver in your application, be sure
 * to request the READ_PHONE_STATE permission at runtime on Android
 * 6.0 and above.
 */
public class IncomingCall extends BroadcastReceiver {

    private static final String TAG = "IncomingCall";
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        try {
            // Obtain the TelephonyManager and register our listener to
            // receive call state callbacks.  On modern Android versions
            // (API 31+), READ_PHONE_STATE permission must be granted at
            // runtime before listening for call state changes.
            TelephonyManager tmgr = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (tmgr != null) {
                tmgr.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "READ_PHONE_STATE permission not granted", se);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while registering phone state listener", e);
        }
    }

    /**
     * Simple PhoneStateListener implementation that logs call state
     * changes.  The incoming number is provided when available; this
     * listener does not initiate any actions on state changes.
     */
    private static class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(TAG, "Call state changed: " + state + ", incoming number: " + incomingNumber);
        }
    }
}