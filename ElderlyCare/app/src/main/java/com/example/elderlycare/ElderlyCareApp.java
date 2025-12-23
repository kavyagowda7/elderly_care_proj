package com.example.elderlycare;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class ElderlyCareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
    }
}
