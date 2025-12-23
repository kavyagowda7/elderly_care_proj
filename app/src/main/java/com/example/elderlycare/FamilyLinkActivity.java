package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class FamilyLinkActivity extends AppCompatActivity {

    private EditText elderEmailInput;
    private Button linkButton;

    private DatabaseReference usersRef;
    private DatabaseReference familyAccessRef;

    private String familyUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_link);

        elderEmailInput = findViewById(R.id.elderEmailInput);
        linkButton = findViewById(R.id.linkButton);

        familyUid = FirebaseAuth.getInstance().getUid();

        usersRef = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        familyAccessRef = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("family_access");

        linkButton.setOnClickListener(v -> linkElder());
    }

    private void linkElder() {

        String elderEmail = elderEmailInput.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(elderEmail)) {
            Toast.makeText(this, "Enter elder email", Toast.LENGTH_SHORT).show();
            return;
        }

        linkButton.setEnabled(false);
        linkButton.setText("Linking...");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                boolean found = false;

                for (DataSnapshot snap : snapshot.getChildren()) {

                    String email = snap.child("email").getValue(String.class);
                    String role = snap.child("role").getValue(String.class);

                    if (email != null) email = email.toLowerCase();

                    if (elderEmail.equals(email) && "Elder".equals(role)) {

                        found = true;
                        String elderUid = snap.getKey();

                        familyAccessRef
                                .child(elderUid)
                                .child(familyUid)
                                .setValue(true)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(
                                            FamilyLinkActivity.this,
                                            "Elder linked successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    startActivity(new Intent(
                                            FamilyLinkActivity.this,
                                            FamilyDashboardActivity.class
                                    ));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                FamilyLinkActivity.this,
                                                "Failed to link elder",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );

                        break;
                    }
                }

                if (!found) {
                    Toast.makeText(
                            FamilyLinkActivity.this,
                            "No elder found with this email",
                            Toast.LENGTH_LONG
                    ).show();

                    linkButton.setEnabled(true);
                    linkButton.setText("Link Elder");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        FamilyLinkActivity.this,
                        "Database error",
                        Toast.LENGTH_SHORT
                ).show();

                linkButton.setEnabled(true);
                linkButton.setText("Link Elder");
            }
        });
    }

}
