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

public class DeviceActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private BroadcastReceiver receiver;
    private Communication communication;

    private int mensaje = 0;
    private Button btnCocheraOff, btnCocheraOn, btnCocheraOpen, btnCocheraClose, btnCocheraReadSensor;
    private Button btnDormitorioOff, btnDormitorioOn, btnDormitorioOpen, btnDormitorioClose;
    private Button btnBanoOn, btnBanoOff;
    private Button btnDormitorio2Off, btnDormitorio2On, btnDormitorio2Open, btnDormitorio2Close;
    private TextView sensorDataCocheraTextView, sensorDataPatioTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ListView devicesListView = findViewById(R.id.lv_devices);

        // Inicializar botones de cochera
        btnCocheraOff = findViewById(R.id.btn_cochera_off);
        btnCocheraOn = findViewById(R.id.btn_cochera_on);
        btnCocheraOpen = findViewById(R.id.btn_cochera_open);
        btnCocheraClose = findViewById(R.id.btn_cochera_close);
        btnCocheraReadSensor = findViewById(R.id.btn_cochera_read_sensor);

        // Inicializar botones de dormitorio
        btnDormitorioOff = findViewById(R.id.btn_dormitorio_off);
        btnDormitorioOn = findViewById(R.id.btn_dormitorio_on);
        btnDormitorioOpen = findViewById(R.id.btn_dormitorio_open);
        btnDormitorioClose = findViewById(R.id.btn_dormitorio_close);

        // Inicializar botones de baño
        btnBanoOn = findViewById(R.id.btn_bano_on);
        btnBanoOff = findViewById(R.id.btn_bano_off);

        // Inicializar botones de dormitorio 2
        btnDormitorio2Off = findViewById(R.id.btn_dormitorio2_off);
        btnDormitorio2On = findViewById(R.id.btn_dormitorio2_on);
        btnDormitorio2Open = findViewById(R.id.btn_dormitorio2_open);
        btnDormitorio2Close = findViewById(R.id.btn_dormitorio2_close);

        // Inicializar TextViews para mostrar datos de sensores
        sensorDataCocheraTextView = findViewById(R.id.tv_cochera_sensor_data);
        sensorDataPatioTextView = findViewById(R.id.tv_patio_sensor_data);

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
                listenForSensorData();
            } else {
                Toast.makeText(this, "Error al conectar con: " + deviceName, Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar botones de cochera
        btnCocheraOff.setOnClickListener(v -> enviarMensaje(1)); // Apagar foco cochera
        btnCocheraOn.setOnClickListener(v -> enviarMensaje(2)); // Encender foco cochera
        btnCocheraOpen.setOnClickListener(v -> enviarMensaje(3)); // Abrir puerta cochera
        btnCocheraClose.setOnClickListener(v -> enviarMensaje(4)); // Cerrar puerta cochera
        btnCocheraReadSensor.setOnClickListener(v -> enviarMensaje(5)); // Leer sensor cochera

        // Configurar botones de dormitorio
        btnDormitorioOff.setOnClickListener(v -> enviarMensaje(6)); // Apagar foco dormitorio
        btnDormitorioOn.setOnClickListener(v -> enviarMensaje(7)); // Encender foco dormitorio
        btnDormitorioOpen.setOnClickListener(v -> enviarMensaje(8)); // Abrir puerta dormitorio
        btnDormitorioClose.setOnClickListener(v -> enviarMensaje(9)); // Cerrar puerta dormitorio

        // Configurar botones de baño
        btnBanoOff.setOnClickListener(v -> enviarMensaje(10)); // Apagar foco baño
        btnBanoOn.setOnClickListener(v -> enviarMensaje(11)); // Encender foco baño

        // Configurar botones de dormitorio 2
        btnDormitorio2Off.setOnClickListener(v -> enviarMensaje(12)); // Apagar foco dormitorio 2
        btnDormitorio2On.setOnClickListener(v -> enviarMensaje(13)); // Encender foco dormitorio 2
        btnDormitorio2Open.setOnClickListener(v -> enviarMensaje(14)); // Abrir puerta dormitorio 2
        btnDormitorio2Close.setOnClickListener(v -> enviarMensaje(15)); // Cerrar puerta dormitorio 2
    }

    private void enviarMensaje(int comando) {
        mensaje = comando;
        if (communication != null && communication.isConnected()) {
            communication.write(new byte[]{(byte) mensaje});
            Toast.makeText(this, "Mensaje enviado: " + mensaje, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay conexión activa. Mensaje no enviado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForSensorData() {
        if (communication != null) {
            communication.setOnMessageReceivedListener(message -> runOnUiThread(() -> {
                sensorDataCocheraTextView.setText("Cochera: " + message );
                sensorDataPatioTextView.setText("Patio: " + message + " % humedad");
            }));
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
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
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
        if (bluetoothAdapter.isDiscovering()) {
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
        if (requestCode == REQUEST_PERMISSION_BT && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            discoverNewDevices();
        } else {
            Toast.makeText(this, "Permisos necesarios denegados", Toast.LENGTH_SHORT).show();
        }
    }
}
