package com.example.elderlycare;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.*;

public class BookAppointmentActivity extends AppCompatActivity {
    Set<String> lockedTimes = new HashSet<>();

    EditText etName, etPhone, etNote;
    FlexboxLayout timeSlotContainer;
    Button btnConfirm, btnInPerson, btnVideo;
    DatePicker datePicker;

    String selectedTime = "";
    String selectedType = "In-Person";
    String doctorUid;

    ProgressDialog progress;

    String[] timeSlots = {
            "9:00 AM", "10:00 AM", "11:00 AM",
            "2:00 PM", "3:00 PM", "4:00 PM"
    };

    private static final String BASE_URL = "https://elderlycare-backend-qrvl.onrender.com/";
    private final OkHttpClient http = new OkHttpClient();

    // Wake backend server
    private void warmUpBackend() {
        Request request = new Request.Builder()
                .url(BASE_URL)
                .get()
                .build();

        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, IOException e) {}
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {}
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        warmUpBackend();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        doctorUid = getIntent().getStringExtra("doctorUid");
        if (doctorUid == null) doctorUid = "unknown_doctor";

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etNote = findViewById(R.id.etNote);
        timeSlotContainer = findViewById(R.id.timeSlotContainer);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnInPerson = findViewById(R.id.btnInPerson);
        btnVideo = findViewById(R.id.btnVideo);
        datePicker = findViewById(R.id.datePicker);

        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Saving appointment...");

        generateTimeSlots();
        setupTypeSelector();

        // ----------------- PREFILL FOR RESCHEDULE (NO DB change yet) -----------------
        String oldDate = getIntent().getStringExtra("date");
        String oldTime = getIntent().getStringExtra("time");
        String oldName = getIntent().getStringExtra("name");
        String oldPhone = getIntent().getStringExtra("phone");

        if (oldName != null) etName.setText(oldName);
        if (oldPhone != null) etPhone.setText(oldPhone);

        if (oldDate != null && oldTime != null) {
            try {
                String[] parts = oldDate.split("-");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1;
                int year = Integer.parseInt(parts[2]);
                datePicker.updateDate(year, month, day);
            } catch (Exception ignored) {}

            selectedTime = oldTime;

            timeSlotContainer.post(() -> {
                for (int i = 0; i < timeSlotContainer.getChildCount(); i++) {
                    View child = timeSlotContainer.getChildAt(i);
                    if (child instanceof Button) {
                        Button bt = (Button) child;
                        if (bt.getText().toString().equals(oldTime)) {
                            bt.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500));
                        }
                    }
                }
            });
        }

        // fetch locked slots for initially selected date
        String initDate = getSelectedDateString();
        fetchLockedSlots(initDate);

        // listen for date changes
        datePicker.init(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    String newDate = String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year);
                    fetchLockedSlots(newDate);
                });

        btnConfirm.setOnClickListener(v -> saveAppointment());
    }

    private void setupTypeSelector() {
        setButtonSelected(btnInPerson, true);
        setButtonSelected(btnVideo, false);

        btnInPerson.setOnClickListener(v -> {
            selectedType = "In-Person";
            setButtonSelected(btnInPerson, true);
            setButtonSelected(btnVideo, false);
        });

        btnVideo.setOnClickListener(v -> {
            selectedType = "Video Call";
            setButtonSelected(btnVideo, true);
            setButtonSelected(btnInPerson, false);
        });
    }

    private void setButtonSelected(Button b, boolean selected) {
        if (selected) {
            b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500));
        } else {
            b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_light));
        }
    }

    private void generateTimeSlots() {
        timeSlotContainer.removeAllViews();

        for (String t : timeSlots) {
            Button b = new Button(this);
            b.setText(t);

            b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_light));
            b.setTextColor(ContextCompat.getColor(this, R.color.black));

            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(16, 12, 16, 12);
            b.setLayoutParams(lp);

            b.setOnClickListener(v -> {
                selectedTime = t;

                for (int i = 0; i < timeSlotContainer.getChildCount(); i++) {
                    View child = timeSlotContainer.getChildAt(i);
                    child.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_light));
                }

                b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500));
            });

            timeSlotContainer.addView(b);
        }

        // ⭐ After generating, reapply locking
        applyLockedSlots();
    }


    private String getSelectedDateString() {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1;
        int year = datePicker.getYear();
        return String.format("%02d-%02d-%04d", day, month, year);
    }

    private void saveAppointment() {
        String patientName = etName.getText().toString().trim();
        String patientPhone = etPhone.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (TextUtils.isEmpty(patientName) || TextUtils.isEmpty(patientPhone)) {
            Toast.makeText(this, "Please enter name & phone", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedDate = getSelectedDateString();
        String patientUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progress.show();

        try {
            JSONObject json = new JSONObject();
            json.put("doctorUid", doctorUid);
            json.put("patientUid", patientUid);
            json.put("patientName", patientName);
            json.put("patientPhone", patientPhone);
            json.put("date", selectedDate);
            json.put("time", selectedTime);
            json.put("type", selectedType);
            json.put("note", note);

            RequestBody body = RequestBody.create(json.toString(),
                    MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(BASE_URL + "create-appointment")
                    .post(body)
                    .build();

            http.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(BookAppointmentActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    runOnUiThread(() -> progress.dismiss());
                    if (!response.isSuccessful()) {
                        final String err = response.body() != null ? response.body().string() : "Unknown server error";
                        runOnUiThread(() ->
                                Toast.makeText(BookAppointmentActivity.this, "Server error: " + err, Toast.LENGTH_LONG).show()
                        );
                    } else {
                        // New appointment created successfully.
                        // If this was a reschedule flow, now update the OLD appointment to "Rescheduled"
                        boolean isReschedule = getIntent().getBooleanExtra("isReschedule", false);
                        String oldAppointmentId = getIntent().getStringExtra("appointmentId");
                        String patientUidLocal = patientUid; // final for inner scope
                        if (isReschedule && oldAppointmentId != null) {
                            // mark old appointment in both doctor and user nodes
                            FirebaseDatabase.getInstance(
                                            "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                                    ).getReference("appointments")
                                    .child(doctorUid)
                                    .child(oldAppointmentId)
                                    .child("status")
                                    .setValue("Rescheduled");

                            FirebaseDatabase.getInstance(
                                            "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                                    ).getReference("user_appointments")
                                    .child(patientUidLocal)
                                    .child(oldAppointmentId)
                                    .child("status")
                                    .setValue("Rescheduled");
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(BookAppointmentActivity.this, "Appointment requested (Pending)", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                }
            });

        } catch (JSONException je) {
            progress.dismiss();
            Toast.makeText(this, "JSON error: " + je.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void fetchLockedSlots(String date) {
        Request request = new Request.Builder()
                .url(BASE_URL + "locked-slots?doctorUid=" + doctorUid + "&date=" + date)
                .get()
                .build();

        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, IOException e) {}

            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String s = response.body() != null ? response.body().string() : "";

                try {
                    JSONObject obj = new JSONObject(s);
                    JSONArray locked = obj.optJSONArray("locked");
                    if (locked == null) return;

                    lockedTimes.clear();
                    for (int i = 0; i < locked.length(); i++) {
                        lockedTimes.add(locked.getString(i));
                    }

                    runOnUiThread(() -> applyLockedSlots());


                } catch (Exception ex) {}
            }
        });
    }
    private void applyLockedSlots() {
        for (int i = 0; i < timeSlotContainer.getChildCount(); i++) {
            View child = timeSlotContainer.getChildAt(i);

            if (child instanceof Button) {
                Button b = (Button) child;
                String t = b.getText().toString();

                if (lockedTimes.contains(t)) {
                    b.setEnabled(false);
                    b.setAlpha(0.4f);

                    if (selectedTime.equals(t)) {
                        selectedTime = "";
                    }
                } else {
                    b.setEnabled(true);
                    b.setAlpha(1f);
                }
            }
        }
    }

}
