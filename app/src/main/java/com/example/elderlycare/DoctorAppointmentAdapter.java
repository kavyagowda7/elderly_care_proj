// File: DoctorAppointmentAdapter.java
package com.example.elderlycare;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import okhttp3.*;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorAppointmentAdapter extends RecyclerView.Adapter<DoctorAppointmentAdapter.Holder> {

    Context ctx;
    List<AppointmentModel> list;

    OkHttpClient client = new OkHttpClient();

    public DoctorAppointmentAdapter(Context ctx, List<AppointmentModel> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        return new Holder(LayoutInflater.from(ctx).inflate(R.layout.item_doctor_appointment, parent, false));
        return new DoctorAppointmentAdapter.Holder(LayoutInflater.from(ctx)
                .inflate(R.layout.item_doctor_appointment, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        AppointmentModel m = list.get(pos);

        h.tvPatientName.setText(m.patientName);
        h.tvNote.setText(m.note);
        h.tvDateTime.setText(m.date + " • " + m.time);
        h.tvType.setText(m.type);
        h.tvStatus.setText(m.status);

        if (m.status.equals("Pending")) {
            h.btnAccept.setVisibility(View.VISIBLE);
            h.btnReject.setVisibility(View.VISIBLE);
        } else {
            h.btnAccept.setVisibility(View.GONE);
            h.btnReject.setVisibility(View.GONE);
        }

        h.btnAccept.setOnClickListener(v -> sendStatusToBackend(m, "Confirmed"));
        h.btnReject.setOnClickListener(v -> sendStatusToBackend(m, "Rejected"));

        // 🆕 HIGHLIGHT TODAY'S APPOINTMENT
        String todayStr = new java.text.SimpleDateFormat(
                "dd-MM-yyyy",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());

        if (m.date.equals(todayStr)) {
            h.itemView.setBackgroundColor(0xFFE3F2FD); // Light blue
        } else {
            h.itemView.setBackgroundColor(0x00000000); // Reset
        }

    }

    private void sendStatusToBackend(AppointmentModel m, String newStatus) {
        try {
            JSONObject json = new JSONObject();
            json.put("doctorUid", FirebaseAuth.getInstance().getUid());
            json.put("appointmentId", m.appointmentId);
            json.put("patientUid", m.patientUid);

            String endpoint = newStatus.equals("Confirmed") ?
                    "confirm-appointment" : "reject-appointment";

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ApiConstants.BASE_URL + endpoint)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }



                @Override
                public void onResponse(Call call, Response response) {
                    Log.d("CHAT_DEBUG", "onResponse called, success = " + response.isSuccessful());
                    if (response.isSuccessful()) {

                        // UPDATE FIREBASE
                        DatabaseReference ref = FirebaseDatabase.getInstance(
                                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app"
                                ).getReference("appointments")
                                .child(FirebaseAuth.getInstance().getUid())
                                .child(m.appointmentId)
                                .child("status");

                        ref.setValue(newStatus);
                        Log.d("CHAT_DEBUG", "newStatus=" + newStatus);

                        if ("Confirmed".equalsIgnoreCase(newStatus)) {
                            createChatIfNotExists(
                            m.appointmentId,
                                    FirebaseAuth.getInstance().getUid(), // doctorUid
                                    m.patientUid                          // elderUid
                            );
                        }

                        // Update local model
                        m.status = newStatus;

                        ((DoctorAppointmentsActivity) ctx).runOnUiThread(() -> {
                            notifyDataSetChanged();
                            Toast.makeText(ctx, "Updated to " + newStatus, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createChatIfNotExists(
            String appointmentId,
            String doctorUid,
            String elderUid
    ) {
        DatabaseReference chatRef =
                FirebaseDatabase.getInstance(
                                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app"
                        )
                        .getReference("chats")
                        .child(appointmentId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {

                    Map<String, Object> chatData = new HashMap<>();

                    Map<String, Object> participants = new HashMap<>();
                    participants.put(doctorUid, true);
                    participants.put(elderUid, true);

                    chatData.put("participants", participants);
                    chatData.put("messages", new HashMap<>());

                    chatRef.setValue(chatData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }




    @Override public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvNote, tvDateTime, tvType, tvStatus;
        Button btnAccept, btnReject;

        public Holder(@NonNull View v) {
            super(v);
            tvPatientName = v.findViewById(R.id.tvPatientName);
            tvNote = v.findViewById(R.id.tvNote);
            tvDateTime = v.findViewById(R.id.tvDateTime);
            tvType = v.findViewById(R.id.tvType);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnReject = v.findViewById(R.id.btnReject);
        }
    }
}
