package com.example.medpro;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.widget.NumberPicker;

import androidx.fragment.app.Fragment;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AccountFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountFragment.
     */
    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
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
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Find the ConstraintLayout by ID
        final ConstraintLayout constraintLayout = view.findViewById(R.id.constraintLayout);

        // Set an OnClickListener to animate the view on click
        constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Create an ObjectAnimator for translation (flip effect)
                ObjectAnimator animator = ObjectAnimator.ofFloat(constraintLayout, "translationX", 0f, 50f);  // 50f is the amount of movement
                animator.setDuration(300);  // Duration of the animation
                animator.setRepeatCount(1);  // Repeat once to go back to the original position
                animator.setRepeatMode(ObjectAnimator.REVERSE);  // Flip the direction after the first movement
                animator.start();  // Start the animation
            }
        });
        FirebaseAuth auth;
        auth = FirebaseAuth.getInstance();
        FirebaseUser user;
        user = auth.getCurrentUser();
        Button myButton = view.findViewById(R.id.button3);
        TextView tv = view.findViewById(R.id.gmailString);
        tv.setText(user.getEmail());

        TextView height, weight, age, phone;
        height = view.findViewById(R.id.heightValue);
        weight = view.findViewById(R.id.weightValue);
        age = view.findViewById(R.id.ageValue);
        phone = view.findViewById(R.id.phone);
        userDetailsDBHelper udb = new userDetailsDBHelper(getActivity());
        Cursor c = udb.getDataByGmail(user.getEmail());




        height.setText("Not Set");
        weight.setText("Not Set");
        age.setText("Not Set");
        phone.setText("Not Set");

        Button addButton;
        addButton =view.findViewById(R.id.addInfoButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), updateInfo.class);
                startActivity(i);
            }
        });

        // Set an OnClickListener for the button
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                 Toast.makeText(getActivity(), "Logging Out", Toast.LENGTH_SHORT).show();
                Toast.makeText(getActivity(), "Logged Out of "+user.getEmail(), Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
