// File: ScanReceiptActivity.java
package com.example.elderlycare;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;


public class ScanReceiptActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;
    private static final int CAMERA_REQUEST = 101;

    Dialog verifyDialog;

    ImageView receiptImage;
    LinearLayout btnChooseImage;
    View btnTakePhoto;
    Button btnScan;
    Bitmap selectedBitmap;
    ProgressDialog dialog;

    FirebaseFirestore firestore;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_receipt);



        if (checkSelfPermission(android.Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 20);
        }

        receiptImage = findViewById(R.id.receiptImage);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnScan = findViewById(R.id.btnScan);
        ensureExactAlarmPermission();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Extracting...");
        dialog.setCancelable(false);

        FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/");

        auth = FirebaseAuth.getInstance();

        btnChooseImage.setOnClickListener(v -> openGallery());

        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> openCamera());
        }

        btnScan.setOnClickListener(v -> {
            if (selectedBitmap == null) {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show();
                return;
            }
            callBackendOCR();
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ScanReceipt", "openCamera error", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && data != null) {
            try {
                Uri uri = data.getData();
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                receiptImage.setImageBitmap(selectedBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == CAMERA_REQUEST) {
            if (data != null && data.getExtras() != null) {
                try {
                    Bitmap cameraBitmap = (Bitmap) data.getExtras().get("data");
                    if (cameraBitmap != null) {
                        selectedBitmap = cameraBitmap;
                        receiptImage.setImageBitmap(selectedBitmap);
                    } else {
                        Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void callBackendOCR() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.setMessage("Scanning with AI...");
        dialog.show();

        try {
            File file = new File(getCacheDir(), "upload.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            RequestBody reqFile = RequestBody.create(file, okhttp3.MediaType.parse("image/jpeg"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);

            ApiService api = RetrofitClient.getClient().create(ApiService.class);

            Call<ResponseBody> call = api.uploadImage(body);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    dialog.dismiss();

                    if (!response.isSuccessful()) {
                        Toast.makeText(ScanReceiptActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        String json = response.body().string();
                        Log.d("AI_OCR", json);

                        List<MedicineModel> meds = parseJsonFromAI(json);
                        showMedicineDialog(meds);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ScanReceiptActivity.this, "Parse Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    dialog.dismiss();
                    Toast.makeText(ScanReceiptActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<MedicineModel> parseJsonFromAI(String json) throws Exception {

        List<MedicineModel> list = new ArrayList<>();

        JSONObject obj = new JSONObject(json);
        JSONArray meds = obj.getJSONArray("medicines");

        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.getJSONObject(i);

            String name = m.optString("name", "");
            String dosage = m.optString("dosage", "");
            String timing = m.optString("timing", "");
            String duration = m.optString("duration", "");

            list.add(new MedicineModel(name, dosage, timing, duration));
        }

        return list;
    }

    private void showMedicineDialog(List<MedicineModel> medicineList) {

        if (verifyDialog != null && verifyDialog.isShowing()) {
            return;
        }

        verifyDialog = new Dialog(this);

        View view = getLayoutInflater().inflate(R.layout.dialog_medicine_verification, null);
        verifyDialog.setContentView(view);

        if (verifyDialog.getWindow() != null) {
            verifyDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        LinearLayout container = view.findViewById(R.id.containerMedicineList);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        container.removeAllViews();

        Button btnAdd = new Button(this);
        btnAdd.setText("+ Add Medicine");

        if (medicineList != null && !medicineList.isEmpty()) {
            for (MedicineModel m : medicineList) {
                addMedicineCard(container, m);
            }
        } else {
            addMedicineCard(container, new MedicineModel("", "", "", ""));
        }

        container.addView(btnAdd);

        btnAdd.setOnClickListener(v -> {
            int insertIndex = container.getChildCount() - 1;
            if (insertIndex < 0) insertIndex = 0;

            View card = getLayoutInflater().inflate(R.layout.item_medicine_entry, container, false);
            ImageView delete = card.findViewById(R.id.btnDelete);
            delete.setOnClickListener(d -> container.removeView(card));

            container.addView(card, insertIndex);
        });

        btnConfirm.setOnClickListener(v -> {

            Log.d("TEST", "Confirm button clicked");

            List<MedicineModel> finalList = new ArrayList<>();

            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);

                if (child instanceof Button) continue;

                EditText etName = child.findViewById(R.id.etMedicineName);
                EditText etDosage = child.findViewById(R.id.etDosage);
                EditText etTimings = child.findViewById(R.id.etTimings);
                EditText etDuration = child.findViewById(R.id.etDuration);

                if (etName == null) continue;

                String name = etName.getText().toString().trim();
                String dosage = etDosage.getText().toString().trim();
                String timings = etTimings.getText().toString().trim();
                String duration = etDuration.getText().toString().trim();

                if (name.isEmpty()) continue;

                finalList.add(new MedicineModel(name, dosage, timings, duration));
            }

            if (finalList.isEmpty()) {
                Toast.makeText(this, "Please enter at least 1 medicine", Toast.LENGTH_SHORT).show();
                return;
            }

            saveMedicinesToRealtimeDB(finalList, success -> {
                if (success) {
                    Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();

                    if (verifyDialog != null && verifyDialog.isShowing()) {
                        verifyDialog.dismiss();
                    }

                    Intent intent = new Intent(ScanReceiptActivity.this, ElderDashboardActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(this, "Failed to save medicines!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        verifyDialog.show();
    }

    private void addMedicineCard(LinearLayout container, MedicineModel m) {
        View card = getLayoutInflater().inflate(R.layout.item_medicine_entry, container, false);

        EditText etName = card.findViewById(R.id.etMedicineName);
        EditText etDosage = card.findViewById(R.id.etDosage);
        EditText etTimings = card.findViewById(R.id.etTimings);
        EditText etDuration = card.findViewById(R.id.etDuration);
        ImageView delete = card.findViewById(R.id.btnDelete);

        etName.setText(m.getMedicineName());
        etDosage.setText(m.getDosage());
        etTimings.setText(m.getTimings());
        etDuration.setText(m.getDuration());

        delete.setOnClickListener(v -> container.removeView(card));

        container.addView(card, container.getChildCount());
    }

    private void saveMedicinesToRealtimeDB(List<MedicineModel> medicineList, OnSaveCompleteListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w("ScanReceipt", "saveMedicinesToRealtimeDB: currentUser is null");
            listener.onComplete(false);
            return;
        }

        String uid = currentUser.getUid();

        if (uid == null) {
            listener.onComplete(false);
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users")
                .child(uid)
                .child("current_prescriptions");

        rootRef.removeValue().addOnCompleteListener(t -> {

            int total = medicineList.size();
            int[] savedCount = {0};

            for (MedicineModel m : medicineList) {
                DatabaseReference childRef = rootRef.push();
                String id = childRef.getKey();
                m.setId(id);

                Map<String, Object> data = new HashMap<>();
                data.put("id", id);
                data.put("medicineName", m.getMedicineName());
                data.put("dosage", m.getDosage());
                data.put("timings", m.getTimings());
                data.put("duration", m.getDuration());
                data.put("startDate", System.currentTimeMillis());
                data.put("takenToday", false);
                data.put("daysCompleted", 0);
                data.put("totalDays", m.getTotalDays());

                childRef.setValue(data)
                        .addOnSuccessListener(unused -> {
                            savedCount[0]++;

                            // EXISTING alarm logic
                            scheduleAlarmsForMedicine(m);


                            if (savedCount[0] == total) {
                                listener.onComplete(true);
                            }
                        })
                        .addOnFailureListener(e -> listener.onComplete(false));
            }

            if (medicineList.size() == 0) listener.onComplete(true);
        });
    }

    private void scheduleAlarmsForMedicine(MedicineModel m) {
        Log.d("ALARM_DEBUG", "scheduleAlarmsForMedicine() CALLED for " + m.getMedicineName());


        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w("ScanReceipt", "scheduleAlarmsForMedicine: currentUser is null, skipping alarm scheduling");
            return;
        }
        String uid = currentUser.getUid();
        if (uid == null) return;

        DatabaseReference scheduleRef = FirebaseDatabase.getInstance().getReference(
                "users/" + uid + "/medicationSchedule"
        );

        scheduleRef.get().addOnSuccessListener(snapshot -> {
            String tone = snapshot.child("reminderTone").getValue(String.class);
//            scheduleFirstExactAlarm(m, "12:40", tone);

            Log.d("ALARM_DEBUG", "medicationSchedule snapshot = " + snapshot.getValue());

            String morning = snapshot.child("morningTime").getValue(String.class);
            String afternoon = snapshot.child("afternoonTime").getValue(String.class);
            String night = snapshot.child("nightTime").getValue(String.class);


            String pattern = m.getTimings();

            if (pattern != null && pattern.contains("-")) {
                String[] p = pattern.split("-");

                if (p.length >= 3) {
                    if (p[0].equals("1") && morning != null) {
                        scheduleFirstExactAlarm(m, morning, tone);
                        scheduleDailyAlarm(m, morning, tone);
                    }

                    if (p[1].equals("1") && afternoon != null) {
                        scheduleFirstExactAlarm(m, afternoon, tone);
                        scheduleDailyAlarm(m, afternoon, tone);
                    }

                    if (p[2].equals("1") && night != null) {
                        scheduleFirstExactAlarm(m, night, tone);
                        scheduleDailyAlarm(m, night, tone);
                    }

                }

            } else if (pattern != null && !pattern.isEmpty()) {
                scheduleDailyAlarm(m, pattern, tone);
            }

        }).addOnFailureListener(e -> Log.e("ScanReceipt", "Failed reading medicationSchedule", e));
    }

    private void scheduleFirstExactAlarm(MedicineModel m, String hhmm, String tone) {
        try {
            String[] split = hhmm.split(":");
            if (split.length < 2) return;

            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);

            if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("medName", m.getMedicineName());
            intent.putExtra("dosage", m.getDosage());
            intent.putExtra("medicineId", m.getId());
            intent.putExtra("tone", tone);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    (m.getId() + "_first_" + hhmm).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

            if (am != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.getTimeInMillis(),
                        pi
                );
            }

            Log.d("ALARM_DEBUG", "First exact alarm scheduled at " + cal.getTime());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }


    private void scheduleDailyAlarm(MedicineModel m, String hhmm, String tone) {

        try {
            String[] split = hhmm.split(":");
            if (split.length < 2) return;
            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);

            if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("medName", m.getMedicineName());
            intent.putExtra("dosage", m.getDosage());
            intent.putExtra("medicineId", m.getId());
            intent.putExtra("tone", tone);

            int requestCode = (m.getId() + hhmm).hashCode();

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.getTimeInMillis(),
                        pi
                );
            } else {
                am.setExact(
                        AlarmManager.RTC_WAKEUP,
                        cal.getTimeInMillis(),
                        pi
                );
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleMedicineAlarmsSimple(
            MedicineModel m,
            String morning,
            String afternoon,
            String night
    ) {
        String pattern = m.getTimings(); // e.g. 1-0-1
        if (pattern == null) return;

        String[] p = pattern.split("-");
        if (p.length != 3) return;

        if (p[0].equals("1") && morning != null)
            scheduleDailyAlarm(m, morning, null);

        if (p[1].equals("1") && afternoon != null)
            scheduleDailyAlarm(m, afternoon, null);

        if (p[2].equals("1") && night != null)
            scheduleDailyAlarm(m, night, null);
    }





    private long convertToEpoch(String hhmm, int dayOffset) {
        try {
            String[] s = hhmm.split(":");
            int hour = Integer.parseInt(s[0]);
            int minute = Integer.parseInt(s[1]);

            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, dayOffset);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);

            return c.getTimeInMillis();

        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }


    interface OnSaveCompleteListener {
        void onComplete(boolean success);
    }

}
