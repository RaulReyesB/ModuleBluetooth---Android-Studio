package com.utxj.casitaiot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Communication extends Thread {
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean connected = false;

    private OnMessageReceivedListener messageListener; // Listener para recibir mensajes

    public Communication(BluetoothDevice device, BluetoothAdapter adapter) {
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            adapter.cancelDiscovery();
            socket.connect();
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            connected = true;
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024]; // Buffer para almacenar los datos recibidos
        int bytes;

        while (connected) {
            try {
                // Leer datos del dispositivo Bluetooth
                bytes = inputStream.read(buffer);
                String message = new String(buffer, 0, bytes).trim();

                // Notificar al listener si hay datos disponibles
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
                close();
                break;
            }
        }
    }

    public void write(byte[] data) {
        try {
            if (outputStream != null) {
                outputStream.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void cancel() {
        close();
    }

    private void close() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }

    // Establecer el listener para recibir mensajes
    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    // Interfaz para manejar mensajes recibidos
    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }
}
