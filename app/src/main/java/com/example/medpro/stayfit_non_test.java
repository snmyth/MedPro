package com.example.medpro;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.telephony.SmsManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class stayfit_non_test extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "BluetoothSerial";

    private Handler accidentHandler = new Handler();
    private Runnable accidentRunnable;
    private static final String TRUSTED_NUMBER = "+919326420240"; // Replace with actual number
    private static final int SMS_PERMISSION_CODE = 101;
    private static final UUID UUID_SERIAL_PORT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean isAccidentHandled = false;
    private MediaPlayer emergencyPlayer;

    private Runnable soundRunnable;
    private Runnable countdownRunnable;
    private static final int COUNTDOWN_SECONDS = 10;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Handler handler;

    private TextView gyroXTextView, gyroYTextView, gyroZTextView;
    private TextView accXTextView, accYTextView, accZTextView;
    private TextView temperatureTextView, speedTextView, inclinationTextView, caloriesTextView, cadenceTextView;
    private TextView timeElapsedTextView, distanceTravelledTextView;
    private Button startButton, stopButton, calibrateButton, endButton;

    private AudioManager audioManager;

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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 102;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Phone accelerometer variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double phoneAccX = 0, phoneAccY = 0, phoneAccZ = 0;
    private double previousPhoneAccX = 0, previousPhoneAccY = 0, previousPhoneAccZ = 0;
    private static final double ACCIDENT_THRESHOLD = 20.0; // Threshold for detecting an accident

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stayfit_non_test);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bluetooth connection
        String deviceAddress = "14:2B:2F:DB:01:3E"; // Replace with your ESP32 MAC Address
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
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

        // Initialize phone accelerometer
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Toast.makeText(this, "Phone accelerometer not available", Toast.LENGTH_SHORT).show();
            }
        }

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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            phoneAccX = event.values[0];
            phoneAccY = event.values[1];
            phoneAccZ = event.values[2];

            // Accident detection logic using phone accelerometer
            double deltaAccX = Math.abs(phoneAccX - previousPhoneAccX);
            double deltaAccY = Math.abs(phoneAccY - previousPhoneAccY);
            double deltaAccZ = Math.abs(phoneAccZ - previousPhoneAccZ);

            if (deltaAccX > ACCIDENT_THRESHOLD || deltaAccY > ACCIDENT_THRESHOLD || deltaAccZ > ACCIDENT_THRESHOLD) {
                handleAccident();
            }

            previousPhoneAccX = phoneAccX;
            previousPhoneAccY = phoneAccY;
            previousPhoneAccZ = phoneAccZ;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
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


    private int previousVolume; // To restore the original volume

    // Class variables



    private Runnable alarmRunnable;
    private Runnable messageRunnable;
    private static final int ALARM_DELAY = 5000; // 5 seconds
    private static final int MESSAGE_DELAY = 10000; // 10 seconds

    private void handleAccident() {
        // Prevent multiple accident handling
        if (isAccidentHandled) return;
        isAccidentHandled = true;

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Accident Detected!");
            builder.setMessage("Are you okay? Emergency alert will activate in 5 seconds.");
            builder.setCancelable(false);

            // Create dialog
            AlertDialog dialog = builder.create();
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel Emergency", (dialogInterface, which) -> {
                // User canceled - reset everything
                accidentHandler.removeCallbacks(alarmRunnable);
                accidentHandler.removeCallbacks(messageRunnable);
                stopEmergencySound();
                isAccidentHandled = false;
                resumeSession();
                dialog.dismiss();
            });

            dialog.setOnDismissListener(dialogInterface -> {
                // Ensure clean up if dialog is dismissed by other means
                accidentHandler.removeCallbacks(alarmRunnable);
                accidentHandler.removeCallbacks(messageRunnable);
                stopEmergencySound();
                isAccidentHandled = false;
            });

            dialog.show();

            // Schedule alarm to play after 5 seconds
            alarmRunnable = () -> {
                playEmergencySound();
                builder.setMessage("Are you okay? Emergency message will be sent in 5 seconds.");
            };
            accidentHandler.postDelayed(alarmRunnable, ALARM_DELAY);

            // Schedule message to send after 10 seconds
            messageRunnable = () -> {
                sendAccidentMessage();
                // Do not stop the emergency sound, it continues after the message
            };
            accidentHandler.postDelayed(messageRunnable, MESSAGE_DELAY);
        });
    }


    private void playEmergencySound() {
        // Initialize audio manager
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            // Set the audio stream to STREAM_MUSIC
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

            // Use AudioAttributes to configure the sound routing
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            // Set the phone speaker routing
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(true); // This is just a fallback, you may not need it
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            // Play emergency sound using MediaPlayer
            emergencyPlayer = new MediaPlayer();
            try {
                emergencyPlayer.setAudioAttributes(audioAttributes); // Use AudioAttributes for routing
                emergencyPlayer.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.emergency_alarm)); // Use the raw sound file
                emergencyPlayer.setLooping(true); // Loop the sound
                emergencyPlayer.prepare();

                emergencyPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Stop and release when finished
            emergencyPlayer.setOnCompletionListener(mp -> {
                emergencyPlayer.release();
                emergencyPlayer = null;
            });
        }
    }



    private void resumeSession() {
        isRunning = true;
        timeHandler.post(updateTimeRunnable);
    }

    private void executeEmergencyProtocol(AudioManager audioManager, int originalVolume) {
        // Stop the session
        isRunning = false;
        timeHandler.removeCallbacks(updateTimeRunnable);
        saveSessionData();

        // Send emergency message
        sendAccidentMessage();

        // Restore original volume when done
        if (emergencyPlayer != null) {
            emergencyPlayer.setOnCompletionListener(mp -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            });
        }

        // Disconnect Bluetooth
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }

    // Stop emergency alert sound and restore volume
    private void stopEmergencySound() {
        if (emergencyPlayer != null) {
            emergencyPlayer.stop();
            emergencyPlayer.release();
            emergencyPlayer = null;
        }

        // Restore original volume
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
        }
    }

    private void sendAccidentMessage() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, SMS_PERMISSION_CODE);
            return;
        }

        // Get the current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Create the emergency message with location
                        String locationLink = String.format("https://www.google.com/maps?q=%f,%f", location.getLatitude(), location.getLongitude());
                        String message = String.format("EMERGENCY! User might be in accident. Last known location: %s", locationLink);

                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(TRUSTED_NUMBER, null, message, null, null);
                        Toast.makeText(this, "Emergency message sent", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show());
    }


    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendAccidentMessage();
            } else {
                Toast.makeText(this, "SMS permission denied - cannot send emergency message", Toast.LENGTH_LONG).show();
            }
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

        // Unregister accelerometer listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
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