package com.example.projecta;

public class FavoriteItem {
    private String name;
    private String imageUrl;

    public FavoriteItem(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
