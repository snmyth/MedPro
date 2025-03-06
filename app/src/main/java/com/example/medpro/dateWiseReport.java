package com.example.medpro;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class dateWiseReport extends AppCompatActivity {

    private TextView selectDateTextView, distanceTravelledTextView, caloriesBurntTextView, totalTimeTextView;
    private DatabaseReference databaseReference;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_date_wise_report);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        selectDateTextView = findViewById(R.id.selectDateTextView);
        distanceTravelledTextView = findViewById(R.id.distanceTravelledTextView);
        caloriesBurntTextView = findViewById(R.id.caloriesBurntTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("sessions").child(userId);

        selectDateTextView.setOnClickListener(view -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    String selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    selectDateTextView.setText("Selected Date: " + selectedDate);
                    fetchReportByDate(selectedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void fetchReportByDate(String selectedDate) {
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                double totalDistance = 0.0;
                double totalCalories = 0.0;
                long totalSeconds = 0;
                boolean dataFound = false;

                for (DataSnapshot sessionSnapshot : task.getResult().getChildren()) {
                    cycleData data = sessionSnapshot.getValue(cycleData.class);
                    if (data != null && selectedDate.equals(data.getDate())) {
                        totalDistance += data.getDistanceTravelled();
                        totalCalories += data.getCaloriesBurnt();
                        totalSeconds += data.getTotalTimeInSeconds();
                        dataFound = true;
                    }
                }

                if (dataFound) {
                    String formattedTime = formatSecondsToTime(totalSeconds);
                    distanceTravelledTextView.setText(String.format("%.2f km", totalDistance));
                    caloriesBurntTextView.setText(String.format("%.0f kcal", totalCalories));
                    totalTimeTextView.setText(formattedTime);
                    Toast.makeText(this, "Report Loaded", Toast.LENGTH_SHORT).show();
                } else {
                    distanceTravelledTextView.setText("0 km");
                    caloriesBurntTextView.setText("0 kcal");
                    totalTimeTextView.setText("00:00");
                    Toast.makeText(this, "No data for selected date", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatSecondsToTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
