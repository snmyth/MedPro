package com.example.medpro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {
    public Button getStarted;
    private FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();

        Log.d("Debug", "onStart called");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d("Debug", "User is signed in: " + currentUser.getUid());
            Intent i = new Intent(MainActivity.this, patient_home.class);
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
        setContentView(R.layout.activity_main);


        getStarted = (Button) findViewById(R.id.button);
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, patient_login.class);
                startActivity(i);


            }
        });

    }

}