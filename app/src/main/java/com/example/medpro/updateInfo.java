package com.example.medpro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class updateInfo extends AppCompatActivity {
    userDetailsDBHelper udb;

    NumberPicker age, height, weight;
    EditText mobile;
    Button submit;
    FirebaseAuth auth;

    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_info);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        age = findViewById(R.id.numberPickerAge);
        height = findViewById(R.id.numberPickerHeight);
        weight = findViewById(R.id.numberPickerWeight);
        mobile = findViewById(R.id.editTextMobile);
        age.setMinValue(18);
        age.setMaxValue(100);
        height.setMinValue(100);
        height.setMaxValue(250);
        weight.setMinValue(20);
        weight.setMaxValue(200);

        submit = findViewById(R.id.submitButton);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ageValue = String.valueOf(age.getValue());
                String heightValue = String.valueOf(height.getValue());
                String weightValue = String.valueOf(weight.getValue());
                String mobileValue = mobile.getText().toString();
              if (ageValue == null || ageValue.isEmpty())
              {
                  Toast.makeText(updateInfo.this, "Please Enter Age", Toast.LENGTH_SHORT).show();

              }
              else if (heightValue == null || heightValue.isEmpty()) {
                  Toast.makeText(updateInfo.this, "Please Enter Height", Toast.LENGTH_SHORT).show();
              }
              else if (weightValue == null || weightValue.isEmpty()) {
                  Toast.makeText(updateInfo.this, "Please Enter Weight", Toast.LENGTH_SHORT).show();
              }
              else if (mobileValue == null || mobileValue.isEmpty()) {
                  Toast.makeText(updateInfo.this, "Please Enter Mobile Number", Toast.LENGTH_SHORT).show();
              }
              else {
                  udb.insertData(age.getValue(), height.getValue(), weight.getValue(), mobile.getText().toString(), user.getEmail());

                  Toast.makeText(updateInfo.this, "Updated Successfully", Toast.LENGTH_SHORT).show();
              }


            }

        });






    }
}