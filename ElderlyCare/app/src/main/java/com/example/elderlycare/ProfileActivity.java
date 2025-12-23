package com.example.elderlycare;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

public class ProfileActivity extends AppCompatActivity {

    private EditText inputDob, inputAddress, inputCaregiver, inputPhone, inputEmail;
    private TextView userName, btnEdit;
    private ImageView btnBack;
    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users")
                .child(auth.getCurrentUser().getUid())
                .child("patient_info");

        // Initialize UI components
        inputDob = findViewById(R.id.inputDob);
        inputAddress = findViewById(R.id.inputAddress);
        inputCaregiver = findViewById(R.id.inputCaregiver);
        inputPhone = findViewById(R.id.inputPhone);
        inputEmail = findViewById(R.id.inputEmail);
        userName = findViewById(R.id.userName);
        btnEdit = findViewById(R.id.btnEdit);
        btnBack = findViewById(R.id.btnBack);

        // Load existing profile data
        loadUserData();

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Edit/Save toggle
        btnEdit.setOnClickListener(v -> {
            if (isEditing) {
                saveProfileChanges();
            } else {
                enableEditing(true);
            }
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String dob = snapshot.child("dob").getValue(String.class);
                    String city = snapshot.child("city").getValue(String.class);
                    String pincode = snapshot.child("pincode").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = auth.getCurrentUser().getEmail();

                    userName.setText(name != null ? name : "");
                    inputDob.setText(dob != null ? dob : "");
                    inputAddress.setText((city != null ? city : "") + (pincode != null ? ", " + pincode : ""));
                    inputPhone.setText(phone != null ? phone : "");
                    inputEmail.setText(email != null ? email : "");
                    inputCaregiver.setText("Not Assigned"); // Placeholder
                } else {
                    Toast.makeText(ProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableEditing(boolean enable) {
        isEditing = enable;
        inputDob.setEnabled(enable);
        inputAddress.setEnabled(enable);
        inputCaregiver.setEnabled(enable);
        inputPhone.setEnabled(enable);

        btnEdit.setText(enable ? "Save" : "Edit");
    }

    private void saveProfileChanges() {
        String newDob = inputDob.getText().toString().trim();
        String newAddress = inputAddress.getText().toString().trim();
        String newCaregiver = inputCaregiver.getText().toString().trim();
        String newPhone = inputPhone.getText().toString().trim();

        if (newDob.isEmpty() || newAddress.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split city and pincode if available
        String city = "", pincode = "";
        if (newAddress.contains(",")) {
            String[] parts = newAddress.split(",", 2);
            city = parts[0].trim();
            pincode = parts[1].trim();
        } else {
            city = newAddress;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("dob", newDob);
        updatedData.put("city", city);
        updatedData.put("pincode", pincode);
        updatedData.put("phone", newPhone);

        userRef.updateChildren(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    enableEditing(false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
