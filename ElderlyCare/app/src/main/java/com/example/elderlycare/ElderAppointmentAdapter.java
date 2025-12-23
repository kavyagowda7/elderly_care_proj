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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ElderAppointmentAdapter extends RecyclerView.Adapter<ElderAppointmentAdapter.Holder> {

    Context ctx;
    List<AppointmentModel> list;


    public ElderAppointmentAdapter(Context ctx, List<AppointmentModel> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(ctx)
                .inflate(R.layout.item_elder_appointment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        AppointmentModel m = list.get(pos);

        /* ---------------------------
             FETCH DOCTOR NAME
         --------------------------- */
        DatabaseReference docRef = FirebaseDatabase.getInstance(
                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                ).getReference("users")
                .child(m.doctorUid)
                .child("doctor_info")
                .child("fullName");

        docRef.get().addOnSuccessListener(snap -> {
            if (snap.exists())
                h.tvDoctorName.setText("Dr. " + snap.getValue(String.class));
            else
                h.tvDoctorName.setText("Doctor");
        });


        /* ---------------------------
               BASIC FIELDS
         --------------------------- */
        h.tvDate.setText(m.date);
        h.tvTime.setText(m.time);
        h.tvType.setText(m.type);
        h.tvStatus.setText(m.status);

        /* ---------------------------
               STATUS COLOR
         --------------------------- */
        switch (m.status) {
            case "Confirmed":
                h.tvStatus.setTextColor(Color.parseColor("#4CAF50"));  // Green
                break;
            case "Pending":
                h.tvStatus.setTextColor(Color.parseColor("#FFC107"));  // Yellow
                break;
            case "Rejected":
                h.tvStatus.setTextColor(Color.parseColor("#E53935"));  // Red
                break;
            case "Rescheduled":
                h.tvStatus.setTextColor(Color.parseColor("#0288D1"));  // Blue
                break;
        }

        /* --------------------------------------------------
                SHOW CLINIC ADDRESS (ONLY IF IN-PERSON)
           -------------------------------------------------- */
        if (m.type != null && m.type.equalsIgnoreCase("In-Person")) {

            DatabaseReference addressRef = FirebaseDatabase.getInstance(
                            "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                    ).getReference("users")
                    .child(m.doctorUid)
                    .child("doctor_info")
                    .child("clinicAddress");

            addressRef.get().addOnSuccessListener(snap -> {
                if (snap.exists()) {
                    h.tvClinicAddress.setText("Clinic: " + snap.getValue(String.class));
                    h.tvClinicAddress.setVisibility(View.VISIBLE);
                } else {
                    h.tvClinicAddress.setVisibility(View.GONE);
                }
            });

        } else {
            h.tvClinicAddress.setVisibility(View.GONE);
        }


        /* ---------------------------
              RESCHEDULE VISIBILITY
         --------------------------- */
        if (m.status.equalsIgnoreCase("Confirmed") ||
                m.status.equalsIgnoreCase("Rejected")) {
            h.btnReschedule.setVisibility(View.VISIBLE);
        } else {
            h.btnReschedule.setVisibility(View.GONE);
        }

        /* ---------------------------
               RESCHEDULE CLICK
         --------------------------- */
        h.btnReschedule.setOnClickListener(v -> {
            Intent i = new Intent(ctx, BookAppointmentActivity.class);
            i.putExtra("doctorUid", m.doctorUid);
            i.putExtra("appointmentId", m.appointmentId);
            i.putExtra("isReschedule", true);

            // Prefill user details
            i.putExtra("name", m.patientName);
            i.putExtra("phone", m.patientPhone);
            i.putExtra("date", m.date);
            i.putExtra("time", m.time);

            ctx.startActivity(i);
        });


        /* ---------------------------
              View Details (optional)
         --------------------------- */
        h.btnViewDetails.setOnClickListener(v -> {
            // Optional future implementation
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvDoctorName, tvDate, tvTime, tvType, tvStatus;
        TextView tvClinicAddress;
        Button btnViewDetails, btnReschedule;

        public Holder(@NonNull View v) {
            super(v);

            tvDoctorName = v.findViewById(R.id.tvDoctorName);
            tvDate = v.findViewById(R.id.tvDate);
            tvTime = v.findViewById(R.id.tvTime);
            tvType = v.findViewById(R.id.tvType);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnViewDetails = v.findViewById(R.id.btnViewDetails);
            btnReschedule = v.findViewById(R.id.btnReschedule);

            tvClinicAddress = v.findViewById(R.id.tvClinicAddress); // ⭐ ADDED
        }
    }
}
