package com.example.elderlycare;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ReportsAnalyticsDashboard extends AppCompatActivity {

    ProgressBar progressMedicine;
    TextView tvMedicineSummary, tvAppointmentSummary, tvHealthStatus, tvAlertSummary;

    DatabaseReference medicineRef, appointmentRef;
    String elderUid;

    int totalMeds = 0, takenMeds = 0;
    int completed = 0, upcoming = 0, rejected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_analytics_dashboard);

        elderUid = FirebaseAuth.getInstance().getUid();

        progressMedicine = findViewById(R.id.progressMedicine);
        tvMedicineSummary = findViewById(R.id.tvMedicineSummary);
        tvAppointmentSummary = findViewById(R.id.tvAppointmentSummary);
        tvHealthStatus = findViewById(R.id.tvHealthStatus);
        tvAlertSummary = findViewById(R.id.tvAlertSummary);

        medicineRef = FirebaseDatabase.getInstance()
                .getReference("medicine_schedule")
                .child(elderUid);

        appointmentRef = FirebaseDatabase.getInstance()
                .getReference("user_appointments")
                .child(elderUid);

        loadMedicineData();
        loadAppointmentData();
    }

    private void loadMedicineData() {
        medicineRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                totalMeds = 0;
                takenMeds = 0;

                for (DataSnapshot s : snapshot.getChildren()) {
                    totalMeds++;
                    Boolean taken = s.child("taken").getValue(Boolean.class);
                    if (taken != null && taken) takenMeds++;
                }

                int percent = totalMeds == 0 ? 0 : (takenMeds * 100 / totalMeds);
                progressMedicine.setProgress(percent);
                tvMedicineSummary.setText(
                        "Taken " + takenMeds + " of " + totalMeds + " (" + percent + "%)"
                );

                updateHealthStatus(percent);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAppointmentData() {
        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                completed = upcoming = rejected = 0;

                for (DataSnapshot s : snapshot.getChildren()) {
                    String status = s.child("status").getValue(String.class);
                    if (status == null) continue;

                    switch (status) {
                        case "Completed": completed++; break;
                        case "Confirmed":
                        case "Pending": upcoming++; break;
                        case "Rejected": rejected++; break;
                    }
                }

                tvAppointmentSummary.setText(
                        "Completed: " + completed +
                                " | Upcoming: " + upcoming +
                                " | Rejected: " + rejected
                );

                tvAlertSummary.setText(
                        "Appointment reminders sent: " + (completed + upcoming)
                );
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateHealthStatus(int adherencePercent) {
        if (adherencePercent >= 80) {
            tvHealthStatus.setText("Status: Good");
            tvHealthStatus.setTextColor(0xFF2E7D32);
        } else if (adherencePercent >= 50) {
            tvHealthStatus.setText("Status: Moderate");
            tvHealthStatus.setTextColor(0xFFF9A825);
        } else {
            tvHealthStatus.setText("Status: Needs Attention");
            tvHealthStatus.setTextColor(0xFFC62828);
        }
    }
}
