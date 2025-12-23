package com.example.elderlycare;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class PatientAppointmentsActivity extends AppCompatActivity {

    RecyclerView recyclerAppointments;
    List<AppointmentModel> list = new ArrayList<>();
    PatientAppointmentAdapter adapter;

    Button btnBookNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_appointments);

        recyclerAppointments = findViewById(R.id.recyclerAppointments);
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAppointmentAdapter(this, list);
        recyclerAppointments.setAdapter(adapter);

        btnBookNew = findViewById(R.id.btnBookNew);
        btnBookNew.setOnClickListener(v -> {
            finish();
        });

        loadAppointments();
    }

    private void loadAppointments() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String url = "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app";

        DatabaseReference ref = FirebaseDatabase.getInstance(url)
                .getReference("user_appointments")
                .child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    AppointmentModel m = s.getValue(AppointmentModel.class);
                    if (m != null && m.status.equalsIgnoreCase("Confirmed")) {
                        list.add(m);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
