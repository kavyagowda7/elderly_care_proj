package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class DoctorRegistrationActivity extends AppCompatActivity {

    EditText edtFullName, edtAge, edtSpecialist, edtHomeCity, edtHomePincode,
            edtClinicAddress, edtClinicCity, edtClinicPincode;
    Button btnSubmit;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_registration);

        auth = FirebaseAuth.getInstance();

        edtFullName = findViewById(R.id.edt_fullname);
        edtAge = findViewById(R.id.edt_age);
        edtSpecialist = findViewById(R.id.edt_specialist);
        edtHomeCity = findViewById(R.id.edt_home_city);
        edtHomePincode = findViewById(R.id.edt_home_pincode);
        edtClinicAddress = findViewById(R.id.edt_clinic_address);
        edtClinicCity = findViewById(R.id.edt_clinic_city);
        edtClinicPincode = findViewById(R.id.edt_clinic_pincode);

        btnSubmit = findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(v -> saveDoctorData());
    }

    private void saveDoctorData() {

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> doctorData = new HashMap<>();
        doctorData.put("fullName", edtFullName.getText().toString().trim());
        doctorData.put("age", edtAge.getText().toString().trim());
        doctorData.put("specialist", edtSpecialist.getText().toString().trim());
        doctorData.put("homeCity", edtHomeCity.getText().toString().trim());
        doctorData.put("homePincode", edtHomePincode.getText().toString().trim());
        doctorData.put("clinicAddress", edtClinicAddress.getText().toString().trim());
        doctorData.put("clinicCity", edtClinicCity.getText().toString().trim());
        doctorData.put("clinicPincode", edtClinicPincode.getText().toString().trim());

        // DEFAULT phone + rating
        doctorData.put("phone", "");    // empty for now
        doctorData.put("rating", "4.8");

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/");

        // SAVE ROLE
        db.getReference("users").child(uid).child("role").setValue("Doctor");

        // SAVE DOCTOR INFO
        db.getReference("users")
                .child(uid)
                .child("doctor_info")
                .setValue(doctorData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Completed!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, DoctorDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
