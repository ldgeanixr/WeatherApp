package com.example.googlemapapi.NetworkRequests;

import com.google.android.gms.maps.model.LatLng;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OpenWeatherClient {

    @GET("/data/2.5/weather")
    Call<WeatherInfo> weatherInfoLatLon(@Query("lat")double lat,
                                        @Query("lon")double lon,
                                        @Query("appid")String appid,
                                        @Query("units") String units);

}
