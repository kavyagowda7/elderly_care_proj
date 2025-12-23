package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ReminderTypeActivity extends AppCompatActivity {

    private RadioGroup reminderGroup;
    private Button btnNext;
    private FirebaseAuth auth;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_type);

        reminderGroup = findViewById(R.id.alarmTypeGroup);
        btnNext = findViewById(R.id.btnNext);

        auth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        btnNext.setOnClickListener(v -> {
            int selectedId = reminderGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a reminder type", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selected = findViewById(selectedId);
            String reminderType = selected.getText().toString();

            String uid = auth.getCurrentUser().getUid();
            ref.child(uid).child("medicationSchedule").child("reminderType").setValue(reminderType)
                    .addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent(this, ReminderToneActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    });
        });
    }
}
