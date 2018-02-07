package com.mbientlab.metawear.app;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_START_BLE_SCAN= 1;
    public static String patient_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivityForResult(new Intent(MainActivity.this, PatientName.class), REQUEST_START_BLE_SCAN));

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setOnClickListener(view -> startActivityForResult(new Intent(MainActivity.this, ScannerActivity.class), REQUEST_START_BLE_SCAN));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_START_BLE_SCAN:
                    BluetoothDevice selectedDevice = data.getParcelableExtra(PatientName.EXTRA_DEVICE);
                    patient_name = data.getStringExtra(PatientName.EXTRA_PATIENT_NAME);
                    if (selectedDevice != null) {
                        ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_content)).addNewDevice(selectedDevice);
                    }
                    break;
            }
        }
    }

}
