package com.example.medpro;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class bluetoothStayfir extends AppCompatActivity {

    private static final String TAG = "BluetoothSerial";
    private static final UUID UUID_SERIAL_PORT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Handler handler;

    private TextView gyroXTextView, gyroYTextView, gyroZTextView;
    private TextView accXTextView, accYTextView, accZTextView;
    private TextView temperatureTextView, speedTextView, inclinationTextView, caloriesTextView;
    private Button startButton, stopButton;

    private boolean isRunning = false;
    private double speed1 = 0; // km/h
    private double caloriesBurnt1 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_stayfir);

        // Initialize views
        gyroXTextView = findViewById(R.id.gyroX);
        gyroYTextView = findViewById(R.id.gyroY);
        gyroZTextView = findViewById(R.id.gyroZ);
        accXTextView = findViewById(R.id.accX);
        accYTextView = findViewById(R.id.accY);
        accZTextView = findViewById(R.id.accZ);
        temperatureTextView = findViewById(R.id.temperature);
        speedTextView = findViewById(R.id.speed);
        inclinationTextView = findViewById(R.id.inclination);
        caloriesTextView = findViewById(R.id.calories);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bluetooth connection
        String deviceAddress = "14:2B:2F:DB:01:3E"; // Replace with your ESP32 MAC Address
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SERIAL_PORT);
            bluetoothSocket.connect();
            Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to device", e);
            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handler = new Handler();

        // Button listeners
        startButton.setOnClickListener(v -> {
            isRunning = true;
            readSensorData();
            Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
        });

        stopButton.setOnClickListener(v -> {
            isRunning = false;
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void readSensorData() {
        new Thread(() -> {
            try (InputStream inputStream = bluetoothSocket.getInputStream()) {
                byte[] buffer = new byte[1024];
                int bytes;

                while (isRunning) {
                    bytes = inputStream.read(buffer);
                    String data = new String(buffer, 0, bytes);

                    handler.post(() -> updateUI(data));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading data", e);
                runOnUiThread(() -> Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private double previousTime = 0;
    private double speed = 0.0; // Instantaneous speed in m/s
    private double smoothedSpeed = 0.0; // Smoothed speed using low-pass filter
    private final double NOISE_THRESHOLD = 0.05;  // Threshold for noise filtering
    private final double MOVEMENT_THRESHOLD = 0.1;  // Threshold to detect movement
    private final double dampingFactor = 0.1; // Low-pass filter factor (adjust for smoother results)
    private double caloriesBurnt = 0.0; // Calories burned

    private void updateUI(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);

            // Get accelerometer and gyroscope values
            double accX = jsonObject.getDouble("accX");
            double accY = jsonObject.getDouble("accY");
            double accZ = jsonObject.getDouble("accZ");

            // Display accelerometer values
            gyroXTextView.setText("Gyro X: " + jsonObject.getString("gyroX"));
            gyroYTextView.setText("Gyro Y: " + jsonObject.getString("gyroY"));
            gyroZTextView.setText("Gyro Z: " + jsonObject.getString("gyroZ"));
            accXTextView.setText("Acc X: " + accX);
            accYTextView.setText("Acc Y: " + accY);
            accZTextView.setText("Acc Z: " + accZ);
            temperatureTextView.setText("Temp: " + jsonObject.getString("temperature"));

            // Time calculation (if previousTime is 0, set it to current time)
            double currentTime = System.currentTimeMillis();
            if (previousTime == 0) {
                previousTime = currentTime;
            }

            double deltaTime = (currentTime - previousTime) / 1000.0; // in seconds
            previousTime = currentTime;

            // Remove gravity effect and calculate net acceleration on the X and Y axes
            double netAccX = accX - 9.8;  // Gravity effect on X-axis
            double netAccY = accY - 9.8;  // Gravity effect on Y-axis

            // Calculate the horizontal acceleration magnitude (ignoring Z-axis)
            double netHorizontalAcceleration = Math.sqrt(netAccX * netAccX + netAccY * netAccY);

            // If the net acceleration is too low, assume the sensor is stationary and reset speed
            if (netHorizontalAcceleration < MOVEMENT_THRESHOLD) {
                smoothedSpeed = 0;  // Reset speed when stationary
            } else {
                // If there's movement, update speed based on the net horizontal acceleration
                speed = netHorizontalAcceleration * deltaTime;
                // Apply low-pass filter for damping
                smoothedSpeed = smoothedSpeed + dampingFactor * (speed - smoothedSpeed);
            }

            // If smoothed speed is below the noise threshold, reset it to 0
            if (Math.abs(smoothedSpeed) < NOISE_THRESHOLD) {
                smoothedSpeed = 0;
            }

            // Cap the speed to a max value (for sanity)
            if (Math.abs(smoothedSpeed) > 50) {  // Max speed cap (for sanity)
                smoothedSpeed = 50;
            }

            // Convert speed from m/s to km/h
            double speedInKmh = Math.abs(smoothedSpeed) * 3.6; // m/s to km/h
            speedTextView.setText("Speed: " + String.format("%.2f km/h", speedInKmh-5));

            // Inclination Calculation (just for reference)
            double inclination = Math.atan2(accY, accZ) * (180 / Math.PI);  // Convert radians to degrees
            inclinationTextView.setText("Inclination: " + String.format("%.2f°", inclination));

            // Calories Burnt Calculation (simple formula)
            caloriesBurnt += (speedInKmh / 100) * 0.5; // Example formula
            caloriesTextView.setText("Calories Burnt: " + String.format("%.2f kcal", caloriesBurnt));

        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}
