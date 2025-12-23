package com.example.elderlycare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    TextView tvMedName, tvDosage;
    Button btnTaken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        tvMedName = findViewById(R.id.tvMedName);
        tvDosage = findViewById(R.id.tvDosage);
        btnTaken = findViewById(R.id.btnTaken);

        String medicineId = getIntent().getStringExtra("medicineId");
        String medName = getIntent().getStringExtra("medName");
        String dosage = getIntent().getStringExtra("dosage");

        tvMedName.setText(medName);
        tvDosage.setText(dosage);

        btnTaken.setOnClickListener(v -> {
            markMedicineAsTaken(medicineId);
        });
    }

    private void markMedicineAsTaken(String medicineId) {

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference medRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid)
                .child("current_prescriptions")
                .child(medicineId);

        // Today's date key
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // We save taken history
        medRef.child("takenToday").setValue(true);
        medRef.child("lastTaken").setValue(today);
        medRef.child("daysCompleted").get()
                .addOnSuccessListener(snapshot -> {

                    int daysDone = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                    medRef.child("daysCompleted").setValue(daysDone + 1);

                    Toast.makeText(this, "Marked as taken!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
