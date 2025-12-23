package com.example.elderlycare;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class VideoAppointmentAdapter extends RecyclerView.Adapter<VideoAppointmentAdapter.Holder> {

    Context ctx;
    List<AppointmentModel> list;

    public VideoAppointmentAdapter(Context ctx, List<AppointmentModel> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(ctx)
                .inflate(R.layout.item_video_appointment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {

        AppointmentModel m = list.get(pos);

        // Doctor Name Fetch
        FirebaseDatabase.getInstance(
                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                ).getReference("users")
                .child(m.doctorUid)
                .child("doctor_info")
                .child("fullName")
                .get().addOnSuccessListener(snap -> {
                    if (snap.exists())
                        h.tvName.setText("Dr. " + snap.getValue(String.class));
                });

        // Initials
        String initials = ("" + m.patientName.charAt(0)).toUpperCase();
        h.tvInitial.setText(initials);

        h.tvSpeciality.setText("General Physician"); // If you have speciality in DB replace here
        h.tvTime.setText(m.date + " at " + m.time);

        h.btnJoin.setOnClickListener(v -> {
            String roomName = "eldercare_" + m.appointmentId; // Unique meeting room for each appointment
            Intent i = new Intent(ctx, JitsiMeetingActivity.class);
            i.putExtra("room", roomName);
            ctx.startActivity(i);
        });
    // ✅ DATE-BASED EXPIRY LOGIC (CONSISTENT WITH APPOINTMENTS)
        String todayStr = new java.text.SimpleDateFormat(
                "dd-MM-yyyy",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());

        boolean isExpired =
                m.status.equalsIgnoreCase("Completed") ||
                        (m.status.equalsIgnoreCase("Confirmed") && m.date.compareTo(todayStr) < 0);

        if (isExpired) {
            h.btnJoin.setEnabled(false);
            h.btnJoin.setText("Expired");
            h.btnJoin.setAlpha(0.6f);
        } else {
            h.btnJoin.setEnabled(true);
            h.btnJoin.setText("Join");
            h.btnJoin.setAlpha(1f);
        }

// 🟡 HIGHLIGHT TODAY'S VIDEO CALL (REUSE todayStr)
        if (m.date.equals(todayStr)) {
            h.itemView.setBackgroundColor(0xFFFFF3E0); // Light orange
        } else {
            h.itemView.setBackgroundColor(0x00000000);
        }




    }

    @Override
    public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvInitial, tvName, tvSpeciality, tvTime;
        Button btnJoin;

        public Holder(@NonNull View v) {
            super(v);

            tvInitial = v.findViewById(R.id.tvInitial);
            tvName = v.findViewById(R.id.tvName);
            tvSpeciality = v.findViewById(R.id.tvSpeciality);
            tvTime = v.findViewById(R.id.tvTime);
            btnJoin = v.findViewById(R.id.btnJoin);
        }
    }
}
