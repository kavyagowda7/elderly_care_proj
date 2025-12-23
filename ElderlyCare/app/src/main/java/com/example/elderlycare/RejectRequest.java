package com.example.elderlycare;

public class RejectRequest {
    String doctorUid;
    String appointmentId;

    public RejectRequest(String doctorUid, String appointmentId) {
        this.doctorUid = doctorUid;
        this.appointmentId = appointmentId;
    }
}
