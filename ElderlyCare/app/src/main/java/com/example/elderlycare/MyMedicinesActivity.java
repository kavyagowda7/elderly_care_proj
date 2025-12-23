package com.example.elderlycare;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyMedicinesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    MedicineAdapter adapter;
    List<MedicineModel> medList = new ArrayList<>();

    TextView tvDate;

    private final String DB_URL = "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_medicines);

        tvDate = findViewById(R.id.tvDate);
        recyclerView = findViewById(R.id.rvMedicines);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineAdapter(medList);
        recyclerView.setAdapter(adapter);

        setTodayDate();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Perform daily reset if needed (resets takenMorning/afternoon/night)
        performDailyResetIfNeeded(uid, () -> loadMedicines());

        // loadMedicines(); // will be called in reset callback
    }

    private void setTodayDate() {
        String today = new SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(new Date());
        tvDate.setText(today);
    }

    private void loadMedicines() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(uid)
                .child("current_prescriptions");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                medList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    MedicineModel m = snap.getValue(MedicineModel.class);

                    if (m != null) {

                        // Ensure ID is attached
                        m.setId(snap.getKey());

                        // Ensure startDate exists
                        Object sd = snap.child("startDate").getValue();
                        if (sd != null) {
                            try { m.setStartDate(Long.parseLong(sd.toString())); } catch (Exception ignored) {}
                        }

                        // totalDays
                        Object td = snap.child("totalDays").getValue();
                        if (td != null) {
                            try { m.setTotalDays(Integer.parseInt(td.toString())); } catch (Exception ignored) {}
                        }

                        // daysCompleted
                        Object dc = snap.child("daysCompleted").getValue();
                        if (dc != null) {
                            try { m.setDaysCompleted(Integer.parseInt(dc.toString())); } catch (Exception ignored) {}
                        }

                        // timings -> parse to doses
                        String timings = snap.child("timings").getValue(String.class);
                        if (timings != null) {
                            m.parseTimingsToDoses(timings);
                        } else {
                            m.parseTimingsToDoses(m.getTimings());
                        }

                        // taken flags per dose (fallbacks)
                        Boolean tm = snap.child("takenMorning").getValue(Boolean.class);
                        Boolean ta = snap.child("takenAfternoon").getValue(Boolean.class);
                        Boolean tn = snap.child("takenNight").getValue(Boolean.class);

                        if (tm != null) m.setTakenMorning(tm);
                        if (ta != null) m.setTakenAfternoon(ta);
                        if (tn != null) m.setTakenNight(tn);

                        // legacy takenToday handling
                        Boolean takenTodayLegacy = snap.child("takenToday").getValue(Boolean.class);
                        if (takenTodayLegacy != null) {
                            // if none of the dose flags are present, map legacy to morning
                            if (tm == null && ta == null && tn == null && takenTodayLegacy) {
                                m.setTakenMorning(true);
                            }
                        }

                        // lastCompletedDate
                        String lcd = snap.child("lastCompletedDate").getValue(String.class);
                        if (lcd != null) m.setLastCompletedDate(lcd);

                        medList.add(m);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MyMedicines", "loadMedicines cancelled", error.toException());
            }
        });
    }

    // Daily reset logic: ensure taken flags are false when new day starts.
    private void performDailyResetIfNeeded(String uid, Runnable afterReset) {

        DatabaseReference metaRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(uid)
                .child("medicationMeta");

        final String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        metaRef.child("lastResetDate").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String last = snapshot.getValue(String.class);
                if (last != null && last.equals(today)) {
                    // already reset today
                    afterReset.run();
                    return;
                }

                // need to reset all current_prescriptions taken flags
                DatabaseReference prescriptionsRef = FirebaseDatabase.getInstance(DB_URL)
                        .getReference("users")
                        .child(uid)
                        .child("current_prescriptions");

                prescriptionsRef.get().addOnSuccessListener(presSnap -> {
                    for (DataSnapshot s : pResIterable(presSnap)) {
                        String key = s.getKey();
                        if (key == null) continue;

                        // build map to reset only required fields (do not wipe other fields)
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("takenMorning", false);
                        updates.put("takenAfternoon", false);
                        updates.put("takenNight", false);
                        // do NOT change daysCompleted here
                        prescriptionsRef.child(key).updateChildren(updates);
                    }

                    // set lastResetDate = today
                    metaRef.child("lastResetDate").setValue(today).addOnCompleteListener(t -> {
                        afterReset.run();
                    });

                }).addOnFailureListener(e -> {
                    // even on failure, attempt to continue
                    afterReset.run();
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                afterReset.run();
            }
        });
    }

    // helper: safe iterable wrapper
    private Iterable<DataSnapshot> pResIterable(DataSnapshot ds) {
        List<DataSnapshot> list = new ArrayList<>();
        for (DataSnapshot c : ds.getChildren()) list.add(c);
        return list;
    }
}
