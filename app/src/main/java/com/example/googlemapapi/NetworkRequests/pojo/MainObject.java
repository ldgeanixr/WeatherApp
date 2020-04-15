package com.example.googlemapapi.NetworkRequests.pojo;

import com.google.gson.annotations.SerializedName;

public class MainObject {

    @SerializedName("temp")
    private Double temp;

    public MainObject(Double temp) {
        this.temp = temp;
    }


    public Double getTemp() {
        return temp;
    }

    public void setTemp(Double temp) {
        this.temp = temp;
    }
}
