package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class MedicationIntroActivity extends AppCompatActivity {

    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_intro);

        nextButton = findViewById(R.id.btnNext);

        nextButton.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("users")
                    .child(uid)
                    .child("onboardingStatus")
                    .child("medicationIntroDone")
                    .setValue(true);

            Intent intent = new Intent(MedicationIntroActivity.this, SetMorningTimeActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        });
    }
}
