package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(this::checkFlow, 1500);
    }

    private void checkFlow() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // User not logged in → go to onboarding
        if (user == null) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        String uid = user.getUid();
        String role=";";
        DatabaseReference userRef;
         FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                     @Override
                     public void onDataChange(@NonNull DataSnapshot snapshot) {
                         String role=snapshot.child("role").getValue().toString();
                         switch (role) {

                             case "Elder":
                                 
                                 DataSnapshot onboardingSnapshot = snapshot.child("onboardingStatus");

                                 boolean patient = getFlag(onboardingSnapshot, "patientInfoDone");
                                 boolean intro = getFlag(onboardingSnapshot, "medicationIntroDone");
                                 boolean morning = getFlag(onboardingSnapshot, "morningTimeDone");
                                 boolean afternoon = getFlag(onboardingSnapshot, "afternoonTimeDone");
                                 boolean night = getFlag(onboardingSnapshot, "nightTimeDone");
                                 boolean tone = getFlag(onboardingSnapshot, "reminderToneDone");


                                 if (!patient) {
                                     Toast.makeText(SplashActivity.this, "opening form here", Toast.LENGTH_SHORT).show();
                                     startActivity(new Intent(SplashActivity.this, PatientInfoActivity.class));
                                 }
                                 else if (!intro) {
                                     startActivity(new Intent(SplashActivity.this, MedicationIntroActivity.class));
                                 }
                                 else if (!morning) {
                                     startActivity(new Intent(SplashActivity.this, SetMorningTimeActivity.class));
                                 }
                                 else if (!afternoon) {
                                     startActivity(new Intent(SplashActivity.this, SetAfternoonTimeActivity.class));
                                 }
                                 else if (!night) {
                                     startActivity(new Intent(SplashActivity.this, SetNightTimeActivity.class));
                                 }
                                 else if (!tone) {
                                     startActivity(new Intent(SplashActivity.this, ReminderToneActivity.class));
                                 }
                                 else {
                                     startActivity(new Intent(SplashActivity.this, ElderDashboardActivity.class));
                                 }

                                 finish();
                                 break;


                             case "Doctor":
                                 if (snapshot.child("doctor_info").exists()) {
                                     startActivity(new Intent(SplashActivity.this, DoctorDashboardActivity.class));
                                 } else {
                                     startActivity(new Intent(SplashActivity.this, DoctorRegistrationActivity.class));
                                 }

                                 finish();
                                 return;

                             case "Family":
                                 startActivity(new Intent(SplashActivity.this, FamilyDashboardActivity.class));
                                 finish();
                                 break;

                             default:
                                 Toast.makeText(SplashActivity.this, "Invalid role.", Toast.LENGTH_SHORT).show();
                         }
                     }

                     @Override
                     public void onCancelled(@NonNull DatabaseError error) {

                     }
                 });
//        Log.d("user id",FirebaseAuth.getInstance().getUid());
//        // Read onboardingStatus
//        FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
//                .getReference("users")
//                .child(uid)
//                .child("onboardingStatus")
//                .get()
//                .addOnSuccessListener(snapshot -> {
//
//                    boolean patient = getFlag(snapshot, "patientInfoDone");
//                    boolean intro = getFlag(snapshot, "medicationIntroDone");
//                    boolean morning = getFlag(snapshot, "morningTimeDone");
//                    boolean afternoon = getFlag(snapshot, "afternoonTimeDone");
//                    boolean night = getFlag(snapshot, "nightTimeDone");
//                    boolean tone = getFlag(snapshot, "reminderToneDone");
//
//
//
//                    // Navigate based on incomplete flags
//                    if (!patient && !snapshot.child("role").getValue().toString().equalsIgnoreCase("doctor")) {
//                        Toast.makeText(this, "opening fomr here" +
//                                "", Toast.LENGTH_SHORT).show();
//
//                        startActivity(new Intent(this, PatientInfoActivity.class));
//
//                    }
//                    else if (!intro) {
//                        startActivity(new Intent(this, MedicationIntroActivity.class));
//                    }
//                    else if (!morning) {
//                        startActivity(new Intent(this, SetMorningTimeActivity.class));
//                    }
//                    else if (!afternoon) {
//                        startActivity(new Intent(this, SetAfternoonTimeActivity.class));
//                    }
//                    else if (!night) {
//                        startActivity(new Intent(this, SetNightTimeActivity.class));
//                    }
//                    else if (!tone) {
//                        startActivity(new Intent(this, ReminderToneActivity.class));
//                    }
//                    else {
//                        // All steps completed!
//                        startActivity(new Intent(this, ElderDashboardActivity.class));
//                    }
//
//                    finish();
//                });
    }

    private boolean getFlag(DataSnapshot snapshot, String key) {
        Boolean value = snapshot.child(key).getValue(Boolean.class);
        return value != null && value;
    }
}
