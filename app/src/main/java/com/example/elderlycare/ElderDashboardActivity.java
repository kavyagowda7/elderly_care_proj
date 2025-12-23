package com.example.elderlycare;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import android.app.AlarmManager;
import android.provider.Settings;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.firebase.messaging.FirebaseMessaging;
import okhttp3.OkHttpClient;

import org.json.JSONObject;




public class ElderDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private String uid;

    private static final int REQ_POST_NOTIFICATIONS = 101;   // ← existing code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elder_dashboard);

        new Thread(() -> {
            try {
                URL url = new URL("https://YOUR_RENDER_URL/run-reminder-check");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1001
            );
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // SAVE FCM TOKEN FOR ELDER (PATIENT)
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {

                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid == null) return;

                    OkHttpClient client = new OkHttpClient();

                    JSONObject json = new JSONObject();
                    try {
                        json.put("uid", uid);
                        json.put("token", token);
                    } catch (Exception ignored) {}

                    RequestBody body = RequestBody.create(
                            json.toString(),
                            MediaType.get("application/json; charset=utf-8")
                    );

                    Request request = new Request.Builder()
                            .url("https://elderlycare-backend-qrvl.onrender.com/save-token")
                            .post(body)
                            .build();

                    client.newCall(request).enqueue(new okhttp3.Callback() {
                        @Override public void onFailure(okhttp3.Call call, IOException e) {}
                        @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {}
                    });
                });



        // Ask for notification permission on Android 13+
        ensureNotificationPermission();



        findViewById(R.id.cardDashboard).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, ReportsAnalyticsDashboard.class));
        });

        findViewById(R.id.cardUploadReports).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, DocumentsListActivity.class));
        });

        findViewById(R.id.cardTele).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, TeleConsultationActivity.class));
        });

        findViewById(R.id.cardAppointments).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, ElderAppointmentsActivity.class));
        });

        // Scan card click
        findViewById(R.id.cardScan).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, ScanReceiptActivity.class));
        });

        // My Medicines card click
        findViewById(R.id.cardMedicines).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, MyMedicinesActivity.class));
        });

        findViewById(R.id.cardDoctors).setOnClickListener(v -> {
            startActivity(new Intent(ElderDashboardActivity.this, FindDoctorsActivity.class));
        });

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid != null) {
            userRef = FirebaseDatabase.getInstance(
                    "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
            ).getReference("users").child(uid).child("patient_info");
        }

        // Open drawer
        findViewById(R.id.menuButton).setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );

        // Load header data
        updateHeaderData();

        // Handle menu click
        navigationView.setNavigationItemSelectedListener(item -> {
            handleMenuClick(item);
            return true;
        });

        //testing alarm
//        findViewById(R.id.cardMedicines).setOnLongClickListener(v -> {
//
//            Intent i = new Intent(ElderDashboardActivity.this, ReminderReceiver.class);
//            i.putExtra("medName", "TEST MED");
//            i.putExtra("dosage", "1 Tablet");
//
//            PendingIntent pi = PendingIntent.getBroadcast(
//                    ElderDashboardActivity.this,
//                    999,
//                    i,
//                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//            );
//
//            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
//
//            if (am != null) {
//                am.setExactAndAllowWhileIdle(
//                        AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + 10_000, // 10 seconds
//                        pi
//                );
//            }
//
//            Toast.makeText(this, "Test alarm scheduled (10s)", Toast.LENGTH_SHORT).show();
//            return true;
//        });




    }

    // ✔ existing method
    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQ_POST_NOTIFICATIONS) {
            // User granted or denied — safe
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void updateHeaderData() {
        if (userRef == null) return;

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue(String.class);
                String age = snapshot.child("age").getValue(String.class);

                TextView profileName = navigationView.getHeaderView(0).findViewById(R.id.profileName);
                TextView profileAge = navigationView.getHeaderView(0).findViewById(R.id.profileAge);

                if (name != null) profileName.setText(name);
                if (age != null) profileAge.setText("Age: " + age);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void handleMenuClick(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));

        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, ProfileNotificationActivity.class));

        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawers();
    }








}
