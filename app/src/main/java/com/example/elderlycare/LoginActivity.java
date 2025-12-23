package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;


public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, registerButton;
    private TextView forgotPassword, changeRole;
    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private ProgressBar progressBar;
    private boolean isPasswordVisible = false;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        selectedRole = getIntent().getStringExtra("selectedRole");
        if (selectedRole != null) {
            Toast.makeText(this, "Selected Role: " + selectedRole, Toast.LENGTH_SHORT).show();
        }

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        changeRole = findViewById(R.id.changeRole);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        passwordInput.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (passwordInput.getRight() -
                        passwordInput.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility();
                    return true;
                }
            }
            return false;
        });

        loginButton.setOnClickListener(v -> loginUser());

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.putExtra("roletype",selectedRole);
            startActivity(intent);
        });

        forgotPassword.setOnClickListener(v -> resetPassword());

        changeRole.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0);
        } else {
            passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_open, 0);
        }
        passwordInput.setSelection(passwordInput.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");

                        if (task.isSuccessful()) {

                            String uid = auth.getCurrentUser().getUid();

                            userRef.child(uid).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                                @Override
                                public void onSuccess(DataSnapshot dataSnapshot) {

                                    if (dataSnapshot.exists()) {

                                        String role = dataSnapshot.child("role").getValue(String.class);

                                        if (role == null) {
                                            Toast.makeText(LoginActivity.this, "Role missing.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        switch (role) {

                                            case "Elder":

                                                // 🔥 NEW: Use onboardingStatus flags
                                                DatabaseReference onboardingRef = userRef.child(uid).child("onboardingStatus");

                                                onboardingRef.get().addOnSuccessListener(snap -> {

                                                    boolean patient = getFlag(snap, "patientInfoDone");
                                                    boolean intro = getFlag(snap, "medicationIntroDone");
                                                    boolean morning = getFlag(snap, "morningTimeDone");
                                                    boolean afternoon = getFlag(snap, "afternoonTimeDone");
                                                    boolean night = getFlag(snap, "nightTimeDone");
                                                    boolean tone = getFlag(snap, "reminderToneDone");

                                                    if (!patient) {
                                                        startActivity(new Intent(LoginActivity.this, PatientInfoActivity.class));
                                                    }
                                                    else if (!intro) {
                                                        startActivity(new Intent(LoginActivity.this, MedicationIntroActivity.class));
                                                    }
                                                    else if (!morning) {
                                                        startActivity(new Intent(LoginActivity.this, SetMorningTimeActivity.class));
                                                    }
                                                    else if (!afternoon) {
                                                        startActivity(new Intent(LoginActivity.this, SetAfternoonTimeActivity.class));
                                                    }
                                                    else if (!night) {
                                                        startActivity(new Intent(LoginActivity.this, SetNightTimeActivity.class));
                                                    }
                                                    else if (!tone) {
                                                        startActivity(new Intent(LoginActivity.this, ReminderToneActivity.class));
                                                    }
                                                    else {
                                                        startActivity(new Intent(LoginActivity.this, ElderDashboardActivity.class));
                                                    }

                                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                                    finish();
                                                });

                                                break;

                                            case "Doctor":
                                                if (dataSnapshot.child("doctor_info").exists()) {
                                                    startActivity(new Intent(LoginActivity.this, DoctorDashboardActivity.class));
                                                } else {
                                                    startActivity(new Intent(LoginActivity.this, DoctorRegistrationActivity.class));
                                                }
                                                finish();
                                                break;


                                            case "Family":

                                                String familyUid = auth.getCurrentUser().getUid();

                                                DatabaseReference familyAccessRef =
                                                        FirebaseDatabase.getInstance(
                                                                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
                                                        ).getReference("family_access");

                                                familyAccessRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                        boolean linked = false;

                                                        for (DataSnapshot elderSnap : snapshot.getChildren()) {
                                                            if (elderSnap.hasChild(familyUid)) {
                                                                linked = true;
                                                                break;
                                                            }
                                                        }

                                                        if (linked) {
                                                            // ✅ Already linked → dashboard
                                                            startActivity(new Intent(LoginActivity.this, FamilyDashboardActivity.class));
                                                        } else {
                                                            // 🔥 Not linked → ask elder email
                                                            startActivity(new Intent(LoginActivity.this, FamilyLinkActivity.class));
                                                        }

                                                        finish();
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Toast.makeText(LoginActivity.this,
                                                                "Error checking family access",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                                break;


                                            default:
                                                Toast.makeText(LoginActivity.this, "Invalid role.", Toast.LENGTH_SHORT).show();
                                        }

                                    } else {

                                        userRef.child(uid).child("role").get().addOnCompleteListener(roleTask -> {

                                            if (roleTask.isSuccessful()) {
                                                String storedRole = roleTask.getResult().getValue(String.class);

                                                if (storedRole == null) {
                                                    Toast.makeText(LoginActivity.this, "Role not found in database.", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                Toast.makeText(LoginActivity.this, "Logged in as: " + storedRole, Toast.LENGTH_SHORT).show();

                                                Intent intent;
                                                switch (storedRole) {
                                                    case "Elder":
                                                        intent = new Intent(LoginActivity.this, PatientInfoActivity.class);
                                                        break;
                                                    case "Doctor":
                                                        intent = new Intent(LoginActivity.this, DoctorRegistrationActivity.class);
                                                        break;
                                                    case "Family":
                                                        intent = new Intent(LoginActivity.this, FamilyDashboardActivity.class);
                                                        break;
                                                    default:
                                                        Toast.makeText(LoginActivity.this, "Invalid role type.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                }

                                                startActivity(intent);
                                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                                finish();

                                            } else {
                                                Toast.makeText(LoginActivity.this, "Error fetching role: " +
                                                        roleTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            });

                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed: " +
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean getFlag(DataSnapshot snap, String key) {
        Boolean v = snap.child(key).getValue(Boolean.class);
        return v != null && v;
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Password reset link sent to email", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
