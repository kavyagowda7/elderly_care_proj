package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class FamilyDashboardActivity extends AppCompatActivity {

    private String elderlyId;

    private TextView elderNameText, analyticsText;

    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_dashboard);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(FamilyDashboardActivity.this, LoginActivity.class);

            // 🔥 Clear back stack (VERY IMPORTANT)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });


        elderNameText = findViewById(R.id.elderNameText);
        analyticsText = findViewById(R.id.analyticsText);

        rootRef = FirebaseDatabase
                .getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        View btnViewReports = findViewById(R.id.btnViewReports);
        btnViewReports.setOnClickListener(v -> {
            if (elderlyId == null) {
                Toast.makeText(this, "Elder not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, DocumentsListActivity.class);
            i.putExtra("elderlyId", elderlyId);
            startActivity(i);
        });

        // 🚨 THIS IS THE ONLY ENTRY POINT
        resolveLinkedElder();
    }

    // 🔥 SINGLE RESPONSIBILITY: FIND ELDER UID
    private void resolveLinkedElder() {

        String familyUid = FirebaseAuth.getInstance().getUid();

        DatabaseReference faRef = FirebaseDatabase
                .getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("family_access");

        faRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot elderSnap : snapshot.getChildren()) {

                    if (elderSnap.hasChild(familyUid)) {

                        elderlyId = elderSnap.getKey();

                        loadElderName();
                        loadAnalytics();
                        return;
                    }
                }

                Toast.makeText(
                        FamilyDashboardActivity.this,
                        "No elder linked",
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        FamilyDashboardActivity.this,
                        "family_access read failed",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }




    // 🔥 ELDER NAME COMES FROM USERS
    private void loadElderName() {

        rootRef.child("users")
                .child(elderlyId)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String name = snapshot.getValue(String.class);
                        elderNameText.setText(
                                name != null ? "Elder: " + name : "Elder name missing"
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // 🔥 SIMPLE, SAFE ANALYTICS
    private void loadAnalytics() {

        rootRef.child("patient_documents")
                .child(elderlyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        analyticsText.setText(
                                "Reports uploaded: " + snapshot.getChildrenCount()
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
