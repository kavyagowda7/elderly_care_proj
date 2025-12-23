package com.example.elderlycare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class FindDoctorsActivity extends AppCompatActivity {

    private static final String TAG = "FD_DEBUG";

    RecyclerView recycler;
    ProgressBar progress;
    TextView tvCount;
    EditText searchInput;

    List<DoctorModel> doctorList = new ArrayList<>();
    DoctorAdapter adapter;

    DatabaseReference usersRef;

    // Location (optional)
    FusedLocationProviderClient fusedLocationClient;
    double userLat = 0, userLng = 0;
    private static final int REQ_LOCATION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_doctors);

        // auth check
//        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
//            Toast.makeText(this, "Not signed in — enable login or anonymous auth", Toast.LENGTH_LONG).show();
//        } else {
//            Log.d(TAG, "Signed in as: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
//        }

        recycler = findViewById(R.id.recyclerDoctors);
        progress = findViewById(R.id.progress);
        tvCount = findViewById(R.id.tvCount);
        searchInput = findViewById(R.id.searchInput);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorAdapter(this, new ArrayList<>());
        recycler.setAdapter(adapter);

        String url = "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app";
        usersRef = FirebaseDatabase.getInstance(url).getReference("users");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getUserLocationThenLoad();
        setupSearch();
    }

    private void getUserLocationThenLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    userLat = loc.getLatitude();
                    userLng = loc.getLongitude();
                }
                loadDoctors();
            }).addOnFailureListener(e -> loadDoctors());

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);

            loadDoctors(); // load even if location is denied
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocationThenLoad();
        }
    }

    private void loadDoctors() {
        progress.setVisibility(View.VISIBLE);
        doctorList.clear();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    progress.setVisibility(View.GONE);
                    tvCount.setText("Found 0 doctors");
                    return;
                }

                for (DataSnapshot user : snapshot.getChildren()) {

                    // 1) role check
                    String role = getStr(user, "role");
                    if (role == null)
                        role = getStr(user.child("doctor_info"), "role");

                    if (role == null || !role.equalsIgnoreCase("Doctor"))
                        continue;

                    DataSnapshot info = user.child("doctor_info");
                    if (!info.exists()) continue;

                    // 2) Read doctor fields
                    String name = getStr(info, "fullName");
                    if (name == null) name = getStr(info, "name");

                    if (name == null || name.isEmpty()) continue;

                    String specialist = getStr(info, "specialist");
                    String phone = getStr(info, "phone");
                    String license = getStr(info, "licenseNumber");
                    String clinicAddress = getStr(info, "clinicAddress");
                    String clinicCity = getStr(info, "clinicCity");

                    // 3) working hours + days
                    String start = null, end = null;
                    Map<String, Boolean> days = new HashMap<>();

                    DataSnapshot settings = user.child("doctorSettings");
                    if (settings.exists()) {
                        DataSnapshot wh = settings.child("workingHours");
                        if (wh.exists()) {
                            start = getStr(wh, "start");
                            end = getStr(wh, "end");
                        }

                        DataSnapshot wd = settings.child("workingDays");
                        if (wd.exists()) {
                            for (DataSnapshot d : wd.getChildren()) {
                                Boolean v = d.getValue(Boolean.class);
                                days.put(d.getKey(), v != null && v);
                            }
                        }
                    }

                    // 4) Build model
                    DoctorModel m = new DoctorModel();
                    m.uid = user.getKey();
                    m.fullName = name;
                    m.specialist = specialist;
                    m.phone = phone != null ? phone : "";
                    m.licenseNumber = license;
                    m.clinicAddress = clinicAddress;
                    m.clinicCity = clinicCity;
                    m.startTime = start;
                    m.endTime = end;
                    m.workingDays = days;

                    doctorList.add(m);
                }

                adapter.updateList(new ArrayList<>(doctorList));
                tvCount.setText("Found " + doctorList.size() + " doctors");
                progress.setVisibility(View.GONE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(FindDoctorsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "DB error", error.toException());
            }
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {

                String q = s.toString().trim().toLowerCase(Locale.ROOT);

                if (q.isEmpty()) {
                    adapter.updateList(new ArrayList<>(doctorList));
                    tvCount.setText("Found " + doctorList.size() + " doctors");
                    return;
                }

                List<DoctorModel> filtered = new ArrayList<>();
                for (DoctorModel m : doctorList) {
                    if ((m.fullName != null && m.fullName.toLowerCase().contains(q)) ||
                            (m.specialist != null && m.specialist.toLowerCase().contains(q)) ||
                            (m.clinicCity != null && m.clinicCity.toLowerCase().contains(q))) {
                        filtered.add(m);
                    }
                }

                adapter.updateList(filtered);
                tvCount.setText("Found " + filtered.size() + " doctors");
            }
        });
    }

    private String getStr(DataSnapshot node, String key) {
        if (node == null) return null;
        if (!node.hasChild(key)) return null;
        return node.child(key).getValue(String.class);
    }
}
