package com.platinumacre.realestateapp.models;

public class ModelImageSlider {

    /*---Variables. spellings and case should be same as in firebase db---*/
    String id;
    String imageUrl;

    /*---Empty constructor require for firebase db---*/
    public ModelImageSlider() {

    }

    /*---Constructor with all params---*/
    public ModelImageSlider(String id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    /*---Getter & Setters---*/
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
