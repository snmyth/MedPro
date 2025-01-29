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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class stayfit_non_test extends AppCompatActivity {

    private static final String TAG = "BluetoothSerial";
    private static final UUID UUID_SERIAL_PORT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Handler handler;

    private TextView gyroXTextView, gyroYTextView, gyroZTextView;
    private TextView accXTextView, accYTextView, accZTextView;
    private TextView temperatureTextView, speedTextView, inclinationTextView, caloriesTextView, cadenceTextView;
    private TextView timeElapsedTextView, distanceTravelledTextView;
    private Button startButton, stopButton, calibrateButton, endButton;

    private boolean isRunning = false;
    private double speed1 = 0; // km/h
    private double caloriesBurnt1 = 0;
    private double totalSpeed = 0; // For calculating average speed
    private int speedCount = 0; // For counting speed readings
    private double inclinationOffset = 0.0; // For calibration

    private long startTime = 0;
    private Handler timeHandler = new Handler();
    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedMillis / 1000) % 60;
                int minutes = (int) (elapsedMillis / 1000) / 60;
                String timeString = String.format("%02d:%02d", minutes, seconds);
                timeElapsedTextView.setText(timeString);
                timeHandler.postDelayed(this, 1000);
            }
        }
    };

    private double totalDistance = 0; // Total distance travelled in meters
    private double previousSpeed = 0;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stayfit_non_test);

        // Initialize Firebase Authentication and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("sessions");

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
        cadenceTextView = findViewById(R.id.cadence);
        timeElapsedTextView = findViewById(R.id.timeElapsed);
        distanceTravelledTextView = findViewById(R.id.distanceTravelled);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        // Add End Button

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
            startTime = System.currentTimeMillis();
            timeHandler.post(updateTimeRunnable);
            readSensorData();
            Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
        });

        stopButton.setOnClickListener(v -> {
            isRunning = false;
            timeHandler.removeCallbacks(updateTimeRunnable);
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
            saveSessionData();
            Toast.makeText(this, "Session Ended", Toast.LENGTH_SHORT).show();
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
    private double speed = 0.0;
    private double smoothedSpeed = 0.0;
    private final double NOISE_THRESHOLD = 0.05;
    private final double MOVEMENT_THRESHOLD = 0.1;
    private final double dampingFactor = 0.1;
    private double caloriesBurnt = 0.0;

    private void updateUI(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);

            double accY = jsonObject.getDouble("accY");
            double accZ = jsonObject.getDouble("accZ");
            double accX = jsonObject.getDouble("accX");

            int cadence = jsonObject.getInt("cadence");

            double currentTime = System.currentTimeMillis();
            if (previousTime == 0) {
                previousTime = currentTime;
                return;
            }

            double deltaTime = (currentTime - previousTime) / 1000.0;
            previousTime = currentTime;

            double netAccY = Math.abs(accY);

            if (netAccY < MOVEMENT_THRESHOLD) {
                smoothedSpeed = 0;
            } else {
                speed = netAccY * deltaTime;
                smoothedSpeed = smoothedSpeed + dampingFactor * (speed - smoothedSpeed);
            }

            if (Math.abs(smoothedSpeed) < NOISE_THRESHOLD) {
                smoothedSpeed = 0;
            }

            if (smoothedSpeed > 50) {
                smoothedSpeed = 50;
            }

            int speedInKmh = (int) Math.round(smoothedSpeed * 3.6);
            speedTextView.setText(speedInKmh + " km/h");

            totalDistance += smoothedSpeed * deltaTime;
            double totalDistanceInKm = totalDistance / 1000;
            distanceTravelledTextView.setText(String.format("%.2f km", totalDistanceInKm));

            int inclination = (int) Math.round(Math.atan2(accY, accZ) * (180 / Math.PI)) - (int) inclinationOffset;
            inclinationTextView.setText(inclination + "°");

            caloriesBurnt += (speedInKmh / 100.0) * 0.5;
            int caloriesBurntRounded = (int) Math.round(caloriesBurnt);
            caloriesTextView.setText(caloriesBurntRounded + " kcal");

            double temperature = jsonObject.getDouble("temperature");
            int roundedTemperature = (int) Math.round(temperature);
            temperatureTextView.setText(roundedTemperature + "°C");

            cadenceTextView.setText(cadence + " RPM");

            if (speedInKmh > 0) {
                totalSpeed += speedInKmh;
                speedCount++;
            }
            double averageSpeed = speedCount > 0 ? totalSpeed / speedCount : 0;
            TextView averageSpeedTextView = findViewById(R.id.averageSpeed);
            averageSpeedTextView.setText(String.format("%.1f km/h", averageSpeed));

        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }

    private void saveSessionData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String userEmail = mAuth.getCurrentUser().getEmail();
        double averageSpeed = (speedCount > 0) ? totalSpeed / speedCount : 0;
        String totalTime = timeElapsedTextView.getText().toString();
        double totalDistanceInKm = totalDistance / 1000;

        Session session = new Session(userEmail, caloriesBurnt, averageSpeed, totalTime, totalDistanceInKm);

        mDatabase.child(userId).push().setValue(session)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Session data saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error saving session data", Toast.LENGTH_SHORT).show();
                    }
                });
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

    public static class Session {
        public String email;
        public double caloriesBurnt;
        public double averageSpeed;
        public String totalTime;
        public double distanceTravelled;

        public Session() {
        }

        public Session(String email, double caloriesBurnt, double averageSpeed, String totalTime, double distanceTravelled) {
            this.email = email;
            this.caloriesBurnt = caloriesBurnt;
            this.averageSpeed = averageSpeed;
            this.totalTime = totalTime;
            this.distanceTravelled = distanceTravelled;
        }
    }
}