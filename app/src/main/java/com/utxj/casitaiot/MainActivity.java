package com.utxj.casitaiot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Debe coincidir con el nombre del XML

        Button startButton = findViewById(R.id.startButton); // Asegúrate de que coincida con el XML

        // Ir a la actividad de lista de dispositivos
        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
            startActivity(intent);
        });
    }
}
