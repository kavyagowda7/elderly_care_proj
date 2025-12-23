// File: DoctorAppointmentsActivity.java
package com.example.elderlycare;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DoctorAppointmentsActivity extends AppCompatActivity {

    ImageView backBtn;
    TextView todayCount, upcomingCount, completedCount;
    TextView tabToday, tabUpcoming, tabCompleted;
    LinearLayout emptyStateCard;

    RecyclerView recyclerAppointments;
    DoctorAppointmentAdapter adapter;

    List<AppointmentModel> allAppointments = new ArrayList<>();
    List<AppointmentModel> filteredList = new ArrayList<>();

    DatabaseReference doctorRef;
    String doctorUid;

    // ⭐ Track current selected tab
    private String currentTab = "today";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments);

        doctorUid = FirebaseAuth.getInstance().getUid();
        doctorRef = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("appointments").child(doctorUid);

        initViews();
        enableSwipeToDelete();

        setupTabs();
        loadAppointments();
        fetchTodayLockedSlots();

        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        backBtn = findViewById(R.id.backBtn);

        todayCount = findViewById(R.id.todayCount);
        upcomingCount = findViewById(R.id.upcomingCount);
        completedCount = findViewById(R.id.completedCount);

        tabToday = findViewById(R.id.tabToday);
        tabUpcoming = findViewById(R.id.tabUpcoming);
        tabCompleted = findViewById(R.id.tabCompleted);

        emptyStateCard = findViewById(R.id.emptyStateCard);

        recyclerAppointments = findViewById(R.id.recyclerAppointments);
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DoctorAppointmentAdapter(this, filteredList);
        recyclerAppointments.setAdapter(adapter);
    }

    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        int pos = viewHolder.getAdapterPosition();
                        AppointmentModel m = filteredList.get(pos);

                        if (!m.status.equalsIgnoreCase("Completed") &&
                                !m.status.equalsIgnoreCase("Rejected")) {

                            adapter.notifyItemChanged(pos);
                            Toast.makeText(DoctorAppointmentsActivity.this,
                                    "You can delete only completed appointments",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        deleteAppointment(m, pos);
                    }
                };

        ItemTouchHelper helper = new ItemTouchHelper(swipeCallback);
        helper.attachToRecyclerView(recyclerAppointments);
    }

    private void deleteAppointment(AppointmentModel m, int pos) {

        String doctorUid = FirebaseAuth.getInstance().getUid();
        String patientUid = m.patientUid;

        // doctor side
        DatabaseReference docRef = FirebaseDatabase.getInstance(
                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app"
                ).getReference("appointments")
                .child(doctorUid)
                .child(m.appointmentId);

        // patient side
        DatabaseReference userRef = FirebaseDatabase.getInstance(
                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app"
                ).getReference("user_appointments")
                .child(patientUid)
                .child(m.appointmentId);

        docRef.removeValue();
        userRef.removeValue();

        filteredList.remove(pos);
        adapter.notifyItemRemoved(pos);

        Toast.makeText(this, "Appointment deleted", Toast.LENGTH_SHORT).show();

        // ⭐ Stay on same tab
        applyFilter(currentTab);
    }

    // ⭐⭐⭐ FIXED TAB SWITCHING ⭐⭐⭐
    private void setupTabs() {

        tabToday.setOnClickListener(v -> {
            currentTab = "today";
            applyFilter(currentTab);
        });

        tabUpcoming.setOnClickListener(v -> {
            currentTab = "upcoming";
            applyFilter(currentTab);
        });

        tabCompleted.setOnClickListener(v -> {
            currentTab = "completed";
            applyFilter(currentTab);
        });
    }

    private void loadAppointments() {
        doctorRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAppointments.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    AppointmentModel m = s.getValue(AppointmentModel.class);
                    if (m != null && m.status != null)
                        allAppointments.add(m);
                }

                updateCounts();

                // ⭐ Do NOT force switch tab
                applyFilter(currentTab);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCounts() {
        int today = 0, upcoming = 0, completed = 0;

        String todayStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        for (AppointmentModel m : allAppointments) {

            if (m.date.equals(todayStr))
                today++;

            if (m.status.equalsIgnoreCase("Confirmed") && !m.date.equals(todayStr))
                upcoming++;

            if (m.status.equalsIgnoreCase("Completed") ||
                    m.status.equalsIgnoreCase("Rejected"))
                completed++;
        }

        todayCount.setText(String.valueOf(today));
        upcomingCount.setText(String.valueOf(upcoming));
        completedCount.setText(String.valueOf(completed));
    }

    private void applyFilter(String type) {
        filteredList.clear();

        String todayStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        for (AppointmentModel m : allAppointments) {
            switch (type) {
                case "today":
                    if (m.status.equalsIgnoreCase("Pending") ||
                            m.date.equals(todayStr))
                        filteredList.add(m);
                    break;

                case "upcoming":
                    if (!m.date.equals(todayStr) &&
                            (m.status.equalsIgnoreCase("Pending") ||
                                    m.status.equalsIgnoreCase("Confirmed")))
                        filteredList.add(m);
                    break;

                case "completed":
                    if (m.status.equalsIgnoreCase("Completed") ||
                            m.status.equalsIgnoreCase("Rejected"))
                        filteredList.add(m);
                    break;
            }
        }

        emptyStateCard.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        highlightTab(type);
    }

    private void highlightTab(String type) {
        tabToday.setBackground(null);
        tabUpcoming.setBackground(null);
        tabCompleted.setBackground(null);

        tabToday.setTextColor(0xFF555555);
        tabUpcoming.setTextColor(0xFF555555);
        tabCompleted.setTextColor(0xFF555555);

        TextView selected = type.equals("today") ? tabToday :
                type.equals("upcoming") ? tabUpcoming : tabCompleted;

        selected.setBackground(getDrawable(R.drawable.tab_selected));
        selected.setTextColor(0xFFFFFFFF);
    }

    private void fetchTodayLockedSlots() {
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        OkHttpClient http = new OkHttpClient();

        String url = ApiConstants.BASE_URL +
                "locked-slots?doctorUid=" + doctorUid + "&date=" + today;

        Request request = new Request.Builder().url(url).get().build();

        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, IOException e) {}

            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) return;

                String json = response.body().string();

                try {
                    JSONObject obj = new JSONObject(json);
                    JSONArray arr = obj.getJSONArray("locked");

                } catch (Exception ignored) {}
            }
        });
    }
}
