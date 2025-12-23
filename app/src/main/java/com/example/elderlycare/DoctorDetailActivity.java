package com.example.elderlycare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.Locale;
import java.util.Map;

public class DoctorDetailActivity extends AppCompatActivity {

    TextView tvName, tvSpec, tvLicense, tvPhone, tvAddress, tvHours, tvDays;
    Button btnBook;
    ImageView btnCall, btnLocation;

    String doctorUid;
    DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        tvName = findViewById(R.id.tvName);
        tvSpec = findViewById(R.id.tvSpec);
        tvLicense = findViewById(R.id.tvLicense);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvHours = findViewById(R.id.tvHours);
        tvDays = findViewById(R.id.tvDays);

        btnBook = findViewById(R.id.btnBookAppointment);
        btnCall = findViewById(R.id.btnCallDoctor);
        btnLocation = findViewById(R.id.btnOpenMap);

        doctorUid = getIntent().getStringExtra("doctorUid");
        ref = FirebaseDatabase.getInstance().getReference("users");

        loadDoctorDetails();

        btnBook.setOnClickListener(v -> {
            Intent i = new Intent(DoctorDetailActivity.this, BookAppointmentActivity.class);
            i.putExtra("doctorUid", doctorUid);
            startActivity(i);
        });
    }

    private void loadDoctorDetails() {

        ref.child(doctorUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                DataSnapshot info = snapshot.child("doctor_info");
                DataSnapshot settings = snapshot.child("doctorSettings");

                // BASIC INFO
                String name = info.child("fullName").getValue(String.class);
                String spec = info.child("specialist").getValue(String.class);
                String license = info.child("licenseNumber").getValue(String.class);
                String phone = info.child("phone").getValue(String.class);
                String address = info.child("clinicAddress").getValue(String.class);
                String city = info.child("clinicCity").getValue(String.class);

                tvName.setText("Dr. " + name);
                tvSpec.setText(spec);
                tvLicense.setText("License: " + license);
                tvPhone.setText(phone);
                tvAddress.setText(address + ", " + city);

                // Working Hours
                String start = settings.child("workingHours/start").getValue(String.class);
                String end = settings.child("workingHours/end").getValue(String.class);
                tvHours.setText("Hours: " + start + " - " + end);

                // Working Days
                StringBuilder sb = new StringBuilder();
                Map<String, Boolean> workingDaysMap = new HashMap<>();

                for (DataSnapshot d : settings.child("workingDays").getChildren()) {
                    Boolean v = d.getValue(Boolean.class);
                    if (v != null && v) {
                        workingDaysMap.put(d.getKey().toLowerCase(Locale.ROOT), true);

                        if (sb.length() > 0) sb.append(", ");
                        sb.append(d.getKey());
                    }
                }

                tvDays.setText("Days: " + sb.toString());
            // ✅ CHECK AVAILABILITY BASED ON TODAY
                Calendar cal = Calendar.getInstance();
                String today = cal.getDisplayName(
                        Calendar.DAY_OF_WEEK,
                        Calendar.SHORT,
                        Locale.getDefault()
                ).toLowerCase(Locale.ROOT);

                 // Example: "mon", "tue", etc.
                boolean isAvailableToday = workingDaysMap.containsKey(today);

                // Show availability
                if (isAvailableToday) {
                    btnBook.setEnabled(true);
                    btnBook.setText("Book Appointment");
                } else {
                    btnBook.setEnabled(false);
                    btnBook.setText("Not Available Today");
                }


                // Call button
                btnCall.setOnClickListener(v -> {
                    Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(i);
                });

                // Google Maps (option A)
                btnLocation.setOnClickListener(v -> {
                    String query = Uri.encode(address + ", " + city);
                    Intent mapIntent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:0,0?q=" + query)
                    );
                    startActivity(mapIntent);
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
