package com.example.medpro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Find the buttons by ID
        Button chatWithAmoraButton = view.findViewById(R.id.sendtoAmora);
        Button startWorkout = view.findViewById(R.id.sendtoStayFit);

        // Initialize RecyclerView and adapter
        ArrayList<cycleData> arrayList = new ArrayList<>();
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        myAdapter adapter = new myAdapter(arrayList, getActivity());
        recyclerView.setAdapter(adapter);

        // Firebase reference
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("sessions");

        // Query to get data by email
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();  // Replace with the dynamic email if needed
        Query query = databaseReference.orderByChild("email").equalTo(userEmail);

        // Firebase data retrieval
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("HomeFragment", "onDataChange triggered");
                arrayList.clear(); // Clear previous data

                if (snapshot.exists()) {
                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        // Iterate over each session under the user
                        for (DataSnapshot sessionDataSnapshot : sessionSnapshot.getChildren()) {
                            cycleData data = sessionDataSnapshot.getValue(cycleData.class);
                            if (data != null) {
                                arrayList.add(data); // Add the session data to the list
                                Log.d("HomeFragment", "Session Data: " + sessionDataSnapshot.getKey());  // Log the session key
                                Log.d("HomeFragment", "AverageSpeed: " + data.getAverageSpeed() +
                                        ", CaloriesBurnt: " + data.getCaloriesBurnt() +
                                        ", DistanceTravelled: " + data.getDistanceTravelled() +
                                        ", TotalTime: " + data.getTotalTime());  // Log the session data

                                // Display a Toast for each data entry
                                Toast.makeText(getActivity(), "Data loaded: " + data.getAverageSpeed() + " km/h", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    adapter.notifyDataSetChanged(); // Notify the adapter to update RecyclerView
                    Toast.makeText(getActivity(), "Data loaded successfully", Toast.LENGTH_SHORT).show(); // Toast after data is loaded
                } else {
                    Log.d("HomeFragment", "No matching data found for the email.");
                    Toast.makeText(getActivity(), "No data found for the given email", Toast.LENGTH_SHORT).show(); // Toast when no data is found
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "DatabaseError: " + error.getMessage());
                Toast.makeText(getActivity(), "Error loading data", Toast.LENGTH_SHORT).show(); // Toast on error
            }
        });

        // Start workout activity
        startWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), stayfit_non_test.class);
                startActivity(i);
            }
        });

        // Open chat with Amora activity
        chatWithAmoraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), chatWithAI.class);
                startActivity(intent);
            }
        });

        return view;
    }
}
