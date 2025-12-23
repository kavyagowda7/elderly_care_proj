// File: DoctorDashboardActivity.java
package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;

public class DoctorDashboardActivity extends AppCompatActivity {

    LinearLayout patientListCard, appointmentsCard,TeleCommuincationCard;
    DrawerLayout drawerLayout;
    NavigationView navView;
    ImageView menuIcon;
    TextView headerName, headerSpecialist;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        // SAVE CORRECT FCM TOKEN
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                try {
                    json.put("uid", uid);
                    json.put("token", token);
                } catch (Exception ignored) {}

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(ApiConstants.BASE_URL + "save-token")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call c, IOException e) {}
                    @Override public void onResponse(Call c, Response r) {}
                });
            }
        });

        patientListCard = findViewById(R.id.patientListCard);
        appointmentsCard = findViewById(R.id.appointmentsCard);
        TeleCommuincationCard=findViewById(R.id.TeleCommuincationCard);



        TeleCommuincationCard.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorVideoConsultationActivity.class)));

        patientListCard.setOnClickListener(v ->
                startActivity(new Intent(this, PatientListActivity.class)));

        appointmentsCard.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorAppointmentsActivity.class)));

        drawerLayout = findViewById(R.id.doctorDrawerLayout);
        navView = findViewById(R.id.doctorNavView);
        menuIcon = findViewById(R.id.menuIcon);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        headerName = navView.getHeaderView(0).findViewById(R.id.txtDoctorName);
        headerSpecialist = navView.getHeaderView(0).findViewById(R.id.txtSpecialist);

        loadDoctorData();

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.doc_nav_profile)
                startActivity(new Intent(this, DoctorProfileActivity.class));

            else if (id == R.id.doc_nav_settings)
                startActivity(new Intent(this, DocSettingsActivity.class));

            else if (id == R.id.doc_nav_notifications)
                startActivity(new Intent(this, NotificationsActivity.class));

            else if (id == R.id.doc_nav_logout) {
                auth.signOut();
                Intent intent = new Intent(this, RoleSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void loadDoctorData() {
        String uid = auth.getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users").child(uid).child("doctor_info");

        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String name = snapshot.child("fullName").getValue(String.class);
                String specialist = snapshot.child("specialist").getValue(String.class);

                if (name != null) headerName.setText(name);
                if (specialist != null) headerSpecialist.setText(specialist);
            }
        });
    }
}
