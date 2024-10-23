package com.example.projecta;

public class Restaurant {
    private String name;
    private String imageUrl;
    private String distance;
    private String price;
    private String address;
    private String time;  // Field for time
    private String businessId;
    private double latitude;  // Add a field for latitude
    private double longitude;  // Add a field for longitude
    private String priceTag;
    private boolean isFavorite;

    // Updated constructor to accept latitude and longitude
    public Restaurant(String name, String imageUrl, String distance, String price, String address, String time, String businessId, double latitude, double longitude, String priceTag, boolean isFavorite) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.distance = distance;
        this.price = price;
        this.address = address;
        this.time = time;  // Initialize time
        this.businessId = businessId;
        this.latitude = latitude;  // Initialize latitude
        this.longitude = longitude;  // Initialize longitude
        this.priceTag = priceTag;
        this.isFavorite = isFavorite;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDistance() {
        return distance;
    }

    public String getPriceRange() {
        return price;
    }

    public String getAddress() {
        return address;
    }

    public String getTime() {
        return time;
    }

    public String getBusinessId() {
        return businessId;
    }

    public double getLatitude() {  // Add a getter for latitude
        return latitude;
    }

    public double getLongitude() {  // Add a getter for longitude
        return longitude;
    }

    public String getPriceTag() {
        return priceTag;
    }

    public void setPriceTag(String priceTag) {
        this.priceTag = priceTag;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
