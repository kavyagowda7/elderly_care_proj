package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SetMorningTimeActivity extends AppCompatActivity {

    private TimePicker timePickerMorning;
    private Button btnNext;
    private FirebaseAuth auth;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_morning_time);



        timePickerMorning = findViewById(R.id.timePickerMorning);
        btnNext = findViewById(R.id.btnNext);

        auth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        btnNext.setOnClickListener(v -> {
            int hour = timePickerMorning.getHour();
            int minute = timePickerMorning.getMinute();

            String uid = auth.getCurrentUser().getUid();
            Map<String, Object> data = new HashMap<>();
            data.put("morningTime", hour + ":" + String.format("%02d", minute));

            ref.child(uid).child("medicationSchedule").updateChildren(data)
                    .addOnSuccessListener(unused -> {
                        FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("users")
                                .child(uid)
                                .child("onboardingStatus").child("morningTimeDone")

                                .setValue(true);
                        Intent intent = new Intent(this, SetAfternoonTimeActivity.class);
                        startActivity(intent);
//                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
