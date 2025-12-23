package com.example.elderlycare;

import java.util.HashMap;
import java.util.Map;

public class DoctorModel {

    public String uid;
    public String fullName;
    public String specialist;
    public String licenseNumber;

    public String clinicCity;
    public String clinicAddress;
    public String phone;

    public double clinicLat = 0.0;
    public double clinicLng = 0.0;

    public String startTime;
    public String endTime;

    public Map<String, Boolean> workingDays = new HashMap<>();

    public String rating = "4.8";
    public String availability = "Available Today";

    // Calculated: distance in KM
    public double distanceKm = -1;

    public DoctorModel() {}
}
