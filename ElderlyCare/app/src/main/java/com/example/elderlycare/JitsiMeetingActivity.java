package com.example.elderlycare;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.MalformedURLException;
import java.net.URL;

public class JitsiMeetingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String room = getIntent().getStringExtra("room");
        Log.e("ROOM_DEBUG", "Joining JITSI room: " + room);

        try {
            // ⭐ PUBLIC FREE JITSI — NO LOBBY, ALWAYS DIRECT JOIN
            URL serverURL = new URL("https://meet.jit.si");

            JitsiMeetConferenceOptions options =
                    new JitsiMeetConferenceOptions.Builder()
                            .setServerURL(serverURL)
                            .setRoom(room)  // Backend room ID
//                            .setWelcomePageEnabled(false)
                            .setFeatureFlag("lobby.enabled", false)
                            .setFeatureFlag("prejoinpage.enabled", false)
                            .setFeatureFlag("recording.enabled", false)
                            .setFeatureFlag("invite.enabled", false)
                            .build();

            JitsiMeetActivity.launch(this, options);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        finish();
    }
}
