package com.example.arduino;



import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Intent extra to send the Mac-Address to the ConnectionAvtivity
    public static final String ADDRESS = "com.example.arduino.ADDRESS";
    public static final String NAME = "com.example.arduino.NAME";

    // Button
    Button btnConnect;
    Button btnRefresh;

    // ListView
    ListView listView;

    // TextViews
    TextView deviceName;
    TextView deviceAddress;

    // Bluetooth variables
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothManager manager;
    List savedList;
    Set<BluetoothDevice> pairedDevices;

    // Adapter
    Adapter deviceAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --------- Get Runtime Permissions ---------

        if(ContextCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_DENIED){


            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{"android.permission.BLUETOOTH_CONNECT"},
                    1);
        } else{

        }

        // --------- Initialization ---------


        // Initialize TextViews
        deviceName = findViewById(R.id.DeviceName);
        deviceAddress = findViewById(R.id.DeviceAddress);

        // Initialize ListView
        listView = findViewById(R.id.ListView);

        // Initialize Buttons
        btnConnect = findViewById(R.id.ConnectButton);
        btnRefresh = findViewById(R.id.RefreshButton);

        // ------------------------------------

        btnConnect.setVisibility(View.GONE);

        updateDevices();

        // Click on ListView item to get name and address and be able to connect to it
        listView.setOnItemClickListener((adapterView, view, i, l) -> {

            String temp = deviceAdapter.getItem(i).toString();
            String tempName = temp.substring(0, temp.indexOf("|"));
            String tempAddress = temp.substring(temp.indexOf("|") + 1);

            for (BluetoothDevice btd : pairedDevices) {
                if(btd.getName().equals(tempName) && btd.getAddress().equals(tempAddress)) {
                    deviceName.setText(tempName);
                    deviceAddress.setText(tempAddress);
                    btnConnect.setVisibility(View.VISIBLE);
                    return;
                }
            }
            deviceName.setText(tempName);
            deviceAddress.setText(tempAddress);
            Toast.makeText(getApplicationContext(), "Error while finding device", Toast.LENGTH_SHORT);
        });

        // Connect to Bluetooth device
        btnConnect.setOnClickListener(view -> openConnectionActivity());

        // Refresh devices
        btnRefresh.setOnClickListener(view -> updateDevices());
    }

    // Refresh paired Bluetooth devices and add them to an ArrayList
    public void updateDevices() {
        // Get Bluetooth devices and save them in a list
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();

        savedList = new ArrayList();
        pairedDevices = btAdapter.getBondedDevices();

        // Get all paired Bluetooth devices
        btAdapter.enable();
        for (BluetoothDevice btd : pairedDevices) {
            savedList.add(btd.getName() + "|" + btd.getAddress());
        }
        // Implement the list to the listView
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, savedList);
        listView.setAdapter((ListAdapter) deviceAdapter);

        deviceAddress.setText("");
        deviceName.setText("");
        btnConnect.setVisibility(View.GONE);
    }

    // Start activity to connect to Bluetooth device
    public void openConnectionActivity() {
        String address = deviceAddress.getText().toString();
        String name = deviceName.getText().toString();
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.putExtra(ADDRESS, address);
        intent.putExtra(NAME, name);

        startActivity(intent);
        updateDevices();
    }
}