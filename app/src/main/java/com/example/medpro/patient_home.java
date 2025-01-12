package com.example.medpro;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class patient_home extends AppCompatActivity {

    DrawerLayout drawer;
    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;
    FloatingActionButton fab;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_home);

        // Set up Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up Floating Action Button
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(patient_home.this, chatWithAI.class);
                startActivity(i);
            }
        });

        // Set up BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment selectedFragment = null;

                        // Use if-else instead of switch
                        if (item.getItemId() == R.id.bottom_home) {
                            selectedFragment = new HomeFragment();
                        } else if (item.getItemId() == R.id.bottom_appointment) {
                            selectedFragment = new appointmentFragment();
                        } else if (item.getItemId() == R.id.bottom_stayFit) {
                            selectedFragment = new StayFitFragment();
                        } else if (item.getItemId() == R.id.bottom_user) {
                            selectedFragment = new AccountFragment();
                        }

                        if (selectedFragment != null) {
                            loadFragment(selectedFragment);
                            return true;
                        }

                        return false;
                    }
                });


                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }

                return false;
            }
        });

        // Load the default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.bottom_home);
        }
    }

    // Helper method to load fragments
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // Ensure this container exists in your layout
        transaction.commit();
    }
}
