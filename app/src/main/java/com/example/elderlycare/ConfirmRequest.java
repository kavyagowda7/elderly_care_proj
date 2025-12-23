package com.example.elderlycare;

public class ConfirmRequest {
    String doctorUid;
    String appointmentId;

    public ConfirmRequest(String doctorUid, String appointmentId) {
        this.doctorUid = doctorUid;
        this.appointmentId = appointmentId;
    }
}
