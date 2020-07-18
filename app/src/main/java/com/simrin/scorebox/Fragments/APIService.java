package com.simrin.scorebox.Fragments;

import com.simrin.scorebox.Notifications.MyResponse;
import com.simrin.scorebox.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAEunOrZQ:APA91bHqM0Ags3P1HfWFpDq0PEoyYBz8lOIgazNsCM5wzs4Hp6Msd6jG0jK2lKk94zU89oJ0bQFu4stUzU16_mgVvV5TAnYVqDAY8mzuE-PFt4iC0lJF2gM_DXfY2IaPwgh5k9GpLJ23"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
