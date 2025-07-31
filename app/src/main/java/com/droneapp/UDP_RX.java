package com.droneapp;

import com.generalplus.GoPlusDrone.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.droneapp.CVControlFragment;
import com.droneapp.AlogInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public class UDP_RX {

    AlogInterface Alog = null;

    public UDP_RX() {
        // Construtor padrão
    }

    public void exemplo() {
        byte[] Rx_Data = new byte[0];
        if (Alog != null) {
            Alog.RxData(Rx_Data);
        }
    }
}
