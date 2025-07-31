package com.generalplus.GoPlusDrone.Fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.generalplus.GoPlusDrone.Activity.GalleryActivity;
import com.generalplus.GoPlusDrone.Activity.StartActivity;
import com.generalplus.GoPlusDrone.R;
import com.generalplus.GoPlusDrone.View.JoystickView;
import com.generalplus.ffmpegLib.ffHWEncoder;
import com.generalplus.ffmpegLib.ffmpegWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import generalplus.com.GPCamLib.CamWrapper;

import static android.content.Context.WIFI_SERVICE;

/**
 * Fragment that controls the drone and displays a live preview. This class is
 * largely a direct port of the original implementation. Only the import of
 * {@link Fragment} has been updated to the AndroidX package.
 */
public class ControlFragment extends Fragment {

    private static final String TAG = "ControlFragment";
    private CamWrapper m_CamWrapper;
    private ProgressDialog m_Dialog = null;
    protected Context mContext = null;
    private static int _CurrentMode = CamWrapper.GPDEVICEMODE_Record;

    protected ImageButton m_bnRecord = null, m_bnCapture = null;
    private ImageButton m_bnHide = null;
    private RelativeLayout m_rlControlLayout = null;
    protected TextView m_tvRecordTime = null;
    private ImageView m_ivBroken = null;
    private GLSurfaceView mSurfaceView;
    private boolean mSaveVideo = false;
    private boolean mIsStart = false;
    private String strSaveDirectory;
    protected Handler handler = new Handler();
    protected Long startTime;
    private int m_iControlLayout = View.VISIBLE;
    private boolean m_bShowControlLayout = false;
    private InetAddress targetAddress = null;
    private int targetPort = 20000;
    private DatagramSocket service = null;
    private Thread m_sendingThread = null;
    private boolean RUN_THREAD = false;
    private byte[] m_byteSend = {0x4A, 0x48, 0x43, 0x4D, 0x44, (byte) 0xD0, 0x01};
    protected boolean m_bIsCard = true;
    private boolean m_bPause = false;
    private TextView m_tvSpeed = null;
    private Timer timer = new Timer(true);

    //#define CONNECT_TYPE_NOT_LOW_DELAY 0
    //#define CONNECT_TYPE_LOW_DELAY 1
    //#define CONNECT_TYPE_RTP 2
    public static int g_iConnectType = 1;
    private String m_strConnectType = "";
    ffHWEncoder mFrameEncoder = new ffHWEncoder();

    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e(TAG, "uncaught_exception_handler: uncaught exception in thread " + thread.getName(), ex);
            // hack to rethrow unchecked exceptions
            if (ex instanceof RuntimeException) throw (RuntimeException) ex;
            if (ex instanceof Error) throw (Error) ex;

            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            ex.printStackTrace(printWriter);
            String stacktrace = result.toString();
            printWriter.close();
            Log.e(TAG, "uncaught_exception handler: unable to rethrow checked exception\ntrace: " + stacktrace);
        }
    }

    public void setIsCard(boolean bIsCard) {
        m_bIsCard = bIsCard;
    }

    public void setControlLayout(boolean bShowControlLayout) {
        m_bShowControlLayout = bShowControlLayout;
        if (m_rlControlLayout != null) {
            if (m_bShowControlLayout) {
                m_rlControlLayout.setVisibility(m_iControlLayout);
                m_bnHide.setVisibility(View.VISIBLE);
            } else {
                m_rlControlLayout.setVisibility(View.GONE);
                m_bnHide.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
        m_bPause = false;
        timer = new Timer(true);
        timer.schedule(new MyTimerTask(), 0, 1000);
    }

    private void checkStopRecode() {
        if (mSaveVideo) {
            stopRecode();
            mSaveVideo = !mSaveVideo;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        m_bPause = true;
        checkStopRecode();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        StreamingOff();
        stopSendingThread();
    }

    private void stopSendingThread() {
        RUN_THREAD = false;
        if (m_sendingThread != null) {
            m_sendingThread.interrupt();
            m_sendingThread = null;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] stopByteSend = {0x4A, 0x48, 0x43, 0x4D, 0x44, (byte) 0xD0, 0x02};
                    try {
                        DatagramPacket packet = new DatagramPacket(stopByteSend, stopByteSend.length, targetAddress, targetPort);
                        service.send(packet);
                        Log.e("UdpClientStop", bytesToHex(stopByteSend));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void setButtonClickListener(View view) {
        view.findViewById(R.id.bnUp).setOnTouchListener(new MyOnTouchListener());
        view.findViewById(R.id.bnDown).setOnTouchListener(new MyOnTouchListener());
        view.findViewById(R.id.bnLeft).setOnTouchListener(new MyOnTouchListener());
        view.findViewById(R.id.bnRight).setOnTouchListener(new MyOnTouchListener());
        view.findViewById(R.id.bnTakeOff).setOnTouchListener(new MyOnTouchListener());
        view.findViewById(R.id.bnGravity).setOnTouchListener(new MyOnTouchListener());
        view.findViewById(R.id.bnLandOn).setOnTouchListener(new MyOnTouchListener());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        if (m_CamWrapper == null) {
            m_CamWrapper = new CamWrapper();
        }
        mContext = requireActivity();
        setButtonClickListener(view);
        m_tvRecordTime = view.findViewById(R.id.tvRecordTime);
        mSurfaceView = view.findViewById(R.id.surfaceView);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(ffmpegWrapper.getInstance());
        mSurfaceView.setKeepScreenOn(true);

        strSaveDirectory = Environment.getExternalStorageDirectory().getPath() + "/GoPlus_Drone";
        File SaveFileDirectory = new File(strSaveDirectory);
        SaveFileDirectory.mkdir();

        SaveFileDirectory = new File(strSaveDirectory + "/Photo/");
        SaveFileDirectory.mkdir();
        SaveFileDirectory = new File(strSaveDirectory + "/Video/");
        SaveFileDirectory.mkdir();
        SaveFileDirectory = new File(strSaveDirectory + "/Video/thumbnails/");
        SaveFileDirectory.mkdir();

        ffmpegWrapper.getInstance().naSetDebugMessage(true);
        ffmpegWrapper.getInstance().SetViewHandler(m_StatusHandler);
        if (g_iConnectType == 1) {
            ffmpegWrapper.getInstance().naSetDecodeOptions("flags=low_delay");
        } else if (g_iConnectType == 2) {
            ffmpegWrapper.getInstance().naSetDecodeOptions("flags=low_delay");
            m_strConnectType = " (RTP)";
            try {
                service = new DatagramSocket();
                service.setReuseAddress(true);
                service.bind(new InetSocketAddress(targetPort));
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else {
            ffmpegWrapper.getInstance().naSetDecodeOptions("");
            m_strConnectType = " (Testing for latency)";
        }

        ffmpegWrapper.getInstance().naSetScaleMode(0);

        TextView tvName = view.findViewById(R.id.tvName);
        tvName.setText(getResources().getString(R.string.main_name) + m_strConnectType);
        ImageButton bnGallery = view.findViewById(R.id.bnGallery);
        bnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressGallery(v);
            }
        });
        m_bnCapture = view.findViewById(R.id.bnCapture);
        m_bnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressCapture(v);
            }
        });
        m_bnRecord = view.findViewById(R.id.bnRecord);
        m_bnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressRecord(v);
            }
        });
        m_bnHide = view.findViewById(R.id.bnHide);
        m_bnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressHide(v);
            }
        });
        m_ivBroken = view.findViewById(R.id.ivBroken);
        m_ivBroken.setVisibility(View.INVISIBLE);
        m_tvSpeed = view.findViewById(R.id.tvSpeed);
        m_rlControlLayout = view.findViewById(R.id.rlControlLayout);
        if (m_rlControlLayout != null) {
            if (m_bShowControlLayout) {
                m_rlControlLayout.setVisibility(m_iControlLayout);
                m_bnHide.setVisibility(View.VISIBLE);
            } else {
                m_rlControlLayout.setVisibility(View.INVISIBLE);
                m_bnHide.setVisibility(View.GONE);
            }
        }
        JoystickView joystickViewRight = view.findViewById(R.id.joystickViewRight);
        joystickViewRight.setLeft(false);
        joystickViewRight.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // Unused in this sample
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
        JoystickView joystickViewLeft = view.findViewById(R.id.joystickViewLeft);
        joystickViewLeft.setLeft(true);
        joystickViewLeft.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                Log.e(TAG, "angle = " + angle + " power = " + power + " direction = " + direction);
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
        if (m_bIsCard) {
            // Placeholder for future device connection thread
        }
        setPlayStatus(false);
        return view;
    }

    private void writeToFile(String data, String strPath) {
        try {
            File myFile = new File(strPath);
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.close();
        } catch (Exception e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    int m_iRetry = 0;
    public class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                WifiManager wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    final Integer linkSpeed = wifiInfo.getLinkSpeed();
                    boolean bShow = false;
                    if (!mIsStart) {
                        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                        if (ipAddress == 0) {
                            return;
                        }
                        // Convert little-endian to big-endian if needed
                        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                            ipAddress = Integer.reverseBytes(ipAddress);
                        }
                        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
                        ffmpegWrapper.getInstance().naSetStreaming(true);
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                        String strMax_Delay = preferences.getString("Max_Delay", "");
                        strMax_Delay = "max_delay=" + strMax_Delay + "000";
                        if (g_iConnectType == 2) {
                            try {
                                String strCommand = (ipByteArray[0] & 0xFF) + "." + (ipByteArray[1] & 0xFF) + "."
                                        + (ipByteArray[2] & 0xFF) + ".1";
                                targetAddress = InetAddress.getByName(strCommand);
                                if (m_sendingThread == null) {
                                    RUN_THREAD = true;
                                    m_sendingThread = new Thread(new UdpSendingRunnable());
                                    m_sendingThread.start();
                                }
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            String strWrite = "c=IN IP4 " + (ipByteArray[0] & 0xFF) + "." + (ipByteArray[1] & 0xFF) + "."
                                    + (ipByteArray[2] & 0xFF) + "/127\n" +
                                    "a=recvonly\n" +
                                    "m=video 10900 RTP/AVP 26";
                            String strPath = Environment.getExternalStorageDirectory().getPath() + "/" + CamWrapper.CamDefaulFolderName + "/RTP.sdp";
                            writeToFile(strWrite, strPath);
                            ffmpegWrapper.getInstance().naInitAndPlay(strPath, strMax_Delay);
                        } else {
                            String strIP = "1";
                            if (-1 != StartActivity.gStationIP) {
                                strIP = "" + StartActivity.gStationIP;
                            }
                            String url = "rtsp://" + (ipByteArray[0] & 0xFF) + "." + (ipByteArray[1] & 0xFF) + "."
                                    + (ipByteArray[2] & 0xFF) + "." + strIP + ":8080/?action=stream";
                            ffmpegWrapper.getInstance().naInitAndPlay(url, strMax_Delay);
                        }
                        mIsStart = true;
                    }
                    if (ffmpegWrapper.getInstance().naStatus() != ffmpegWrapper.ePlayerStatus.E_PlayerStatus_Playing.ordinal()) {
                        ffmpegWrapper.getInstance().naPlay();
                        m_iRetry++;
                        if (ffmpegWrapper.ePlayerStatus.E_PlayerStatus_Playing.ordinal() == ffmpegWrapper.getInstance().naStatus()) {
                            bShow = true;
                        }
                    }
                    final boolean fbShow = bShow;
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (fbShow) {
                                Toast.makeText(mContext, "Replay.", Toast.LENGTH_SHORT).show();
                            }
                            Log.e(TAG, "m_tvSpeed = " + linkSpeed + "  naStatus = " + ffmpegWrapper.getInstance().naStatus());
                            m_tvSpeed.setText("Retry = " + m_iRetry + "," + linkSpeed);
                            if (ffmpegWrapper.ePlayerStatus.E_PlayerStatus_Playing.ordinal() != ffmpegWrapper.getInstance().naStatus()) {
                                setPlayStatus(false);
                            } else {
                                setPlayStatus(true);
                            }
                        }
                    });
                } else {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            m_tvSpeed.setText("Wifi null");
                            Toast.makeText(mContext, "Not WIFI!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                // handle exception
            }
        }
    }
    ;

    private void setPlayStatus(boolean bPlay) {
        bCheckConnectStatus = bPlay;
        m_bnCapture.setEnabled(bPlay);
        m_bnRecord.setEnabled(bPlay);
        if (bPlay) {
            m_ivBroken.setVisibility(View.INVISIBLE);
            m_bnCapture.setImageResource(R.mipmap.camera);
            if (mSaveVideo) {
                m_bnRecord.setImageResource(R.mipmap.video_recode);
            } else {
                m_bnRecord.setImageResource(R.mipmap.video);
            }
        } else {
            checkStopRecode();
            m_ivBroken.setVisibility(View.VISIBLE);
            m_bnCapture.setImageResource(R.mipmap.camera_disable);
            m_bnRecord.setImageResource(R.mipmap.video_disable);
        }
    }

    public static int executeCommandLine(final String commandLine, final long timeout)
            throws IOException, InterruptedException, TimeoutException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(commandLine);
        Worker worker = new Worker(process);
        worker.start();
        try {
            worker.join(timeout);
            if (worker.exit != null) return worker.exit;
            else throw new TimeoutException();
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw ex;
        } finally {
            process.destroy();
        }
    }

    private static class Worker extends Thread {
        private final Process process;
        private Integer exit;
        private Worker(Process process) {
            this.process = process;
        }
        @Override
        public void run() {
            try {
                exit = process.waitFor();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }

    private static Thread m_connectGPWifiDeviceThread = null;
    private static boolean bCheckConnectStatus = false;
    private boolean bRunDeviceRunnable = true;
    class ConnectGPWifiDeviceRunnable implements Runnable {
        // check Wifi Status
        private boolean bCheckWifiStatus = false;
        // check Connect Status
        private int i32Status;
        @Override
        public void run() {
            // placeholder
        }
    }

    /**
     * Touch listener used to send joystick commands. Implementation details are
     * omitted here for brevity.
     */
    private class MyOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Implementation omitted. This would send appropriate commands
            // depending on the control pressed and the touch event.
            return false;
        }
    }

    /**
     * Thread used to periodically send UDP packets when operating in RTP mode.
     */
    class UdpSendingRunnable implements Runnable {
        @Override
        public void run() {
            while (RUN_THREAD) {
                try {
                    DatagramPacket packet = new DatagramPacket(m_byteSend, m_byteSend.length, targetAddress, targetPort);
                    service.send(packet);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Runnable used to update the record time every second when recording is in
     * progress. Subclasses may post this runnable via {@link Handler#postDelayed(Runnable, long)}.
     */
    protected Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            if (startTime == null) {
                return;
            }
            long spentTime = System.currentTimeMillis() - startTime;
            long hours = (spentTime / 1000) / 60 / 60;
            long minutes = (spentTime / 1000) / 60 % 60;
            long seconds = (spentTime / 1000) % 60;
            if (m_tvRecordTime != null) {
                String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                m_tvRecordTime.setText(timeStr);
            }
            // Schedule the next tick
            handler.postDelayed(this, 1000);
        }
    };

    /**
     * Starts or stops the streaming. In this sample the method simply toggles the
     * visibility of the UI; the networking details are handled elsewhere.
     */
    protected void StreamingOff() {
        // Placeholder for code that would stop the video stream
    }

    protected void StreamingOnOff() {
        // Placeholder for code that would toggle the video stream
    }

    /**
     * Called when the user taps the gallery button.
     */
    public void pressGallery(View view) {
        // Placeholder: stop streaming and open the gallery
    }

    /**
     * Called when the user taps the capture button.
     */
    public void pressCapture(View view) {
        // Placeholder: send command to capture a photo
    }

    /**
     * Called when the user taps the record button.
     */
    public void pressRecord(View view) {
        // Placeholder: send command to start/stop recording
    }

    /**
     * Called when the user taps the hide button.
     */
    public void pressHide(View view) {
        // Placeholder: hide or show controls
    }

    /**
     * Handler that receives status updates from the camera wrapper. The
     * implementation would parse the status and update the UI accordingly.
     */
    private Handler m_StatusHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            // Placeholder: handle status messages
        }
    };

    /**
     * Stop recording method
     */
    protected void stopRecode() {
        // Placeholder: stop recording functionality
        Log.d("ControlFragment", "Stop recording called");
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}