package com.example.arduino;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Get Bluetooth devices and save them in a list
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();

        Adapter deviceAdapter = refreshDevices();

        // Click on ListView item to get name and address and be able to connect to it
        listView.setOnItemClickListener((adapterView, view, i, l) -> {

            String tempName = deviceAdapter.getItem(i).toString();
            String tempAddress = null;

            for (BluetoothDevice btd : pairedDevices) {
                if(btd.getName().equals(tempName)) {
                    tempAddress = btd.getAddress();
                    break;
                }
            }
            deviceName.setText(tempName);
            deviceAddress.setText(tempAddress);
            btnConnect.setVisibility(View.VISIBLE);
        });

        // Connect to Bluetooth device
        btnConnect.setOnClickListener(view -> openConnectionActivity());

        // Refresh devices
        btnRefresh.setOnClickListener(view -> refreshDevices());
    }

    // Refresh paired Bluetooth devices and add them to an ArrayList
    public Adapter refreshDevices() {
        savedList = new ArrayList();
        pairedDevices = btAdapter.getBondedDevices();

        // Get all paired Bluetooth devices
        btAdapter.enable();
        for (BluetoothDevice btd : pairedDevices) {
            savedList.add(btd.getName());
        }
        // Implement the list to the listView
        Adapter deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, savedList);
        listView.setAdapter((ListAdapter) deviceAdapter);
        return deviceAdapter;
    }

    // Start activity to connect to Bluetooth device
    public void openConnectionActivity() {
        String address = deviceAddress.getText().toString();
        String name = deviceName.getText().toString();
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.putExtra(ADDRESS, address);
        intent.putExtra(NAME, name);

        startActivity(intent);
    }
}