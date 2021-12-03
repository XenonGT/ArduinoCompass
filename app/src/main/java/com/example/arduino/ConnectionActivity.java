package com.example.arduino;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionActivity extends AppCompatActivity implements SensorEventListener {

    // Mac-Address that will be fetched from intent extra
    private String Address = "";
    // UUID that will be used for establishing a connection
    private UUID Uuid;

    // Button
    Button btnDisconnect;
    Button btnTest;

    // TextView
    TextView deviceName;
    TextView degreeRotation;

    // ImageView
    ImageView compassImage;

    // ProgressDialog
    ProgressDialog pD;

    //
    boolean isConnected = true;

    // Bluetooth variables
    BluetoothManager manager;
    BluetoothAdapter btAdapter;
    BluetoothDevice connectedDevice;
    BluetoothSocket btSocket = null;

    // Sensor variables
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;

   private float[] mGravity = new float[3];
   private float[] mGeomagnetic = new float[3];
   private float azimuth = 0f;
   private float currentAzimuth = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_device);

        // --------- Initialization ---------

        // Initialize Buttons
        btnDisconnect = findViewById(R.id.DisconnectButton);
        btnTest = findViewById(R.id.TestButton);

        // Initialize TextViews
        deviceName = findViewById(R.id.Name);
        degreeRotation = findViewById(R.id.Degree);

        // Initialize ImageView
        compassImage = findViewById(R.id.CompassImage);

        // Set Progress Dialog
        pD = new ProgressDialog(this);
        pD.setTitle("Connecting to Device");
        pD.setMessage("Connecting...");
        pD.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pD.setCancelable(false);


        // Get intent extra from previous Activity
        Intent intent = getIntent();
        Address = intent.getStringExtra(MainActivity.ADDRESS);

        // --------- Sensor ---------

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // ------------------------------------

        // Connect to the Bluetooth device
        // Loading screen while connecting
        pD.show();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    connectToDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pD.dismiss();
            }
        });

        // Close connection and return to MainActivity
        btnDisconnect.setOnClickListener(view -> Disconnect());

        // Write test data to Bluetooth device
        btnTest.setOnClickListener(view -> sendSignal());

    }

    public void sendSignal() {
        String degreeText = degreeRotation.getText().toString();
        try {
            String temp = degreeText.substring(0, degreeText.indexOf("°"));
            int degree = Integer.parseInt(temp);
            btSocket.getOutputStream().write(degree);

        } catch (Exception e) {
            deviceName.setText(e.getMessage());
        }

    }

    public void connectToDevice(){
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();

        // Get the UUIDs from the connected device
        connectedDevice = btAdapter.getRemoteDevice(Address);
        ParcelUuid[] uuids = connectedDevice.getUuids();
        Uuid = uuids[0].getUuid();

        // Try to connect a total amount of 3 times
        int counter = 0;
        do{
            try {
                btSocket = connectedDevice.createRfcommSocketToServiceRecord(Uuid);
                btSocket.connect();

            } catch (IOException e) {
                deviceName.setText(e.getMessage());
            }
            counter++;
        } while (!btSocket.isConnected() && counter < 3);

        // If the connection was successful display the devices name, else it will close the activity and return to MainActivity
        if(btSocket.isConnected()) {
            deviceName.setText(connectedDevice.getName());
            isConnected = true;
            return;
        }

        isConnected = false;
        ConnectionActivity.this.finish();
    }

    // Close connection and close activity
    public void Disconnect() {
        try {
            btSocket.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    // --------- Sensor ---------

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        synchronized (this) {
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
            }

            if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * sensorEvent.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * sensorEvent.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * sensorEvent.values[2];
            }

            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if(success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                Animation animation = new RotateAnimation(-currentAzimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                currentAzimuth = azimuth;

                animation.setDuration(500);
                animation.setRepeatCount(0);
                animation.setFillAfter(true);

                compassImage.startAnimation(animation);
            }
        }
        int currDegree = (int) currentAzimuth;
        degreeRotation.setText(currDegree + "°");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
    }
}