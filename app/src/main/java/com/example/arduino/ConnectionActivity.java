package com.example.arduino;


import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class ConnectionActivity extends AppCompatActivity implements SensorEventListener {

    // Mac-Address that will be fetched from intent extra
    private String Address = "";
    private String Name = "";
    // UUID that will be used for establishing a connection
    private UUID Uuid;

    // Button
    Button btnDisconnect;
    Button btnTest;
    Button btnCalibrate;

    // TextView
    TextView deviceName;
    TextView degreeRotation;
    TextView receivedData;
    TextView countdown;

    // ImageView
    ImageView compassImage;

    // ProgressDialog
    ProgressDialog pD;

    // Check if connection is established
    boolean isConnected = false;

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
   private float azimuthOffset = 0f;
   private float currentAzimuth = 0f;
   float prevAzimuth = 0f;
   private int pass360 = 0;

    // Countdown timer
    private  static final long START_IN_MILLIS = 6000;
    private CountDownTimer timer;
    private long timeLeft = START_IN_MILLIS;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_device);

        // --------- Initialization ---------

        // Initialize Buttons
        btnDisconnect = findViewById(R.id.DisconnectButton);
        btnTest = findViewById(R.id.TestButton);
        btnCalibrate = findViewById(R.id.CalibrateButton);

        // Initialize TextViews
        deviceName = findViewById(R.id.Name);
        degreeRotation = findViewById(R.id.Degree);
        receivedData = findViewById(R.id.ReceivedData);
        countdown = findViewById(R.id.Countdown);

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
        Name = intent.getStringExtra(MainActivity.NAME);

        // --------- Sensor ---------

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // ------------------------------------

        deviceName.setText(Name);
        receivedData.setText("");

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

        // Set offset for Degree
        btnCalibrate.setOnClickListener(view ->  calibrate());
    }


    // check if i and y are similar with an offset of +-2
    private boolean checkData(int i, int y){
        if(i == y)
            return true;
        else if(i == y - 1 || i == y - 2)
            return true;
        else if(i == y + 1 || i == y + 2)
            return true;
        else
            return false;
    }

    // calibrate the orientation
    private void calibrate(){
        azimuthOffset = azimuth;
        prevAzimuth = 0f;
        pass360 = 0;
    }

    // start countdown timer
    private void startTimer() {
        if(timer == null) {
            timer = new CountDownTimer(START_IN_MILLIS, 1000) {
                int tempValue = 0;
                final int[] lastDegrees = new int[6];
                int counter = 0;

                @Override
                public void onTick(long millisUntilFinished) {
                    // set timerRunning true
                    timerRunning = true;
                    timeLeft = millisUntilFinished;

                    try{
                        // save current degree rotation and list it in array
                        tempValue = (int) currentAzimuth;
                        lastDegrees[counter] = tempValue;

                        if(counter > 0) {
                            // check each time a new degree rotation was saved, if they are similar
                            if(!checkData(lastDegrees[0], tempValue)){
                                // close timer when data is not similar
                                timerRunning = false;
                                timer = null;
                                cancel();
                            }
                        }
                        counter++;
                        updateCountdown();

                    } catch (Exception e) {
                        receivedData.setText(e.getMessage());
                        timerRunning = false;
                        timer = null;
                        cancel();
                    }
                }

                @Override
                public void onFinish() {
                    int signal = lastDegrees[0];
                    // send a signal to Bluetooth device
                    new Thread() {
                        @Override
                        public void run() {
                            sendSignal(signal);
                        }
                    }.start();
                    timer = null;
                    timerRunning = false;
                }

            }.start();
        }
    }

    // update countdown
    private void updateCountdown() {
        int seconds = (int) (timeLeft / 1000) % 60;

        btnTest.setText("Send Data " + Integer.toString(seconds));
    }

    // send a specific signal
    public void sendSignal(int signal) {
        try {
            btSocket.getOutputStream().write(signal);

        } catch (Exception e) {
            receivedData.setText(e.getMessage());
        }

        try {
            receiveSignal();

        } catch (Exception e) {
            receivedData.setText(e.getMessage());
        }
    }

    // send the current degree signal
    public void sendSignal() {
        String degreeText = degreeRotation.getText().toString();
        try {
            String temp = degreeText.substring(0, degreeText.indexOf("°"));
            int degree = Integer.parseInt(temp);
            btSocket.getOutputStream().write(degree);

        } catch (Exception e) {
           // receivedData.setText(e.getMessage());
        }


        try {
            receiveSignal();

        } catch (Exception e) {
          //  receivedData.setText(e.getMessage());
        }
    }

    // receive messages from Bluetooth device
    public void receiveSignal() throws IOException {
        // Inputstream to get all inputs
        InputStream inStream = btSocket.getInputStream();
        int byteCount = inStream.available();

        if(byteCount > 0) {
            // read the input and convert it to a message
            byte[] rawBytes = new byte[byteCount];
            inStream.read(rawBytes);
            String message = new String(rawBytes, "UTF-8");
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if(receivedData.getText().toString().equals("Data: Erhalten")){
                        receivedData.setText("Data: " + message + "!");
                    } else {
                        receivedData.setText("Data: " + message);
                    }
                }
            });


        }
    }

    // connect to Bluetooth device
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
                receivedData.setText(e.getMessage());
            }
            counter++;
        } while (!btSocket.isConnected() && counter < 3);

        // If the connection was successful return, else it will close the activity and return to MainActivity
        if(btSocket.isConnected()) {
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
                currentAzimuth = (azimuth - azimuthOffset + 360) % 360;

                // Idee divide in 0,90,180,270,360 areas
                    // 0-90 90-180 180-270 270-360
                    int test = (int) prevAzimuth;
                    int test2 = (int) currentAzimuth;

                if (4 < test2 && 180 > test2 &&  test == 0  ){
                    pass360 += 1;
                    Log.i("INfo", test + " " + test2);
                    prevAzimuth = currentAzimuth;
                }
                //Rüchlauf
                if (4 < test && 180 > test &&  test2 == 0  ){
                    pass360 -= 1;
                    Log.i("INfo", test + " " + test2);
                    prevAzimuth = currentAzimuth;
                }

                if (356 > test2 && 180 < test2 && test == 0 ) {
                    pass360 -= 1;
                    Log.i("INfo", test + " " + test2);
                    prevAzimuth = currentAzimuth;
                }
                // Rücklauf
                if (356 > test && 180 < test && test2 == 0 ) {
                    pass360 += 1;
                    Log.i("INfo", test + " " + test2);
                    prevAzimuth = currentAzimuth;
                }

                if(test2 == 0){
                        prevAzimuth = currentAzimuth;
                }

                animation.setDuration(500);
                animation.setRepeatCount(0);
                animation.setFillAfter(true);

                compassImage.startAnimation(animation);
            }
        }
        int currDegree = (int) currentAzimuth ;

        // if a connection is established and no countdown timer is currently running, start a new one
        if(isConnected) {
            if(!timerRunning) {
                startTimer();
            }
        }

        if(0 == pass360){
            degreeRotation.setText(currDegree + "°" + " not Rotated" +  pass360);
        } else
        if( 0 > pass360){
            degreeRotation.setText(-currDegree + "°" + " In Left Rotation." +  pass360);
        } else {
            degreeRotation.setText(currDegree + "°" + " In Right Rotation." + pass360);
        }
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