package com.example.elderlycare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedViewHolder> {

    private List<MedicineModel> list;
    private Context context;

    private final String DB_URL = "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public MedicineAdapter(List<MedicineModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.item_medicine_card, parent, false);
        return new MedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder h, int pos) {

        MedicineModel m = list.get(pos);

        h.tvName.setText(m.getMedicineName());
        h.tvDosage.setText(m.getDosage());
        h.tvTime.setText(formatTimingsReadable(m));

        // ❌ tvInventory removed in new UI → comment out
        // h.tvInventory.setText(String.valueOf(m.getTotalDays()));

        h.tvFrequency.setText("Daily, ongoing");

        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                Locale.getDefault()).format(new Date(m.getStartDate()));

        // ❌ tvStartDate is BACK → now set properly
        h.tvStartDate.setText("From: " + formattedDate);


        // Progress calculation:
        int totalDays = Math.max(m.getTotalDays(), 1);
        int daysCompleted = m.getDaysCompleted();
        int progressPercent = (int) ((daysCompleted * 100.0) / totalDays);
        if (progressPercent > 100) progressPercent = 100;
        h.progressBar.setProgress(progressPercent);

        long daysPassed = getDaysPassed(m.getStartDate());
        long daysLeft = Math.max(totalDays - daysCompleted, 0);

        // ❌ removed tvDaysLeft in new UI
        // h.tvDaysLeft.setText(daysLeft + " days left");


        // ❌ Dose buttons removed in new UI → comment entire block
        /*
        h.btnMorning.setVisibility(m.isDoseMorningRequired() ? View.VISIBLE : View.GONE);
        h.btnAfternoon.setVisibility(m.isDoseAfternoonRequired() ? View.VISIBLE : View.GONE);
        h.btnNight.setVisibility(m.isDoseNightRequired() ? View.VISIBLE : View.GONE);

        h.btnMorning.setImageResource(m.isTakenMorning() ? R.drawable.ic_check_selected : R.drawable.ic_check_unselected);
        h.btnAfternoon.setImageResource(m.isTakenAfternoon() ? R.drawable.ic_check_selected : R.drawable.ic_check_unselected);
        h.btnNight.setImageResource(m.isTakenNight() ? R.drawable.ic_check_selected : R.drawable.ic_check_unselected);

        h.btnMorning.setOnClickListener(v -> toggleDose(h, m, "morning", pos));
        h.btnAfternoon.setOnClickListener(v -> toggleDose(h, m, "afternoon", pos));
        h.btnNight.setOnClickListener(v -> toggleDose(h, m, "night", pos));
        */
    }


    // --------------------------------------------------------------------------------------------
    // DOSE LOGIC LEFT EXACTLY AS IT IS (NOT REMOVED, JUST NOT USED IN UI)
    // --------------------------------------------------------------------------------------------

    private void toggleDose(MedViewHolder h, MedicineModel m, String dose, int pos) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference presRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(uid)
                .child("current_prescriptions")
                .child(m.getId());

        boolean newVal = false;
        Map<String, Object> updates = new HashMap<>();

        if ("morning".equals(dose)) {
            newVal = !m.isTakenMorning();
            m.setTakenMorning(newVal);
            updates.put("takenMorning", newVal);
        } else if ("afternoon".equals(dose)) {
            newVal = !m.isTakenAfternoon();
            m.setTakenAfternoon(newVal);
            updates.put("takenAfternoon", newVal);
        } else {
            newVal = !m.isTakenNight();
            m.setTakenNight(newVal);
            updates.put("takenNight", newVal);
        }

        presRef.updateChildren(updates).addOnSuccessListener(unused -> {

            notifyItemChanged(pos);

            boolean allTaken = true;
            if (m.isDoseMorningRequired() && !m.isTakenMorning()) allTaken = false;
            if (m.isDoseAfternoonRequired() && !m.isTakenAfternoon()) allTaken = false;
            if (m.isDoseNightRequired() && !m.isTakenNight()) allTaken = false;

            String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

            if (allTaken) {
                String lastCompleted = m.getLastCompletedDate();
                if (lastCompleted == null || !lastCompleted.equals(today)) {
                    int newDays = m.getDaysCompleted() + 1;
                    m.setDaysCompleted(newDays);
                    m.setLastCompletedDate(today);

                    Map<String, Object> upd2 = new HashMap<>();
                    upd2.put("daysCompleted", newDays);
                    upd2.put("lastCompletedDate", today);

                    presRef.updateChildren(upd2);

                    notifyItemChanged(pos);

                    if (newDays >= m.getTotalDays()) {
                        moveToCompleted(uid, m);
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void moveToCompleted(String uid, MedicineModel m) {
        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL).getReference();
        DatabaseReference fromRef = root.child("users").child(uid).child("current_prescriptions").child(m.getId());
        DatabaseReference toRef = root.child("completed_prescriptions").child(uid).child(m.getId());

        fromRef.get().addOnSuccessListener(ds -> {
            Object value = ds.getValue();
            if (value != null) {
                toRef.setValue(value).addOnSuccessListener(u -> {
                    fromRef.removeValue();
                    Toast.makeText(context, m.getMedicineName() + " completed and moved to history", Toast.LENGTH_LONG).show();
                });
            }
        }).addOnFailureListener(e -> {});
    }

    private long getDaysPassed(long startDate) {
        long diff = System.currentTimeMillis() - startDate;
        return diff / (1000L * 60 * 60 * 24);
    }

    private String formatTimingsReadable(MedicineModel m) {
        String t = m.getTimings();
        if (t != null && !t.isEmpty()) return t;

        StringBuilder sb = new StringBuilder();
        if (m.isDoseMorningRequired()) sb.append("Morning ");
        if (m.isDoseAfternoonRequired()) {
            if (sb.length() > 0) sb.append("• ");
            sb.append("Afternoon ");
        }
        if (m.isDoseNightRequired()) {
            if (sb.length() > 0) sb.append("• ");
            sb.append("Night ");
        }
        return sb.toString().trim();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MedViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvDosage, tvTime;
        ProgressBar progressBar;

        // TextView tvDaysLeft;  // ❌ removed in UI
        // ImageView btnMorning, btnAfternoon, btnNight; // ❌ removed
        // TextView tvInventory; // ❌ removed in UI

        TextView tvFrequency, tvStartDate;

        public MedViewHolder(@NonNull View v) {
            super(v);

            tvName = v.findViewById(R.id.tvMedName);
            tvDosage = v.findViewById(R.id.tvDosage);
            tvTime = v.findViewById(R.id.tvTime);
            progressBar = v.findViewById(R.id.progressDays);

            tvFrequency = v.findViewById(R.id.tvFrequency);
            tvStartDate = v.findViewById(R.id.tvStartDate);
        }
    }
}
