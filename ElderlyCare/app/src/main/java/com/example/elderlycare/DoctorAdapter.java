package com.example.elderlycare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.Holder> {

    private List<DoctorModel> list;
    private final Context ctx;

    public DoctorAdapter(Context ctx, List<DoctorModel> list) {
        this.ctx = ctx;
        this.list = list != null ? list : new ArrayList<>();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_doctor, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        DoctorModel d = list.get(pos);

        // Name & specialist
        h.tvName.setText(d.fullName != null ? "Dr. " + d.fullName : "Dr.");
        h.tvSpecialist.setText(d.specialist != null ? d.specialist : "");

        // Rating
        h.tvRating.setText((d.rating != null ? d.rating : "4.8") + " ★");

        // Initials
        if (d.fullName != null) {
            String[] parts = d.fullName.split(" ");
            String init = "";
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (parts[i].length() > 0)
                    init += parts[i].substring(0, 1).toUpperCase(Locale.ROOT);
            }
            h.tvInitials.setText(init);
        } else {
            h.tvInitials.setText("DR");
        }

        // License
        if (d.licenseNumber != null && !d.licenseNumber.isEmpty()) {
            h.tvLicense.setText("License: " + d.licenseNumber);
            h.tvLicense.setVisibility(View.VISIBLE);
        } else {
            h.tvLicense.setVisibility(View.GONE);
        }

        // Working Hours
        if (d.startTime != null && d.endTime != null) {
            h.txtWorkingHours.setText("🕒 " +
                    formatTime(d.startTime) + " – " + formatTime(d.endTime));
        } else {
            h.txtWorkingHours.setText("🕒 Not available");
        }

        // Working Days
        if (d.workingDays != null && !d.workingDays.isEmpty()) {
            h.txtWorkingDays.setText("📅 " + formatDays(d.workingDays));
        } else {
            h.txtWorkingDays.setText("📅 Not available");
        }

        // Availability
        h.tvAvailability.setText(d.availability != null ? d.availability : "");

        // Distance
        if (d.distanceKm > 0) {
            h.tvDistance.setText(String.format(Locale.getDefault(), "📍 %.1f km away", d.distanceKm));
            h.tvDistance.setVisibility(View.VISIBLE);
        } else {
            h.tvDistance.setVisibility(View.GONE);
        }

        // Book → BookAppointmentActivity
        h.btnBook.setOnClickListener(v -> {
            Intent i = new Intent(ctx, BookAppointmentActivity.class);
            i.putExtra("doctorUid", d.uid);
            i.putExtra("doctorName", d.fullName);
            ctx.startActivity(i);
        });

        // Call button (dialer)
        h.btnCall.setOnClickListener(v -> {
            if (d.phone != null && !d.phone.isEmpty()) {
                ctx.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + d.phone)));
            }
        });

        // Click card → Doctor profile
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(ctx, DoctorDetailActivity.class);
            i.putExtra("doctorUid", d.uid);
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateList(List<DoctorModel> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    class Holder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvSpecialist, tvRating, tvAvailability;
        TextView txtWorkingHours, txtWorkingDays, tvLicense, tvDistance;
        Button btnBook;
        ImageView btnCall;

        public Holder(View v) {
            super(v);
            tvInitials = v.findViewById(R.id.tvInitials);
            tvName = v.findViewById(R.id.tvName);
            tvSpecialist = v.findViewById(R.id.tvSpecialist);
            tvRating = v.findViewById(R.id.tvRating);
            tvAvailability = v.findViewById(R.id.tvAvailability);
            txtWorkingHours = v.findViewById(R.id.txtWorkingHours);
            txtWorkingDays = v.findViewById(R.id.txtWorkingDays);
            btnBook = v.findViewById(R.id.btnBook);
            btnCall = v.findViewById(R.id.btnCall);
            tvLicense = v.findViewById(R.id.tvLicense);
            tvDistance = v.findViewById(R.id.tvDistance);
        }
    }

    // Convert "21:00" → "9:00 PM"
    private String formatTime(String t24) {
        try {
            SimpleDateFormat f24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat f12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return f12.format(f24.parse(t24));
        } catch (Exception e) {
            return t24;
        }
    }

    // Convert {Mon:true, Tue:true...} to "Mon – Fri" or "Tue, Thu"
    private String formatDays(Map<String, Boolean> map) {
        List<String> order = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<String> active = new ArrayList<>();

        for (String d : order) {
            Boolean v = map.get(d);
            if (v != null && v) active.add(d);
        }

        if (active.isEmpty()) return "Not available";

        int first = order.indexOf(active.get(0));
        int last = order.indexOf(active.get(active.size() - 1));

        // continuous range?
        boolean isContinuous = (last - first + 1) == active.size();
        if (isContinuous) {
            return order.get(first) + " – " + order.get(last);
        }
        return String.join(", ", active);
    }
}
