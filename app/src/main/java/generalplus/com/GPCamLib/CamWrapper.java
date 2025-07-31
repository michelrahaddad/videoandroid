package generalplus.com.GPCamLib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Java wrapper around the native GPCam camera library.  This class
 * exposes a set of constants and native methods used to control the
 * GeneralPlus camera hardware.  It maintains a static instance for
 * callbacks and allows clients to register a handler to receive
 * camera status updates.
 */
public class CamWrapper {

    private static final String TAG = "CamWrapper";
    private static String mParameterFilePath;
    private static String mParameterFileName;
    private static Handler mNowViewHandler;
    private static int mNowViewIndex;
    private static CamWrapper sInstance;
    private static boolean sIsNewFile = false;

    // Streaming and command endpoints used by the GeneralPlus camera.
    public static final String STREAMING_URL = "rtsp://192.168.25.1:8080/?action=stream";
    public static final String COMMAND_URL = "192.168.25.1";
    /**
     * Default folder on external storage where the camera stores its
     * configuration and media.  Historically this project used
     * "CVGoPlus_Drone" as the folder name.  Retain this name to
     * remain compatible with existing code and avoid breaking file
     * lookups in AlogInterface and GPINIReader.
     */
    public static final String CamDefaulFolderName = "CVGoPlus_Drone";
    public static final String SaveFileToDevicePath = "/DCIM/Camera/";
    public static final String SaveLogFileName = "GoPlusDroneCmdLog";
    public static final String ConfigFileName = "GoPlusDroneConf.ini";
    public static final String ParameterFileName = "Menu.xml";
    public static final String EventMessgae_SMS = "android.provider.Telephony.SMS_RECEIVED";
    public static final int SupportMaxLogLength = 65536;
    public static final int SupportMaxShowLogLength = 200;

    // Error codes reported by the native library
    public static final int Error_ServerIsBusy     = 0xFFFF;
    public static final int Error_InvalidCommand   = 0xFFFE;
    public static final int Error_RequestTimeOut   = 0xFFFD;
    public static final int Error_ModeError        = 0xFFFC;
    public static final int Error_NoStorage        = 0xFFFB;
    public static final int Error_WriteFail        = 0xFFFA;
    public static final int Error_GetFileListFail  = 0xFFF9;
    public static final int Error_GetThumbnailFail = 0xFFF8;
    public static final int Error_FullStorage      = 0xFFF7;
    public static final int Error_SocketClosed     = 0xFFC1;
    public static final int Error_LostConnection   = 0xFFC0;

    // Communication ports
    public static final int STREAMING_PORT = 8080;
    public static final int COMMAN_PORT    = 8081;

    // GP_SOCK_TYPE (2 bytes)
    public static final int GP_SOCK_TYPE_CMD = 0x0001;
    public static final int GP_SOCK_TYPE_ACK = 0x0002;
    public static final int GP_SOCK_TYPE_NAK = 0x0003;

    // GP_SOCK_MODE_ID (1 byte)
    public static final int GPSOCK_MODE_General        = 0x00;
    public static final int GPSOCK_MODE_Record         = 0x01;
    public static final int GPSOCK_MODE_CapturePicture = 0x02;
    public static final int GPSOCK_MODE_Playback       = 0x03;
    public static final int GPSOCK_MODE_Menu           = 0x04;
    public static final int GPSOCK_MODE_Firmware       = 0x05;
    public static final int GPSOCK_MODE_Vendor         = 0xFF;

    // GP_SOCK_CMD_ID (1 byte)
    public static final int GPSOCK_General_CMD_SetMode            = 0x00;
    public static final int GPSOCK_General_CMD_GetDeviceStatus    = 0x01;
    public static final int GPSOCK_General_CMD_GetParameterFile   = 0x02;
    public static final int GPSOCK_General_CMD_Poweroff           = 0x03;
    public static final int GPSOCK_General_CMD_RestartStreaming   = 0x04;
    public static final int GPSOCK_General_CMD_AuthDevice         = 0x05;
    public static final int GPSOCK_Record_CMD_Start               = 0x00;
    public static final int GPSOCK_Record_CMD_Audio               = 0x01;
    public static final int GPSOCK_CapturePicture_CMD_Capture     = 0x00;
    public static final int GPSOCK_Playback_CMD_Start             = 0x00;
    public static final int GPSOCK_Playback_CMD_Pause             = 0x01;
    public static final int GPSOCK_Playback_CMD_GetFileCount      = 0x02;
    public static final int GPSOCK_Playback_CMD_GetNameList       = 0x03;
    public static final int GPSOCK_Playback_CMD_GetThumbnail      = 0x04;
    public static final int GPSOCK_Playback_CMD_GetRawData        = 0x05;
    public static final int GPSOCK_Playback_CMD_Stop              = 0x06;
    public static final int GPSOCK_Playback_CMD_ERROR             = 0xFF;
    public static final int GPSOCK_Menu_CMD_GetParameter          = 0x00;
    public static final int GPSOCK_Menu_CMD_SetParameter          = 0x01;
    public static final int GPSOCK_Vendor_CMD_Vendor              = 0x00;

    // Connection status
    public static final int GPTYPE_ConnectionStatus_Idle         = 0x00;
    public static final int GPTYPE_ConnectionStatus_Connecting   = 0x01;
    public static final int GPTYPE_ConnectionStatus_Connected    = 0x02;
    public static final int GPTYPE_ConnectionStatus_DisConnected = 0x03;
    public static final int GPTYPE_ConnectionStatus_SocketClosed = 0x0A;

    // Device modes
    public static final int GPDEVICEMODE_Record   = 0x00;
    public static final int GPDEVICEMODE_Capture  = 0x01;
    public static final int GPDEVICEMODE_Playback = 0x02;
    public static final int GPDEVICEMODE_Menu     = 0x03;
    public static final int GPDEVICEMODE_USB      = 0x04;

    // Battery levels
    public static final int GPBATTERTY_LEVEL0   = 0x00;
    public static final int GPBATTERTY_LEVEL1   = 0x01;
    public static final int GPBATTERTY_LEVEL2   = 0x02;
    public static final int GPBATTERTY_LEVEL3   = 0x03;
    public static final int GPBATTERTY_LEVEL4   = 0x04;
    public static final int GPBATTERTY_GHARGE   = 0x05;

    // View modes
    public static final int GPVIEW_STREAMING  = 0x00;
    public static final int GPVIEW_MENU       = 0x01;
    public static final int GPVIEW_FILELIST   = 0x02;

    // Callback types for camera status and data
    public static final int GPCALLBACKTYPE_CAMSTATUS = 0x00;
    public static final int GPCALLBACKTYPE_CAMDATA   = 0x01;

    // File flags
    public static final int GPFILEFLAG_AVISTREAMING = 0x01;
    public static final int GPFILEFLAG_JPGSTREAMING = 0x02;

    // Firmware commands
    public static final int GPSOCK_Firmware_CMD_Download  = 0x00;
    public static final int GPSOCK_Firmware_CMD_SendRawData = 0x01;
    public static final int GPSOCK_Firmware_CMD_Upgrade   = 0x02;

    // Callback keys used to pack status messages into a Bundle
    public static final String GPFILECALLBACKTYPE_FILEURL   = "FileURL";
    public static final String GPFILECALLBACKTYPE_FILEINDEX = "FileIndex";
    public static final String GPFILECALLBACKTYPE_FILEFLAG  = "FileFlag";
    public static final String GPCALLBACKSTATUSTYPE_CMDINDEX = "CmdIndex";
    public static final String GPCALLBACKSTATUSTYPE_CMDTYPE  = "CmdType";
    public static final String GPCALLBACKSTATUSTYPE_CMDMODE  = "CmdMode";
    public static final String GPCALLBACKSTATUSTYPE_CMDID    = "CmdID";
    public static final String GPCALLBACKSTATUSTYPE_DATASIZE = "DataSize";
    public static final String GPCALLBACKSTATUSTYPE_DATA     = "Data";

    static {
        // Load the native GPCam library when the class is first used.
        try {
            Log.i(TAG, "Trying to load GPCam.so ...");
            System.loadLibrary("GPCam");
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "Cannot load GPCam.so", ule);
        }
    }

    public CamWrapper() {
        sInstance = this;
    }

    /**
     * Registers a handler that will receive camera status updates.  The
     * handler's {@link Handler#handleMessage(Message)} method will be
     * invoked with messages whose {@code what} value indicates the
     * callback type (e.g. {@link #GPCALLBACKTYPE_CAMSTATUS}).
     *
     * @param viewHandler handler to receive messages
     * @param viewIndex   identifier for the view (not used by this stub)
     */
    public void setViewHandler(Handler viewHandler, int viewIndex) {
        mNowViewHandler = viewHandler;
        mNowViewIndex = viewIndex;
    }

    /** Returns the singleton instance of CamWrapper. */
    public static CamWrapper getComWrapperInstance() {
        return sInstance;
    }

    // Native callback invoked when camera data is available.  The
    // implementation is provided by the native library.  This stub
    // simply logs the callback.
    void GPCamDataCallBack(boolean isWrite, int dataSize, byte[] data) {
        // Data callbacks are not handled in this Java stub.  Native code
        // will process the data directly.
    }

    // Native callback invoked when camera status changes.  This stub
    // forwards the status to the registered handler.
    void GPCamStatusCallBack(int cmdIndex, int type, int mode,
                             int cmdID, int dataSize, byte[] data) {
        if (mNowViewHandler != null) {
            Message msg = Message.obtain();
            msg.what = GPCALLBACKTYPE_CAMSTATUS;
            Bundle bundle = new Bundle();
            bundle.putInt(GPCALLBACKSTATUSTYPE_CMDINDEX, cmdIndex);
            bundle.putInt(GPCALLBACKSTATUSTYPE_CMDTYPE, type);
            bundle.putInt(GPCALLBACKSTATUSTYPE_CMDMODE, mode);
            bundle.putInt(GPCALLBACKSTATUSTYPE_CMDID, cmdID);
            bundle.putInt(GPCALLBACKSTATUSTYPE_DATASIZE, dataSize);
            bundle.putByteArray(GPCALLBACKSTATUSTYPE_DATA, data);
            msg.setData(bundle);
            mNowViewHandler.sendMessage(msg);
        }
    }

    // Native method declarations.  These methods are defined in the
    // native library and provide the core camera functionality.
    public native int GPCamConnectToDevice(String ipAddress, int port);
    public native void GPCamDisconnect();
    private native void GPCamSetDownloadPath(String path);
    public native int GPCamAbort(int index);
    public native int GPCamSendSetMode(int mode);
    public native int GPCamSendGetStatus();
    private native int GPCamSendGetParameterFile(String fileName);
    public native int GPCamSendPowerOff();
    public native int GPCamSendRestartStreaming();
    public native int GPCamSendRecordCmd();
    public native int GPCamSendAudioOnOff(boolean isOn);
    public native int GPCamSendCapturePicture();
    public native int GPCamSendStartPlayback(int index);
    public native int GPCamSendPausePlayback();
    public native int GPCamSendGetFullFileList();
    public native int GPCamSendGetFileThumbnail(int index);
    public native int GPCamSendGetFileRawdata(int index);
    public native int GPCamSendStopPlayback();
    public native int GPCamSetNextPlaybackFileListIndex(int index);
    public native int GPCamSendGetParameter(int id);
    public native int GPCamSendSetParameter(int id, int size, byte[] data);
    public native int GPCamSendFirmwareDownload(long fileSize, long checksum);
    public native int GPCamSendFirmwareRawData(long size, byte[] data);
    public native int GPCamSendFirmwareUpgrade();
    public native int GPCamSendVendorCmd(byte[] data, int size);
    public native int GPCamGetStatus();
    public native String GPCamGetFileName(int index);
    public native boolean GPCamGetFileTime(int index, byte[] time);
    public native int GPCamGetFileIndex(int index);
    public native int GPCamGetFileSize(int index);
    public native byte GPCamGetFileExt(int index);
    public native void GPCamClearCommandQueue();

    // Public wrappers around a few private native methods.
    public void setGPCamSetDownloadPath(String filePath) {
        mParameterFilePath = filePath;
        GPCamSetDownloadPath(mParameterFilePath);
    }

    public String getGPCamSetDownloadPath() {
        return mParameterFilePath;
    }

    public void setGPCamSendGetParameterFile(String fileName) {
        mParameterFileName = fileName;
        GPCamSendGetParameterFile(mParameterFileName);
    }

    public String getGPCamSendGetParameterFile() {
        return mParameterFileName;
    }

    // Additional methods with capital S for compatibility
    public void SetGPCamSetDownloadPath(String filePath) {
        setGPCamSetDownloadPath(filePath);
    }

    public void SetGPCamSendGetParameterFile(String fileName) {
        setGPCamSendGetParameterFile(fileName);
    }

    public void setIsNewFile(boolean newFile) {
        sIsNewFile = newFile;
    }

    public boolean getIsNewFile() {
        return sIsNewFile;
    }

    /**
     * Set view handler for camera wrapper
     */
    public void SetViewHandler(android.os.Handler handler, int viewType) {
        // Placeholder: set view handler for camera wrapper
        mNowViewHandler = handler;
        // viewType can be used to determine the type of view (e.g., GPVIEW_FILELIST)
    }
}