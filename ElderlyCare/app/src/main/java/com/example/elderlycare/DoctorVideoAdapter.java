package com.example.elderlycare;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.*;

public class DoctorVideoAdapter extends RecyclerView.Adapter<DoctorVideoAdapter.Holder> {

    Context ctx;
    List<AppointmentModel> list;

    private static final String BACKEND_BASE = "https://elderlycare-backend-qrvl.onrender.com/";
    private final OkHttpClient http = new OkHttpClient();

    public DoctorVideoAdapter(Context ctx, List<AppointmentModel> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(ctx)
                .inflate(R.layout.item_doctor_video, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {

        AppointmentModel m = list.get(pos);

        h.tvName.setText(m.patientName);
        h.tvTime.setText(m.date + " at " + m.time);
        h.tvInitial.setText(m.patientName.substring(0, 1).toUpperCase());

        h.btnJoin.setText("Start Call");

        h.btnJoin.setOnClickListener(v -> {

            // ⭐ Doctor should NOT generate roomId here anymore

            sendBackendVideoNotification(
                    m.patientUid,
                    m.doctorUid,
                    m.appointmentId
            );
        });
    }

    // ⭐ NEW FIXED METHOD — Backend generates roomId
    private void sendBackendVideoNotification(String patientUid, String doctorUid, String appointmentId) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "video_call");
            json.put("targetUid", patientUid);
            json.put("doctorUid", doctorUid);
            json.put("appointmentId", appointmentId);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(BACKEND_BASE + "send-video")
                    .post(body)
                    .build();

            http.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e("DoctorVideo", "Backend error: " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {

                    if (!response.isSuccessful()) {
                        Log.e("DoctorVideo", "send-video failed " + response.code());
                        return;
                    }

                    String res = response.body().string();
                    try {
                        JSONObject obj = new JSONObject(res);
                        String roomId = obj.getString("roomId"); // ⭐ BACKEND ROOM ID

                        Log.e("ROOM_DEBUG", "Doctor joining backend room: " + roomId);

                        // Save SAME room in Firebase
                        FirebaseDatabase.getInstance(
                                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("appointments")
                                .child(doctorUid)
                                .child(appointmentId)
                                .child("roomId")
                                .setValue(roomId);

                        FirebaseDatabase.getInstance(
                                        "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("appointments")
                                .child(doctorUid)
                                .child(appointmentId)
                                .child("videoStatus")
                                .setValue("DoctorStarted");

                        // ⭐ Doctor opens Jitsi with same room
                        Intent i = new Intent(ctx, JitsiMeetingActivity.class);
                        i.putExtra("room", roomId);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(i);

                    } catch (JSONException e) {
                        Log.e("DoctorVideo", "JSON error: " + e.getMessage());
                    }
                }
            });

        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvTime;
        Button btnJoin;

        public Holder(@NonNull View v) {
            super(v);
            tvInitial = v.findViewById(R.id.tvInitial);
            tvName = v.findViewById(R.id.tvName);
            tvTime = v.findViewById(R.id.tvTime);
            btnJoin = v.findViewById(R.id.btnJoin);
        }
    }
}
