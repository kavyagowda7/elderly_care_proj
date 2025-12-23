package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    private LinearLayout doctorRole, elderRole, familyRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection); // keep your same XML file name

        // Initialize Views
        doctorRole = findViewById(R.id.doctorRole);
        elderRole = findViewById(R.id.elderRole);
        familyRole = findViewById(R.id.familyRole);

        // Handle Doctor Role Click
        doctorRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin("Doctor");
            }
        });

        // Handle Elder Role Click
        elderRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin("Elder");
            }
        });

        // Handle Family Role Click
        familyRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin("Family");
            }
        });
    }

    private void navigateToLogin(String role) {
        Toast.makeText(this, "Logging in as " + role, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
        intent.putExtra("selectedRole", role);
        startActivity(intent);
        finish();
    }
}
