package com.example.elderlycare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DoctorProfileActivity extends AppCompatActivity {

    EditText edtName, edtSpecialty, edtLicense, edtEmail, edtPhone;
    Button btnUpdate;

    DatabaseReference ref;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users").child(uid).child("doctor_info");

        edtName = findViewById(R.id.edtName);
        edtSpecialty = findViewById(R.id.edtSpecialty);
        edtLicense = findViewById(R.id.edtLicense);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        btnUpdate = findViewById(R.id.btnUpdate);

        loadDoctorInfo();

        btnUpdate.setOnClickListener(v -> updateDoctorInfo());
    }

    private void loadDoctorInfo() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                edtName.setText(snap.child("fullName").getValue(String.class));
                edtSpecialty.setText(snap.child("specialist").getValue(String.class));
                edtLicense.setText(snap.child("licenseNumber").getValue(String.class));
                edtEmail.setText(snap.child("email").getValue(String.class));
                edtPhone.setText(snap.child("phone").getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDoctorInfo() {

        Map<String, Object> map = new HashMap<>();
        map.put("fullName", edtName.getText().toString());
        map.put("specialist", edtSpecialty.getText().toString());
        map.put("licenseNumber", edtLicense.getText().toString());
        map.put("email", edtEmail.getText().toString());
        map.put("phone", edtPhone.getText().toString());

        ref.updateChildren(map)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
