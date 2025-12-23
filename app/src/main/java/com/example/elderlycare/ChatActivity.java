package com.example.elderlycare;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    RecyclerView chatRecycler;
    EditText messageInput;
    ImageButton sendBtn;

    List<ChatMessage> messageList = new ArrayList<>();
    ChatAdapter adapter;

    DatabaseReference chatRef;

    String appointmentId;
    String currentUid;
    String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 🔹 Get data
        appointmentId = getIntent().getStringExtra("appointmentId");
        role = getIntent().getStringExtra("role");
        currentUid = FirebaseAuth.getInstance().getUid();

        // 🔹 Firebase reference
        chatRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(appointmentId)
                .child("messages");

        // 🔹 Views
        chatRecycler = findViewById(R.id.chatRecycler);
        messageInput = findViewById(R.id.messageInput);
        sendBtn = findViewById(R.id.sendBtn);

        adapter = new ChatAdapter(messageList, currentUid);
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatRecycler.setAdapter(adapter);

        listenForMessages();

        sendBtn.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        String msgId = chatRef.push().getKey();

        ChatMessage msg = new ChatMessage(
                currentUid,
                role,
                text,
                "text",
                System.currentTimeMillis()
        );

        chatRef.child(msgId).setValue(msg);
        messageInput.setText("");
    }

    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    ChatMessage msg = s.getValue(ChatMessage.class);
                    if (msg != null) messageList.add(msg);
                }
                adapter.notifyDataSetChanged();
                chatRecycler.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
