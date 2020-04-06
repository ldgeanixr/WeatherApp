package com.example.googlemapapi.NetworkRequests;

import com.google.gson.annotations.SerializedName;

public class WeatherInfo {

    @SerializedName("main")
    private MainObject main;

    public WeatherInfo(MainObject main){
        this.main = main;
    }


    public MainObject getMain() {
        return main;
    }

    public void setMain(MainObject main) {
        this.main = main;
    }
}
