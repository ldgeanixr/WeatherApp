package com.example.googlemapapi.NetworkRequests.pojo;

import com.example.googlemapapi.NetworkRequests.pojo.MainObject;
import com.example.googlemapapi.NetworkRequests.pojo.WeatherObject;
import com.google.gson.annotations.SerializedName;

public class WeatherInfo {

    @SerializedName("main")
    private MainObject main;

    @SerializedName("weather")
    private WeatherObject[] weather;

    public WeatherInfo(MainObject main, WeatherObject weather){
        this.main = main;
        this.weather = new WeatherObject[0];
        this.weather[0] = weather;
    }


    public MainObject getMain() {
        return main;
    }
    public WeatherObject getWeather(){return weather[0];}

    public void setMain(MainObject main) {
        this.main = main;
    }

    public void setWeather(WeatherObject weather) {
        this.weather[0] = weather;
    }
}
