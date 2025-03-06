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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                arrayList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        cycleData data = sessionSnapshot.getValue(cycleData.class);
                        if (data != null && userEmail.equals(data.getEmail())) {
                            arrayList.add(data);  // Add only if emails match
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getActivity(), "Data loaded successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "No sessions found for the user.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Database error: " + error.getMessage());
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

        Button getReportButton = view.findViewById(R.id.getReport);
        getReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), dateWiseReport.class);
            startActivity(intent);
        });


        return view;
    }
}
