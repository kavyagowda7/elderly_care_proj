package com.example.elderlycare;


public class ChatMessage {
    public String senderId;
    public String senderRole; // elder / doctor
    public String message;
    public String type; // text / image / pdf
    public long timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderRole,
                       String message, String type, long timestamp) {
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
    }
}
