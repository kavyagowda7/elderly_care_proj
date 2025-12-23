package com.example.elderlycare;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PatientInfoActivity extends AppCompatActivity {

    private EditText nameInput, ageInput, dobInput, cityInput, pincodeInput, phoneInput;
    private Spinner genderSpinner;
    private Button continueButton;
    private FirebaseAuth auth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_info);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // Initialize UI
        nameInput = findViewById(R.id.nameInput);
        ageInput = findViewById(R.id.ageInput);
        dobInput = findViewById(R.id.dobInput);
        cityInput = findViewById(R.id.cityInput);
        pincodeInput = findViewById(R.id.pincodeInput);
        phoneInput = findViewById(R.id.phoneInput); // ✅ Added phone input
        genderSpinner = findViewById(R.id.genderSpinner);
        continueButton = findViewById(R.id.continueButton);

        // Date Picker for DOB
        dobInput.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    PatientInfoActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        dobInput.setText(selectedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        // Continue button
        continueButton.setOnClickListener(v -> savePatientInfo());
    }

    private void savePatientInfo() {
        String name = nameInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String city = cityInput.getText().toString().trim();
        String pincode = pincodeInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || age.isEmpty() || dob.isEmpty() || gender.isEmpty()
                || city.isEmpty() || pincode.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number (optional)
        if (phone.length() < 10) {
            Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> patientData = new HashMap<>();
        patientData.put("name", name);
        patientData.put("age", age);
        patientData.put("dob", dob);
        patientData.put("gender", gender);
        patientData.put("city", city);
        patientData.put("pincode", pincode);
        patientData.put("phone", phone);

        userRef.child(uid).child("patient_info").setValue(patientData)
                .addOnSuccessListener(aVoid -> {

                    FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("users")
                            .child(uid)
                            .child("onboardingStatus").child("patientInfoDone")
                            .setValue(true);

                    Toast.makeText(this, "Details saved successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PatientInfoActivity.this, MedicationIntroActivity.class));

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
