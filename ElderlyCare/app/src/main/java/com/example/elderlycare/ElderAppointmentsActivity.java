package com.example.elderlycare;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;

public class ElderAppointmentsActivity extends AppCompatActivity {

    TextView tabUpcoming, tabPast, tabRejected;
    RecyclerView recyclerView;
    LinearLayout emptyState;

    List<AppointmentModel> all = new ArrayList<>();
    List<AppointmentModel> filtered = new ArrayList<>();

    ElderAppointmentAdapter adapter;

    DatabaseReference ref;
    String patientUid;

    // ⭐ TRACK CURRENT TAB
    private String currentTab = "upcoming";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elder_appointments);

        patientUid = FirebaseAuth.getInstance().getUid();

        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("user_appointments").child(patientUid);

        initViews();
        loadAppointments();
        setupTabs();
        enableSwipeToDelete();
    }

    private void initViews() {
        tabUpcoming = findViewById(R.id.tabUpcoming);
        tabPast = findViewById(R.id.tabPast);
        tabRejected = findViewById(R.id.tabRejected);

        recyclerView = findViewById(R.id.recyclerElderAppointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyState = findViewById(R.id.emptyState);

        adapter = new ElderAppointmentAdapter(this, filtered);
        recyclerView.setAdapter(adapter);
    }

    // ⭐⭐ FIXED – TAB SWITCHING ONLY WHEN USER CLICKS ⭐⭐
    private void setupTabs() {
        tabUpcoming.setOnClickListener(v -> {
            currentTab = "upcoming";
            applyFilter(currentTab);
        });

        tabPast.setOnClickListener(v -> {
            currentTab = "past";
            applyFilter(currentTab);
        });

        tabRejected.setOnClickListener(v -> {
            currentTab = "rejected";
            applyFilter(currentTab);
        });
    }

    private void loadAppointments() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                all.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    AppointmentModel ap = s.getValue(AppointmentModel.class);
                    if (ap != null) all.add(ap);
                }

                // ⭐ DO NOT SWITCH TABS — Stay on currentTab
                applyFilter(currentTab);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilter(String type) {
        filtered.clear();

        for (AppointmentModel m : all) {
            switch (type) {
                case "upcoming":
                    if (!m.status.equalsIgnoreCase("Rejected") &&
                            !m.status.equalsIgnoreCase("Completed"))
                        filtered.add(m);
                    break;

                case "past":
                    if (m.status.equalsIgnoreCase("Completed") ||
                            m.status.equalsIgnoreCase("Rescheduled"))
                        filtered.add(m);
                    break;

                case "rejected":
                    if (m.status.equalsIgnoreCase("Rejected"))
                        filtered.add(m);
                    break;
            }
        }

        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        highlightTab(type);
    }

    private void highlightTab(String t) {
        tabUpcoming.setBackground(null);
        tabPast.setBackground(null);
        tabRejected.setBackground(null);

        tabUpcoming.setTextColor(0xFF555555);
        tabPast.setTextColor(0xFF555555);
        tabRejected.setTextColor(0xFF555555);

        TextView selected = t.equals("upcoming") ? tabUpcoming :
                t.equals("past") ? tabPast : tabRejected;

        selected.setTextColor(0xFFFFFFFF);
        selected.setBackground(getDrawable(R.drawable.tab_selected));
    }

    // swipe delete (unchanged except adding refresh using currentTab)
    private void enableSwipeToDelete() {

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                    ColorDrawable background = new ColorDrawable(Color.parseColor("#E53935"));

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int pos = viewHolder.getAdapterPosition();
                        AppointmentModel m = filtered.get(pos);

                        if (!(m.status.equalsIgnoreCase("Completed") ||
                                m.status.equalsIgnoreCase("Rescheduled") ||
                                m.status.equalsIgnoreCase("Rejected"))) {

                            adapter.notifyItemChanged(pos);
                            Toast.makeText(ElderAppointmentsActivity.this,
                                    "Only past or rejected appointments can be deleted",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // delete nodes
                        DatabaseReference doctorRef = FirebaseDatabase.getInstance(
                                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                        ).getReference("appointments").child(m.doctorUid).child(m.appointmentId);

                        DatabaseReference userRef = FirebaseDatabase.getInstance(
                                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                        ).getReference("user_appointments").child(patientUid).child(m.appointmentId);

                        doctorRef.removeValue();
                        userRef.removeValue();

                        filtered.remove(pos);
                        adapter.notifyItemRemoved(pos);

                        Toast.makeText(ElderAppointmentsActivity.this, "Deleted", Toast.LENGTH_SHORT).show();

                        // ⭐ Stay on the SAME TAB
                        applyFilter(currentTab);
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c,
                                            @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState,
                                            boolean isCurrentlyActive) {

                        View itemView = viewHolder.itemView;
                        background.setBounds(itemView.getRight() + (int)dX,
                                itemView.getTop(), itemView.getRight(), itemView.getBottom());
                        background.draw(c);
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY,
                                actionState, isCurrentlyActive);
                    }
                };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }
}
