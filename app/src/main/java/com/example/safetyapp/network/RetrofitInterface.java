package com.example.safetyapp.network;




import com.example.safetyapp.model.JWSRequest;
import com.example.safetyapp.model.Response;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RetrofitInterface {

    @POST("verify")
    Call<Response> getResult(@Body JWSRequest request, @Query("key") String apiKey);
}
