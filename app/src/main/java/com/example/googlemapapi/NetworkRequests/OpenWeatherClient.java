package com.example.googlemapapi.NetworkRequests;

import com.example.googlemapapi.NetworkRequests.pojo.WeatherInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherClient {

    @GET("/data/2.5/weather")
    Call<WeatherInfo> weatherInfoLatLon(@Query("lat")double lat,
                                        @Query("lon")double lon,
                                        @Query("appid")String appid,
                                        @Query("units") String units);

}
