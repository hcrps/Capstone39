package com.mbientlab.metawear.app;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;


public class PatientName extends AppCompatActivity {
    public static final int REQUEST_START_BLE_SCAN= 1;
    public static final String EXTRA_DEVICE= "com.mbientlab.metawear.app.ScannerActivity.EXTRA_DEVICE";
    public static final String EXTRA_PATIENT_NAME = "com.example.mbientlab.metawear.app.NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patientname);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }
    /** Called when the user taps the Send button */
    public void addPatient(View view) {
        // Save patient name and go to scanner activity
        Intent intent = new Intent(this, ScannerActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String name = editText.getText().toString();
        intent.putExtra(EXTRA_PATIENT_NAME, name);
        startActivityForResult(intent,REQUEST_START_BLE_SCAN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_START_BLE_SCAN:
                    Intent result = new Intent();
                    BluetoothDevice btDevice = data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                    BluetoothDevice patient_name = data.getParcelableExtra(ScannerActivity.EXTRA_PATIENT_NAME);
                    result.putExtra(EXTRA_DEVICE, btDevice);
                    result.putExtra(EXTRA_PATIENT_NAME, patient_name);
                    setResult(RESULT_OK, result);
                    finish();
                    // BluetoothDevice selectedDevice= data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                    // if (selectedDevice != null) {
                    //      ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_content)).addNewDevice(selectedDevice);
                    // }
                    // break;
            }
        }
    }

}
