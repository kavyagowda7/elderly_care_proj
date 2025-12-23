package com.example.elderlycare;

import android.util.Log;

public class FakeNotificationAPI {

    public static void sendToPatient(String patientUid, String roomId) {
        Log.d("FakeNotification", "Sending video call invite to: " + patientUid + " room=" + roomId);
    }
}
