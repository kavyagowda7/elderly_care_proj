package com.example.elderlycare;

public class AppointmentRequest {
    String doctorUid;
    String patientUid;
    String patientName;
    String date;
    String time;
    String note;

    public AppointmentRequest(String doctorUid, String patientUid, String patientName,
                              String date, String time, String note) {
        this.doctorUid = doctorUid;
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.date = date;
        this.time = time;
        this.note = note;
    }
}
