package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TeleConsultationActivity extends AppCompatActivity {

    RecyclerView recyclerVideoList;
    LinearLayout emptyCard;

    List<AppointmentModel> videoAppointments = new ArrayList<>();
    VideoAppointmentAdapter adapter;

    DatabaseReference ref;
    String patientUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teleconsultation);

        patientUid = FirebaseAuth.getInstance().getUid();

        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("user_appointments").child(patientUid);

        recyclerVideoList = findViewById(R.id.recyclerVideoAppointments);
        recyclerVideoList.setLayoutManager(new LinearLayoutManager(this));

        emptyCard = findViewById(R.id.emptyCard);

        adapter = new VideoAppointmentAdapter(this, videoAppointments);
        recyclerVideoList.setAdapter(adapter);

        loadVideoAppointments();

        findViewById(R.id.backBtn).setOnClickListener(v -> onBackPressed());
    }

    private void loadVideoAppointments() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                videoAppointments.clear();

                String todayStr = new java.text.SimpleDateFormat(
                        "dd-MM-yyyy",
                        java.util.Locale.getDefault()
                ).format(new java.util.Date());

                for (DataSnapshot s : snapshot.getChildren()) {
                    AppointmentModel m = s.getValue(AppointmentModel.class);
                    if (m == null || m.status == null) continue;

                    if (!m.type.equalsIgnoreCase("Video Call")) continue;
                    if (m.status.equalsIgnoreCase("Rejected")) continue;

                    // ✅ AUTO-COMPLETE ONLY AFTER DAY ENDS (CONFIRMED ONLY)
                    if (m.status.equalsIgnoreCase("Confirmed")
                            && m.date.compareTo(todayStr) < 0) {

                        s.getRef().child("status").setValue("Completed");
                        m.status = "Completed";
                    }

                    // ❌ DO NOT SHOW COMPLETED VIDEO CALLS HERE
                    if (m.status.equalsIgnoreCase("Completed")) continue;

                    videoAppointments.add(m);
                }

                // ✅ SORT: Pending → Confirmed → by date
                Collections.sort(videoAppointments, (a, b) -> {
                    if (a.status.equalsIgnoreCase("Pending") &&
                            !b.status.equalsIgnoreCase("Pending"))
                        return -1;

                    if (!a.status.equalsIgnoreCase("Pending") &&
                            b.status.equalsIgnoreCase("Pending"))
                        return 1;

                    return a.date.compareTo(b.date);
                });

                emptyCard.setVisibility(videoAppointments.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
