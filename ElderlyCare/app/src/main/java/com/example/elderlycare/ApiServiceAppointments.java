package com.example.elderlycare;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiServiceAppointments {

    @POST("create-appointment")
    Call<ResponseBody> createAppointment(@Body AppointmentRequest req);

    @POST("confirm-appointment")
    Call<ResponseBody> confirmAppointment(@Body ConfirmRequest req);

    @POST("reject-appointment")
    Call<ResponseBody> rejectAppointment(@Body RejectRequest req);

    @GET("locked-slots")
    Call<ResponseBody> getLockedSlots(
            @Query("doctorUid") String doctorUid,
            @Query("date") String date
    );
}
