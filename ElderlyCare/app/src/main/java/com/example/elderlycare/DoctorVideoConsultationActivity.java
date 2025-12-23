package com.example.elderlycare;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DoctorVideoConsultationActivity extends AppCompatActivity {

    RecyclerView recycler;
    LinearLayout emptyView;

    List<AppointmentModel> list = new ArrayList<>();
    DoctorVideoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_teleconsult);

        recycler = findViewById(R.id.recyclerDoctorVideoAppointments);
        emptyView = findViewById(R.id.emptyDoctorVideo);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorVideoAdapter(this, list);
        recycler.setAdapter(adapter);

        findViewById(R.id.backBtn).setOnClickListener(v -> onBackPressed());

        loadVideoAppointments();
    }

    private void loadVideoAppointments() {
        String uid = FirebaseAuth.getInstance().getUid();

        FirebaseDatabase.getInstance(
                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                ).getReference("appointments")
                .child(uid)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();

                        for (DataSnapshot s : snapshot.getChildren()) {
                            AppointmentModel m = s.getValue(AppointmentModel.class);

                            if (m != null &&
                                    m.type.equalsIgnoreCase("Video Call") &&
                                    !m.status.equalsIgnoreCase("Rejected")) {

                                list.add(m);
                            }
                        }

                        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
