package com.example.medpro;

public class cycleData {

    private String averageSpeed;
    private String caloriesBurnt;
    private String distanceTravelled;
    private String totalTime;
    private String email; // We will store the user's email here

    public cycleData() {
        // Default constructor required for Firebase
    }

    // Getter and Setter methods for each field
    public String getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(String averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public String getCaloriesBurnt() {
        return caloriesBurnt;
    }

    public void setCaloriesBurnt(String caloriesBurnt) {
        this.caloriesBurnt = caloriesBurnt;
    }

    public String getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(String distanceTravelled) {
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
