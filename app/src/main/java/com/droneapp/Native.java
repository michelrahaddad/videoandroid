package com.generalplus.GoPlusDrone;
import com.generalplus.GoPlusDrone.R;

import com.droneapp.AlogInterface;

public class Native {
    private AlogInterface Alog;
    
    static {
        System.loadLibrary("echo");
    }
    
    public void logMessage(final String message) {
       return;
    }
    
    public Native(AlogInterface mAlog) {
        Alog = mAlog;
    }
    
    public boolean TCP_C = true;
    
    public void RxData(final byte[] Rx_Data) {
        if (Alog != null) {
            Alog.RxData(Rx_Data);
        }
    }
    
    public native void nativeStartTcpServer(int port) throws Exception;
    public native void nativeStartTcpClient(String ip, int port, String message) throws Exception;
    public native void nativesocketThreadsinit();
    public native void nativesocketThreads(String ip, int port);
    public native void nativeStopSocket();
    public native int nativeisconnect();
    public native void nativesenddata(byte[] array, int No_delay) throws Exception;
    public native void nativeStartUdpServer(int port) throws Exception;
    public native void nativeStartUdpClient(String ip, int port, byte[] array) throws Exception;
}

