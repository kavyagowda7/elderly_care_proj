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

public class SetNightTimeActivity extends BaseActivity {

    private TimePicker timePickerNight;
    private Button btnNext;
    private FirebaseAuth auth;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_night_time);

        // ✅ Add toolbar back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Night Medication Time");
        }

        // ✅ Initialize UI
        timePickerNight = findViewById(R.id.timePickerNight);
        btnNext = findViewById(R.id.btnNext);

        // ✅ Firebase setup
        auth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        // ✅ Handle Next button click
        btnNext.setOnClickListener(v -> {
            int hour = timePickerNight.getHour();
            int minute = timePickerNight.getMinute();

            String uid = auth.getCurrentUser().getUid();
            Map<String, Object> data = new HashMap<>();
            data.put("nightTime", hour + ":" + String.format("%02d", minute));

            ref.child(uid).child("medicationSchedule").updateChildren(data)
                    .addOnSuccessListener(unused -> {
                        FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("users")
                                .child(uid)
                                .child("onboardingStatus").child("nightTimeDone")

                                .setValue(true);

                        Toast.makeText(this, "Night time saved!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, ReminderToneActivity.class);
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
            onBackPressed(); // Takes user back to the previous page (Afternoon)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
