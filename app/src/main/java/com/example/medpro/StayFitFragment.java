package com.example.medpro;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StayFitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StayFitFragment extends Fragment {

    // Fragment initialization parameters
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public StayFitFragment() {
        // Required empty public constructor
    }

    public static StayFitFragment newInstance(String param1, String param2) {
        StayFitFragment fragment = new StayFitFragment();
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
        View view = inflater.inflate(R.layout.fragment_stay_fit, container, false);

        // Button to navigate to Bluetooth activity
        Button bluetoothButton = view.findViewById(R.id.button5);
        bluetoothButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), bluetoothStayfir.class);
            startActivity(intent);
        });

        return view;
    }
}
