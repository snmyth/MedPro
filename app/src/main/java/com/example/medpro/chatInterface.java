package com.example.medpro;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class chatInterface extends AppCompatActivity implements OnInitListener {

    private static final String TAG = "chatInterface";
    private static final String GEMINI_API_KEY = "huh";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private EditText userInputField;
    private TextView userMessageTextView;
    private TextView aiResponseTextView;
    private ScrollView scrollView;

    private TextToSpeech textToSpeech;  // Declare TextToSpeech instance

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_interface);

        // Initialize UI elements
        userInputField = findViewById(R.id.editTextTextMultiLine);
        userMessageTextView = findViewById(R.id.textViewUserMessage);
        aiResponseTextView = findViewById(R.id.textViewAIResponse);
        scrollView = findViewById(R.id.scrollViewAIResponse);
        ImageButton sendButton = findViewById(R.id.imageButton);
        Button voiceButton = findViewById(R.id.voicebtn);  // Add a button for TTS

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set click listener for send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String getTEE = userInputField.getText().toString().trim();
                String genPrompt ="Developer prompt:You are being used as a lightweight AI health assistant here, Your name is Amora, also you are supposed to reply to users' queries in descriptive and understandable words, also dont use rich test as it is getting converted into voice, so try not disappointing the user!, as of now the user is saying :";
                String userMessage = genPrompt + getTEE;
                userInputField.setText("");
                if (!userMessage.isEmpty()) {
                    userMessageTextView.setText(getTEE);
                    sendMessageToGeminiAPI(userMessage);
                } else {
                    Toast.makeText(chatInterface.this, "Please enter a message.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set click listener for voice button (TTS)
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String aiMessage = aiResponseTextView.getText().toString().trim();
                if (!aiMessage.isEmpty()) {
                    speak(aiMessage);  // Convert the AI response to speech
                } else {
                    Toast.makeText(chatInterface.this, "AI response is empty.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language for TTS (You can change this to different languages)
            int langResult = textToSpeech.setLanguage(Locale.US);
            if (langResult == TextToSpeech.LANG_MISSING_DATA | langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported or missing data.");
            } else {
                Log.d(TAG, "TextToSpeech Initialized successfully.");
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed.");
        }
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void sendMessageToGeminiAPI(String userMessage) {
        try {
            // Create JSON payload following the correct structure for Gemini
            JSONObject jsonPayload = new JSONObject();
            JSONArray contentsArray = new JSONArray();

            JSONObject content = new JSONObject();
            JSONArray partsArray = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", userMessage);  // Add user message to "text" field
            partsArray.put(part);

            content.put("parts", partsArray);  // Add parts array to "content"
            contentsArray.put(content);

            jsonPayload.put("contents", contentsArray);  // Add contents array to the payload

            // Create request body
            RequestBody body = RequestBody.create(JSON, jsonPayload.toString());

            // Build request
            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .build();

            // Send request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(chatInterface.this, "Failed to get response.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : null;
                    Log.d(TAG, "Response received: " + responseBody);  // Log the raw response

                    if (response.isSuccessful() && responseBody != null) {
                        runOnUiThread(() -> {
                            try {
                                // Log the full response to verify its structure
                                Log.d(TAG, "Full response: " + responseBody);

                                // Parse the JSON response
                                JSONObject jsonResponse = new JSONObject(responseBody);

                                // Check if "candidates" field exists
                                if (jsonResponse.has("candidates")) {
                                    // Get the first candidate object
                                    JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                                    JSONObject content = candidate.getJSONObject("content");
                                    JSONArray partsArray = content.getJSONArray("parts");

                                    // Get the text from the first part of the "parts" array
                                    String aiMessage = partsArray.getJSONObject(0).getString("text");

                                    // Set the AI response in the UI
                                    aiResponseTextView.setText(aiMessage);
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                } else {
                                    Log.e(TAG, "'candidates' field not found in the response.");
                                    Toast.makeText(chatInterface.this, "Error: 'candidates' field missing.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
                                Toast.makeText(chatInterface.this, "Error parsing AI response.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "Unsuccessful response: " + response.code() + " - " + responseBody);
                        runOnUiThread(() -> userMessageTextView.setText(responseBody));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            Toast.makeText(this, "Error creating request.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
