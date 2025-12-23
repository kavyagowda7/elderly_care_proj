// FirebaseUtil.java
package com.example.elderlycare;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtil {
    public static final String DB_URL =
            "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public static FirebaseDatabase db() {
        return FirebaseDatabase.getInstance(DB_URL);
    }
}
