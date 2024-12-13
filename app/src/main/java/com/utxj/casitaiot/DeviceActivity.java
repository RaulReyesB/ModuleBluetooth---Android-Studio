package com.utxj.casitaiot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

public class DeviceActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private BroadcastReceiver receiver;
    private Communication communication; // Comunicación simplificada

    private int mensaje = 0; // Variable global para almacenar el mensaje
    private Button btnUp, btnDown, btnLeft, btnRight, btnStop, btnReadSensor;
    private Button btnBedroomLightOff, btnBedroomLightOn, btnBedroomServoOpen, btnBedroomServoClose;
    private TextView sensorDataTextView; // TextView para mostrar datos del sensor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ListView devicesListView = findViewById(R.id.lv_devices);

        // Botones para enviar comandos
        btnUp = findViewById(R.id.btn_up);
        btnDown = findViewById(R.id.btn_down);
        btnLeft = findViewById(R.id.btn_left);
        btnRight = findViewById(R.id.btn_right);
        btnStop = findViewById(R.id.btn_stop);
        btnReadSensor = findViewById(R.id.btn_read_sensor);
        // Botones para enviar comandos
        btnBedroomLightOff = findViewById(R.id.btnBedroomLightOff);
        btnBedroomLightOn = findViewById(R.id.btnBedroomLightOn);
        btnBedroomServoOpen = findViewById(R.id.btnBedroomServoOpen);
        btnBedroomServoClose = findViewById(R.id.btnBedroomServoClose);

        // TextView para mostrar datos del sensor
        sensorDataTextView = findViewById(R.id.tv_sensor_data);

        // Configurar adaptador para la lista
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        devicesListView.setAdapter(adapter);

        // Buscar dispositivos
        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(view -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
                return;
            }
            checkPermissionsAndStartDiscovery();
        });

        // Manejar clics en la lista de dispositivos
        devicesListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDevice = deviceList.get(position);
            String deviceAddress = selectedDevice.split("\\(")[1].replace(")", ""); // Dirección MAC
            String deviceName = selectedDevice.split(" \\(")[0]; // Nombre del dispositivo
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            // Inicializar comunicación
            communication = new Communication(device, bluetoothAdapter);
            communication.start();

            if (communication.isConnected()) {
                Toast.makeText(this, "Conectado a: " + deviceName, Toast.LENGTH_SHORT).show();
                listenForSensorData(); // Escuchar datos del sensor
            } else {
                Toast.makeText(this, "Error al conectar con: " + deviceName, Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar botones para enviar comandos
        btnUp.setOnClickListener(v -> {
            mensaje = 1; // Comando para LED apagado
            enviarDatos();
        });
        btnDown.setOnClickListener(v -> {
            mensaje = 2; // Comando para LED encendido
            enviarDatos();
        });
        btnLeft.setOnClickListener(v -> {
            mensaje = 4; // Comando para mover servo a 180°
            enviarDatos();
        });
        btnRight.setOnClickListener(v -> {
            mensaje = 3; // Comando para mover servo a 0°
            enviarDatos();
        });
        btnStop.setOnClickListener(v -> {
            mensaje = 0; // Comando para detener
            enviarDatos();
        });

        btnBedroomLightOff.setOnClickListener(v ->{
            mensaje = 5;
            enviarDatos();
        });

        btnBedroomLightOn.setOnClickListener(v ->{
            mensaje = 6;
            enviarDatos();
        });

        btnBedroomServoClose.setOnClickListener(v ->{
            mensaje = 7;
            enviarDatos();
        });


        // Botón para leer distancia del sensor
        btnReadSensor.setOnClickListener(v -> {
            mensaje = 5; // Comando para leer el sensor
            enviarDatos();
        });
    }

    private void enviarDatos() {
        if (communication != null && communication.isConnected()) {
            communication.write(new byte[]{(byte) mensaje}); // Envía el entero como byte
            Toast.makeText(this, "Mensaje enviado: " + mensaje, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay conexión activa. Mensaje no enviado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForSensorData() {
        if (communication != null) {
            communication.setOnMessageReceivedListener(message -> {
                runOnUiThread(() -> {
                    sensorDataTextView.setText("Distancia: " + message + " cm");
                });
            });
        }
    }

    private void checkPermissionsAndStartDiscovery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_BT);
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_BT);
                return;
            }
        }
        discoverNewDevices();
    }

    private void discoverNewDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso para escanear dispositivos denegado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && device.getName() != null) {
                        String deviceInfo = device.getName() + " (" + device.getAddress() + ")";
                        if (!deviceList.contains(deviceInfo)) {
                            deviceList.add(deviceInfo);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        };
        registerReceiver(receiver, filter);
        Toast.makeText(this, "Buscando dispositivos...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        if (communication != null) {
            communication.cancel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverNewDevices();
            } else {
                Toast.makeText(this, "Permisos necesarios denegados", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
