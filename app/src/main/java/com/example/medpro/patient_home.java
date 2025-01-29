package com.example.medpro;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class patient_home extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_home);

        // Set up Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                // Use if-else to determine which fragment to show
                if (item.getItemId() == R.id.bottom_home) {
                    selectedFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.bottom_ai) {
                    Intent i = new Intent(patient_home.this, chatWithAI.class);
                    startActivity(i);
                    return true; // Don't load fragment if launching another activity
                } else if (item.getItemId() == R.id.bottom_stayFit) {
                    selectedFragment = new StayFitFragment();
                } else if (item.getItemId() == R.id.bottom_user) {
                    selectedFragment = new AccountFragment();
                }

                // Check if the selected fragment is null (meaning no valid selection)
                if (selectedFragment == null) {
                    selectedFragment = new HomeFragment(); // Load default HomeFragment if no valid selection
                }

                loadFragment(selectedFragment);
                return true;
            }
        });

        // Load the default fragment on first launch
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.bottom_home); // Default to HomeFragment
        }
    }

    // Helper method to load fragments
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // Ensure this container exists in your layout
        transaction.commit();
    }
}
