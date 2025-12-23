package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SetAfternoonTimeActivity extends BaseActivity {

    private TimePicker timePickerAfternoon;
    private Button btnNext;
    private FirebaseAuth auth;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_afternoon_time);

        // ✅ Add back arrow in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Afternoon Medication Time");
        }

        timePickerAfternoon = findViewById(R.id.timePickerAfternoon);
        btnNext = findViewById(R.id.btnNext);

        auth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        btnNext.setOnClickListener(v -> {
            int hour = timePickerAfternoon.getHour();
            int minute = timePickerAfternoon.getMinute();

            String uid = auth.getCurrentUser().getUid();
            Map<String, Object> data = new HashMap<>();
            data.put("afternoonTime", hour + ":" + String.format("%02d", minute));

            ref.child(uid).child("medicationSchedule").updateChildren(data)
                    .addOnSuccessListener(unused -> {
                        FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("users")
                                .child(uid)
                                .child("onboardingStatus").child("afternoonTimeDone")

                                .setValue(true);

                        Toast.makeText(this, "Afternoon time saved!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, SetNightTimeActivity.class);
                        startActivity(intent);
//                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // ✅ Handle toolbar back arrow click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Takes user back to the previous screen (SetMorningTimeActivity)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
