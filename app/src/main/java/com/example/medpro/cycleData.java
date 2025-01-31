package com.example.medpro;

public class cycleData {

    private double averageSpeed;      // Change to double for numeric data
    private double caloriesBurnt;     // Change to double for numeric data
    private double distanceTravelled; // Change to double for numeric data
    private String totalTime;         // Keep as String if time is stored as a String
    private String email;             // Keep as String for email

    public cycleData() {
        // Default constructor required for Firebase
    }

    // Getter and Setter methods for each field
    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public double getCaloriesBurnt() {
        return caloriesBurnt;
    }

    public void setCaloriesBurnt(double caloriesBurnt) {
        this.caloriesBurnt = caloriesBurnt;
    }

    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(double distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(String totalTime) {
        this.totalTime = totalTime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
