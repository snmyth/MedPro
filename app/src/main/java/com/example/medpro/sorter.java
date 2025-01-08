package com.example.medpro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;


public class sorter extends AppCompatActivity {
public Button pt, doc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sorter);
        pt = (Button) findViewById(R.id.patient);
        doc = (Button) findViewById(R.id.doctor);
        pt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(sorter.this,patient_login.class);
                startActivity(i);

            }
        });


    }
}