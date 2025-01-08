package com.example.medpro;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class patient_login extends AppCompatActivity {
    private Button register, login;
    private FirebaseAuth mAuth;
    private TextInputEditText email, password;
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();

        Log.d("Debug", "onStart called");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d("Debug", "User is signed in: " + currentUser.getUid());
            Intent i = new Intent(patient_login.this, patient_home.class);
            startActivity(i);
            finish();
        } else {
            Log.d("Debug", "No user signed in");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_login);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        register = findViewById(R.id.register);
        login = findViewById(R.id.login);
        email = findViewById(R.id.Email);
        password = findViewById(R.id.password);

        // Login button functionality
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email1 = email.getText().toString().trim();
                String password1 = password.getText().toString().trim();

                if (TextUtils.isEmpty(email1)) {
                    Toast.makeText(patient_login.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password1)) {
                    Toast.makeText(patient_login.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Firebase authentication
                mAuth.signInWithEmailAndPassword(email1, password1)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(patient_login.this, "Logged In", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(patient_login.this, patient_home.class);
                                    startActivity(intent);
                                    finish(); // Close the login activity
                                } else {
                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                                    Toast.makeText(patient_login.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Register button functionality
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(patient_login.this, parient_register.class);
                startActivity(intent);
            }
        });
    }
}
