package com.utxj.casitaiot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ActionsActivity extends AppCompatActivity {

    private BluetoothSocket bluetoothSocket;
    private DataOutputStream dataOutputStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID estándar SPP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actions);

        // Obtener dirección y nombre del dispositivo desde el Intent
        String deviceAddress = getIntent().getStringExtra("deviceAddress");
        String deviceName = getIntent().getStringExtra("deviceName");

        // Configurar conexión Bluetooth
        if (setupConnection(deviceAddress, deviceName)) {
            Toast.makeText(this, "Conexión establecida con " + deviceName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al conectar con " + deviceName, Toast.LENGTH_SHORT).show();
        }

        // Configurar botones
        Button buttonSend1 = findViewById(R.id.buttonSend1);
        Button buttonSend2 = findViewById(R.id.buttonSend2);

        buttonSend1.setOnClickListener(v -> sendCommand(1)); // Enviar entero 1
        buttonSend2.setOnClickListener(v -> sendCommand(2)); // Enviar entero 2
    }

    private boolean setupConnection(String deviceAddress, String deviceName) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            // Intentar conexión con socket seguro
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            dataOutputStream = new DataOutputStream(bluetoothSocket.getOutputStream());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
            return false;
        }
    }

    private void sendCommand(int command) {
        if (dataOutputStream != null) {
            new Thread(() -> {
                try {
                    dataOutputStream.writeInt(command); // Enviar el comando como entero
                    runOnUiThread(() -> Toast.makeText(this, "Comando enviado: " + command, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Error al enviar comando", Toast.LENGTH_SHORT).show());
                    closeConnection();
                }
            }).start();
        } else {
            Toast.makeText(this, "No hay conexión activa", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeConnection() {
        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnection();
    }
}
