package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import generalplus.com.GPCamLib.CamWrapper;

/**
 * Activity that establishes a connection to the GoPlus device over Wi‑Fi and
 * sets up directories for saving captured media.  Once the connection is
 * successful, it launches {@link WifiActivity} to allow the user to select
 * their network.
 */
public class StartCardActivity extends Activity {
    protected String strSaveDirectory;
    protected ProgressDialog mDialog;
    protected static Thread mConnectGPWifiDeviceThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create directories for saving files on external storage
        strSaveDirectory = Environment.getExternalStorageDirectory().getPath() + "/" + CamWrapper.CamDefaulFolderName;
        File saveFileDirectory = new File(strSaveDirectory);
        saveFileDirectory.mkdir();

        File cameraFileDirectory = new File(Environment.getExternalStorageDirectory().getPath() + CamWrapper.SaveFileToDevicePath);
        if (!cameraFileDirectory.exists()) {
            cameraFileDirectory.mkdirs();
        }
    }

    /**
     * Runnable that attempts to ping the GoPlus device and then connect to its
     * command socket.  On success, it sets up the download path and requests
     * the parameter file.  Once finished it notifies the UI thread and
     * launches the next activity.
     */
    class ConnectGPWifiDeviceRunnable implements Runnable {
        // Check connect status
        private boolean bCheckWifiStatus = false;
        private boolean bCheckConnectStatus = false;
        private int i32Status;
        private int i32RetryCount = 20;

        @Override
        public void run() {
            // Wait if a previous connection is still closing
            if (bCheckConnectStatus) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                int returnVal = -1;
                try {
                    returnVal = executeCommandLine("ping -c 1 " + CamWrapper.COMMAND_URL, 1500);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                bCheckWifiStatus = (returnVal == 0);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            if (bCheckWifiStatus) {
                CamWrapper.getComWrapperInstance().GPCamConnectToDevice(CamWrapper.COMMAND_URL, CamWrapper.COMMAN_PORT);
                while (bCheckWifiStatus) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i32Status = CamWrapper.getComWrapperInstance().GPCamGetStatus();
                    switch (i32Status) {
                        case CamWrapper.GPTYPE_ConnectionStatus_Idle:
                        case CamWrapper.GPTYPE_ConnectionStatus_Connecting:
                            bCheckConnectStatus = false;
                            bCheckWifiStatus = true;
                            break;
                        case CamWrapper.GPTYPE_ConnectionStatus_Connected:
                            bCheckConnectStatus = true;
                            bCheckWifiStatus = false;
                            CamWrapper.getComWrapperInstance().SetGPCamSetDownloadPath(strSaveDirectory);
                            CamWrapper.getComWrapperInstance().SetGPCamSendGetParameterFile(CamWrapper.ParameterFileName);
                            CamWrapper.getComWrapperInstance().GPCamGetStatus();
                            break;
                        case CamWrapper.GPTYPE_ConnectionStatus_DisConnected:
                        case CamWrapper.GPTYPE_ConnectionStatus_SocketClosed:
                            i32RetryCount--;
                            if (i32RetryCount == 0) {
                                bCheckConnectStatus = false;
                                bCheckWifiStatus = false;
                                CamWrapper.getComWrapperInstance().GPCamDisconnect();
                            }
                            break;
                    }
                }
            }

            // Dismiss the progress dialog on the UI thread and continue
            mDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bCheckConnectStatus) {
                        Intent toWifiActivity = new Intent(StartCardActivity.this, WifiActivity.class);
                        Bundle b = new Bundle();
                        b.putBoolean("IsCard", true);
                        toWifiActivity.putExtras(b);
                        startActivity(toWifiActivity);
                    } else {
                        Toast.makeText(StartCardActivity.this, "Please connect to GoPlus Drone or retry to reset GoPlus Drone first.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mConnectGPWifiDeviceThread = null;
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

    /**
     * Execute a shell command with a timeout.  Returns the process exit
     * status, or throws a {@link TimeoutException} if the timeout expires.
     */
    public int executeCommandLine(final String commandLine, final long timeout) throws IOException, InterruptedException, TimeoutException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(commandLine);

        Worker worker = new Worker(process);
        worker.start();
        try {
            worker.join(timeout);
            if (worker.exit != null)
                return worker.exit;
            else
                throw new TimeoutException();
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw ex;
        } finally {
            process.destroy();
        }
    }
}