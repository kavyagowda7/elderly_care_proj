package com.example.elderlycare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.VH> {

    List<Prescription> list;

    public PrescriptionAdapter(List<Prescription> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Prescription p = list.get(position);
        StringBuilder sb = new StringBuilder();
        for (MedicineModel m : p.medicines) {
            sb.append(m.getMedicineName()).append(" | ").append(m.getDosage()).append(" | ").append(m.getTimings()).append(" | ").append(m.getDuration()).append("\n");
        }
        holder.tvContent.setText(sb.toString());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvContent;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvPrescriptionContent);
        }
    }
}
