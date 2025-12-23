package com.example.elderlycare;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IncomingCallActivity extends AppCompatActivity {

    MediaPlayer player;
    String roomId, doctorUid, appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        // Modern lockscreen behaviour
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);

            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);

        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        TextView txtTitle = findViewById(R.id.txtIncomingTitle);
        TextView txtBody  = findViewById(R.id.txtIncomingBody);
        Button btnAccept  = findViewById(R.id.btnAcceptCall);
        Button btnReject  = findViewById(R.id.btnRejectCall);

        roomId = getIntent().getStringExtra("roomId");
        doctorUid = getIntent().getStringExtra("doctorUid");
        appointmentId = getIntent().getStringExtra("appointmentId");

        txtTitle.setText("Incoming Video Call");
        txtBody.setText("Tap to accept");

        playRingtone();

        btnAccept.setOnClickListener(v -> {
            stopRingtone();
            Intent i = new Intent(this, JitsiMeetingActivity.class);
            i.putExtra("room", roomId);
            startActivity(i);
            finish();
        });

        btnReject.setOnClickListener(v -> {
            stopRingtone();
            finish();
        });
    }

    private void playRingtone() {
        try {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ringtone);

            player = new MediaPlayer();
            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Exception ignore) {}
    }

    @Override
    protected void onDestroy() {
        stopRingtone();
        super.onDestroy();
    }
}
