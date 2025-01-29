package com.example.medpro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class myAdapter extends RecyclerView.Adapter<myAdapter.myViewHolder> {

    ArrayList<cycleData> cycleData;
    Context context;

    public myAdapter(ArrayList<cycleData> cycleData, Context context) {
        this.cycleData = cycleData;
        this.context = context;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        return new myViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        cycleData data = cycleData.get(position);

        holder.averageSpeedText.setText(data.getAverageSpeed());
        holder.caloriesBurntText.setText(data.getCaloriesBurnt());
        holder.distanceTravelledText.setText(data.getDistanceTravelled());
        holder.totalTimeText.setText(data.getTotalTime());
    }

    @Override
    public int getItemCount() {
        return cycleData.size();
    }

    public static class myViewHolder extends RecyclerView.ViewHolder {
        TextView averageSpeedText, caloriesBurntText, distanceTravelledText, totalTimeText;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            averageSpeedText = itemView.findViewById(R.id.averageSpeedText);
            caloriesBurntText = itemView.findViewById(R.id.caloriesBurntText);
            distanceTravelledText = itemView.findViewById(R.id.distanceTravelledText);
            totalTimeText = itemView.findViewById(R.id.totalTimeText);
        }
    }
}
