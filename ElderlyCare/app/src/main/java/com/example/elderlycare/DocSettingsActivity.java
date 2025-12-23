package com.example.elderlycare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class DocSettingsActivity extends AppCompatActivity {

    Switch switchPush, switchEmail, switchSound;
    TimePicker startTimePicker, endTimePicker;
    Button btnUpdateHours;

    FirebaseAuth auth;
    DatabaseReference doctorRef;
    ToggleButton dayMon, dayTue, dayWed, dayThu, dayFri, daySat, daySun;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_settings);

        auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();

        doctorRef = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users").child(uid).child("doctorSettings");

        switchPush = findViewById(R.id.switchPush);
        switchEmail = findViewById(R.id.switchEmail);
        switchSound = findViewById(R.id.switchSound);

        startTimePicker = findViewById(R.id.startTimePicker);
        endTimePicker = findViewById(R.id.endTimePicker);

        dayMon = findViewById(R.id.dayMon);
        dayTue = findViewById(R.id.dayTue);
        dayWed = findViewById(R.id.dayWed);
        dayThu = findViewById(R.id.dayThu);
        dayFri = findViewById(R.id.dayFri);
        daySat = findViewById(R.id.daySat);
        daySun = findViewById(R.id.daySun);


        btnUpdateHours = findViewById(R.id.btnUpdateHours);

        btnUpdateHours.setOnClickListener(v -> saveWorkingHours());
    }

    private void saveWorkingHours() {

        int startHour = startTimePicker.getHour();
        int startMin = startTimePicker.getMinute();

        int endHour = endTimePicker.getHour();
        int endMin = endTimePicker.getMinute();

        doctorRef.child("workingHours").child("start")
                .setValue(startHour + ":" + String.format("%02d", startMin));

        doctorRef.child("workingHours").child("end")
                .setValue(endHour + ":" + String.format("%02d", endMin));

        // Save Working Days
        HashMap<String, Boolean> days = new HashMap<>();
        days.put("Mon", dayMon.isChecked());
        days.put("Tue", dayTue.isChecked());
        days.put("Wed", dayWed.isChecked());
        days.put("Thu", dayThu.isChecked());
        days.put("Fri", dayFri.isChecked());
        days.put("Sat", daySat.isChecked());
        days.put("Sun", daySun.isChecked());

        doctorRef.child("workingDays").setValue(days);

        Toast.makeText(this, "Working schedule updated!", Toast.LENGTH_SHORT).show();
    }

}
