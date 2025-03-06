package com.example.medpro;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.telephony.SmsManager;

import com.google.common.util.concurrent.ListenableFuture;


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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoOutput;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.io.File;
import android.util.Size;

public class stayfit_non_test extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "BluetoothSerial";
    private static final int REQUEST_CAMERA_PERMISSION = 100;


    private Handler accidentHandler = new Handler();
    private Runnable accidentRunnable;
    private static final String TRUSTED_NUMBER = "+919326420240"; // Replace with actual number
    private static final int SMS_PERMISSION_CODE = 101;
    private static final UUID UUID_SERIAL_PORT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean isAccidentHandled = false;
    private MediaPlayer emergencyPlayer;

    private VideoCapture<Recorder> videoCapture;
    private Recording recording;

    private Runnable soundRunnable;
    private Runnable countdownRunnable;
    private static final int COUNTDOWN_SECONDS = 10;
    private Interpreter tflite;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean isDetecting = false;
    private final int INPUT_SIZE = 640; // Change according to your model
    private Paint boxPaint;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private Handler handler;
    TextView bpmTextView;

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
    private static final double ACCIDENT_ACCELERATION_THRESHOLD = 20.0; // Increased to 20.0 m/s² (about 2.0g)
    private static final double ACCIDENT_GYRO_THRESHOLD = 7.0; // Increased to 7.0 rad/s
    private static final double ACCIDENT_THRESHOLD = 12.0; // Increased to 12.0 for phone accelerometer
    private static final long MIN_TIME_BETWEEN_DETECTIONS = 5000; // 5 seconds minimum between detections
    private long lastDetectionTime = 0;

    private boolean isWarmedUp = false;
    private static final long WARMUP_PERIOD = 2000; // 2 seconds warm-up period
    private long startWarmupTime = 0;
    private boolean isFirstReading = true;

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
        bpmTextView = findViewById(R.id.bpm);
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

        PreviewView previewView = findViewById(R.id.previewView);
        ImageView overlayView = findViewById(R.id.overlayView);
        startCamera(previewView, overlayView);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bluetooth connection setup (same as your code)
        String deviceAddress = "14:2B:2F:DB:01:3E"; // Replace with your ESP32 MAC Address
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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

        PreviewView previewVieww = findViewById(R.id.previewView);
        ImageView overlayVieww = findViewById(R.id.overlayView);

        // Request camera permission before initializing CameraX
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(previewVieww, overlayVieww);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }



    private void loadModel() {
        try {
            MappedByteBuffer tfliteModel = loadModelFile("yolov5su.tflite");
            tflite = new Interpreter(tfliteModel);
            Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show();
        }
    }

    // Load model from assets
    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Convert ImageProxy to Bitmap
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    // Draw detections
    private Bitmap drawDetections(Bitmap bitmap, List<RectF> boxes, List<String> labels) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        if (boxPaint == null) {
            boxPaint = new Paint();
            boxPaint.setColor(Color.RED);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(4f);
            boxPaint.setTextSize(50f);
        }

        for (int i = 0; i < boxes.size(); i++) {
            RectF box = boxes.get(i);
            canvas.drawRect(box, boxPaint);
            canvas.drawText(labels.get(i), box.left, box.top - 10, boxPaint);
        }
        return mutableBitmap;
    }

    // Process image for TFLite detection
    private void runObjectDetection(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        float[][][][] input = new float[1][INPUT_SIZE][INPUT_SIZE][3];

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                input[0][y][x][0] = (Color.red(pixel) / 255.0f);
                input[0][y][x][1] = (Color.green(pixel) / 255.0f);
                input[0][y][x][2] = (Color.blue(pixel) / 255.0f);
            }
        }

        // Output shape depends on your YOLO model (adjust accordingly)
        float[][][] output = new float[1][84][8400];
        tflite.run(input, output);


        // Process output (implement proper post-processing for YOLO)
        List<RectF> boxes = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (float[] detection : output[0]) {
            float confidence = detection[4];
            if (confidence > 0.5f) { // Confidence threshold
                float xCenter = detection[0] * bitmap.getWidth();
                float yCenter = detection[1] * bitmap.getHeight();
                float width = detection[2] * bitmap.getWidth();
                float height = detection[3] * bitmap.getHeight();

                float left = xCenter - width / 2;
                float top = yCenter - height / 2;
                boxes.add(new RectF(left, top, left + width, top + height));
                labels.add("Object: " + String.format("%.2f", confidence));
            }
        }
        // Call your camera start method


        PreviewView overlayView = findViewById(R.id.previewView);
        runOnUiThread(() -> {
            // Create an ImageView overlay if not already in your layout
            ImageView overlayImageView = findViewById(R.id.overlayView);


            // Create overlay bitmap
            Bitmap overlayBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(overlayBitmap);
            canvas.drawBitmap(bitmap, 0, 0, null); // Draw original frame

            Paint boxPaint = new Paint();
            boxPaint.setColor(Color.GREEN);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(6f);
            boxPaint.setAntiAlias(true);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(42f);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setAntiAlias(true);
            textPaint.setShadowLayer(8f, 2f, 2f, Color.BLACK); // Better text visibility

            for (int i = 0; i < boxes.size(); i++) {
                RectF box = boxes.get(i);
                canvas.drawRect(box, boxPaint);
                canvas.drawText(labels.get(i), box.left, box.top - 12, textPaint);
            }

            // Update overlay image
            overlayImageView.setImageBitmap(overlayBitmap);
        });

    }

    // Start Camera with Object Detection
    private void startCamera(PreviewView previewView, ImageView overlayView) {
        // Check for camera and audio permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        loadModel(); // Load TFLite model

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();

                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Error initializing camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));

        // Button listeners for recording
        startButton.setOnClickListener(v -> {
            // Reset all detection-related variables
            isWarmedUp = false;
            startWarmupTime = 0;
            isFirstReading = true;
            isAccidentHandled = false;
            lastDetectionTime = 0;

            // Initialize BPM to a safe value
            bpmTextView.setText("Calculating BPM...");

            // Start the session and recording
            isRunning = true;
            startTime = System.currentTimeMillis();
            timeHandler.post(updateTimeRunnable);
            readSensorData();
            startRecording();
            Toast.makeText(this, "Session Started", Toast.LENGTH_SHORT).show();
        });

        stopButton.setOnClickListener(v -> {
            isRunning = false;
            timeHandler.removeCallbacks(updateTimeRunnable);
            stopRecording();
            saveSessionData();
            Toast.makeText(this, "Session Stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void startRecording() {
        if (recording != null) {
            return;
        }

        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                .format(System.currentTimeMillis());
        File mediaDir = new File(getExternalMediaDirs()[0], "StayFit");
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        File file = new File(mediaDir, name + ".mp4");

        FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(file).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        recording = videoCapture.getOutput()
                .prepareRecording(this, fileOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        Log.d(TAG, "Recording started");
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                        if (!finalizeEvent.hasError()) {
                            String msg = "Video capture succeeded: " + file.getAbsolutePath();
                            Log.d(TAG, msg);
                        } else {
                            recording = null;
                            String msg = "Error: " + finalizeEvent.getError();
                            Log.e(TAG, msg);
                        }
                    }
                });
    }

    private void stopRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            phoneAccX = event.values[0];
            phoneAccY = event.values[1];
            phoneAccZ = event.values[2];

            // Initialize previous values on first reading
            if (isFirstReading) {
                previousPhoneAccX = phoneAccX;
                previousPhoneAccY = phoneAccY;
                previousPhoneAccZ = phoneAccZ;
                isFirstReading = false;
                return;
            }

            // Check if we're still in warm-up period
            if (!isWarmedUp) {
                if (startWarmupTime == 0) {
                    startWarmupTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - startWarmupTime >= WARMUP_PERIOD) {
                    isWarmedUp = true;
                    Log.d(TAG, "Warm-up period completed");
                } else {
                    // Update previous values during warm-up
                    previousPhoneAccX = phoneAccX;
                    previousPhoneAccY = phoneAccY;
                    previousPhoneAccZ = phoneAccZ;
                    return;
                }
            }

            // Log sensor values for debugging
            Log.d(TAG, String.format("Phone Accelerometer - X: %.2f, Y: %.2f, Z: %.2f",
                    phoneAccX, phoneAccY, phoneAccZ));

            // Accident detection logic using phone accelerometer
            double deltaAccX = Math.abs(phoneAccX - previousPhoneAccX);
            double deltaAccY = Math.abs(phoneAccY - previousPhoneAccY);
            double deltaAccZ = Math.abs(phoneAccZ - previousPhoneAccZ);

            // Log delta values for debugging
            Log.d(TAG, String.format("Delta Acceleration - X: %.2f, Y: %.2f, Z: %.2f (Threshold: %.2f)",
                    deltaAccX, deltaAccY, deltaAccZ, ACCIDENT_THRESHOLD));

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDetectionTime < MIN_TIME_BETWEEN_DETECTIONS) {
                return; // Skip detection if too soon after last detection
            }

            if (deltaAccX > ACCIDENT_THRESHOLD || deltaAccY > ACCIDENT_THRESHOLD || deltaAccZ > ACCIDENT_THRESHOLD) {
                // For phone accelerometer, we'll use the last known BPM value
                String currentBPM = bpmTextView.getText().toString().replace(" ", "").replace("BPM", "");
                Log.d(TAG, "Phone accelerometer - Checking BPM value: '" + currentBPM + "'");
                Log.d(TAG, "Phone accelerometer thresholds - X: " + deltaAccX + " > " + ACCIDENT_THRESHOLD +
                        ", Y: " + deltaAccY + " > " + ACCIDENT_THRESHOLD +
                        ", Z: " + deltaAccZ + " > " + ACCIDENT_THRESHOLD);

                try {
                    // Try to parse BPM as a number
                    double bpmValue = Double.parseDouble(currentBPM);
                    if (bpmValue < 40) {
                        Log.d(TAG, "Phone accelerometer threshold exceeded AND heartbeat is below 40! Triggering accident detection.");
                        lastDetectionTime = currentTime;
                        handleAccident(currentBPM);
                    } else {
                        Log.d(TAG, "Phone accelerometer threshold exceeded but heartbeat is not below 40 (BPM: " + bpmValue + "). Ignoring as false positive.");
                    }
                } catch (NumberFormatException e) {
                    // If BPM is not a number (e.g., "Calculating BPM..."), treat it as a potential emergency
                    if (currentBPM.equals("CalculatingBPM...")) {
                        Log.d(TAG, "Phone accelerometer threshold exceeded AND BPM is calculating! Triggering accident detection.");
                        lastDetectionTime = currentTime;
                        handleAccident(currentBPM);
                    } else {
                        Log.d(TAG, "Invalid BPM value: '" + currentBPM + "'. Ignoring as false positive.");
                    }
                }
            } else {
                Log.d(TAG, "Phone accelerometer thresholds not exceeded - X: " + deltaAccX + " <= " + ACCIDENT_THRESHOLD +
                        ", Y: " + deltaAccY + " <= " + ACCIDENT_THRESHOLD +
                        ", Z: " + deltaAccZ + " <= " + ACCIDENT_THRESHOLD);
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

            // Get accelerometer and gyroscope values
            double accX = jsonObject.getDouble("accX");
            double accY = jsonObject.getDouble("accY");
            double accZ = jsonObject.getDouble("accZ");
            double gyroX = jsonObject.getDouble("gyroX");
            double gyroY = jsonObject.getDouble("gyroY");
            double gyroZ = jsonObject.getDouble("gyroZ");
            String bpmStr = jsonObject.getString("bpm");

            // Log raw sensor values
            Log.d(TAG, String.format("Raw Sensor Values - Acc: X=%.2f, Y=%.2f, Z=%.2f, Gyro: X=%.2f, Y=%.2f, Z=%.2f, BPM: %s",
                    accX, accY, accZ, gyroX, gyroY, gyroZ, bpmStr));

            // Calculate total acceleration magnitude
            double totalAcceleration = Math.sqrt(accX * accX + accY * accY + accZ * accZ);

            // Calculate total angular velocity magnitude
            double totalAngularVelocity = Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);

            // Log calculated magnitudes and thresholds
            Log.d(TAG, String.format("Accident Detection - Total Acc: %.2f (threshold: %.2f), Total Gyro: %.2f (threshold: %.2f)",
                    totalAcceleration, ACCIDENT_ACCELERATION_THRESHOLD,
                    totalAngularVelocity, ACCIDENT_GYRO_THRESHOLD));

            long currentTime = System.currentTimeMillis();

            // Skip detection during warm-up period
            if (!isWarmedUp) {
                if (startWarmupTime == 0) {
                    startWarmupTime = currentTime;
                    Log.d(TAG, "Starting warm-up period");
                } else if (currentTime - startWarmupTime >= WARMUP_PERIOD) {
                    isWarmedUp = true;
                    Log.d(TAG, "Warm-up period completed");
                } else {
                    Log.d(TAG, "Still in warm-up period, skipping detection");
                    return;
                }
            }

            if (currentTime - lastDetectionTime < MIN_TIME_BETWEEN_DETECTIONS) {
                Log.d(TAG, "Skipping detection - too soon after last detection");
                return;
            }

            // Clean up BPM string - remove any spaces and "BPM" suffix
            String cleanBPM = bpmStr.replace(" ", "").replace("BPM", "");
            Log.d(TAG, "Cleaned BPM value: '" + cleanBPM + "'");

            // Only proceed with accident detection if we have a valid BPM value
            if (cleanBPM.equals("CalculatingBPM...")) {
                Log.d(TAG, "BPM still calculating, skipping detection");
                return;
            }

            try {
                double bpmValue = Double.parseDouble(cleanBPM);
                // Check for sudden acceleration/deceleration only if BPM is below 40
                if (bpmValue < 40 && (totalAcceleration > ACCIDENT_ACCELERATION_THRESHOLD ||
                        totalAngularVelocity > ACCIDENT_GYRO_THRESHOLD)) {

                    Log.d(TAG, "Accident detected! BPM: " + bpmValue +
                            ", Total Acc: " + totalAcceleration +
                            ", Total Gyro: " + totalAngularVelocity);

                    lastDetectionTime = currentTime;
                    handleAccident(cleanBPM);
                } else {
                    Log.d(TAG, "No accident - BPM: " + bpmValue +
                            ", Total Acc: " + totalAcceleration +
                            ", Total Gyro: " + totalAngularVelocity);
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "Invalid BPM value: '" + cleanBPM + "', skipping detection");
            }

            int cadence = jsonObject.getInt("cadence");

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


            bpmTextView.setText(bpmStr.equals("Calculating BPM...") ? bpmStr : bpmStr + " BPM");
            cadenceTextView.setText(cadence + " RPM");
            double temperature = 0;
            temperatureTextView.setText((int) Math.round(temperature) + "°C");


            totalDistance += smoothedSpeed * deltaTime;
            double totalDistanceInKm = totalDistance / 1000;
            distanceTravelledTextView.setText(String.format("%.2f km", totalDistanceInKm));

            int inclination = (int) Math.round(Math.atan2(accY, accZ) * (180 / Math.PI)) - (int) inclinationOffset;
            inclinationTextView.setText(inclination + "°");

            caloriesBurnt += (speedInKmh / 100.0) * 0.5;
            int caloriesBurntRounded = (int) Math.round(caloriesBurnt);
            caloriesTextView.setText(caloriesBurntRounded + " kcal");

            temperature = jsonObject.getDouble("temperature");
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

    private void handleAccident(String bpmStr) {
        // Log the accident detection attempt
        Log.d(TAG, "Attempting to handle accident. BPM: " + bpmStr);

        // Prevent multiple accident handling
        if (isAccidentHandled) {
            Log.d(TAG, "Accident already being handled");
            return;
        }

        isAccidentHandled = true;
        Log.d(TAG, "Starting accident handling protocol");

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("⚠️ ACCIDENT DETECTED!")
                    .setMessage("Are you okay? Emergency alert will activate in 5 seconds.")
                    .setCancelable(false);

            // Create dialog
            AlertDialog dialog = builder.create();
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel Emergency", (dialogInterface, which) -> {
                Log.d(TAG, "User cancelled emergency alert");
                // User canceled - reset everything
                accidentHandler.removeCallbacks(alarmRunnable);
                accidentHandler.removeCallbacks(messageRunnable);
                stopEmergencySound();
                isAccidentHandled = false;
                dialog.dismiss();
            });

            dialog.setOnDismissListener(dialogInterface -> {
                Log.d(TAG, "Emergency dialog dismissed");
                // Ensure clean up if dialog is dismissed by other means
                accidentHandler.removeCallbacks(alarmRunnable);
                accidentHandler.removeCallbacks(messageRunnable);
                stopEmergencySound();
                isAccidentHandled = false;
            });

            dialog.show();
            Log.d(TAG, "Emergency dialog shown");

            // Schedule alarm to play after 5 seconds
            alarmRunnable = () -> {
                Log.d(TAG, "Playing emergency sound");
                playEmergencySound();
                builder.setMessage("Are you okay? Emergency message will be sent in 5 seconds.");
            };
            accidentHandler.postDelayed(alarmRunnable, ALARM_DELAY);

            // Schedule message to send after 10 seconds
            messageRunnable = () -> {
                Log.d(TAG, "Sending emergency message");
                sendAccidentMessage();
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


    // Inside your saveSessionData() method
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

        // Get current date in yyyy-MM-dd format
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Create session with date
        Session session = new Session(userEmail, caloriesBurnt, averageSpeed, totalTime, totalDistanceInKm, currentDate);

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
        if (recording != null) {
            recording.close();
            recording = null;
        }
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
        public String date;  // Added date field

        public Session() { }

        public Session(String email, double caloriesBurnt, double averageSpeed, String totalTime, double distanceTravelled, String date) {
            this.email = email;
            this.caloriesBurnt = caloriesBurnt;
            this.averageSpeed = averageSpeed;
            this.totalTime = totalTime;
            this.distanceTravelled = distanceTravelled;
            this.date = date;  // Initialize date
        }
    }

}

