package com.example.googlemapapi.NetworkRequests.pojo;


import com.google.gson.annotations.SerializedName;

public class WeatherObject {

    @SerializedName("id")
    private int id;
    @SerializedName("main")
    private String main;
    @SerializedName("description")
    private String description;
    @SerializedName("icon")
    private String icon;


    public WeatherObject(int id, String main, String description, String icon) {
        this.setId(id);
        this.setMain(main);
        this.setDescription(description);
        this.setIcon(icon);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return "i_"+icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
