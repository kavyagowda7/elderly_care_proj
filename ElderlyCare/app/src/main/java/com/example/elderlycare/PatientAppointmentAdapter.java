package com.example.elderlycare;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

public class PatientAppointmentAdapter extends RecyclerView.Adapter<PatientAppointmentAdapter.Holder> {

    Context ctx;
    List<AppointmentModel> list;

    public PatientAppointmentAdapter(Context ctx, List<AppointmentModel> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(ctx).inflate(R.layout.item_patient_appointment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        AppointmentModel m = list.get(pos);

        // Initials
        String[] parts = m.doctorUid.split("");
        h.tvInitials.setText(parts.length >= 2 ? parts[0].toUpperCase() + parts[1].toUpperCase() : "DR");

        h.tvDoctorName.setText("Dr. " + m.patientName); // placeholder; update with doctor info if available
        h.tvSpeciality.setText("General Physician");

        h.tvStatus.setText(m.status);

        // Date formatting
        h.tvDate.setText(m.date);
        h.tvTime.setText(m.time);

        // Type styling
        h.tvType.setText(m.type);

        h.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(ctx, DoctorDetailActivity.class);
            i.putExtra("doctorUid", m.doctorUid);
            ctx.startActivity(i);
        });

        h.btnReschedule.setOnClickListener(v -> {
            Toast.makeText(ctx, "Reschedule coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvDoctorName, tvSpeciality, tvStatus, tvDate, tvTime, tvType;
        Button btnViewDetails, btnReschedule;

        public Holder(@NonNull View v) {
            super(v);
            tvInitials = v.findViewById(R.id.tvInitials);
            tvDoctorName = v.findViewById(R.id.tvDoctorName);
            tvSpeciality = v.findViewById(R.id.tvSpeciality);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDate = v.findViewById(R.id.tvDate);
            tvTime = v.findViewById(R.id.tvTime);
            tvType = v.findViewById(R.id.tvType);
            btnViewDetails = v.findViewById(R.id.btnViewDetails);
            btnReschedule = v.findViewById(R.id.btnReschedule);
        }
    }
}
